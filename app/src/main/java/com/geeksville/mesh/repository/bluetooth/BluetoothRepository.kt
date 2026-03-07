

package com.geeksville.mesh.repository.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import co.touchlab.kermit.Logger
import com.geeksville.mesh.repository.radio.BleConstants.BLE_NAME_PATTERN
import com.geeksville.mesh.repository.radio.BleConstants.BTM_SERVICE_UUID
import com.geeksville.mesh.util.registerReceiverCompat
import dagger.Lazy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.distinctByPeripheral
import no.nordicsemi.kotlin.ble.core.Manager
import org.meshtastic.core.common.hasBluetoothPermission
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.di.ProcessLifecycle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@Singleton
class BluetoothRepository
@Inject
constructor(
    private val application: Application,
    private val bluetoothBroadcastReceiverLazy: Lazy<BluetoothBroadcastReceiver>,
    private val dispatchers: CoroutineDispatchers,
    @ProcessLifecycle private val processLifecycle: Lifecycle,
    private val centralManager: CentralManager,
) {
    private val _state =
        MutableStateFlow(
            BluetoothState(

                hasPermissions = true,
            ),
        )
    val state: StateFlow<BluetoothState> = _state.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<Peripheral>>(emptyList())
    val scannedDevices: StateFlow<List<Peripheral>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null

    init {
        processLifecycle.coroutineScope.launch(dispatchers.default) {
            updateBluetoothState()
            bluetoothBroadcastReceiverLazy.get().let { receiver ->
                application.registerReceiverCompat(receiver, receiver.intentFilter)
            }
        }
    }

    fun refreshState() {
        processLifecycle.coroutineScope.launch(dispatchers.default) { updateBluetoothState() }
    }

    fun isValid(bleAddress: String): Boolean = BluetoothAdapter.checkBluetoothAddress(bleAddress)

    @OptIn(ExperimentalUuidApi::class)
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning.value) return

        scanJob?.cancel()
        _scannedDevices.value = emptyList()

        scanJob =
            processLifecycle.coroutineScope.launch(dispatchers.default) {
                centralManager
                    .scan(5.seconds) { ServiceUuid(BTM_SERVICE_UUID.toKotlinUuid()) }
                    .distinctByPeripheral()
                    .map { it.peripheral }
                    .onStart { _isScanning.value = true }
                    .onCompletion { _isScanning.value = false }
                    .catch { ex ->
                        Logger.w(ex) { "Bluetooth scan failed" }
                        _isScanning.value = false
                    }
                    .collect { peripheral ->
                        
                        val currentList = _scannedDevices.value
                        _scannedDevices.value =
                            (currentList.filterNot { it.address == peripheral.address } + peripheral)
                    }
            }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    suspend fun bond(peripheral: Peripheral) {
        peripheral.createBond()
        updateBluetoothState()
    }

    @SuppressLint("MissingPermission")
    fun isBonded(address: String): Boolean =
        if (application.hasBluetoothPermission()) {
            centralManager.getBondedPeripherals().any { it.address == address }
        } else {
            false
        }

    @OptIn(ExperimentalUuidApi::class)
    internal suspend fun updateBluetoothState() {
        val hasPerms = application.hasBluetoothPermission()
        val enabled = centralManager.state.value == Manager.State.POWERED_ON
        val newState =
            BluetoothState(
                hasPermissions = hasPerms,
                enabled = enabled,
                bondedDevices = getBondedAppPeripherals(enabled),
            )

        _state.emit(newState)
        Logger.d { "Detected our bluetooth access=$newState" }
    }

    @SuppressLint("MissingPermission")
    private fun getBondedAppPeripherals(enabled: Boolean): List<Peripheral> =
        if (enabled && application.hasBluetoothPermission()) {
            centralManager.getBondedPeripherals().filter(::isMatchingPeripheral)
        } else {
            emptyList()
        }

    @OptIn(ExperimentalUuidApi::class)
    private fun isMatchingPeripheral(peripheral: Peripheral): Boolean {
        val nameMatches = peripheral.name?.matches(Regex(BLE_NAME_PATTERN)) ?: false
        val hasRequiredService =
            peripheral.services(listOf(BTM_SERVICE_UUID.toKotlinUuid())).value?.isNotEmpty() ?: false

        return nameMatches || hasRequiredService
    }
}

