

package com.geeksville.mesh.ui.sharing

import android.net.Uri
import android.os.RemoteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.meshtastic.core.analytics.DataPair
import org.meshtastic.core.analytics.platform.PlatformAnalytics
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.model.util.toChannelSet
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.ui.util.getChannelList
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.AppOnlyProtos
import org.meshtastic.proto.ChannelProtos
import org.meshtastic.proto.ConfigProtos.Config
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import org.meshtastic.proto.channelSet
import org.meshtastic.proto.config
import org.meshtastic.proto.copy
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel
@Inject
constructor(
    private val serviceRepository: ServiceRepository,
    private val radioConfigRepository: RadioConfigRepository,
    private val analytics: PlatformAnalytics,
) : ViewModel() {

    val connectionState = serviceRepository.connectionState

    val localConfig =
        radioConfigRepository.localConfigFlow.stateInWhileSubscribed(initialValue = LocalConfig.getDefaultInstance())

    val channels = radioConfigRepository.channelSetFlow.stateInWhileSubscribed(initialValue = channelSet {})

    val isManaged: Boolean
        get() = localConfig.value.security.isManaged

    var txEnabled: Boolean
        get() = localConfig.value.lora.txEnabled
        set(value) {
            updateLoraConfig { it.copy { txEnabled = value } }
        }

    var region: Config.LoRaConfig.RegionCode
        get() = localConfig.value.lora.region
        set(value) {
            updateLoraConfig { it.copy { region = value } }
        }

    private val _requestChannelSet = MutableStateFlow<AppOnlyProtos.ChannelSet?>(null)
    val requestChannelSet: StateFlow<AppOnlyProtos.ChannelSet?>
        get() = _requestChannelSet

    fun requestChannelUrl(url: Uri, onError: () -> Unit) = runCatching { _requestChannelSet.value = url.toChannelSet() }
        .onFailure { _ ->
            onError()
        }

    fun clearRequestChannelUrl() {
        _requestChannelSet.value = null
    }

    fun setChannels(channelSet: AppOnlyProtos.ChannelSet) = viewModelScope.launch {
        getChannelList(channelSet.settingsList, channels.value.settingsList).forEach(::setChannel)
        radioConfigRepository.replaceAllSettings(channelSet.settingsList)

        val newConfig = config { lora = channelSet.loraConfig }
        if (localConfig.value.lora != newConfig.lora) setConfig(newConfig)
    }

    fun setChannel(channel: ChannelProtos.Channel) {
        try {
            serviceRepository.meshService?.setChannel(channel.toByteArray())
        } catch (_: RemoteException) {
        }
    }

    fun setConfig(config: Config) {
        try {
            serviceRepository.meshService?.setConfig(config.toByteArray())
        } catch (_: RemoteException) {
        }
    }

    fun trackShare() {
        analytics.track("share", DataPair("content_type", "channel"))
    }

    private inline fun updateLoraConfig(crossinline body: (Config.LoRaConfig) -> Config.LoRaConfig) {
        val data = body(localConfig.value.lora)
        setConfig(config { lora = data })
    }
}

