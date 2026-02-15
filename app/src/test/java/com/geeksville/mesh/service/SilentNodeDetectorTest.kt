/*
 * Copyright (c) 2026 Meshtastic LLC
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

    // ── 1. Heartbeat tracking ───────────────────────────────────────

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
        // Pre-populate nodeDB with position data
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

    // ── 2. Silence detection ────────────────────────────────────────

    @Test
    fun `node marked SILENT after timeout`() {
        detector.onPacketReceived(NODE_A)

        // Simulate time passing beyond threshold
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        // Trigger the check manually (same logic as scanLoop)
        invokeCheckAllNodes()

        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node stays ONLINE before timeout`() {
        detector.onPacketReceived(NODE_A)

        // Only 10 seconds ago — under the 20s threshold
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - 10_000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node stays ONLINE if DB lastHeard is fresh`() {
        detector.onPacketReceived(NODE_A)

        // Our tracker thinks it's been too long
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        // But the node DB says it was heard recently (firmware updated lastHeard)
        val nowSecs = (System.currentTimeMillis() / 1000).toInt()
        nodeManager.updateNodeInfo(NODE_A) { entity ->
            entity.lastHeard = nowSecs - 30 // 30 seconds ago — well within timeout
        }

        invokeCheckAllNodes()

        // Should stay ONLINE because DB says node is still fresh
        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
    }

    // ── 3. Low battery exemption (Rule 6) ───────────────────────────

    @Test
    fun `node with battery below 5 percent is ignored`() {
        detector.onPacketReceived(NODE_A)

        // Set battery to 3% before timeout
        detector.trackedNodes[NODE_A]!!.lastBatteryPercent = 3
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        // Should stay ONLINE — low battery exemption
        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node with battery exactly 5 percent is NOT exempt`() {
        detector.onPacketReceived(NODE_A)

        detector.trackedNodes[NODE_A]!!.lastBatteryPercent = 5
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        // 5% is NOT below threshold, so it SHOULD be marked SILENT
        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `node with unknown battery is NOT exempt`() {
        detector.onPacketReceived(NODE_A)

        // -1 means unknown battery
        detector.trackedNodes[NODE_A]!!.lastBatteryPercent = -1
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    // ── 4. Neighbor confirmation ────────────────────────────────────

    @Test
    fun `SILENT promoted to CONFIRMED_SILENT with remote report`() {
        // Get NODE_A into SILENT state
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()
        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)

        // Another node reports NODE_A as silent
        detector.onSilenceReportReceived(
            SilenceReport(
                reporterNodeNum = NODE_B,
                silentNodeNum = NODE_A,
                timestampMs = System.currentTimeMillis(),
            ),
        )

        // Run check again — should now confirm
        invokeCheckAllNodes()

        assertEquals(NodePresenceState.CONFIRMED_SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `SILENT stays SILENT without remote report`() {
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()

        // No remote reports added — run check again
        invokeCheckAllNodes()

        // Should stay SILENT, not promoted
        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    @Test
    fun `SILENT auto-promotes to CONFIRMED_SILENT after timeout without neighbor reports`() {
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()
        assertEquals(NodePresenceState.SILENT, detector.trackedNodes[NODE_A]!!.state)

        // Simulate the unconfirmed promote timeout elapsing
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
        invokeCheckAllNodes() // second pass
        invokeCheckAllNodes() // third pass

        verify(exactly = 1) {
            notifications.showSilentNodeNotification(
                nodeNum = NODE_A,
                title = any(),
                message = any(),
            )
        }
    }

    // ── 5. Recovery ─────────────────────────────────────────────────

    @Test
    fun `node recovers to ONLINE when packet received again`() {
        // Get to CONFIRMED_SILENT
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes()
        detector.onSilenceReportReceived(
            SilenceReport(NODE_B, NODE_A, System.currentTimeMillis()),
        )
        invokeCheckAllNodes()
        assertEquals(NodePresenceState.CONFIRMED_SILENT, detector.trackedNodes[NODE_A]!!.state)

        // Node comes back
        detector.onPacketReceived(NODE_A)

        assertEquals(NodePresenceState.ONLINE, detector.trackedNodes[NODE_A]!!.state)
        assertFalse(detector.trackedNodes[NODE_A]!!.notificationFired)
    }

    // ── 6. Silence report parsing ───────────────────────────────────

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

        // Returns true (consumed) but doesn't add to remote reports
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

        // Should still count as 1 unique reporter
        // Verify by: get NODE_A to SILENT, then check it confirms (needs >=1 report)
        detector.onPacketReceived(NODE_A)
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000
        invokeCheckAllNodes() // → SILENT
        invokeCheckAllNodes() // → should confirm with the 1 unique report

        assertEquals(NodePresenceState.CONFIRMED_SILENT, detector.trackedNodes[NODE_A]!!.state)
    }

    // ── 7. Graceful exit ────────────────────────────────────────────

    @Test
    fun `graceful exit prevents silence detection`() {
        detector.onPacketReceived(NODE_A)

        // NODE_A announces graceful exit
        detector.tryParseGracefulExit(
            "MESH_EXIT|$NODE_A|${System.currentTimeMillis()}",
            NODE_A,
        )

        // Simulate timeout
        detector.trackedNodes[NODE_A]!!.lastSeenMs =
            System.currentTimeMillis() - SilentNodeDetector.SILENCE_TIMEOUT_MS - 1000

        invokeCheckAllNodes()

        // Should stay ONLINE — graceful exit exemption
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

        // Returns true (consumed) but doesn't add to graceful exits
        assertTrue(result)
        assertFalse(detector.gracefullyExitedNodes.containsKey(MY_NODE))
    }

    @Test
    fun `graceful exit cleared when node comes back`() {
        // NODE_A announces exit
        detector.tryParseGracefulExit(
            "MESH_EXIT|$NODE_A|${System.currentTimeMillis()}",
            NODE_A,
        )
        assertTrue(detector.gracefullyExitedNodes.containsKey(NODE_A))

        // NODE_A comes back
        detector.onPacketReceived(NODE_A)

        assertFalse(detector.gracefullyExitedNodes.containsKey(NODE_A))
    }

    // ── Helper ──────────────────────────────────────────────────────

    /**
     * Calls the private checkAllNodes() method via reflection.
     * This avoids needing to wait for the coroutine scan loop in tests.
     */
    private fun invokeCheckAllNodes() {
        val method = SilentNodeDetector::class.java.getDeclaredMethod("checkAllNodes")
        method.isAccessible = true
        method.invoke(detector)
    }
}
