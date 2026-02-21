
package com.geeksville.mesh.service

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
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.proto.MeshProtos

class MeshMessageProcessorTest {

    private val nodeManager: MeshNodeManager = mockk(relaxed = true)
    private val serviceRepository: ServiceRepository = mockk(relaxed = true)
    private val meshLogRepository: MeshLogRepository = mockk(relaxed = true)
    private val router: MeshRouter = mockk(relaxed = true)
    private val fromRadioDispatcher: FromRadioPacketHandler = mockk(relaxed = true)
    private val meshLogRepositoryLazy = dagger.Lazy { meshLogRepository }
    private val dataHandler: MeshDataHandler = mockk(relaxed = true)

    private val isNodeDbReady = MutableStateFlow(false)
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var processor: MeshMessageProcessor

    @Before
    fun setUp() {
        every { nodeManager.isNodeDbReady } returns isNodeDbReady
        every { router.dataHandler } returns dataHandler
        processor =
            MeshMessageProcessor(nodeManager, serviceRepository, meshLogRepositoryLazy, router, fromRadioDispatcher)
        processor.start(testScope)
    }

    @Test
    fun `early packets are buffered and flushed when DB is ready`() = runTest(testDispatcher) {
        val packet =
            MeshProtos.MeshPacket.newBuilder()
                .apply {
                    id = 123
                    decoded = MeshProtos.Data.newBuilder().setPortnumValue(1).build()
                }
                .build()

        isNodeDbReady.value = false
        testScheduler.runCurrent() 

        processor.handleReceivedMeshPacket(packet, 999)

        verify(exactly = 0) { dataHandler.handleReceivedData(any(), any(), any(), any()) }

        isNodeDbReady.value = true
        testScheduler.runCurrent() 

        verify(exactly = 1) { dataHandler.handleReceivedData(match { it.id == 123 }, any(), any(), any()) }
    }

    @Test
    fun `packets are processed immediately if DB is already ready`() = runTest(testDispatcher) {
        val packet =
            MeshProtos.MeshPacket.newBuilder()
                .apply {
                    id = 456
                    decoded = MeshProtos.Data.newBuilder().setPortnumValue(1).build()
                }
                .build()

        isNodeDbReady.value = true
        testScheduler.runCurrent()

        processor.handleReceivedMeshPacket(packet, 999)

        verify(exactly = 1) { dataHandler.handleReceivedData(match { it.id == 456 }, any(), any(), any()) }
    }
}

