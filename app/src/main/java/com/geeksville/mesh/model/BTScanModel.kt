

package com.geeksville.mesh.model

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.RemoteException
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.geeksville.mesh.repository.bluetooth.BluetoothRepository
import com.geeksville.mesh.repository.network.NetworkRepository
import com.geeksville.mesh.repository.network.NetworkRepository.Companion.toAddressString
import com.geeksville.mesh.repository.radio.RadioInterfaceService
import com.geeksville.mesh.repository.usb.UsbRepository
import com.geeksville.mesh.service.MeshService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.datastore.RecentAddressesDataSource
import org.meshtastic.core.datastore.model.RecentAddress
import org.meshtastic.core.model.util.anonymize
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.meshtastic
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class BTScanModel
@Inject
constructor(
    private val application: Application,
    private val serviceRepository: ServiceRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val usbRepository: UsbRepository,
    private val usbManagerLazy: dagger.Lazy<UsbManager>,
    private val networkRepository: NetworkRepository,
    private val radioInterfaceService: RadioInterfaceService,
    private val recentAddressesDataSource: RecentAddressesDataSource,
    private val nodeRepository: NodeRepository,
) : ViewModel() {
    private val context: Context
        get() = application.applicationContext

    val showMockInterface: StateFlow<Boolean> =
        MutableStateFlow(radioInterfaceService.isMockInterface()).asStateFlow()

    val errorText = MutableLiveData<String?>(null)
    private val bondedBleDevicesFlow: StateFlow<List<DeviceListEntry.Ble>> =
        bluetoothRepository.state
            .map { ble -> ble.bondedDevices.map { DeviceListEntry.Ble(it) } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val scannedBleDevicesFlow: StateFlow<List<DeviceListEntry.Ble>> =
        bluetoothRepository.scannedDevices
            .map { peripherals -> peripherals.map { DeviceListEntry.Ble(it) } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val processedDiscoveredTcpDevicesFlow: StateFlow<List<DeviceListEntry.Tcp>> =
        combine(networkRepository.resolvedList, recentAddressesDataSource.recentAddresses) { tcpServices, recentList ->
            val recentMap = recentList.associateBy({ it.address }, { it.name })
            tcpServices
                .map { service ->
                    val address = "t${service.toAddressString()}"
                    val txtRecords = service.attributes 
                    val shortNameBytes = txtRecords["shortname"]
                    val idBytes = txtRecords["id"]

                    val shortName =
                        shortNameBytes?.let { String(it, Charsets.UTF_8) } ?: getString(Res.string.meshtastic)
                    val deviceId = idBytes?.let { String(it, Charsets.UTF_8) }?.replace("!", "")
                    var displayName = recentMap[address] ?: shortName
                    if (deviceId != null && displayName.split("_").any { it == deviceId }) {
                        displayName += "_$deviceId"
                    }
                    DeviceListEntry.Tcp(displayName, address)
                }
                .sortedBy { it.name }
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _bleNameCache = MutableStateFlow<Map<String, String>>(emptyMap())

    val bleDevicesForUi: StateFlow<List<DeviceListEntry>> =
        combine(bondedBleDevicesFlow, scannedBleDevicesFlow, _bleNameCache) { bonded, scanned, cache ->
            val bondedAddresses = bonded.map { it.fullAddress }.toSet()
            val uniqueScanned = scanned.filterNot { it.fullAddress in bondedAddresses }
            (bonded + uniqueScanned)
                .map { entry ->
                    val cachedName = cache[entry.fullAddress]
                    if (cachedName != null) DeviceListEntry.Ble(entry.peripheral, cachedName) else entry
                }
                .sortedBy { it.name }
        }
            .stateInWhileSubscribed(initialValue = emptyList())

    private val usbDevicesFlow: StateFlow<List<DeviceListEntry.Usb>> =
        usbRepository.serialDevicesWithDrivers
            .map { usb -> usb.map { (_, d) -> DeviceListEntry.Usb(radioInterfaceService, usbManagerLazy.get(), d) } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mockDevice = DeviceListEntry.Mock("Demo Mode")

    val usbDevicesForUi: StateFlow<List<DeviceListEntry>> =
        combine(usbDevicesFlow, showMockInterface) { usb, showMock ->
            usb + if (showMock) listOf(mockDevice) else emptyList()
        }
            .stateInWhileSubscribed(initialValue = if (showMockInterface.value) listOf(mockDevice) else emptyList())

    private val filteredRecentTcpDevicesFlow: StateFlow<List<DeviceListEntry.Tcp>> =
        combine(recentAddressesDataSource.recentAddresses, processedDiscoveredTcpDevicesFlow) {
                recentList,
                discoveredDevices,
            ->
            val discoveredDeviceAddresses = discoveredDevices.map { it.fullAddress }.toSet()
            recentList
                .filterNot { recentAddress -> discoveredDeviceAddresses.contains(recentAddress.address) }
                .map { recentAddress -> DeviceListEntry.Tcp(recentAddress.name, recentAddress.address) }
                .sortedBy { it.name }
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val discoveredTcpDevicesForUi: StateFlow<List<DeviceListEntry>> =
        processedDiscoveredTcpDevicesFlow.stateInWhileSubscribed(initialValue = listOf())

    val recentTcpDevicesForUi: StateFlow<List<DeviceListEntry>> =
        filteredRecentTcpDevicesFlow.stateInWhileSubscribed(initialValue = listOf())

    val selectedAddressFlow: StateFlow<String?> = radioInterfaceService.currentDeviceAddressFlow

    val selectedNotNullFlow: StateFlow<String> =
        selectedAddressFlow
            .map { it ?: NO_DEVICE_SELECTED }
            .stateInWhileSubscribed(initialValue = selectedAddressFlow.value ?: NO_DEVICE_SELECTED)

    val spinner: StateFlow<Boolean> = bluetoothRepository.isScanning

    init {
        serviceRepository.statusMessage.onEach { errorText.value = it }.launchIn(viewModelScope)

        combine(bondedBleDevicesFlow, scannedBleDevicesFlow, nodeRepository.nodeDBbyNum) {
                bonded, scanned, nodeDB -> Triple(bonded, scanned, nodeDB)
        }.onEach { (bonded, scanned, nodeDB) ->
            if (nodeDB.isEmpty()) return@onEach
            val bondedAddresses = bonded.map { it.fullAddress }.toSet()
            val allDevices = bonded + scanned.filterNot { it.fullAddress in bondedAddresses }
            val updates = allDevices.mapNotNull { entry ->
                val suffix = entry.peripheral.name?.substringAfterLast("_")
                    ?.takeIf { it.length == 4 }?.lowercase()
                val longName = suffix?.let { s ->
                    nodeDB.values.find { node ->
                        !node.isUnknownUser &&
                            node.user.id.removePrefix("!").takeLast(4).lowercase() == s
                    }?.user?.longName?.takeIf { it.isNotBlank() }
                }
                if (longName != null) entry.fullAddress to longName else null
            }.toMap()
            if (updates.isNotEmpty()) _bleNameCache.update { it + updates }
        }.launchIn(viewModelScope)

        Logger.d { "BTScanModel created" }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothRepository.stopScan()
        Logger.d { "BTScanModel cleared" }
    }

    fun setErrorText(text: String) {
        errorText.value = text
    }

    fun stopScan() {
        Logger.d { "stopping scan" }
        bluetoothRepository.stopScan()
    }

    fun refreshPermissions() {
        bluetoothRepository.refreshState()
    }

    fun startScan() {
        Logger.d { "starting ble scan" }
        bluetoothRepository.startScan()
    }

    private fun changeDeviceAddress(address: String) {
        try {
            serviceRepository.meshService?.let { service -> MeshService.changeDeviceAddress(context, service, address) }
        } catch (ex: RemoteException) {
            Logger.e(ex) { "changeDeviceSelection failed, probably it is shutting down" }
        }
    }

    private fun requestBonding(entry: DeviceListEntry.Ble) {
        Logger.i { "Starting bonding for ${entry.peripheral.address.anonymize}" }
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                bluetoothRepository.bond(entry.peripheral)
                Logger.i { "Bonding complete for ${entry.peripheral.address.anonymize}, selecting device..." }
                changeDeviceAddress(entry.fullAddress)
            } catch (ex: SecurityException) {
                Logger.e(ex) { "Bonding failed for ${entry.peripheral.address.anonymize} Permissions not granted" }
                serviceRepository.setErrorMessage("Bonding failed: ${ex.message} Permissions not granted")
            } catch (ex: Exception) {
                Logger.e(ex) { "Bonding failed for ${entry.peripheral.address.anonymize}" }
                serviceRepository.setErrorMessage("Bonding failed: ${ex.message}")
            }
        }
    }

    private fun requestPermission(it: DeviceListEntry.Usb) {
        usbRepository
            .requestPermission(it.driver.device)
            .onEach { granted ->
                if (granted) {
                    Logger.i { "User approved USB access" }
                    changeDeviceAddress(it.fullAddress)
                } else {
                    Logger.e { "USB permission denied for device ${it.address}" }
                }
            }
            .launchIn(viewModelScope)
    }

    fun addRecentAddress(address: String, name: String) {
        if (!address.startsWith("t")) return
        viewModelScope.launch { recentAddressesDataSource.add(RecentAddress(address, name)) }
    }

    fun removeRecentAddress(address: String) {
        viewModelScope.launch { recentAddressesDataSource.remove(address) }
    }

    fun onSelected(it: DeviceListEntry): Boolean = when (it) {
        is DeviceListEntry.Ble -> {
            if (it.bonded) {
                changeDeviceAddress(it.fullAddress)
                true
            } else {
                requestBonding(it)
                false
            }
        }
        is DeviceListEntry.Usb -> {
            if (it.bonded) {
                changeDeviceAddress(it.fullAddress)
                true
            } else {
                requestPermission(it)
                false
            }
        }
        is DeviceListEntry.Tcp -> {
            viewModelScope.launch {
                addRecentAddress(it.fullAddress, it.name)
                changeDeviceAddress(it.fullAddress)
            }
            true
        }
        is DeviceListEntry.Mock -> {
            changeDeviceAddress(it.fullAddress)
            true
        }
    }

    fun disconnect() {
        changeDeviceAddress(NO_DEVICE_SELECTED)
    }
}

const val NO_DEVICE_SELECTED = "n"

