
package org.meshtastic.core.datastore

import androidx.datastore.core.DataStore
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.meshtastic.proto.AppOnlyProtos.ChannelSet
import org.meshtastic.proto.ChannelProtos.Channel
import org.meshtastic.proto.ChannelProtos.ChannelSettings
import org.meshtastic.proto.ConfigProtos
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelSetDataSource @Inject constructor(private val channelSetStore: DataStore<ChannelSet>) {
    val channelSetFlow: Flow<ChannelSet> =
        channelSetStore.data.catch { exception ->
            
            if (exception is IOException) {
                Logger.e { "Error reading DeviceConfig settings: ${exception.message}" }
                emit(ChannelSet.getDefaultInstance())
            } else {
                throw exception
            }
        }

    suspend fun clearChannelSet() {
        channelSetStore.updateData { preference -> preference.toBuilder().clear().build() }
    }

    suspend fun replaceAllSettings(settingsList: List<ChannelSettings>) {
        channelSetStore.updateData { preference ->
            preference.toBuilder().clearSettings().addAllSettings(settingsList).build()
        }
    }

    suspend fun updateChannelSettings(channel: Channel) {
        if (channel.role == Channel.Role.DISABLED) return
        channelSetStore.updateData { preference ->
            val builder = preference.toBuilder()
            
            while (builder.settingsCount <= channel.index) {
                builder.addSettings(ChannelSettings.getDefaultInstance())
            }
            
            builder.setSettings(channel.index, channel.settings).build()
        }
    }

    suspend fun setLoraConfig(config: ConfigProtos.Config.LoRaConfig) {
        channelSetStore.updateData { preference -> preference.toBuilder().setLoraConfig(config).build() }
    }
}

