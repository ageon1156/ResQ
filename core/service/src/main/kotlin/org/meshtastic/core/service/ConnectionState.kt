

package org.meshtastic.core.service

sealed class ConnectionState {
    
    data object Disconnected : ConnectionState()

    data object Connecting : ConnectionState()

    data object Connected : ConnectionState()

    data object DeviceSleep : ConnectionState()

    fun isConnected() = this == Connected

    fun isConnecting() = this == Connecting

    fun isDisconnected() = this == Disconnected

    fun isDeviceSleep() = this == DeviceSleep
}

