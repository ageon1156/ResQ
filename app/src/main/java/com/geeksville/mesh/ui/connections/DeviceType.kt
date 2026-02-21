

package com.geeksville.mesh.ui.connections

import com.geeksville.mesh.model.NO_DEVICE_SELECTED

enum class DeviceType {
    BLE,
    TCP,
    USB,
    ;

    companion object {
        fun fromAddress(address: String): DeviceType? = when (address.firstOrNull()) {
            'x' -> BLE
            's' -> USB
            't' -> TCP
            'm' -> USB 
            'n' ->
                when (address) {
                    NO_DEVICE_SELECTED -> null
                    else -> null
                }

            else -> null
        }
    }
}

