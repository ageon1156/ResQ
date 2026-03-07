package org.meshtastic.core.model.voice

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class VoiceFragment(
    val fragmentIndex: Int,
    val totalFragments: Int,
    val sessionId: ByteArray,
    val timestampSeconds: Int,
    val audioData: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VoiceFragment) return false
        return fragmentIndex == other.fragmentIndex &&
            totalFragments == other.totalFragments &&
            sessionId.contentEquals(other.sessionId) &&
            timestampSeconds == other.timestampSeconds &&
            audioData.contentEquals(other.audioData)
    }

    override fun hashCode(): Int {
        var result = fragmentIndex
        result = 31 * result + totalFragments
        result = 31 * result + sessionId.contentHashCode()
        result = 31 * result + timestampSeconds
        result = 31 * result + audioData.contentHashCode()
        return result
    }

    fun sessionIdHex(): String = sessionId.joinToString("") { "%02x".format(it) }
}

private const val HEADER_SIZE = 14

fun VoiceFragment.encode(): ByteArray {
    val buf = ByteBuffer.allocate(HEADER_SIZE + audioData.size).order(ByteOrder.BIG_ENDIAN)
    buf.put(fragmentIndex.toByte())
    buf.put(totalFragments.toByte())
    buf.put(sessionId, 0, 8)
    buf.putInt(timestampSeconds)
    buf.put(audioData)
    return buf.array()
}

fun ByteArray.decodeVoiceFragment(): VoiceFragment? {
    if (size < HEADER_SIZE) return null
    val buf = ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN)
    val fragmentIndex = buf.get().toInt() and 0xFF
    val totalFragments = buf.get().toInt() and 0xFF
    val sessionId = ByteArray(8).also { buf.get(it) }
    val timestampSeconds = buf.getInt()
    val audioData = ByteArray(size - HEADER_SIZE).also { buf.get(it) }
    return VoiceFragment(
        fragmentIndex = fragmentIndex,
        totalFragments = totalFragments,
        sessionId = sessionId,
        timestampSeconds = timestampSeconds,
        audioData = audioData,
    )
}
