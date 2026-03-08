

package org.meshtastic.core.ui.qr

import android.os.RemoteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.ui.util.getChannelList
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.AppOnlyProtos
import org.meshtastic.proto.ChannelProtos
import org.meshtastic.proto.ConfigProtos.Config
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import org.meshtastic.proto.channelSet
import org.meshtastic.proto.config
import javax.inject.Inject

@HiltViewModel
class ScannedQrCodeViewModel
@Inject
constructor(
    private val radioConfigRepository: RadioConfigRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    val channels = radioConfigRepository.channelSetFlow.stateInWhileSubscribed(initialValue = channelSet {})

    private val localConfig =
        radioConfigRepository.localConfigFlow.stateInWhileSubscribed(initialValue = LocalConfig.getDefaultInstance())

    fun setChannels(channelSet: AppOnlyProtos.ChannelSet) = viewModelScope.launch {
        getChannelList(channelSet.settingsList, channels.value.settingsList).forEach(::setChannel)
        radioConfigRepository.replaceAllSettings(channelSet.settingsList)

        val newConfig = config { lora = channelSet.loraConfig }
        if (localConfig.value.lora != newConfig.lora) setConfig(newConfig)
    }

    private fun setChannel(channel: ChannelProtos.Channel) {
        try {
            serviceRepository.meshService?.setChannel(channel.toByteArray())
        } catch (ex: RemoteException) {
        }
    }

    private fun setConfig(config: Config) {
        try {
            serviceRepository.meshService?.setConfig(config.toByteArray())
        } catch (ex: RemoteException) {
        }
    }
}

