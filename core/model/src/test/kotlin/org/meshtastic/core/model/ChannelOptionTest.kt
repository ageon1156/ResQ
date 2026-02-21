

package org.meshtastic.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.meshtastic.proto.ConfigProtos.Config.LoRaConfig.ModemPreset

class ChannelOptionTest {

    @Test
    fun `ensure every ModemPreset is mapped in ChannelOption`() {
        
        val unmappedPresets =
            ModemPreset.entries.filter {
                
                it != ModemPreset.UNRECOGNIZED
            }

        unmappedPresets.forEach { preset ->
            
            val channelOption = ChannelOption.from(preset)

            assertNotNull(
                "Missing ChannelOption mapping for ModemPreset: '${preset.name}'. " +
                    "Please add a corresponding entry to the ChannelOption enum class.",
                channelOption,
            )
        }
    }

    @Test
    fun `ensure no extra mappings exist in ChannelOption`() {
        val protoPresets = ModemPreset.entries.filter { it != ModemPreset.UNRECOGNIZED }.toSet()
        val mappedPresets = ChannelOption.entries.map { it.modemPreset }.toSet()

        assertEquals(
            "The set of ModemPresets in protobufs does not match the set of ModemPresets mapped in ChannelOption. " +
                "Check for removed presets in protobufs or duplicate mappings in ChannelOption.",
            protoPresets,
            mappedPresets,
        )

        assertEquals(
            "Each ChannelOption must map to a unique ModemPreset.",
            protoPresets.size,
            ChannelOption.entries.size,
        )
    }
}

