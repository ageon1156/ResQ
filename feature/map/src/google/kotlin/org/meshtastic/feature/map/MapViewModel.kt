
package org.meshtastic.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.meshtastic.core.common.BuildConfigProvider
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.PacketRepository
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.navigation.MapRoutes
import org.meshtastic.core.prefs.map.MapPrefs
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.LocalOnlyProtos.LocalConfig
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class MapViewModel
@Inject
constructor(
    mapPrefs: MapPrefs,
    packetRepository: PacketRepository,
    private val nodeRepository: NodeRepository,
    serviceRepository: ServiceRepository,
    radioConfigRepository: RadioConfigRepository,
    buildConfigProvider: BuildConfigProvider,
    savedStateHandle: SavedStateHandle,
) : BaseMapViewModel(mapPrefs, nodeRepository, packetRepository, serviceRepository) {

    private val _selectedWaypointId = MutableStateFlow(savedStateHandle.toRoute<MapRoutes.Map>().waypointId)
    val selectedWaypointId: StateFlow<Int?> = _selectedWaypointId.asStateFlow()

    var mapStyleId: Int
        get() = mapPrefs.mapStyle
        set(value) {
            mapPrefs.mapStyle = value
        }

    val lastDownloadLat: Float get() = mapPrefs.lastDownloadLat
    val lastDownloadLng: Float get() = mapPrefs.lastDownloadLng

    fun updateLastDownloadCenter(lat: Double, lng: Double) {
        mapPrefs.lastDownloadLat = lat.toFloat()
        mapPrefs.lastDownloadLng = lng.toFloat()
    }

    val localConfig =
        radioConfigRepository.localConfigFlow.stateInWhileSubscribed(initialValue = LocalConfig.getDefaultInstance())

    val config
        get() = localConfig.value

    val applicationId = buildConfigProvider.applicationId

    override fun getUser(userId: String?) = nodeRepository.getUser(userId ?: DataPacket.ID_BROADCAST)
}
