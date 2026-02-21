

package org.meshtastic.feature.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.meshtastic.core.datastore.UiPreferencesDataSource
import org.meshtastic.feature.emergency.data.BasicSurvivalTopic
import org.meshtastic.feature.emergency.data.DisasterSurvivalTopic
import org.meshtastic.feature.emergency.data.EmergencyGuideData
import org.meshtastic.feature.emergency.data.EmergencyRepository
import org.meshtastic.feature.emergency.data.FirstAidTopic
import javax.inject.Inject

sealed interface EmergencyUiState {
    data object Loading : EmergencyUiState
    data class Success(val data: EmergencyGuideData) : EmergencyUiState
    data class Error(val message: String) : EmergencyUiState
}

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val emergencyRepository: EmergencyRepository,
    private val uiPreferencesDataSource: UiPreferencesDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Loading)
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    val disclaimerAccepted: StateFlow<Boolean> = uiPreferencesDataSource.emergencyDisclaimerAccepted

    init {
        viewModelScope.launch {
            emergencyRepository.getGuideData()
                .onSuccess { _uiState.value = EmergencyUiState.Success(it) }
                .onFailure { _uiState.value = EmergencyUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun acceptDisclaimer() {
        uiPreferencesDataSource.setEmergencyDisclaimerAccepted(true)
    }

    fun getFirstAidTopic(topicId: String): FirstAidTopic? =
        emergencyRepository.getFirstAidTopic(topicId)

    fun getDisasterSurvivalTopic(topicId: String): DisasterSurvivalTopic? =
        emergencyRepository.getDisasterSurvivalTopic(topicId)

    fun getBasicSurvivalTopic(topicId: String): BasicSurvivalTopic? =
        emergencyRepository.getBasicSurvivalTopic(topicId)
}

