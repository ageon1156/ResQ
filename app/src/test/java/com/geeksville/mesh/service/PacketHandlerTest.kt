
package com.geeksville.mesh.service

import com.geeksville.mesh.repository.radio.RadioInterfaceService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.meshtastic.core.data.repository.MeshLogRepository
import org.meshtastic.core.data.repository.PacketRepository
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.proto.MeshProtos

class PacketHandlerTest {

    private val packetRepository: PacketRepository = mockk(relaxed = true)
    private val serviceBroadcasts: MeshServiceBroadcasts = mockk(relaxed = true)
    private val radioInterfaceService: RadioInterfaceService = mockk(relaxed = true)
    private val meshLogRepository: MeshLogRepository = mockk(relaxed = true)
    private val connectionStateHolder: ConnectionStateHandler = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var handler: PacketHandler

    @Before
    fun setUp() {
        handler =
            PacketHandler(
                dagger.Lazy { packetRepository },
                serviceBroadcasts,
                radioInterfaceService,
                dagger.Lazy { meshLogRepository },
                connectionStateHolder,
            )
        handler.start(testScope)
    }

    @Test
    fun `sendToRadio with ToRadio Builder sends immediately`() {
        val builder =
            MeshProtos.ToRadio.newBuilder().apply { packet = MeshProtos.MeshPacket.newBuilder().setId(123).build() }

        handler.sendToRadio(builder)

        verify { radioInterfaceService.sendToRadio(any()) }

    }

    @Test
    fun `sendToRadio with MeshPacket queues and sends when connected`() = runTest(testDispatcher) {
        val packet = MeshProtos.MeshPacket.newBuilder().setId(456).build()
        every { connectionStateHolder.connectionState } returns MutableStateFlow(ConnectionState.Connected)

        handler.sendToRadio(packet)
        testScheduler.runCurrent()

        verify { radioInterfaceService.sendToRadio(any()) }
    }

    @Test
    fun `handleQueueStatus completes deferred`() = runTest(testDispatcher) {
        val packet = MeshProtos.MeshPacket.newBuilder().setId(789).build()
        every { connectionStateHolder.connectionState } returns MutableStateFlow(ConnectionState.Connected)

        handler.sendToRadio(packet)
        testScheduler.runCurrent()

        val status =
            MeshProtos.QueueStatus.newBuilder()
                .apply {
                    meshPacketId = 789
                    res = 0 
                    free = 1
                }
                .build()

        handler.handleQueueStatus(status)
        testScheduler.runCurrent()

    }
}

