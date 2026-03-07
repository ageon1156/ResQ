package org.meshtastic.core.audio

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Codec2Wrapper @Inject constructor() {

    companion object {
        init {
            System.loadLibrary("codec2_jni")
        }

        const val MODE_3200 = 0
        const val MODE_2400 = 1
        const val MODE_1600 = 2
        const val MODE_1400 = 3
        const val MODE_1300 = 4
        const val MODE_1200 = 5
        const val MODE_700C = 8
        const val DEFAULT_MODE = MODE_1200
    }

    external fun encode(pcm: ShortArray, mode: Int): ByteArray
    external fun decode(codec2Data: ByteArray, mode: Int): ShortArray
    external fun getSamplesPerFrame(mode: Int): Int
    external fun getBytesPerFrame(mode: Int): Int
}
