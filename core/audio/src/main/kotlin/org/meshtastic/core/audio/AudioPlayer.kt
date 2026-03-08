package org.meshtastic.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    private val codec2: Codec2Wrapper,
) {
    companion object {
        private const val SAMPLE_RATE_HZ = AudioRecorder.SAMPLE_RATE_HZ
    }

    suspend fun play(codec2Bytes: ByteArray, mode: Int = Codec2Wrapper.DEFAULT_MODE) = withContext(Dispatchers.IO) {
        val pcm = codec2.decode(codec2Bytes, mode)
        if (pcm.isEmpty()) {
            return@withContext
        }

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()

        val durationMs = (pcm.size.toLong() * 1000L) / SAMPLE_RATE_HZ
        Thread.sleep(durationMs + 200)
        track.stop()
        track.release()
    }
}
