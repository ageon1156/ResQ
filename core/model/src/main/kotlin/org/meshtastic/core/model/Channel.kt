
package org.meshtastic.core.model

import com.google.protobuf.ByteString
import org.meshtastic.core.common.byteArrayOfInts
import org.meshtastic.core.common.xorHash
import org.meshtastic.proto.ChannelProtos
import org.meshtastic.proto.ConfigKt.loRaConfig
import org.meshtastic.proto.ConfigProtos
import org.meshtastic.proto.ConfigProtos.Config.LoRaConfig.ModemPreset
import org.meshtastic.proto.channelSettings
import java.security.SecureRandom

data class Channel(
    val settings: ChannelProtos.ChannelSettings = default.settings,
    val loraConfig: ConfigProtos.Config.LoRaConfig = default.loraConfig,
) {
    companion object {
        
        private val channelDefaultKey =
            byteArrayOfInts(
                0xd4,
                0xf1,
                0xbb,
                0x3a,
                0x20,
                0x29,
                0x07,
                0x59,
                0xf0,
                0xbc,
                0xff,
                0xab,
                0xcf,
                0x4e,
                0x69,
                0x01,
            )

        private val cleartextPSK = ByteString.EMPTY
        private val defaultPSK = byteArrayOfInts(1) 

        val default =
            Channel(
                channelSettings { psk = ByteString.copyFrom(defaultPSK) },
                
                loRaConfig {
                    usePreset = true
                    modemPreset = ModemPreset.LONG_FAST
                    hopLimit = 3
                    txEnabled = true
                },
            )

        fun getRandomKey(size: Int = 32): ByteString {
            val bytes = ByteArray(size)
            val random = SecureRandom()
            random.nextBytes(bytes)
            return ByteString.copyFrom(bytes)
        }
    }

    val name: String
        get() =
            settings.name.ifEmpty {

                if (loraConfig.usePreset) {
                    when (loraConfig.modemPreset) {
                        ModemPreset.SHORT_TURBO -> "ShortTurbo"
                        ModemPreset.SHORT_FAST -> "ShortFast"
                        ModemPreset.SHORT_SLOW -> "ShortSlow"
                        ModemPreset.MEDIUM_FAST -> "MediumFast"
                        ModemPreset.MEDIUM_SLOW -> "MediumSlow"
                        ModemPreset.LONG_FAST -> "LongFast"
                        ModemPreset.LONG_SLOW -> "LongSlow"
                        ModemPreset.LONG_MODERATE -> "LongMod"
                        ModemPreset.VERY_LONG_SLOW -> "VLongSlow"
                        ModemPreset.LONG_TURBO -> "LongTurbo"
                        else -> "Invalid"
                    }
                } else {
                    "Custom"
                }
            }

    val psk: ByteString
        get() =
            if (settings.psk.size() != 1) {
                settings.psk 
            } else {
                
                val pskIndex = settings.psk.byteAt(0).toInt()

                if (pskIndex == 0) {
                    cleartextPSK
                } else {
                    
                    val bytes = channelDefaultKey.clone()
                    bytes[bytes.size - 1] = (0xff and (bytes[bytes.size - 1] + pskIndex - 1)).toByte()
                    ByteString.copyFrom(bytes)
                }
            }

    val hash: Int
        get() = xorHash(name.toByteArray()) xor xorHash(psk.toByteArray())

    val channelNum: Int
        get() = loraConfig.channelNum(name)

    val radioFreq: Float
        get() = loraConfig.radioFreq(channelNum)

    override fun equals(other: Any?): Boolean =
        (other is Channel) && psk.toByteArray() contentEquals other.psk.toByteArray() && name == other.name

    override fun hashCode(): Int {
        var result = settings.hashCode()
        result = 31 * result + loraConfig.hashCode()
        return result
    }
}

