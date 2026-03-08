

package org.meshtastic.core.data.datasource

import android.app.Application
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.meshtastic.core.model.BootloaderOtaQuirk
import javax.inject.Inject

class BootloaderOtaQuirksJsonDataSource @Inject constructor(private val application: Application) {
    @OptIn(ExperimentalSerializationApi::class)
    fun loadBootloaderOtaQuirksFromJsonAsset(): List<BootloaderOtaQuirk> = runCatching {
        val inputStream = application.assets.open("device_bootloader_ota_quirks.json")
        inputStream.use { Json.decodeFromStream<ListWrapper>(it).devices }
    }
        .getOrDefault(emptyList())

    @Serializable private data class ListWrapper(val devices: List<BootloaderOtaQuirk> = emptyList())
}

