

package org.meshtastic.feature.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.meshtastic.core.data.repository.QuickChatActionRepository
import org.meshtastic.core.database.entity.QuickChatAction
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import javax.inject.Inject

@HiltViewModel
class QuickChatViewModel @Inject constructor(private val quickChatActionRepository: QuickChatActionRepository) :
    ViewModel() {
    val quickChatActions
        get() = quickChatActionRepository.getAllActions().stateInWhileSubscribed(initialValue = emptyList())

    fun updateActionPositions(actions: List<QuickChatAction>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (position in actions.indices) {
                quickChatActionRepository.setItemPosition(actions[position].uuid, position)
            }
        }
    }

    fun addQuickChatAction(action: QuickChatAction) =
        viewModelScope.launch(Dispatchers.IO) { quickChatActionRepository.upsert(action) }

    fun deleteQuickChatAction(action: QuickChatAction) =
        viewModelScope.launch(Dispatchers.IO) { quickChatActionRepository.delete(action) }
}

