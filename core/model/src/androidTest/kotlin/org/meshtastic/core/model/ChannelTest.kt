
package org.meshtastic.core.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.meshtastic.core.model.util.URL_PREFIX
import org.meshtastic.core.model.util.getChannelUrl
import org.meshtastic.core.model.util.toChannelSet
import org.meshtastic.proto.ConfigProtos
import org.meshtastic.proto.channelSet
import org.meshtastic.proto.copy

@RunWith(AndroidJUnit4::class)
class ChannelTest {
    @Test
    fun channelUrlGood() {
        val ch = channelSet {
            settings.add(Channel.default.settings)
            loraConfig = Channel.default.loraConfig
        }
        val channelUrl = ch.getChannelUrl()

        Assert.assertTrue(channelUrl.toString().startsWith(URL_PREFIX))
        Assert.assertEquals(channelUrl.toChannelSet(), ch)
    }

    @Test
    fun channelHashGood() {
        val ch = Channel.default

        Assert.assertEquals(8, ch.hash)
    }

    @Test
    fun numChannelsGood() {
        val ch = Channel.default

        Assert.assertEquals(104, ch.loraConfig.numChannels)
    }

    @Test
    fun channelNumGood() {
        val ch = Channel.default

        Assert.assertEquals(20, ch.channelNum)
    }

    @Test
    fun radioFreqGood() {
        val ch = Channel.default

        Assert.assertEquals(906.875f, ch.radioFreq)
    }

    @Test
    fun allModemPresetsHaveValidNames() {
        ConfigProtos.Config.LoRaConfig.ModemPreset.values().forEach { preset ->

            if (preset.name == "UNSET" || preset.name == "UNRECOGNIZED") return@forEach

            val loraConfig =
                Channel.default.loraConfig.copy {
                    usePreset = true
                    modemPreset = preset
                }
            val channel = Channel(loraConfig = loraConfig)

            Assert.assertNotEquals("Preset ${preset.name} should typically have a valid name", "Invalid", channel.name)
        }
    }
}

