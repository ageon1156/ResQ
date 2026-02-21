

package com.geeksville.mesh.ui.connections

import androidx.lifecycle.ViewModel
import com.geeksville.mesh.repository.bluetooth.BluetoothRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.database.entity.MyNodeEntity
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.prefs.ui.UiPrefs
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel
@Inject
constructor(
    radioConfigRepository: RadioConfigRepository,
    serviceRepository: ServiceRepository,
    nodeRepository: NodeRepository,
    bluetoothRepository: BluetoothRepository,
    private val uiPrefs: UiPrefs,
) : ViewModel() {

    val localConfig: StateFlow<LocalConfig> =
        radioConfigRepository.localConfigFlow.stateInWhileSubscribed(initialValue = LocalConfig.getDefaultInstance())

    val connectionState = serviceRepository.connectionState

    val myNodeInfo: StateFlow<MyNodeEntity?> = nodeRepository.myNodeInfo

    val ourNodeInfo: StateFlow<Node?> = nodeRepository.ourNodeInfo

    val bluetoothState = bluetoothRepository.state

    private val _hasShownNotPairedWarning = MutableStateFlow(uiPrefs.hasShownNotPairedWarning)
    val hasShownNotPairedWarning: StateFlow<Boolean> = _hasShownNotPairedWarning.asStateFlow()

    fun suppressNoPairedWarning() {
        _hasShownNotPairedWarning.value = true
        uiPrefs.hasShownNotPairedWarning = true
    }
}

