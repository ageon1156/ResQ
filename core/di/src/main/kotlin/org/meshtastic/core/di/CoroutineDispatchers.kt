

package org.meshtastic.core.di

import kotlinx.coroutines.CoroutineDispatcher

data class CoroutineDispatchers(
    val io: CoroutineDispatcher,
    val main: CoroutineDispatcher,
    val default: CoroutineDispatcher,
)

