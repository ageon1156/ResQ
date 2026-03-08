

package org.meshtastic.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BootloaderWarningDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val DISMISSED_BOOTLOADER_ADDRESSES = stringPreferencesKey("dismissed-bootloader-addresses")
    }

    private val dismissedAddressesFlow =
        dataStore.data.map { preferences ->
            val jsonString = preferences[PreferencesKeys.DISMISSED_BOOTLOADER_ADDRESSES] ?: return@map emptySet()

            runCatching { Json.decodeFromString<List<String>>(jsonString).toSet() }
                .getOrDefault(emptySet())
        }

    suspend fun isDismissed(address: String): Boolean = dismissedAddressesFlow.first().contains(address)

    suspend fun dismiss(address: String) {
        val current = dismissedAddressesFlow.first()
        if (current.contains(address)) return

        val updated = (current + address).toList()
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISMISSED_BOOTLOADER_ADDRESSES] = Json.encodeToString(updated)
        }
    }
}

