

package org.meshtastic.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.meshtastic.core.data.model.CustomTileProviderConfig
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.prefs.map.MapTileProviderPrefs
import javax.inject.Inject
import javax.inject.Singleton

interface CustomTileProviderRepository {
    fun getCustomTileProviders(): Flow<List<CustomTileProviderConfig>>

    suspend fun addCustomTileProvider(config: CustomTileProviderConfig)

    suspend fun updateCustomTileProvider(config: CustomTileProviderConfig)

    suspend fun deleteCustomTileProvider(configId: String)

    suspend fun getCustomTileProviderById(configId: String): CustomTileProviderConfig?
}

@Singleton
class CustomTileProviderRepositoryImpl
@Inject
constructor(
    private val json: Json,
    private val dispatchers: CoroutineDispatchers,
    private val mapTileProviderPrefs: MapTileProviderPrefs,
) : CustomTileProviderRepository {

    private val customTileProvidersStateFlow = MutableStateFlow<List<CustomTileProviderConfig>>(emptyList())

    init {
        loadDataFromPrefs()
    }

    override fun getCustomTileProviders(): Flow<List<CustomTileProviderConfig>> =
        customTileProvidersStateFlow.asStateFlow()

    override suspend fun addCustomTileProvider(config: CustomTileProviderConfig) {
        val newList = customTileProvidersStateFlow.value + config
        customTileProvidersStateFlow.value = newList
        saveDataToPrefs(newList)
    }

    override suspend fun updateCustomTileProvider(config: CustomTileProviderConfig) {
        val newList = customTileProvidersStateFlow.value.map { if (it.id == config.id) config else it }
        customTileProvidersStateFlow.value = newList
        saveDataToPrefs(newList)
    }

    override suspend fun deleteCustomTileProvider(configId: String) {
        val newList = customTileProvidersStateFlow.value.filterNot { it.id == configId }
        customTileProvidersStateFlow.value = newList
        saveDataToPrefs(newList)
    }

    override suspend fun getCustomTileProviderById(configId: String): CustomTileProviderConfig? =
        customTileProvidersStateFlow.value.find { it.id == configId }

    private fun loadDataFromPrefs() {
        val jsonString = mapTileProviderPrefs.customTileProviders
        if (jsonString != null) {
            try {
                customTileProvidersStateFlow.value = json.decodeFromString<List<CustomTileProviderConfig>>(jsonString)
            } catch (e: SerializationException) {
                customTileProvidersStateFlow.value = emptyList()
            }
        } else {
            customTileProvidersStateFlow.value = emptyList()
        }
    }

    private suspend fun saveDataToPrefs(providers: List<CustomTileProviderConfig>) {
        withContext(dispatchers.io) {
            try {
                val jsonString = json.encodeToString(providers)
                mapTileProviderPrefs.customTileProviders = jsonString
            } catch (e: SerializationException) {
            }
        }
    }
}

