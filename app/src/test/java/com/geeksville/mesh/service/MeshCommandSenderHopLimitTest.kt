
package com.geeksville.mesh.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.proto.ConfigProtos.Config
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import org.meshtastic.proto.MeshProtos.MeshPacket

class MeshCommandSenderHopLimitTest {

    private val packetHandler: PacketHandler = mockk(relaxed = true)
    private val nodeManager = MeshNodeManager()
    private val connectionStateHolder: ConnectionStateHandler = mockk(relaxed = true)
    private val radioConfigRepository: RadioConfigRepository = mockk(relaxed = true)

    private val localConfigFlow = MutableStateFlow(LocalConfig.getDefaultInstance())
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    private lateinit var commandSender: MeshCommandSender

    @Before
    fun setUp() {
        val connectedFlow = MutableStateFlow(ConnectionState.Connected)
        every { connectionStateHolder.connectionState } returns connectedFlow
        every { radioConfigRepository.localConfigFlow } returns localConfigFlow

        commandSender = MeshCommandSender(packetHandler, nodeManager, connectionStateHolder, radioConfigRepository)
        commandSender.start(testScope)
    }

    @Test
    fun `sendData uses default hop limit when config hop limit is zero`() = runTest(testDispatcher) {
        val packet =
            DataPacket(
                to = DataPacket.ID_BROADCAST,
                bytes = byteArrayOf(1, 2, 3),
                dataType = 1, 
            )

        val meshPacketSlot = slot<MeshPacket>()
        every { packetHandler.sendToRadio(capture(meshPacketSlot)) } returns Unit

        localConfigFlow.value =
            LocalConfig.newBuilder().setLora(Config.LoRaConfig.newBuilder().setHopLimit(0)).build()

        commandSender.sendData(packet)

        verify(exactly = 1) { packetHandler.sendToRadio(any<MeshPacket>()) }

        val capturedHopLimit = meshPacketSlot.captured.hopLimit
        assertTrue("Hop limit should be greater than 0, but was $capturedHopLimit", capturedHopLimit > 0)
        assertEquals(3, capturedHopLimit)
    }

    @Test
    fun `sendData respects non-zero hop limit from config`() = runTest(testDispatcher) {
        val packet = DataPacket(to = DataPacket.ID_BROADCAST, bytes = byteArrayOf(1, 2, 3), dataType = 1)

        val meshPacketSlot = slot<MeshPacket>()
        every { packetHandler.sendToRadio(capture(meshPacketSlot)) } returns Unit

        localConfigFlow.value =
            LocalConfig.newBuilder().setLora(Config.LoRaConfig.newBuilder().setHopLimit(7)).build()

        commandSender.sendData(packet)

        verify { packetHandler.sendToRadio(any<MeshPacket>()) }
        assertEquals(7, meshPacketSlot.captured.hopLimit)
    }
}

