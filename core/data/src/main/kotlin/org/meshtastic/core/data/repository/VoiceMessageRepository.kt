package org.meshtastic.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.meshtastic.core.model.voice.VoiceFragment
import org.meshtastic.core.model.voice.VoiceMessage
import javax.inject.Inject
import javax.inject.Singleton

private const val FRAGMENT_TIMEOUT_MS = 30_000L
private const val BYTES_PER_SECOND_450BPS = 56.25f

@Singleton
class VoiceMessageRepository @Inject constructor() {

    private data class Session(
        val fromNodeNum: Int,
        val timestampMs: Long,
        val fragments: MutableList<VoiceFragment?>,
        val receivedAt: Long = System.currentTimeMillis(),
    )

    private val pending = mutableMapOf<String, Session>()

    private val recentMessages = object : LinkedHashMap<String, VoiceMessage>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, VoiceMessage>) = size > 5
    }

    fun getRecent(sessionId: String): VoiceMessage? = recentMessages[sessionId]

    private val _assembledMessages = MutableSharedFlow<VoiceMessage>(extraBufferCapacity = 8)
    val assembledMessages: SharedFlow<VoiceMessage> = _assembledMessages.asSharedFlow()

    fun addFragment(fragment: VoiceFragment, fromNodeNum: Int): VoiceMessage? {
        pruneStale()

        val key = fragment.sessionIdHex()

        val session = pending.getOrPut(key) {
            Session(
                fromNodeNum = fromNodeNum,
                timestampMs = fragment.timestampSeconds.toLong() * 1000L,
                fragments = MutableList(fragment.totalFragments) { null },
            )
        }

        if (fragment.fragmentIndex >= session.fragments.size) {
            Logger.w { "VoiceMessage: fragment index ${fragment.fragmentIndex} out of bounds (total=${fragment.totalFragments})" }
            return null
        }

        session.fragments[fragment.fragmentIndex] = fragment

        val allReceived = session.fragments.all { it != null }
        if (!allReceived) return null

        pending.remove(key)

        val codec2Bytes = session.fragments
            .mapNotNull { it?.audioData }
            .fold(ByteArray(0)) { acc, bytes -> acc + bytes }

        val durationSeconds = codec2Bytes.size / BYTES_PER_SECOND_450BPS

        Logger.i { "VoiceMessage assembled: sessionId=$key, ${codec2Bytes.size} bytes, ${durationSeconds}s" }

        val message = VoiceMessage(
            sessionId = key,
            fromNodeNum = session.fromNodeNum,
            timestampMs = session.timestampMs,
            codec2Bytes = codec2Bytes,
            durationSeconds = durationSeconds,
        )
        recentMessages[key] = message
        _assembledMessages.tryEmit(message)
        return message
    }

    private fun pruneStale() {
        val cutoff = System.currentTimeMillis() - FRAGMENT_TIMEOUT_MS
        val stale = pending.entries.filter { it.value.receivedAt < cutoff }.map { it.key }
        stale.forEach {
            Logger.d { "VoiceMessage: pruning stale session $it" }
            pending.remove(it)
        }
    }
}
