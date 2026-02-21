
package com.geeksville.mesh.service

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.core.service.MeshServiceNotifications
import org.meshtastic.proto.TelemetryProtos
import org.meshtastic.proto.telemetry

class SilentNodeDetectorTest {

    private lateinit var nodeManager: MeshNodeManager
    private val notifications: MeshServiceNotifications = mockk(relaxed = true)
    private val commandSender: MeshCommandSender = mockk(relaxed = true)
    private val dataHandler: MeshDataHandler = mockk(relaxed = true)
    private val serviceBroadcasts: MeshServiceBroadcasts = mockk(relaxed = true)
    private lateinit var detector: SilentNodeDetector

    companion object {
        private const val MY_NODE = 100
        private const val NODE_A = 200
        private const val NODE_B = 300
        private const val NODE_C = 400
    }

    @Before
    fun setUp() {
        nodeManager = MeshNodeManager()
        nodeManager.myNodeNum = MY_NODE
        detector = SilentNodeDetector(
            nodeManager,
            notifications,
            commandSender,
            dagger.Lazy { dataHandler },
            serviceBroadcasts,
        )
    }

    @Test
    fun `onPacketReceived creates tracked entry`() {
        detector.onPacketReceived(NODE_A)

        assertTrue(detector.trackedNodes.containsKey(NODE_A))
        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `onPacketReceived updates lastSeenMs`() {
        detector.onPacketReceived(NODE_A)
        val first = detector.trackedNodes[NODE_A]!!.lastSeenMs

        Thread.sleep(50)
        detector.onPacketReceived(NODE_A)
        val second = detector.trackedNodes[NODE_A]!!.lastSeenMs

        assertTrue(second > first)
    }

    @Test
    fun `onPacketReceived ignores own node`() {
        detector.onPacketReceived(MY_NODE)

        assertFalse(detector.trackedNodes.containsKey(MY_NODE))
    }

    @Test
    fun `onPacketReceived snapshots GPS from nodeDB`() {
        
        nodeManager.updateNodeInfo(NODE_A) { entity ->
            entity.latitude = 34.052
            entity.longitude = -118.244
        }

        detector.onPacketReceived(NODE_A)

        val tracked = detector.trackedNodes[NODE_A]!!
        assertEquals(34.052, tracked.lastLatitude, 0.001)
        assertEquals(-118.244, tracked.lastLongitude, 0.001)
    }

    @Test
    fun `onPacketReceived snapshots battery from nodeDB`() {
        nodeManager.updateNodeInfo(NODE_A) { entity ->
            entity.deviceTelemetry = telemetry {
                deviceMetrics = TelemetryProtos.DeviceMetrics.newBuilder()
                    .setBatteryLevel(72)
                    .build()
            }
        }

        detector.onPacketReceived(NODE_A)

        assertEquals(72, detector.trackedNodes[NODE_A]!!.lastBatteryPercent)
    }

    @Test
    fun `node marked SILENT after timeout`() {
        detector.onPacketReceived(NODE_A)

        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node stays ONLINE before timeout`() {
        detector.onPacketReceived(NODE_A)

        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - 10_000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node stays ONLINE if DB lastHeard is fresh`() {
        detector.onPacketReceived(NODE_A)

        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        val nowSecs = (System.currentTimeMillis() / 1000).toInt()
        nodeManager.updateNodeInfo(NODE_A) { entity ->
            entity.lastHeard = nowSecs - 30 
        }

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node with battery below 5 percent is ignored`() {
        detector.onPacketReceived(NODE_A)

        detector.trackedNodes[NODE_A]!!.lastBatteryPercent = 3
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node with battery exactly 5 percent is NOT exempt`() {
        detector.onPacketReceived(NODE_A)

        detector.trackedNodes[NODE_A]!!.lastBatteryPercent = 5
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node with unknown battery is NOT exempt`() {
        detector.onPacketReceived(NODE_A)

        detector.trackedNodes[NODE_A]!!.lastBatteryPercent = -1
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `SILENT promoted to CONFIRMED_SILENT with remote report`() {
        
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()
        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)

        detector.onSilenceReportReceived(
            SilenceReport(
                reporterNodeNum = NODE_B,
                silentNodeNum = NODE_A,
                timestampMs = System.currentTimeMillis(),
            ),
        )

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.CONFIRMED_SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `SILENT stays SILENT without remote report`() {
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `SILENT auto-promotes to CONFIRMED_SILENT after timeout without neighbor reports`() {
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()
        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)

        detector.trackedNodes[NODE_A]!!.silentSinceMs =
            System.currentTimeMillis() - SilentNodeDetector.UNCONFIRMED_PROMOTE_MS - 1000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.CONFIRMED_SILENT, detector.trackedNodes[NODE_A]!!.state)
        verify(exactly = 1) {
            notifications.showSilentNodeNotification(
                nodeNum = NODE_A,
                title = any(),
                message = any(),
            )
        }
    }

    @Test
    fun `notification fires on CONFIRMED_SILENT`() {
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()

        detector.onSilenceReportReceived(
            SilenceReport(NODE_B, NODE_A, System.currentTimeMillis()),
        )
        invokeCheckAllNodes()

        verify(exactly = 1) {
            notifications.showSilentNodeNotification(
                nodeNum = NODE_A,
                title = any(),
                message = any(),
            )
        }
    }

    @Test
    fun `notification fires only once for same node`() {
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()

        detector.onSilenceReportReceived(
            SilenceReport(NODE_B, NODE_A, System.currentTimeMillis()),
        )
        invokeCheckAllNodes()
        invokeCheckAllNodes() 
        invokeCheckAllNodes() 

        verify(exactly = 1) {
            notifications.showSilentNodeNotification(
                nodeNum = NODE_A,
                title = any(),
                message = any(),
            )
        }
    }

    @Test
    fun `node recovers to ONLINE when packet received again`() {
        
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()
        detector.onSilenceReportReceived(
            SilenceReport(NODE_B, NODE_A, System.currentTimeMillis()),
        )
        invokeCheckAllNodes()
        assertEquals(NodePresenceState.CONFIRMED_SILENT, detector.trackedNodes[NODE_A]!!.state)

        detector.onPacketReceived(NODE_A)

        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
        assertFalse(detector.trackedNodes[NODE_A]!!.notificationFired)
    }

    @Test
    fun `tryParseSilenceReport parses valid report`() {
        val result = detector.tryParseSilenceReport(
            "SILENCE_REPORT|$NODE_B|$NODE_A|${System.currentTimeMillis()}",
            NODE_B,
        )

        assertTrue(result)
    }

    @Test
    fun `tryParseSilenceReport rejects non-report text`() {
        assertFalse(detector.tryParseSilenceReport("Hello world", NODE_B))
        assertFalse(detector.tryParseSilenceReport("SILENCE_REPORT|bad", NODE_B))
        assertFalse(detector.tryParseSilenceReport("", NODE_B))
    }

    @Test
    fun `tryParseSilenceReport ignores own reports`() {
        val result = detector.tryParseSilenceReport(
            "SILENCE_REPORT|$MY_NODE|$NODE_A|${System.currentTimeMillis()}",
            MY_NODE,
        )

        assertTrue(result)
    }

    @Test
    fun `duplicate reports from same reporter are deduplicated`() {
        detector.onSilenceReportReceived(
            SilenceReport(NODE_B, NODE_A, System.currentTimeMillis()),
        )
        detector.onSilenceReportReceived(
            SilenceReport(NODE_B, NODE_A, System.currentTimeMillis()),
        )

        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes() 
        invokeCheckAllNodes() 

        assertEquals(NodePresenceState.CONFIRMED_SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `graceful exit prevents silence detection`() {
        detector.onPacketReceived(NODE_A)

        detector.tryParseGracefulExit(
            "MESH_EXIT|$NODE_A|${System.currentTimeMillis()}",
            NODE_A,
        )

        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `tryParseGracefulExit parses valid exit message`() {
        val result = detector.tryParseGracefulExit(
            "MESH_EXIT|$NODE_A|${System.currentTimeMillis()}",
            NODE_A,
        )

        assertTrue(result)
        assertTrue(detector.gracefullyExitedNodes.containsKey(NODE_A))
    }

    @Test
    fun `tryParseGracefulExit rejects non-exit text`() {
        assertFalse(detector.tryParseGracefulExit("Hello world", NODE_A))
        assertFalse(detector.tryParseGracefulExit("MESH_EXIT|bad", NODE_A))
        assertFalse(detector.tryParseGracefulExit("", NODE_A))
    }

    @Test
    fun `tryParseGracefulExit ignores own exit messages`() {
        val result = detector.tryParseGracefulExit(
            "MESH_EXIT|$MY_NODE|${System.currentTimeMillis()}",
            MY_NODE,
        )

        assertTrue(result)
        assertFalse(detector.gracefullyExitedNodes.containsKey(MY_NODE))
    }

    @Test
    fun `graceful exit cleared when node comes back`() {
        
        detector.tryParseGracefulExit(
            "MESH_EXIT|$NODE_A|${System.currentTimeMillis()}",
            NODE_A,
        )
        assertTrue(detector.gracefullyExitedNodes.containsKey(NODE_A))

        detector.onPacketReceived(NODE_A)

        assertFalse(detector.gracefullyExitedNodes.containsKey(NODE_A))
    }

    private fun invokeCheckAllNodes() {
        val method = SilentNodeDetector::class.java.getDeclaredMethod("checkAllNodes")
        method.isAccessible = true
        method.invoke(detector)
    }
}
