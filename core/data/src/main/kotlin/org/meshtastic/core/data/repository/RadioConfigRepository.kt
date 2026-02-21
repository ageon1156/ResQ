
package org.meshtastic.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.meshtastic.core.datastore.ChannelSetDataSource
import org.meshtastic.core.datastore.LocalConfigDataSource
import org.meshtastic.core.datastore.ModuleConfigDataSource
import org.meshtastic.core.model.util.getChannelUrl
import org.meshtastic.proto.AppOnlyProtos.ChannelSet
import org.meshtastic.proto.ChannelProtos.Channel
import org.meshtastic.proto.ChannelProtos.ChannelSettings
import org.meshtastic.proto.ClientOnlyProtos.DeviceProfile
import org.meshtastic.proto.ConfigProtos.Config
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import org.meshtastic.proto.LocalOnlyProtos.LocalModuleConfig
import org.meshtastic.proto.ModuleConfigProtos.ModuleConfig
import org.meshtastic.proto.deviceProfile
import javax.inject.Inject

class RadioConfigRepository
@Inject
constructor(
    private val nodeDB: NodeRepository,
    private val channelSetDataSource: ChannelSetDataSource,
    private val localConfigDataSource: LocalConfigDataSource,
    private val moduleConfigDataSource: ModuleConfigDataSource,
) {

    val channelSetFlow: Flow<ChannelSet> = channelSetDataSource.channelSetFlow

    suspend fun clearChannelSet() {
        channelSetDataSource.clearChannelSet()
    }

    suspend fun replaceAllSettings(settingsList: List<ChannelSettings>) {
        channelSetDataSource.replaceAllSettings(settingsList)
    }

    suspend fun updateChannelSettings(channel: Channel) = channelSetDataSource.updateChannelSettings(channel)

    val localConfigFlow: Flow<LocalConfig> = localConfigDataSource.localConfigFlow

    suspend fun clearLocalConfig() {
        localConfigDataSource.clearLocalConfig()
    }

    suspend fun setLocalConfig(config: Config) {
        localConfigDataSource.setLocalConfig(config)
        if (config.hasLora()) channelSetDataSource.setLoraConfig(config.lora)
    }

    val moduleConfigFlow: Flow<LocalModuleConfig> = moduleConfigDataSource.moduleConfigFlow

    suspend fun clearLocalModuleConfig() {
        moduleConfigDataSource.clearLocalModuleConfig()
    }

    suspend fun setLocalModuleConfig(config: ModuleConfig) {
        moduleConfigDataSource.setLocalModuleConfig(config)
    }

    val deviceProfileFlow: Flow<DeviceProfile> =
        combine(nodeDB.ourNodeInfo, channelSetFlow, localConfigFlow, moduleConfigFlow) {
                node,
                channels,
                localConfig,
                localModuleConfig,
            ->
            deviceProfile {
                node?.user?.let {
                    longName = it.longName
                    shortName = it.shortName
                }
                channelUrl = channels.getChannelUrl().toString()
                config = localConfig
                moduleConfig = localModuleConfig
                if (node != null && localConfig.position.fixedPosition) {
                    fixedPosition = node.position
                }
            }
        }
}

