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

import co.touchlab.kermit.Logger
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.util.latLongToMeter
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
    private val nodeManager: MeshNodeManager,
    private val serviceNotifications: MeshServiceNotifications,
    private val commandSender: MeshCommandSender,
    private val dataHandler: Lazy<MeshDataHandler>,
    private val serviceBroadcasts: MeshServiceBroadcasts,
) {
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

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
        scanJob = scope.launch { scanLoop() }
        Logger.i { "SilentNodeDetector started" }
    }

    fun stop() {
        scanJob?.cancel()
        scanJob = null
    }

    // ── 1. Record heartbeat (call from MeshDataHandler on every incoming packet) ──

    fun onPacketReceived(fromNodeNum: Int) {
        if (fromNodeNum == nodeManager.myNodeNum) return

        val tracked = trackedNodes.getOrPut(fromNodeNum) { TrackedNode(nodeNum = fromNodeNum) }
        tracked.lastSeenMs = System.currentTimeMillis()

        // Snapshot latest GPS + battery from the node DB
        nodeManager.nodeDBbyNodeNum[fromNodeNum]?.let { entity ->
            tracked.lastLatitude = entity.latitude
            tracked.lastLongitude = entity.longitude
            tracked.lastBatteryPercent = entity.deviceMetrics.batteryLevel
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
        val now = System.currentTimeMillis()

        for ((nodeNum, tracked) in trackedNodes) {
            if (nodeNum == nodeManager.myNodeNum) continue
            // Skip nodes that announced a graceful exit
            if (gracefullyExitedNodes.containsKey(nodeNum)) continue

            // Cross-check with the node database's lastHeard (updated by firmware independently)
            // This catches cases where the node is alive but packets didn't reach our tracker
            if (isNodeFreshInDB(nodeNum, now)) {
                if (tracked.state != NodePresenceState.ONLINE) {
                    Logger.i { "Node $nodeNum still fresh in DB — reverting to ONLINE" }
                    tracked.state = NodePresenceState.ONLINE
                    tracked.silentSinceMs = 0L
                    tracked.notificationFired = false
                    remoteReports.remove(nodeNum)
                }
                tracked.lastSeenMs = now // sync our tracker with DB reality
                continue
            }

            val elapsed = now - tracked.lastSeenMs

            when (tracked.state) {
                NodePresenceState.ONLINE -> {
                    if (elapsed > SILENCE_TIMEOUT_MS) {
                        // Rule 6: ignore if battery was <5% before dropout
                        if (tracked.lastBatteryPercent in 1 until LOW_BATTERY_THRESHOLD) {
                            Logger.d { "Ignoring silence for node $nodeNum (battery was ${tracked.lastBatteryPercent}%)" }
                            continue
                        }
                        tracked.state = NodePresenceState.SILENT
                        tracked.silentSinceMs = now
                        Logger.i { "Node $nodeNum marked SILENT (no heartbeat for ${elapsed}ms)" }

                        // Broadcast a silence report to 1-2 hop neighbors
                        broadcastSilenceReport(nodeNum)
                    }
                }

                NodePresenceState.SILENT -> {
                    val reports = remoteReports[nodeNum]
                    val confirmedByNeighbor = reports != null && reports.isNotEmpty()
                    val silentDuration = now - tracked.silentSinceMs
                    val timedOut = silentDuration >= UNCONFIRMED_PROMOTE_MS

                    if (confirmedByNeighbor || timedOut) {
                        tracked.state = NodePresenceState.CONFIRMED_SILENT
                        if (confirmedByNeighbor) {
                            Logger.i { "Node $nodeNum CONFIRMED SILENT (${reports!!.size} neighbor(s) agree)" }
                        } else {
                            Logger.i { "Node $nodeNum CONFIRMED SILENT (no neighbor response after ${silentDuration / 1000}s)" }
                        }

                        if (!tracked.notificationFired) {
                            fireNotification(tracked)
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
        val gpsStr = if (tracked.lastLatitude != 0.0 || tracked.lastLongitude != 0.0) {
            "%.5f, %.5f".format(tracked.lastLatitude, tracked.lastLongitude)
        } else {
            "Unknown"
        }
        val distStr = computeDistanceString(tracked)

        val alert = buildString {
            append("SILENT NODE DETECTED\n")
            append("Node: $nodeName ($nodeId)\n")
            append("Last seen: $lastSeenStr ago\n")
            append("Last GPS: $gpsStr\n")
            append("Distance: $distStr")
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

    private fun computeDistanceString(tracked: TrackedNode): String {
        val myNode = nodeManager.myNodeNum?.let { nodeManager.nodeDBbyNodeNum[it] } ?: return "Unknown"
        if (myNode.latitude == 0.0 && myNode.longitude == 0.0) return "Unknown"
        if (tracked.lastLatitude == 0.0 && tracked.lastLongitude == 0.0) return "Unknown"

        val meters = latLongToMeter(
            myNode.latitude, myNode.longitude,
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
    private fun isNodeFreshInDB(nodeNum: Int, nowMs: Long): Boolean {
        val entity = nodeManager.nodeDBbyNodeNum[nodeNum] ?: return false
        val lastHeardMs = entity.lastHeard.toLong() * 1000L
        return lastHeardMs > 0 && (nowMs - lastHeardMs) < SILENCE_TIMEOUT_MS
    }

    private fun formatElapsed(ms: Long): String {
        val secs = ms / 1000
        return when {
            secs < 60 -> "${secs}s"
            secs < 3600 -> "${secs / 60}m ${secs % 60}s"
            else -> "${secs / 3600}h ${(secs % 3600) / 60}m"
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
        const val SILENCE_TIMEOUT_MS = 300_000L       // 5 minutes — staggered broadcasts (position/nodeinfo/telemetry) mean a live node sends ~every 5 min
        const val SCAN_INTERVAL_MS = 15_000L          // check every 15 seconds
        const val LOW_BATTERY_THRESHOLD = 5            // ignore if battery < 5%
        const val MAX_BROADCAST_HOPS = 2               // 1-2 hop radius
        const val REPORT_EXPIRY_MS = 300_000L          // 5 minutes
        const val LONGFAST_CHANNEL_INDEX = 0            // LongFast = channel 0
        const val UNCONFIRMED_PROMOTE_MS = 300_000L   // 5 minutes: auto-promote if no neighbor responds
    }
}
