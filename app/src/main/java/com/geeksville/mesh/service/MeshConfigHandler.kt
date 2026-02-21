
package com.geeksville.mesh.service

import com.geeksville.mesh.concurrent.handledLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.proto.ChannelProtos
import org.meshtastic.proto.ConfigProtos
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import org.meshtastic.proto.LocalOnlyProtos.LocalModuleConfig
import org.meshtastic.proto.ModuleConfigProtos
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshConfigHandler
@Inject
constructor(
    private val radioConfigRepository: RadioConfigRepository,
    private val serviceRepository: ServiceRepository,
    private val nodeManager: MeshNodeManager,
) {
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _localConfig = MutableStateFlow(LocalConfig.getDefaultInstance())
    val localConfig = _localConfig.asStateFlow()

    private val _moduleConfig = MutableStateFlow(LocalModuleConfig.getDefaultInstance())
    val moduleConfig = _moduleConfig.asStateFlow()

    private val configTotal = ConfigProtos.Config.getDescriptor().fields.size
    private val moduleTotal = ModuleConfigProtos.ModuleConfig.getDescriptor().fields.size

    fun start(scope: CoroutineScope) {
        this.scope = scope
        radioConfigRepository.localConfigFlow.onEach { _localConfig.value = it }.launchIn(scope)

        radioConfigRepository.moduleConfigFlow.onEach { _moduleConfig.value = it }.launchIn(scope)
    }

    fun handleDeviceConfig(config: ConfigProtos.Config) {
        scope.handledLaunch { radioConfigRepository.setLocalConfig(config) }
        val configCount = _localConfig.value.allFields.size
        serviceRepository.setStatusMessage("Device config ($configCount / $configTotal)")
    }

    fun handleModuleConfig(config: ModuleConfigProtos.ModuleConfig) {
        scope.handledLaunch { radioConfigRepository.setLocalModuleConfig(config) }
        val moduleCount = _moduleConfig.value.allFields.size
        serviceRepository.setStatusMessage("Module config ($moduleCount / $moduleTotal)")
    }

    fun handleChannel(ch: ChannelProtos.Channel) {
        
        scope.handledLaunch { radioConfigRepository.updateChannelSettings(ch) }

        val mi = nodeManager.getMyNodeInfo()
        if (mi != null) {
            serviceRepository.setStatusMessage("Channels (${ch.index + 1} / ${mi.maxChannels})")
        } else {
            serviceRepository.setStatusMessage("Channels (${ch.index + 1})")
        }
    }
}

