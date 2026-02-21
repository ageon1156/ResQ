

package org.meshtastic.feature.emergency.data

import android.app.Application
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import javax.inject.Inject

class EmergencyJsonDataSource @Inject constructor(
    private val application: Application,
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun loadEmergencyGuideData(): EmergencyGuideData =
        application.assets.open("emergency_guide.json").use { inputStream ->
            json.decodeFromStream<EmergencyGuideData>(inputStream)
        }
}

