
package com.geeksville.mesh.repository.radio

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.proto.MeshProtos

class TCPInterfaceTest {

    private val service: RadioInterfaceService = mockk(relaxed = true)
    private val dispatchers: CoroutineDispatchers = mockk(relaxed = true)

    @Test
    fun `keepAlive generates correct heartbeat bytes`() = runTest {
        val address = "192.168.1.1:4403"
        
        val tcpInterface =
            object : TCPInterface(service, dispatchers, address) {
                var capturedBytes: ByteArray? = null

                override fun handleSendToRadio(p: ByteArray) {
                    capturedBytes = p
                }

                override fun connect() {}
            }

        tcpInterface.keepAlive()

        val expectedHeartbeat =
            MeshProtos.ToRadio.newBuilder()
                .setHeartbeat(MeshProtos.Heartbeat.getDefaultInstance())
                .build()
                .toByteArray()

        assertArrayEquals("Heartbeat bytes should match", expectedHeartbeat, tcpInterface.capturedBytes)
    }

    @Test
    fun `sendBytes does not crash when outStream is null`() = runTest {
        val address = "192.168.1.1:4403"
        val tcpInterface =
            object : TCPInterface(service, dispatchers, address) {
                override fun connect() {}
            }

        tcpInterface.sendBytes(byteArrayOf(1, 2, 3))
    }

    @Test
    fun `flushBytes does not crash when outStream is null`() = runTest {
        val address = "192.168.1.1:4403"
        val tcpInterface =
            object : TCPInterface(service, dispatchers, address) {
                override fun connect() {}
            }

        tcpInterface.flushBytes()
    }
}

