

package org.meshtastic.core.data.datasource

import android.app.Application
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.meshtastic.core.model.NetworkDeviceHardware
import javax.inject.Inject

class DeviceHardwareJsonDataSource @Inject constructor(private val application: Application) {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun loadDeviceHardwareFromJsonAsset(): List<NetworkDeviceHardware> =
        application.assets.open("device_hardware.json").use { inputStream ->
            json.decodeFromStream<List<NetworkDeviceHardware>>(inputStream)
        }
}

