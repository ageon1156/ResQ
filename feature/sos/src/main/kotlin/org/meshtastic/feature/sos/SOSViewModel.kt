

package org.meshtastic.feature.sos

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.RemoteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.meshtastic.core.common.hasLocationPermission
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.Portnums
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

sealed interface SOSUiState {
    data object Ready : SOSUiState
    data object Sending : SOSUiState
    data class Sent(val locationIncluded: Boolean) : SOSUiState
    data class Error(val message: String) : SOSUiState
}

@HiltViewModel
class SOSViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serviceRepository: ServiceRepository,
    private val nodeRepository: NodeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SOSUiState>(SOSUiState.Ready)
    val uiState: StateFlow<SOSUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = serviceRepository.connectionState

    private val _showConfirmDialog = MutableStateFlow(false)
    val showConfirmDialog: StateFlow<Boolean> = _showConfirmDialog.asStateFlow()

    private val _lastSentTime = MutableStateFlow<Long?>(null)
    val lastSentTime: StateFlow<Long?> = _lastSentTime.asStateFlow()

    fun requestSOS() {
        _showConfirmDialog.value = true
    }

    fun dismissConfirmation() {
        _showConfirmDialog.value = false
    }

    fun confirmAndSendSOS() {
        _showConfirmDialog.value = false
        viewModelScope.launch {
            _uiState.value = SOSUiState.Sending
            try {
                val location = getLastKnownLocation()
                val messageText = formatSOSMessage(location)
                val packet = DataPacket(
                    to = DataPacket.ID_BROADCAST,
                    bytes = messageText.toByteArray(Charsets.UTF_8),
                    dataType = Portnums.PortNum.TEXT_MESSAGE_APP_VALUE,
                    channel = 0,
                    hopLimit = 7, 
                    wantAck = true,
                    priority = MeshProtos.MeshPacket.Priority.ALERT_VALUE, 
                )
                serviceRepository.meshService?.send(packet)
                    ?: run {
                        _uiState.value = SOSUiState.Error("Mesh service not available")
                        return@launch
                    }
                _lastSentTime.value = System.currentTimeMillis()
                _uiState.value = SOSUiState.Sent(locationIncluded = location != null)
            } catch (ex: RemoteException) {
                _uiState.value = SOSUiState.Error(ex.message ?: "Send failed")
            } catch (ex: Exception) {
                _uiState.value = SOSUiState.Error(ex.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _uiState.value = SOSUiState.Ready
    }

    fun getLocationText(): String? {
        val location = getLastKnownLocation() ?: return null
        return "%.4f, %.4f".format(location.latitude, location.longitude)
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        if (!context.hasLocationPermission()) return null
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    private fun formatSOSMessage(location: Location?): String {
        val longName = nodeRepository.ourNodeInfo.value?.user?.longName

        return buildString {
            if (longName.isNullOrBlank()) {
                appendLine("SOS EMERGENCY")
            } else {
                appendLine("SOS EMERGENCY from $longName")
            }
            if (location != null) {
                appendLine("Location: %.4f, %.4f".format(location.latitude, location.longitude))
                append(
                    "https://maps.google.com/maps?q=%.4f,%.4f".format(
                        location.latitude,
                        location.longitude,
                    )
                )
            } else {
                append("Location: unavailable")
            }
        }
    }
}

