

package com.geeksville.mesh.repository.radio

import com.geeksville.mesh.service.RadioNotConnectedException
import no.nordicsemi.kotlin.ble.client.exception.BluetoothUnavailableException
import no.nordicsemi.kotlin.ble.client.exception.ConnectionFailedException
import no.nordicsemi.kotlin.ble.client.exception.InvalidAttributeException
import no.nordicsemi.kotlin.ble.client.exception.OperationFailedException
import no.nordicsemi.kotlin.ble.client.exception.PeripheralNotConnectedException
import no.nordicsemi.kotlin.ble.client.exception.ScanningException
import no.nordicsemi.kotlin.ble.client.exception.ValueDoesNotMatchException
import no.nordicsemi.kotlin.ble.core.ConnectionState
import no.nordicsemi.kotlin.ble.core.exception.BluetoothException
import no.nordicsemi.kotlin.ble.core.exception.GattException
import no.nordicsemi.kotlin.ble.core.exception.ManagerClosedException

sealed class BleError(val message: String, val shouldReconnect: Boolean) {

    data object PeripheralNotFound : BleError("Peripheral not found", shouldReconnect = false)

    class ConnectionFailed(exception: Throwable) :
        BleError("Connection failed: ${exception.message}", shouldReconnect = true)

    class DiscoveryFailed(message: String) : BleError("Discovery failed: $message", shouldReconnect = true)

    class Disconnected(reason: ConnectionState.Disconnected.Reason?) :
        BleError("Disconnected: ${reason ?: "Unknown reason"}", shouldReconnect = true)

    class GattError(exception: GattException) :
        BleError("Gatt exception: ${exception.message}", shouldReconnect = true)

    class BluetoothError(exception: BluetoothException) :
        BleError("Bluetooth exception: ${exception.message}", shouldReconnect = true)

    class ManagerClosed(exception: ManagerClosedException) :
        BleError("Manager closed: ${exception.message}", shouldReconnect = false)

    class OperationFailed(exception: OperationFailedException) :
        BleError("Operation failed: ${exception.message}", shouldReconnect = true)

    class InvalidAttribute(exception: InvalidAttributeException) :
        BleError("Invalid attribute: ${exception.message}", shouldReconnect = false)

    class Scanning(exception: ScanningException) :
        BleError("Scanning error: ${exception.message}", shouldReconnect = true)

    class BluetoothUnavailable(exception: BluetoothUnavailableException) :
        BleError("Bluetooth unavailable: ${exception.message}", shouldReconnect = false)

    class PeripheralNotConnected(exception: PeripheralNotConnectedException) :
        BleError("Peripheral not connected: ${exception.message}", shouldReconnect = true)

    class ValueDoesNotMatch(exception: ValueDoesNotMatchException) :
        BleError("Value does not match: ${exception.message}", shouldReconnect = true)

    class GenericError(exception: Throwable) :
        BleError("An unexpected error occurred: ${exception.message}", shouldReconnect = true)

    companion object {
        fun from(exception: Throwable): BleError = when (exception) {
            is GattException -> {
                when (exception) {
                    is ConnectionFailedException -> ConnectionFailed(exception)
                    is PeripheralNotConnectedException -> PeripheralNotConnected(exception)
                    is OperationFailedException -> OperationFailed(exception)
                    is ValueDoesNotMatchException -> ValueDoesNotMatch(exception)
                    else -> GattError(exception)
                }
            }
            is BluetoothException -> {
                when (exception) {
                    is BluetoothUnavailableException -> BluetoothUnavailable(exception)
                    is InvalidAttributeException -> InvalidAttribute(exception)
                    is ScanningException -> Scanning(exception)
                    else -> BluetoothError(exception)
                }
            }

            is RadioNotConnectedException -> PeripheralNotFound
            is ManagerClosedException -> ManagerClosed(exception)
            else -> GenericError(exception)
        }
    }
}

