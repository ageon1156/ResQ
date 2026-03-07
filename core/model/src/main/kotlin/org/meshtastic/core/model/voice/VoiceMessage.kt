package org.meshtastic.core.model.voice

data class VoiceMessage(
    val sessionId: String,
    val fromNodeNum: Int,
    val timestampMs: Long,
    val codec2Bytes: ByteArray,
    val durationSeconds: Float,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VoiceMessage) return false
        return sessionId == other.sessionId && fromNodeNum == other.fromNodeNum
    }

    override fun hashCode(): Int = 31 * sessionId.hashCode() + fromNodeNum
}
