package org.meshtastic.feature.voicemessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.audio.AudioPlayer
import org.meshtastic.core.audio.AudioRecorder
import org.meshtastic.core.audio.Codec2Wrapper
import org.meshtastic.core.data.repository.VoiceMessageRepository
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.voice.VoiceFragment
import org.meshtastic.core.model.voice.VoiceMessage
import org.meshtastic.core.model.voice.encode
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.Portnums
import java.util.UUID
import javax.inject.Inject

sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data object Recording : VoiceUiState
    data class Sending(val fragmentsSent: Int, val totalFragments: Int) : VoiceUiState
    data class Error(val message: String) : VoiceUiState
}

private const val INTER_FRAGMENT_DELAY_MS = 500L
private const val MAX_FRAGMENT_PAYLOAD_BYTES = 216
private const val MAX_RECENT_MESSAGES = 5

@HiltViewModel
class VoiceMessageViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val codec2: Codec2Wrapper,
    private val voiceMessageRepository: VoiceMessageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = serviceRepository.connectionState

    private val _incomingMessages = MutableStateFlow<List<VoiceMessage>>(emptyList())
    val incomingMessages: StateFlow<List<VoiceMessage>> = _incomingMessages.asStateFlow()

    init {
        viewModelScope.launch {
            voiceMessageRepository.assembledMessages.collect { message ->
                addIncomingMessage(message)
            }
        }
    }

    private val recordedPcm = mutableListOf<ShortArray>()

    fun startRecording() {
        if (_uiState.value != VoiceUiState.Idle) return
        recordedPcm.clear()
        _uiState.value = VoiceUiState.Recording
        audioRecorder.start { samples -> recordedPcm.add(samples) }
    }

    fun stopRecordingAndSend() {
        if (_uiState.value != VoiceUiState.Recording) return
        audioRecorder.stop()
        viewModelScope.launch { encodeAndSend() }
    }

    fun cancelRecording() {
        audioRecorder.stop()
        recordedPcm.clear()
        _uiState.value = VoiceUiState.Idle
    }

    fun addIncomingMessage(message: VoiceMessage) {
        val updated = (_incomingMessages.value + message).takeLast(MAX_RECENT_MESSAGES)
        _incomingMessages.value = updated
    }

    fun playMessage(message: VoiceMessage) {
        viewModelScope.launch {
            runCatching { audioPlayer.play(message.codec2Bytes) }
                .onFailure { Logger.e(it) { "Playback failed" } }
        }
    }

    private suspend fun encodeAndSend() = withContext(Dispatchers.Default) {
        if (recordedPcm.isEmpty()) {
            _uiState.value = VoiceUiState.Idle
            return@withContext
        }

        val allPcm = ShortArray(recordedPcm.sumOf { it.size })
        var offset = 0
        for (chunk in recordedPcm) {
            chunk.copyInto(allPcm, offset)
            offset += chunk.size
        }
        recordedPcm.clear()

        val codec2Bytes = runCatching {
            codec2.encode(allPcm, Codec2Wrapper.DEFAULT_MODE)
        }.getOrElse { ex ->
            Logger.e(ex) { "Codec2 encode failed" }
            _uiState.value = VoiceUiState.Error("Encoding failed")
            return@withContext
        }

        val fragments = buildFragments(codec2Bytes)
        if (fragments.isEmpty()) {
            _uiState.value = VoiceUiState.Idle
            return@withContext
        }

        _uiState.value = VoiceUiState.Sending(0, fragments.size)

        for ((index, fragment) in fragments.withIndex()) {
            val packet = DataPacket(
                to = DataPacket.ID_BROADCAST,
                bytes = fragment.encode(),
                dataType = Portnums.PortNum.AUDIO_APP_VALUE,
                priority = MeshProtos.MeshPacket.Priority.HIGH_VALUE,
                hopLimit = 0,
                wantAck = true,
                channel = 0,
            )
            try {
                serviceRepository.meshService?.send(packet)
                _uiState.value = VoiceUiState.Sending(index + 1, fragments.size)
            } catch (ex: Exception) {
                Logger.e(ex) { "Voice fragment send failed at index $index" }
                _uiState.value = VoiceUiState.Error("Send failed: ${ex.message}")
                return@withContext
            }
            if (index < fragments.lastIndex) delay(INTER_FRAGMENT_DELAY_MS)
        }

        _uiState.value = VoiceUiState.Idle
    }

    private fun buildFragments(codec2Bytes: ByteArray): List<VoiceFragment> {
        val sessionId = UUID.randomUUID().toString().replace("-", "").take(16)
            .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val timestampSeconds = (System.currentTimeMillis() / 1000L).toInt()

        val chunks = codec2Bytes.toList().chunked(MAX_FRAGMENT_PAYLOAD_BYTES)
        val total = chunks.size

        return chunks.mapIndexed { index, chunk ->
            VoiceFragment(
                fragmentIndex = index,
                totalFragments = total,
                sessionId = sessionId,
                timestampSeconds = timestampSeconds,
                audioData = chunk.toByteArray(),
            )
        }
    }
}
