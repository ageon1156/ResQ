

package org.meshtastic.feature.map.node

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.toList
import org.meshtastic.core.common.BuildConfigProvider
import org.meshtastic.core.data.repository.MeshLogRepository
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.navigation.NodesRoutes
import org.meshtastic.core.prefs.map.MapPrefs
import org.meshtastic.core.ui.util.toPosition
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.map.model.CustomTileSource
import org.meshtastic.proto.MeshProtos.Position
import org.meshtastic.proto.Portnums.PortNum
import javax.inject.Inject

@HiltViewModel
class NodeMapViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    nodeRepository: NodeRepository,
    meshLogRepository: MeshLogRepository,
    buildConfigProvider: BuildConfigProvider,
    private val mapPrefs: MapPrefs,
) : ViewModel() {
    private val destNum = savedStateHandle.toRoute<NodesRoutes.NodeDetailGraph>().destNum

    val node =
        nodeRepository.nodeDBbyNum
            .mapLatest { it[destNum] }
            .distinctUntilChanged()
            .stateInWhileSubscribed(initialValue = null)

    val applicationId = buildConfigProvider.applicationId

    val positionLogs: StateFlow<List<Position>> =
        meshLogRepository
            .getMeshPacketsFrom(destNum!!, PortNum.POSITION_APP_VALUE)
            .map { packets ->
                packets
                    .mapNotNull { it.toPosition() }
                    .asFlow()
                    .distinctUntilChanged { old, new ->
                        old.time == new.time || (old.latitudeI == new.latitudeI && old.longitudeI == new.longitudeI)
                    }
                    .toList()
            }
            .stateInWhileSubscribed(initialValue = emptyList())

    val tileSource
        get() = CustomTileSource.getTileSource(mapPrefs.mapStyle)
}

