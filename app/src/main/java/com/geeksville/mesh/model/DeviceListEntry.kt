

package com.geeksville.mesh.model

import android.hardware.usb.UsbManager
import com.geeksville.mesh.repository.radio.InterfaceId
import com.geeksville.mesh.repository.radio.RadioInterfaceService
import com.hoho.android.usbserial.driver.UsbSerialDriver
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.BondState
import org.meshtastic.core.model.util.anonymize

sealed class DeviceListEntry(open val name: String, open val fullAddress: String, open val bonded: Boolean) {
    val address: String
        get() = fullAddress.substring(1)

    override fun toString(): String =
        "DeviceListEntry(name=${name.anonymize}, addr=${address.anonymize}, bonded=$bonded)"

    @Suppress("MissingPermission")
    data class Ble(val peripheral: Peripheral, val nameOverride: String? = null) :
        DeviceListEntry(
            name = nameOverride ?: peripheral.name ?: "unnamed-${peripheral.address}",
            fullAddress = "x${peripheral.address}",
            bonded = peripheral.bondState.value == BondState.BONDED,
        )

    data class Usb(
        private val radioInterfaceService: RadioInterfaceService,
        private val usbManager: UsbManager,
        val driver: UsbSerialDriver,
    ) : DeviceListEntry(
        name = driver.device.deviceName,
        fullAddress = radioInterfaceService.toInterfaceAddress(InterfaceId.SERIAL, driver.device.deviceName),
        bonded = usbManager.hasPermission(driver.device),
    )

    data class Tcp(override val name: String, override val fullAddress: String) :
        DeviceListEntry(name, fullAddress, true)

    data class Mock(override val name: String) : DeviceListEntry(name, "m", true)
}

