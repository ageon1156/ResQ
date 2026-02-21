
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

enum class NodePresenceState { ONLINE, SILENT, CONFIRMED_SILENT }

class TrackedNode(
    val nodeNum: Int,
    var lastSeenMs: Long = System.currentTimeMillis(),
    var state: NodePresenceState = NodePresenceState.ONLINE,
    var lastBatteryPercent: Int = -1, 
    var lastLatitude: Double = 0.0,
    var lastLongitude: Double = 0.0,
    var silentSinceMs: Long = 0L,
    val notificationFired: AtomicBoolean = AtomicBoolean(false),
    var missedPings: Int = 0,
    var lastPingMs: Long = 0L,
)

data class SilenceReport(
    val reporterNodeNum: Int,
    val silentNodeNum: Int,
    val timestampMs: Long,
)

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

    val trackedNodes = ConcurrentHashMap<Int, TrackedNode>()

    private val remoteReports = ConcurrentHashMap<Int, MutableList<SilenceReport>>()

    val gracefullyExitedNodes = ConcurrentHashMap<Int, Long>()

    fun start(scope: CoroutineScope) {
        this.scope = scope
        scanJob?.cancel()
        pingJob?.cancel()

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

        trackedNodes.clear()
        remoteReports.clear()
        gracefullyExitedNodes.clear()
    }

    private suspend fun cleanupStalePinsOnConnect() {
        
        delay(5_000L)
        val myNum = nodeManager.myNodeNum ?: return
        val now = System.currentTimeMillis()
        for ((nodeNum, _) in nodeManager.nodeDBbyNodeNum) {
            if (nodeNum == myNum) continue
            val lastHeardMs = getNodeLastHeardMs(nodeNum)
            if (lastHeardMs > 0 && (now - lastHeardMs) < SILENCE_TIMEOUT_MS) {
                autoRemoveTriagePin(nodeNum)

                val tracked = trackedNodes.getOrPut(nodeNum) { TrackedNode(nodeNum = nodeNum) }
                if (tracked.lastSeenMs < lastHeardMs) {
                    tracked.lastSeenMs = lastHeardMs
                }
                tracked.state = NodePresenceState.ONLINE
                tracked.missedPings = 0
                tracked.notificationFired.set(false)
            }
        }
        Logger.d { "cleanupStalePinsOnConnect complete" }
    }

    fun onPacketReceived(fromNodeNum: Int) {
        if (fromNodeNum == nodeManager.myNodeNum) return

        val tracked = trackedNodes.getOrPut(fromNodeNum) { TrackedNode(nodeNum = fromNodeNum) }
        tracked.lastSeenMs = System.currentTimeMillis()
        tracked.missedPings = 0

        nodeManager.nodeDBbyNodeNum[fromNodeNum]?.let { entity ->
            tracked.lastBatteryPercent = entity.deviceMetrics.batteryLevel
            if (entity.latitude != 0.0 || entity.longitude != 0.0) {
                tracked.lastLatitude = entity.latitude
                tracked.lastLongitude = entity.longitude
            } else {
                
                getDeviceLocation()?.let { (lat, lng) ->
                    tracked.lastLatitude = lat
                    tracked.lastLongitude = lng
                }
            }
        }

        if (tracked.state != NodePresenceState.ONLINE) {
            Logger.i { "Node $fromNodeNum came back online" }
            tracked.state = NodePresenceState.ONLINE
            tracked.silentSinceMs = 0L
            tracked.notificationFired.set(false)
            remoteReports.remove(fromNodeNum)
        }
        gracefullyExitedNodes.remove(fromNodeNum)

        autoRemoveTriagePin(fromNodeNum)
    }

    fun onSilenceReportReceived(report: SilenceReport) {
        val list = remoteReports.getOrPut(report.silentNodeNum) { mutableListOf() }
        
        if (list.none { it.reporterNodeNum == report.reporterNodeNum }) {
            list.add(report)
            Logger.d { "Received silence report for ${report.silentNodeNum} from ${report.reporterNodeNum}" }
        }
    }

    private suspend fun scanLoop() {
        while (scope.isActive) {
            delay(SCAN_INTERVAL_MS)
            checkAllNodes()
        }
    }

    private fun checkAllNodes() {

        if (connectionStateHolder.connectionState.value !is ConnectionState.Connected) return
        if (nodeManager.myNodeNum == null) return

        val now = System.currentTimeMillis()

        gracefullyExitedNodes.entries.removeAll { (_, exitTime) ->
            now - exitTime > GRACEFUL_EXIT_EXPIRY_MS
        }

        for ((nodeNum, tracked) in trackedNodes) {
            if (nodeNum == nodeManager.myNodeNum) continue
            
            if (gracefullyExitedNodes.containsKey(nodeNum)) continue

            val dbLastHeardMs = getNodeLastHeardMs(nodeNum)
            if (dbLastHeardMs > 0 && (now - dbLastHeardMs) < SILENCE_TIMEOUT_MS
                && tracked.missedPings == 0
            ) {
                if (tracked.state != NodePresenceState.ONLINE) {
                    Logger.i { "Node $nodeNum still fresh in DB — reverting to ONLINE" }
                    tracked.state = NodePresenceState.ONLINE
                    tracked.silentSinceMs = 0L
                    tracked.notificationFired.set(false)
                    remoteReports.remove(nodeNum)
                }
                
                tracked.lastSeenMs = dbLastHeardMs
                continue
            }

            val elapsed = now - tracked.lastSeenMs
            val pingsFailed = tracked.missedPings >= MISSED_PINGS_FOR_SILENT

            when (tracked.state) {
                NodePresenceState.ONLINE -> {
                    
                    if (pingsFailed || elapsed > SILENCE_TIMEOUT_MS) {
                        
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

                        broadcastSilenceReport(nodeNum)
                    }
                }

                NodePresenceState.SILENT -> {
                    val reports = remoteReports[nodeNum]

                    val confirmedByNeighbor = reports != null && reports.any {
                        it.timestampMs - tracked.silentSinceMs >= MIN_CONFIRM_DELAY_MS
                    }
                    val silentDuration = now - tracked.silentSinceMs
                    
                    val promoteTimeout = if (tracked.missedPings >= MISSED_PINGS_FOR_CONFIRMED) {
                        PING_CONFIRMED_PROMOTE_MS
                    } else {
                        UNCONFIRMED_PROMOTE_MS
                    }
                    val timedOut = silentDuration >= promoteTimeout

                    if (confirmedByNeighbor || timedOut) {
                        tracked.state = NodePresenceState.CONFIRMED_SILENT
                        if (confirmedByNeighbor) {
                            Logger.i { "Node $nodeNum CONFIRMED SILENT (${reports?.size ?: 0} neighbor(s) agree)" }
                        } else if (tracked.missedPings >= MISSED_PINGS_FOR_CONFIRMED) {
                            Logger.i { "Node $nodeNum CONFIRMED SILENT (${tracked.missedPings} pings unanswered)" }
                        } else {
                            Logger.i { "Node $nodeNum CONFIRMED SILENT (no neighbor response after ${silentDuration / 1000}s)" }
                        }

                        if (tracked.notificationFired.compareAndSet(false, true)) {
                            fireNotification(tracked)
                            autoPlaceTriagePin(tracked)
                        }
                    }
                }

                NodePresenceState.CONFIRMED_SILENT -> {
                    
                }
            }
        }

        val expiry = now - REPORT_EXPIRY_MS
        remoteReports.values.forEach { list ->
            list.removeAll { it.timestampMs < expiry }
        }
        remoteReports.entries.removeAll { it.value.isEmpty() }
    }

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

        serviceNotifications.showSilentNodeNotification(
            nodeNum = tracked.nodeNum,
            title = "Silent Node: $nodeName",
            message = alert,
        )

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

        scope.launch { triagePinRepository.upsertPin(pin) }

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

    private fun autoRemoveTriagePin(nodeNum: Int) {
        scope.launch {
            triagePinRepository.deletePin("silent-$nodeNum")
            Logger.i { "Auto-removed triage pin for recovered node $nodeNum" }
        }
    }

    private fun computeDistanceString(tracked: TrackedNode): String {
        
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

    private suspend fun pingLoop() {
        delay(PING_INTERVAL_MS) 
        while (scope.isActive) {
            pingAllTrackedNodes()
            delay(PING_INTERVAL_MS)
        }
    }

    private fun pingAllTrackedNodes() {

        if (connectionStateHolder.connectionState.value !is ConnectionState.Connected) return
        if (nodeManager.myNodeNum == null) return

        val now = System.currentTimeMillis()
        for ((nodeNum, tracked) in trackedNodes) {
            if (nodeNum == nodeManager.myNodeNum) continue
            if (gracefullyExitedNodes.containsKey(nodeNum)) continue

            if (now - tracked.lastPingMs < PING_INTERVAL_MS) continue

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
            wantAck = true, 
            priority = MeshProtos.MeshPacket.Priority.BACKGROUND_VALUE,
        )

        try {
            commandSender.sendData(packet)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(e) { "Failed to send heartbeat ping to node $nodeNum" }
        }
    }

    private fun broadcastSilenceReport(silentNodeNum: Int) {
        val myNum = nodeManager.myNodeNum ?: return

        val payload = "SILENCE_REPORT|$myNum|$silentNodeNum|${System.currentTimeMillis()}"

        val packet = DataPacket(
            to = DataPacket.ID_BROADCAST,
            bytes = payload.toByteArray(Charsets.UTF_8),
            dataType = Portnums.PortNum.PRIVATE_APP_VALUE,
            hopLimit = MAX_BROADCAST_HOPS, 
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

    fun tryParseSilenceReport(text: String, fromNodeNum: Int): Boolean {
        if (!text.startsWith("SILENCE_REPORT|")) return false
        val parts = text.split("|")
        if (parts.size != 4) return false

        val reporterNum = parts[1].toIntOrNull() ?: return false
        val silentNum = parts[2].toIntOrNull() ?: return false
        val timestamp = parts[3].toLongOrNull() ?: return false

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

    fun tryParseHeartbeat(text: String, fromNodeNum: Int): Boolean {
        if (!text.startsWith("HB|")) return false

        onPacketReceived(fromNodeNum)
        Logger.d { "Firmware heartbeat from node $fromNodeNum" }
        return true
    }

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

        if (exitingNodeNum == nodeManager.myNodeNum) return true

        gracefullyExitedNodes[exitingNodeNum] = timestamp
        Logger.i { "Node $exitingNodeNum announced graceful exit" }
        return true
    }

    companion object {
        
        const val SILENCE_TIMEOUT_MS = 900_000L       
        const val SCAN_INTERVAL_MS = 15_000L          
        const val UNCONFIRMED_PROMOTE_MS = 600_000L   

        const val PING_INTERVAL_MS = 120_000L         
        const val PING_AFTER_QUIET_MS = 120_000L      
        const val MISSED_PINGS_FOR_SILENT = 2          
        const val MISSED_PINGS_FOR_CONFIRMED = 3       
        const val PING_CONFIRMED_PROMOTE_MS = 60_000L 

        const val LOW_BATTERY_THRESHOLD = 5            
        const val MAX_BROADCAST_HOPS = 2               
        const val REPORT_EXPIRY_MS = 900_000L         
        const val LONGFAST_CHANNEL_INDEX = 0            
        const val MIN_CONFIRM_DELAY_MS = 300_000L     
        const val GRACEFUL_EXIT_EXPIRY_MS = 600_000L  
    }
}
