
package com.geeksville.mesh.repository.radio

import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class StreamInterfaceTest {

    private val service: RadioInterfaceService = mockk(relaxed = true)

    private class TestStreamInterface(service: RadioInterfaceService) : StreamInterface(service) {
        override fun sendBytes(p: ByteArray) {}

        fun testReadChar(c: Byte) = readChar(c)
    }

    private val streamInterface = TestStreamInterface(service)

    @Test
    fun `readChar delivers a 1-byte packet`() {
        
        val packet = byteArrayOf(0x94.toByte(), 0xc3.toByte(), 0x00, 0x01, 0x42)

        packet.forEach { streamInterface.testReadChar(it) }

        verify { service.handleFromRadio(byteArrayOf(0x42)) }
    }

    @Test
    fun `readChar handles zero length packet`() {
        
        val packet = byteArrayOf(0x94.toByte(), 0xc3.toByte(), 0x00, 0x00)

        packet.forEach { streamInterface.testReadChar(it) }

        verify { service.handleFromRadio(byteArrayOf()) }
    }

    @Test
    fun `readChar loses sync on invalid START2`() {
        
        val data = byteArrayOf(0x94.toByte(), 0x00, 0x94.toByte(), 0xc3.toByte(), 0x00, 0x01, 0x55)

        data.forEach { streamInterface.testReadChar(it) }

        verify { service.handleFromRadio(byteArrayOf(0x55)) }
    }

    @Test
    fun `readChar handles multiple packets sequentially`() {
        val packet1 = byteArrayOf(0x94.toByte(), 0xc3.toByte(), 0x00, 0x01, 0x11)
        val packet2 = byteArrayOf(0x94.toByte(), 0xc3.toByte(), 0x00, 0x01, 0x22)

        packet1.forEach { streamInterface.testReadChar(it) }
        packet2.forEach { streamInterface.testReadChar(it) }

        verify { service.handleFromRadio(byteArrayOf(0x11)) }
        verify { service.handleFromRadio(byteArrayOf(0x22)) }
        confirmVerified(service)
    }

    @Test
    fun `readChar handles large packet up to MAX_TO_FROM_RADIO_SIZE`() {
        val size = 512
        val payload = ByteArray(size) { it.toByte() }
        val header = byteArrayOf(0x94.toByte(), 0xc3.toByte(), (size shr 8).toByte(), (size and 0xff).toByte())

        header.forEach { streamInterface.testReadChar(it) }
        payload.forEach { streamInterface.testReadChar(it) }

        verify { service.handleFromRadio(payload) }
    }

    @Test
    fun `readChar loses sync on overly large packet length`() {
        
        val header = byteArrayOf(0x94.toByte(), 0xc3.toByte(), 0x02, 0x01)

        header.forEach { streamInterface.testReadChar(it) }

        verify(exactly = 0) { service.handleFromRadio(any()) }
    }
}

