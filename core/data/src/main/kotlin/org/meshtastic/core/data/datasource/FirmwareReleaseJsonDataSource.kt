

package org.meshtastic.core.data.datasource

import android.app.Application
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.meshtastic.core.model.NetworkFirmwareReleases
import javax.inject.Inject

class FirmwareReleaseJsonDataSource @Inject constructor(private val application: Application) {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun loadFirmwareReleaseFromJsonAsset(): NetworkFirmwareReleases =
        application.assets.open("firmware_releases.json").use { inputStream ->
            json.decodeFromStream<NetworkFirmwareReleases>(inputStream)
        }
}

