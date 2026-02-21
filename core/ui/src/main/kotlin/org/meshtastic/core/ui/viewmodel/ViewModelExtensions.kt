

@file:Suppress("Wrapping", "UnusedImports", "SpacingAroundColon")

package org.meshtastic.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

context(viewModel: ViewModel)
fun <T> Flow<T>.stateInWhileSubscribed(initialValue: T, stopTimeout: Duration = 5.seconds): StateFlow<T> = stateIn(
    scope = viewModel.viewModelScope,
    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = stopTimeout.inWholeMilliseconds),
    initialValue = initialValue,
)

