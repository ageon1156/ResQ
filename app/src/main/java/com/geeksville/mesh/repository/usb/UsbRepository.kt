

package com.geeksville.mesh.repository.usb

import android.app.Application
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.geeksville.mesh.util.registerReceiverCompat
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.di.ProcessLifecycle
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class UsbRepository
@Inject
constructor(
    private val application: Application,
    private val dispatchers: CoroutineDispatchers,
    @ProcessLifecycle private val processLifecycle: Lifecycle,
    private val usbBroadcastReceiverLazy: dagger.Lazy<UsbBroadcastReceiver>,
    private val usbManagerLazy: dagger.Lazy<UsbManager?>,
    private val usbSerialProberLazy: dagger.Lazy<UsbSerialProber>,
) {
    private val _serialDevices = MutableStateFlow(emptyMap<String, UsbDevice>())

    @Suppress("unused") 
    val serialDevices = _serialDevices.asStateFlow()

    @Suppress("unused") 
    val serialDevicesWithDrivers =
        _serialDevices
            .mapLatest { serialDevices ->
                val serialProber = usbSerialProberLazy.get()
                buildMap {
                    serialDevices.forEach { (k, v) -> serialProber.probeDevice(v)?.let { driver -> put(k, driver) } }
                }
            }
            .stateIn(processLifecycle.coroutineScope, SharingStarted.Eagerly, emptyMap())

    @Suppress("unused") 
    val serialDevicesWithPermission =
        _serialDevices
            .mapLatest { serialDevices ->
                usbManagerLazy.get()?.let { usbManager ->
                    serialDevices.filterValues { device -> usbManager.hasPermission(device) }
                } ?: emptyMap()
            }
            .stateIn(processLifecycle.coroutineScope, SharingStarted.Eagerly, emptyMap())

    init {
        processLifecycle.coroutineScope.launch(dispatchers.default) {
            refreshStateInternal()
            usbBroadcastReceiverLazy.get().let { receiver ->
                application.registerReceiverCompat(receiver, receiver.intentFilter)
            }
        }
    }

    fun createSerialConnection(device: UsbSerialDriver, listener: SerialConnectionListener): SerialConnection =
        SerialConnectionImpl(usbManagerLazy, device, listener)

    fun requestPermission(device: UsbDevice): Flow<Boolean> =
        usbManagerLazy.get()?.requestPermission(application, device) ?: emptyFlow()

    fun refreshState() {
        processLifecycle.coroutineScope.launch(dispatchers.default) { refreshStateInternal() }
    }

    private suspend fun refreshStateInternal() =
        withContext(dispatchers.default) { _serialDevices.emit(usbManagerLazy.get()?.deviceList ?: emptyMap()) }
}

