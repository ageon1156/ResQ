/*
 * Copyright (c) 2025-2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.geeksville.mesh.service

import android.annotation.SuppressLint
import android.app.Application
import android.location.LocationManager
import co.touchlab.kermit.Logger
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.meshtastic.core.common.hasLocationPermission
import org.meshtastic.core.data.repository.TriagePinRepository
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.triage.ManualTriagePinPacket
import org.meshtastic.core.model.triage.TriageLevel
import org.meshtastic.core.model.triage.TriagePin
import org.meshtastic.core.model.triage.toDataPacket
import org.meshtastic.core.model.util.latLongToMeter
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.service.MeshServiceNotifications
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.Portnums
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

// ─── Data structures ────────────────────────────────────────────────

enum class NodePresenceState { ONLINE, SILENT, CONFIRMED_SILENT }

data class TrackedNode(
    val nodeNum: Int,
    var lastSeenMs: Long = System.currentTimeMillis(),
    var state: NodePresenceState = NodePresenceState.ONLINE,
    var lastBatteryPercent: Int = -1, // -1 = unknown
    var lastLatitude: Double = 0.0,
    var lastLongitude: Double = 0.0,
    var silentSinceMs: Long = 0L,
    var notificationFired: Boolean = false,
    var missedPings: Int = 0,
    var lastPingMs: Long = 0L,
)

data class SilenceReport(
    val reporterNodeNum: Int,
    val silentNodeNum: Int,
    val timestampMs: Long,
)

// ─── Core detector ──────────────────────────────────────────────────

@Singleton
class SilentNodeDetector
@Inject
constructor(
    private val context: Application,
    private val nodeManager: MeshNodeManager,
    private val serviceNotifications: MeshServiceNotifications,
    private val commandSender: MeshCommandSender,
    private val dataHandler: Lazy<MeshDataHandler>,
    private val serviceBroadcasts: MeshServiceBroadcasts,
    private val triagePinRepository: TriagePinRepository,
    private val connectionStateHolder: ConnectionStateHandler,
) {
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null
    private var pingJob: Job? = null

    /** Per-node heartbeat tracking. Key = nodeNum */
    val trackedNodes = ConcurrentHashMap<Int, TrackedNode>()

    /** Silence reports received from *other* nearby nodes. Key = silentNodeNum */
    private val remoteReports = ConcurrentHashMap<Int, MutableList<SilenceReport>>()

    /** Nodes that announced a graceful exit. Key = nodeNum, Value = exit timestamp */
    val gracefullyExitedNodes = ConcurrentHashMap<Int, Long>()

    // ── Lifecycle ───────────────────────────────────────────────────

    fun start(scope: CoroutineScope) {
        this.scope = scope
        scanJob?.cancel()
        pingJob?.cancel()
        // Always clear stale tracking state on (re)start — timestamps from a
        // previous connection are meaningless and would cause false-positive
        // silence alerts on nodes that were perfectly fine before disconnect.
        trackedNodes.clear()
        remoteReports.clear()
        gracefullyExitedNodes.clear()
        scanJob = scope.launch { scanLoop() }
        pingJob = scope.launch { pingLoop() }
        scope.launch { cleanupStalePinsOnConnect() }
        Logger.i { "SilentNodeDetector started" }
    }

    fun stop() {
        scanJob?.cancel()
        pingJob?.cancel()
        scanJob = null
        pingJob = null
        // Clear all stale tracking state — timestamps from before the disconnect are
        // meaningless. Detection must restart from a clean slate after reconnect.
        trackedNodes.clear()
        remoteReports.clear()
        gracefullyExitedNodes.clear()
    }

    /**
     * Called each time the detector starts (radio reconnect).
     * Removes any lingering silent-node triage pins for nodes that are
     * already heard in the DB — handles the case where the radio was off
     * while the node came back and no packet was received to trigger cleanup.
     */
    private suspend fun cleanupStalePinsOnConnect() {
        // Short delay to let myNodeNum and nodeDB populate after reconnect
        delay(5_000L)
        val myNum = nodeManager.myNodeNum ?: return
        val now = System.currentTimeMillis()
        for ((nodeNum, _) in nodeManager.nodeDBbyNodeNum) {
            if (nodeNum == myNum) continue
            val lastHeardMs = getNodeLastHeardMs(nodeNum)
            if (lastHeardMs > 0 && (now - lastHeardMs) < SILENCE_TIMEOUT_MS) {
                autoRemoveTriagePin(nodeNum)
                // Seed trackedNodes from the firmware's lastHeard so nodes the
                // radio recently heard are given their proper remaining timeout
                // window.  Without this, trackedNodes starts empty after a
                // reconnect and nodes get ping-tested immediately, which can
                // produce false-positive silence alerts if ACKs are slow while
                // the mesh re-stabilises.
                val tracked = trackedNodes.getOrPut(nodeNum) { TrackedNode(nodeNum = nodeNum) }
                if (tracked.lastSeenMs < lastHeardMs) {
                    tracked.lastSeenMs = lastHeardMs
                }
                tracked.state = NodePresenceState.ONLINE
                tracked.missedPings = 0
                tracked.notificationFired = false
            }
        }
        Logger.d { "cleanupStalePinsOnConnect complete" }
    }

    // ── 1. Record heartbeat (call from MeshDataHandler on every incoming packet) ──

    fun onPacketReceived(fromNodeNum: Int) {
        if (fromNodeNum == nodeManager.myNodeNum) return

        val tracked = trackedNodes.getOrPut(fromNodeNum) { TrackedNode(nodeNum = fromNodeNum) }
        tracked.lastSeenMs = System.currentTimeMillis()
        tracked.missedPings = 0

        // Snapshot latest GPS + battery from the node DB
        nodeManager.nodeDBbyNodeNum[fromNodeNum]?.let { entity ->
            tracked.lastBatteryPercent = entity.deviceMetrics.batteryLevel
            if (entity.latitude != 0.0 || entity.longitude != 0.0) {
                tracked.lastLatitude = entity.latitude
                tracked.lastLongitude = entity.longitude
            } else {
                // Node has no GPS — fall back to phone's last known location
                getDeviceLocation()?.let { (lat, lng) ->
                    tracked.lastLatitude = lat
                    tracked.lastLongitude = lng
                }
            }
        }

        // If this node was previously silent or gracefully exited, it's back
        if (tracked.state != NodePresenceState.ONLINE) {
            Logger.i { "Node $fromNodeNum came back online" }
            tracked.state = NodePresenceState.ONLINE
            tracked.silentSinceMs = 0L
            tracked.notificationFired = false
            remoteReports.remove(fromNodeNum)
        }
        gracefullyExitedNodes.remove(fromNodeNum)
        // Always attempt removal — covers the case where trackedNodes was reset
        // (e.g. service restart) while a triage pin was still persisted for this node.
        autoRemoveTriagePin(fromNodeNum)
    }

    // ── 2. Receive silence report from a neighbor node ──────────────

    fun onSilenceReportReceived(report: SilenceReport) {
        val list = remoteReports.getOrPut(report.silentNodeNum) { mutableListOf() }
        // Deduplicate by reporter
        if (list.none { it.reporterNodeNum == report.reporterNodeNum }) {
            list.add(report)
            Logger.d { "Received silence report for ${report.silentNodeNum} from ${report.reporterNodeNum}" }
        }
    }

    // ── 3. Periodic scan loop ───────────────────────────────────────

    private suspend fun scanLoop() {
        while (scope.isActive) {
            delay(SCAN_INTERVAL_MS)
            checkAllNodes()
        }
    }

    private fun checkAllNodes() {
        // Radio is not fully connected — skip silence checks to avoid false positives
        // during DeviceSleep / reconnect gaps.
        if (connectionStateHolder.connectionState.value !is ConnectionState.Connected) return
        if (nodeManager.myNodeNum == null) return

        val now = System.currentTimeMillis()

        // Expire old graceful exit entries so nodes get tracked again
        gracefullyExitedNodes.entries.removeAll { (_, exitTime) ->
            now - exitTime > GRACEFUL_EXIT_EXPIRY_MS
        }

        for ((nodeNum, tracked) in trackedNodes) {
            if (nodeNum == nodeManager.myNodeNum) continue
            // Skip nodes that recently announced a graceful exit (within expiry window)
            if (gracefullyExitedNodes.containsKey(nodeNum)) continue

            // Cross-check with the node database's lastHeard (updated by firmware independently)
            // This catches cases where the node is alive but packets didn't reach our tracker.
            // BUT: if pings are actively failing, don't trust stale DB data — the pings are
            // a more reliable signal than a cached lastHeard timestamp.
            val dbLastHeardMs = getNodeLastHeardMs(nodeNum)
            if (dbLastHeardMs > 0 && (now - dbLastHeardMs) < SILENCE_TIMEOUT_MS
                && tracked.missedPings == 0
            ) {
                if (tracked.state != NodePresenceState.ONLINE) {
                    Logger.i { "Node $nodeNum still fresh in DB — reverting to ONLINE" }
                    tracked.state = NodePresenceState.ONLINE
                    tracked.silentSinceMs = 0L
                    tracked.notificationFired = false
                    remoteReports.remove(nodeNum)
                }
                // Use the actual lastHeard time, not 'now', to avoid resetting the 5-min clock
                tracked.lastSeenMs = dbLastHeardMs
                continue
            }

            val elapsed = now - tracked.lastSeenMs
            val pingsFailed = tracked.missedPings >= MISSED_PINGS_FOR_SILENT

            when (tracked.state) {
                NodePresenceState.ONLINE -> {
                    // Mark SILENT if either: pings failed OR passive timeout exceeded
                    if (pingsFailed || elapsed > SILENCE_TIMEOUT_MS) {
                        // Rule 6: ignore if battery was <5% before dropout
                        if (tracked.lastBatteryPercent in 1 until LOW_BATTERY_THRESHOLD) {
                            Logger.d { "Ignoring silence for node $nodeNum (battery was ${tracked.lastBatteryPercent}%)" }
                            continue
                        }
                        tracked.state = NodePresenceState.SILENT
                        tracked.silentSinceMs = now
                        if (pingsFailed) {
                            Logger.i { "Node $nodeNum marked SILENT (${tracked.missedPings} pings unanswered)" }
                        } else {
                            Logger.i { "Node $nodeNum marked SILENT (no heartbeat for ${elapsed}ms)" }
                        }

                        // Broadcast a silence report to 1-2 hop neighbors
                        broadcastSilenceReport(nodeNum)
                    }
                }

                NodePresenceState.SILENT -> {
                    val reports = remoteReports[nodeNum]
                    // Only count neighbor reports that arrived after a delay from our
                    // own silence detection, to prevent false consensus where two nodes
                    // both flag the same node silent at the same time and confirm each other.
                    val confirmedByNeighbor = reports != null && reports.any {
                        it.timestampMs - tracked.silentSinceMs >= MIN_CONFIRM_DELAY_MS
                    }
                    val silentDuration = now - tracked.silentSinceMs
                    // Auto-promote faster if pings are also failing
                    val promoteTimeout = if (tracked.missedPings >= MISSED_PINGS_FOR_CONFIRMED) {
                        PING_CONFIRMED_PROMOTE_MS
                    } else {
                        UNCONFIRMED_PROMOTE_MS
                    }
                    val timedOut = silentDuration >= promoteTimeout

                    if (confirmedByNeighbor || timedOut) {
                        tracked.state = NodePresenceState.CONFIRMED_SILENT
                        if (confirmedByNeighbor) {
                            Logger.i { "Node $nodeNum CONFIRMED SILENT (${reports!!.size} neighbor(s) agree)" }
                        } else if (tracked.missedPings >= MISSED_PINGS_FOR_CONFIRMED) {
                            Logger.i { "Node $nodeNum CONFIRMED SILENT (${tracked.missedPings} pings unanswered)" }
                        } else {
                            Logger.i { "Node $nodeNum CONFIRMED SILENT (no neighbor response after ${silentDuration / 1000}s)" }
                        }

                        if (!tracked.notificationFired) {
                            fireNotification(tracked)
                            autoPlaceTriagePin(tracked)
                            tracked.notificationFired = true
                        }
                    }
                }

                NodePresenceState.CONFIRMED_SILENT -> {
                    // Already handled; no-op until node comes back
                }
            }
        }

        // Expire stale reports older than 2 minutes
        val expiry = now - REPORT_EXPIRY_MS
        remoteReports.values.forEach { list ->
            list.removeAll { it.timestampMs < expiry }
        }
        remoteReports.entries.removeAll { it.value.isEmpty() }
    }

    // ── 4. Notification ─────────────────────────────────────────────

    private fun fireNotification(tracked: TrackedNode) {
        val entity = nodeManager.nodeDBbyNodeNum[tracked.nodeNum]
        val nodeId = entity?.user?.id ?: DataPacket.nodeNumToDefaultId(tracked.nodeNum)
        val nodeName = entity?.user?.longName ?: nodeId

        val lastSeenStr = formatElapsed(System.currentTimeMillis() - tracked.lastSeenMs)
        val locationStr = if (tracked.lastLatitude != 0.0 || tracked.lastLongitude != 0.0) {
            "https://maps.google.com/maps?q=%.5f,%.5f".format(tracked.lastLatitude, tracked.lastLongitude)
        } else {
            "Location: Unknown"
        }

        val alert = buildString {
            append("SILENT NODE DETECTED\n")
            append("Node: $nodeName ($nodeId)\n")
            append("Last seen: $lastSeenStr ago\n")
            append(locationStr)
        }

        Logger.w { alert }

        // Local Android notification
        serviceNotifications.showSilentNodeNotification(
            nodeNum = tracked.nodeNum,
            title = "Silent Node: $nodeName",
            message = alert,
        )

        // Broadcast to LongFast channel (channel 0) so everyone on the mesh sees it
        broadcastAlertToChannel(alert)
    }

    private fun broadcastAlertToChannel(alert: String) {
        val myNodeNum = nodeManager.myNodeNum ?: return
        val packet = DataPacket(
            to = DataPacket.ID_BROADCAST,
            bytes = alert.toByteArray(Charsets.UTF_8),
            dataType = Portnums.PortNum.TEXT_MESSAGE_APP_VALUE,
            channel = LONGFAST_CHANNEL_INDEX,
            hopLimit = MAX_BROADCAST_HOPS,
            wantAck = false,
        )

        try {
            commandSender.sendData(packet)
            serviceBroadcasts.broadcastMessageStatus(packet)
            dataHandler.get().rememberDataPacket(packet, myNodeNum, false)
            Logger.d { "Broadcast silent-node alert to LongFast channel" }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(e) { "Failed to broadcast silent-node alert to channel" }
        }
    }

    // ── 4c. Auto-triage-pin for silent nodes ──────────────────────────

    /**
     * Places a RED triage pin at the node's last known location and broadcasts it
     * on LongFast so every device on the mesh receives and saves it.
     * The pin uses a deterministic ID ("silent-<nodeNum>") so any device can delete
     * it by ID when the node comes back.
     */
    private fun autoPlaceTriagePin(tracked: TrackedNode) {
        if (tracked.lastLatitude == 0.0 && tracked.lastLongitude == 0.0) {
            Logger.d { "autoPlaceTriagePin: skipping node ${tracked.nodeNum} — no location" }
            return
        }

        val myNodeNum = nodeManager.myNodeNum ?: return
        val myNodeId = nodeManager.nodeDBbyNodeNum[myNodeNum]?.user?.id
            ?: DataPacket.nodeNumToDefaultId(myNodeNum)
        val pinId = "silent-${tracked.nodeNum}"
        val now = System.currentTimeMillis()

        val pin = TriagePin(
            pinId = pinId,
            lat = tracked.lastLatitude,
            lon = tracked.lastLongitude,
            triageLevel = TriageLevel.RED,
            createdBy = myNodeId,
            timestamp = now,
            isSilentNodeConversion = true,
            sourceNodeNum = tracked.nodeNum,
        )

        // Save locally
        scope.launch { triagePinRepository.upsertPin(pin) }

        // Broadcast to LongFast so all devices receive it via handlePrivateApp
        val packet = ManualTriagePinPacket(
            pinId = pinId,
            lat = tracked.lastLatitude,
            lon = tracked.lastLongitude,
            triageLevel = TriageLevel.RED.name,
            createdBy = myNodeId,
            timestamp = now,
        ).toDataPacket(channel = LONGFAST_CHANNEL_INDEX)

        try {
            commandSender.sendData(packet)
            Logger.i { "Auto-placed RED triage pin $pinId for silent node ${tracked.nodeNum}" }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(e) { "Failed to broadcast auto-triage pin for node ${tracked.nodeNum}" }
        }
    }

    /**
     * Removes the auto-placed triage pin for [nodeNum] when the node comes back online.
     * Safe to call even if no pin exists (deletePin is a no-op on missing rows).
     */
    private fun autoRemoveTriagePin(nodeNum: Int) {
        scope.launch {
            triagePinRepository.deletePin("silent-$nodeNum")
            Logger.i { "Auto-removed triage pin for recovered node $nodeNum" }
        }
    }

    private fun computeDistanceString(tracked: TrackedNode): String {
        // Get our position: try node DB first, then phone GPS
        val myNode = nodeManager.myNodeNum?.let { nodeManager.nodeDBbyNodeNum[it] }
        val (myLat, myLng) = if (myNode != null && (myNode.latitude != 0.0 || myNode.longitude != 0.0)) {
            myNode.latitude to myNode.longitude
        } else {
            getDeviceLocation() ?: return "Unknown"
        }

        if (tracked.lastLatitude == 0.0 && tracked.lastLongitude == 0.0) return "Unknown"

        val meters = latLongToMeter(
            myLat, myLng,
            tracked.lastLatitude, tracked.lastLongitude,
        ).toInt()

        return when {
            meters < 1000 -> "${meters}m"
            else -> "%.1fkm".format(meters / 1000.0)
        }
    }

    /**
     * Checks if the node database has a recent lastHeard for this node.
     * lastHeard is in seconds since epoch, updated by the firmware independently of our tracker.
     */
    private fun getNodeLastHeardMs(nodeNum: Int): Long {
        val entity = nodeManager.nodeDBbyNodeNum[nodeNum] ?: return 0L
        return entity.lastHeard.toLong() * 1000L
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(): Pair<Double, Double>? {
        if (!context.hasLocationPermission()) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val location = lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: return null
        return Pair(location.latitude, location.longitude)
    }

    private fun formatElapsed(ms: Long): String {
        val secs = ms / 1000
        return when {
            secs < 60 -> "${secs}s"
            secs < 3600 -> "${secs / 60}m ${secs % 60}s"
            else -> "${secs / 3600}h ${(secs % 3600) / 60}m"
        }
    }

    // ── 4b. Active heartbeat ping loop ────────────────────────────────

    private suspend fun pingLoop() {
        delay(PING_INTERVAL_MS) // initial delay to let nodes populate
        while (scope.isActive) {
            pingAllTrackedNodes()
            delay(PING_INTERVAL_MS)
        }
    }

    private fun pingAllTrackedNodes() {
        // Radio is not fully connected — skip pinging entirely so that a DeviceSleep
        // blip or reconnect gap does NOT increment missedPings and trigger false alerts.
        if (connectionStateHolder.connectionState.value !is ConnectionState.Connected) return
        if (nodeManager.myNodeNum == null) return

        val now = System.currentTimeMillis()
        for ((nodeNum, tracked) in trackedNodes) {
            if (nodeNum == nodeManager.myNodeNum) continue
            if (gracefullyExitedNodes.containsKey(nodeNum)) continue

            // Only ping if enough time passed since last ping
            if (now - tracked.lastPingMs < PING_INTERVAL_MS) continue

            // If we haven't heard from this node recently, send a ping
            val timeSinceLastSeen = now - tracked.lastSeenMs
            if (timeSinceLastSeen > PING_AFTER_QUIET_MS) {
                sendPing(nodeNum)
                tracked.lastPingMs = now
                tracked.missedPings++
                Logger.d { "Ping sent to node $nodeNum (missed=${tracked.missedPings})" }
            }
        }
    }

    private fun sendPing(nodeNum: Int) {
        val nodeId = DataPacket.nodeNumToDefaultId(nodeNum)
        val payload = "HEARTBEAT_PING|${System.currentTimeMillis()}"

        val packet = DataPacket(
            to = nodeId,
            bytes = payload.toByteArray(Charsets.UTF_8),
            dataType = Portnums.PortNum.PRIVATE_APP_VALUE,
            hopLimit = MAX_BROADCAST_HOPS,
            wantAck = true, // firmware auto-replies with ACK, which triggers onPacketReceived
            priority = MeshProtos.MeshPacket.Priority.BACKGROUND_VALUE,
        )

        try {
            commandSender.sendData(packet)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(e) { "Failed to send heartbeat ping to node $nodeNum" }
        }
    }

    // ── 5. Hop-limited silence report broadcast (1-2 hops only) ─────

    private fun broadcastSilenceReport(silentNodeNum: Int) {
        val myNum = nodeManager.myNodeNum ?: return

        // Encode: "SILENCE_REPORT|<reporterNodeNum>|<silentNodeNum>|<timestampMs>"
        val payload = "SILENCE_REPORT|$myNum|$silentNodeNum|${System.currentTimeMillis()}"

        val packet = DataPacket(
            to = DataPacket.ID_BROADCAST,
            bytes = payload.toByteArray(Charsets.UTF_8),
            dataType = Portnums.PortNum.PRIVATE_APP_VALUE,
            hopLimit = MAX_BROADCAST_HOPS, // 2 hops: immediate neighbors only
            wantAck = false,
            priority = MeshProtos.MeshPacket.Priority.BACKGROUND_VALUE,
        )

        try {
            commandSender.sendData(packet)
            Logger.d { "Broadcast silence report for node $silentNodeNum (hop limit=$MAX_BROADCAST_HOPS)" }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(e) { "Failed to broadcast silence report" }
        }
    }

    // ── 6. Parse incoming silence reports from other nodes ──────────

    fun tryParseSilenceReport(text: String, fromNodeNum: Int): Boolean {
        if (!text.startsWith("SILENCE_REPORT|")) return false
        val parts = text.split("|")
        if (parts.size != 4) return false

        val reporterNum = parts[1].toIntOrNull() ?: return false
        val silentNum = parts[2].toIntOrNull() ?: return false
        val timestamp = parts[3].toLongOrNull() ?: return false

        // Don't process our own reports
        if (reporterNum == nodeManager.myNodeNum) return true

        onSilenceReportReceived(
            SilenceReport(
                reporterNodeNum = reporterNum,
                silentNodeNum = silentNum,
                timestampMs = timestamp,
            ),
        )
        return true
    }

    // ── 6b. Parse firmware heartbeat beacons ─────────────────────────

    fun tryParseHeartbeat(text: String, fromNodeNum: Int): Boolean {
        if (!text.startsWith("HB|")) return false
        // Firmware heartbeat: "HB|<nodeNumHex>|<uptimeSecs>"
        // Receiving this means the node is alive — update tracker
        onPacketReceived(fromNodeNum)
        Logger.d { "Firmware heartbeat from node $fromNodeNum" }
        return true
    }

    // ── 7. Graceful exit — announce intentional departure ───────────

    fun broadcastGracefulExit() {
        val myNum = nodeManager.myNodeNum ?: return
        val payload = "MESH_EXIT|$myNum|${System.currentTimeMillis()}"

        val packet = DataPacket(
            to = DataPacket.ID_BROADCAST,
            bytes = payload.toByteArray(Charsets.UTF_8),
            dataType = Portnums.PortNum.PRIVATE_APP_VALUE,
            hopLimit = MAX_BROADCAST_HOPS,
            wantAck = false,
        )

        try {
            commandSender.sendData(packet)
            Logger.i { "Broadcast graceful exit for node $myNum" }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(e) { "Failed to broadcast graceful exit" }
        }
    }

    fun tryParseGracefulExit(text: String, fromNodeNum: Int): Boolean {
        if (!text.startsWith("MESH_EXIT|")) return false
        val parts = text.split("|")
        if (parts.size != 3) return false

        val exitingNodeNum = parts[1].toIntOrNull() ?: return false
        val timestamp = parts[2].toLongOrNull() ?: return false

        // Don't process our own exit messages
        if (exitingNodeNum == nodeManager.myNodeNum) return true

        gracefullyExitedNodes[exitingNodeNum] = timestamp
        Logger.i { "Node $exitingNodeNum announced graceful exit" }
        return true
    }

    companion object {
        // ── Passive detection (fallback if pings can't reach node) ──
        const val SILENCE_TIMEOUT_MS = 900_000L       // 15 min — passive fallback for nodes out of ping range
        const val SCAN_INTERVAL_MS = 15_000L          // check every 15 seconds
        const val UNCONFIRMED_PROMOTE_MS = 600_000L   // 10 min: auto-promote if no neighbor responds (passive path)

        // ── Active heartbeat ping ──
        const val PING_INTERVAL_MS = 120_000L         // ping every 2 minutes
        const val PING_AFTER_QUIET_MS = 120_000L      // start pinging after 2 min of no packets
        const val MISSED_PINGS_FOR_SILENT = 2          // 2 missed pings (~4 min) → SILENT
        const val MISSED_PINGS_FOR_CONFIRMED = 3       // 3 missed pings (~6 min) → fast-track to CONFIRMED
        const val PING_CONFIRMED_PROMOTE_MS = 60_000L // 1 min after SILENT if pings also failing → CONFIRMED

        // ── Shared ──
        const val LOW_BATTERY_THRESHOLD = 5            // ignore if battery < 5%
        const val MAX_BROADCAST_HOPS = 2               // 1-2 hop radius
        const val REPORT_EXPIRY_MS = 900_000L         // 15 min
        const val LONGFAST_CHANNEL_INDEX = 0            // LongFast = channel 0
        const val MIN_CONFIRM_DELAY_MS = 300_000L     // 5 min: prevents false consensus
        const val GRACEFUL_EXIT_EXPIRY_MS = 600_000L  // 10 min: after this, track the node again
    }
}
