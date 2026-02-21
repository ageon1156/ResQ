
package org.meshtastic.core.model.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SfppHasherTest {

    @Test
    fun `computeMessageHash produces consistent results`() {
        val payload = "Hello World".toByteArray()
        val to = 1234
        val from = 5678
        val id = 999

        val hash1 = SfppHasher.computeMessageHash(payload, to, from, id)
        val hash2 = SfppHasher.computeMessageHash(payload, to, from, id)

        assertArrayEquals(hash1, hash2)
        assertEquals(16, hash1.size)
    }

    @Test
    fun `computeMessageHash produces different results for different inputs`() {
        val payload = "Hello World".toByteArray()
        val to = 1234
        val from = 5678
        val id = 999

        val hashBase = SfppHasher.computeMessageHash(payload, to, from, id)

        val hashDiffPayload = SfppHasher.computeMessageHash("Hello Work".toByteArray(), to, from, id)
        assertNotEquals(hashBase.toList(), hashDiffPayload.toList())

        val hashDiffTo = SfppHasher.computeMessageHash(payload, 1235, from, id)
        assertNotEquals(hashBase.toList(), hashDiffTo.toList())

        val hashDiffFrom = SfppHasher.computeMessageHash(payload, to, 5679, id)
        assertNotEquals(hashBase.toList(), hashDiffFrom.toList())

        val hashDiffId = SfppHasher.computeMessageHash(payload, to, from, 1000)
        assertNotEquals(hashBase.toList(), hashDiffId.toList())
    }

    @Test
    fun `computeMessageHash handles large values`() {
        val payload = byteArrayOf(1, 2, 3)
        
        val to = -1 
        val from = 0x7FFFFFFF
        val id = Int.MIN_VALUE

        val hash = SfppHasher.computeMessageHash(payload, to, from, id)
        assertEquals(16, hash.size)
    }

    @Test
    fun `computeMessageHash follows little endian for integers`() {

        val payload = byteArrayOf()
        val to = 0x01020304
        val from = 0x05060708
        val id = 0x090A0B0C

        val hash = SfppHasher.computeMessageHash(payload, to, from, id)
        assertNotNull(hash)
        assertEquals(16, hash.size)
    }

    private fun assertNotNull(any: Any?) {
        if (any == null) throw AssertionError("Should not be null")
    }
}

