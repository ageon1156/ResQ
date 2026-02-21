
package com.geeksville.mesh.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.meshtastic.core.service.ConnectionState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionStateHandler @Inject constructor() {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    fun setState(state: ConnectionState) {
        _connectionState.value = state
    }
}

