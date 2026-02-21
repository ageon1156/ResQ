

@file:Suppress("MagicNumber")

package org.meshtastic.core.model

import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.label_long_fast
import org.meshtastic.core.strings.label_long_moderate
import org.meshtastic.core.strings.label_long_slow
import org.meshtastic.core.strings.label_long_turbo
import org.meshtastic.core.strings.label_medium_fast
import org.meshtastic.core.strings.label_medium_slow
import org.meshtastic.core.strings.label_short_fast
import org.meshtastic.core.strings.label_short_slow
import org.meshtastic.core.strings.label_short_turbo
import org.meshtastic.core.strings.label_very_long_slow
import org.meshtastic.proto.ConfigProtos.Config.LoRaConfig
import org.meshtastic.proto.ConfigProtos.Config.LoRaConfig.ModemPreset
import org.meshtastic.proto.ConfigProtos.Config.LoRaConfig.RegionCode
import kotlin.math.floor

private fun hash(name: String): UInt { 
    var hash = 5381u
    for (c in name) {
        hash += (hash shl 5) + c.code.toUInt()
    }
    return hash
}

private val ModemPreset.bandwidth: Float
    get() {
        for (option in ChannelOption.entries) {
            if (option.modemPreset == this) return option.bandwidth
        }
        return 0f
    }

private fun LoRaConfig.bandwidth(regionInfo: RegionInfo?) = if (usePreset) {
    modemPreset.bandwidth * if (regionInfo?.wideLora == true) 3.25f else 1f
} else {
    when (bandwidth) {
        31 -> .03125f
        62 -> .0625f
        200 -> .203125f
        400 -> .40625f
        800 -> .8125f
        1600 -> 1.6250f
        else -> bandwidth / 1000f
    }
}

val LoRaConfig.numChannels: Int
    get() {
        val regionInfo = RegionInfo.fromRegionCode(region)
        if (regionInfo == null) return 0

        val bw = bandwidth(regionInfo)
        if (bw <= 0f) return 1 

        val num = floor((regionInfo.freqEnd - regionInfo.freqStart) / bw)

        return if (num > 0) num.toInt() else 1
    }

internal fun LoRaConfig.channelNum(primaryName: String): Int = when {
    channelNum != 0 -> channelNum
    numChannels == 0 -> 0
    else -> (hash(primaryName) % numChannels.toUInt()).toInt() + 1
}

internal fun LoRaConfig.radioFreq(channelNum: Int): Float {
    if (overrideFrequency != 0f) return overrideFrequency + frequencyOffset
    val regionInfo = RegionInfo.fromRegionCode(region)
    return if (regionInfo != null) {
        (regionInfo.freqStart + bandwidth(regionInfo) / 2) + (channelNum - 1) * bandwidth(regionInfo)
    } else {
        0f
    }
}

@Suppress("MagicNumber")
enum class RegionInfo(
    val regionCode: RegionCode,
    val description: String,
    val freqStart: Float,
    val freqEnd: Float,
    val wideLora: Boolean = false,
) {
    
    UNSET(RegionCode.UNSET, "Please set a region", 902.0f, 928.0f),

    US(RegionCode.US, "United States", 902.0f, 928.0f),

    EU_433(RegionCode.EU_433, "European Union 433MHz", 433.0f, 434.0f),

    EU_868(RegionCode.EU_868, "European Union 868MHz", 869.4f, 869.65f),

    CN(RegionCode.CN, "China", 470.0f, 510.0f),

    JP(RegionCode.JP, "Japan", 920.5f, 923.5f),

    ANZ(RegionCode.ANZ, "Australia / Brazil / New Zealand", 915.0f, 928.0f),

    KR(RegionCode.KR, "Korea", 920.0f, 923.0f),

    TW(RegionCode.TW, "Taiwan", 920.0f, 925.0f),

    RU(RegionCode.RU, "Russia", 868.7f, 869.2f),

    IN(RegionCode.IN, "India", 865.0f, 867.0f),

    NZ_865(RegionCode.NZ_865, "New Zealand 865MHz", 864.0f, 868.0f),

    TH(RegionCode.TH, "Thailand", 920.0f, 925.0f),

    UA_433(RegionCode.UA_433, "Ukraine 433MHz", 433.0f, 434.7f),

    UA_868(RegionCode.UA_868, "Ukraine 868MHz", 868.0f, 868.6f),

    MY_433(RegionCode.MY_433, "Malaysia 433MHz", 433.0f, 435.0f),

    MY_919(RegionCode.MY_919, "Malaysia 919MHz", 919.0f, 924.0f),

    SG_923(RegionCode.SG_923, "Singapore 923MHz", 917.0f, 925.0f),

    PH_433(RegionCode.PH_433, "Philippines 433MHz", 433.0f, 434.7f),

    PH_868(RegionCode.PH_868, "Philippines 868MHz", 868.0f, 869.4f),

    PH_915(RegionCode.PH_915, "Philippines 915MHz", 915.0f, 918.0f),

    LORA_24(RegionCode.LORA_24, "2.4 GHz", 2400.0f, 2483.5f, wideLora = true),

    ANZ_433(RegionCode.ANZ_433, "Australia / New Zealand 433MHz", 433.05f, 434.79f),

    KZ_433(RegionCode.KZ_433, "Kazakhstan 433MHz", 433.075f, 434.775f),

    KZ_863(RegionCode.KZ_863, "Kazakhstan 863MHz", 863.0f, 868.0f, wideLora = true),

    NP_865(RegionCode.NP_865, "Nepal 865MHz", 865.0f, 868.0f, wideLora = false),

    BR_902(RegionCode.BR_902, "Brazil 902MHz", 902.0f, 907.5f, wideLora = false),
    ;

    companion object {
        fun fromRegionCode(regionCode: RegionCode): RegionInfo? = entries.find { it.regionCode == regionCode }
    }
}

enum class ChannelOption(val modemPreset: ModemPreset, val labelRes: StringResource, val bandwidth: Float) {
    
    VERY_LONG_SLOW(ModemPreset.VERY_LONG_SLOW, Res.string.label_very_long_slow, 0.0625f),
    LONG_TURBO(ModemPreset.LONG_TURBO, Res.string.label_long_turbo, 0.500f),
    LONG_FAST(ModemPreset.LONG_FAST, Res.string.label_long_fast, 0.250f),
    LONG_MODERATE(ModemPreset.LONG_MODERATE, Res.string.label_long_moderate, 0.125f),
    LONG_SLOW(ModemPreset.LONG_SLOW, Res.string.label_long_slow, 0.125f),
    MEDIUM_FAST(ModemPreset.MEDIUM_FAST, Res.string.label_medium_fast, 0.250f),
    MEDIUM_SLOW(ModemPreset.MEDIUM_SLOW, Res.string.label_medium_slow, 0.250f),
    SHORT_FAST(ModemPreset.SHORT_FAST, Res.string.label_short_fast, 0.250f),
    SHORT_SLOW(ModemPreset.SHORT_SLOW, Res.string.label_short_slow, 0.250f),
    SHORT_TURBO(ModemPreset.SHORT_TURBO, Res.string.label_short_turbo, 0.500f),
    ;

    companion object {
        
        val DEFAULT = LONG_FAST

        fun from(modemPreset: ModemPreset?): ChannelOption? {
            if (modemPreset == null) return null
            
            return entries.find { it.modemPreset == modemPreset }
        }
    }
}

