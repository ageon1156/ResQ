

package com.geeksville.mesh.repository.bluetooth

import no.nordicsemi.kotlin.ble.client.android.Peripheral
import org.meshtastic.core.model.util.anonymize

data class BluetoothState(
    
    val hasPermissions: Boolean = false,
    
    val enabled: Boolean = false,
    
    val bondedDevices: List<Peripheral> = emptyList(),
) {
    override fun toString(): String =
        "BluetoothState(hasPermissions=$hasPermissions, enabled=$enabled, bondedDevices=${bondedDevices.map {
            it.anonymize
        }})"
}

