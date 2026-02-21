
package org.meshtastic.core.model.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

object SfppHasher {
    private const val HASH_SIZE = 16
    private const val INT_BYTES = 4

    fun computeMessageHash(encryptedPayload: ByteArray, to: Int, from: Int, id: Int): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(encryptedPayload)
        digest.update(ByteBuffer.allocate(INT_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(to).array())
        digest.update(ByteBuffer.allocate(INT_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(from).array())
        digest.update(ByteBuffer.allocate(INT_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(id).array())
        return digest.digest().copyOf(HASH_SIZE)
    }
}

