package org.meshtastic.core.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import co.touchlab.kermit.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor() {

    companion object {
        const val SAMPLE_RATE_HZ = 8000
        const val MAX_RECORDING_MS = 5_000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    fun start(onSamples: (ShortArray) -> Unit) {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL, FORMAT)
        val bufSize = maxOf(minBuf, SAMPLE_RATE_HZ * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_HZ,
            CHANNEL,
            FORMAT,
            bufSize,
        )

        val record = audioRecord ?: return
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Logger.e { "AudioRecord failed to initialize" }
            return
        }

        isRecording = true
        record.startRecording()

        val frameBuffer = ShortArray(SAMPLE_RATE_HZ / 25)
        val deadline = System.currentTimeMillis() + MAX_RECORDING_MS

        Thread {
            while (isRecording && System.currentTimeMillis() < deadline) {
                val read = record.read(frameBuffer, 0, frameBuffer.size)
                if (read > 0) {
                    onSamples(frameBuffer.copyOf(read))
                }
            }
        }.start()
    }

    fun stop() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
