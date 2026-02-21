

package org.meshtastic.core.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.service.ServiceAction
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.AdminProtos
import javax.inject.Inject

@HiltViewModel
class SharedContactViewModel
@Inject
constructor(
    nodeRepository: NodeRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    val unfilteredNodes: StateFlow<List<Node>> =
        nodeRepository.getNodes().stateInWhileSubscribed(initialValue = emptyList())

    fun addSharedContact(sharedContact: AdminProtos.SharedContact) =
        viewModelScope.launch { serviceRepository.onServiceAction(ServiceAction.ImportContact(sharedContact)) }
}

