
package com.geeksville.mesh.repository.radio

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import co.touchlab.kermit.Logger
import com.geeksville.mesh.concurrent.handledLaunch
import com.geeksville.mesh.repository.bluetooth.BluetoothRepository
import com.geeksville.mesh.repository.network.NetworkRepository
import com.geeksville.mesh.util.ignoreException
import com.geeksville.mesh.util.toRemoteExceptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.meshtastic.core.analytics.platform.PlatformAnalytics
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.di.ProcessLifecycle
import org.meshtastic.core.model.util.anonymize
import org.meshtastic.core.prefs.radio.RadioPrefs
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.proto.MeshProtos
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class RadioInterfaceService
@Inject
constructor(
    private val context: Application,
    private val dispatchers: CoroutineDispatchers,
    private val bluetoothRepository: BluetoothRepository,
    private val networkRepository: NetworkRepository,
    @ProcessLifecycle private val processLifecycle: Lifecycle,
    private val radioPrefs: RadioPrefs,
    private val interfaceFactory: InterfaceFactory,
    private val analytics: PlatformAnalytics,
) {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedData = MutableSharedFlow<ByteArray>()
    val receivedData: SharedFlow<ByteArray> = _receivedData

    private val _connectionError = MutableSharedFlow<BleError>()
    val connectionError: SharedFlow<BleError> = _connectionError.asSharedFlow()

    private val _currentDeviceAddressFlow = MutableStateFlow(radioPrefs.devAddr)
    val currentDeviceAddressFlow: StateFlow<String?> = _currentDeviceAddressFlow.asStateFlow()

    var serviceScope = CoroutineScope(dispatchers.io + SupervisorJob())

    private var radioIf: IRadioInterface = NopInterface("")

    private var isStarted = false

    private fun initStateListeners() {
        bluetoothRepository.state
            .onEach { state ->
                if (state.enabled) {
                    startInterface()
                } else if (radioIf is NordicBleInterface) {
                    stopInterface()
                }
            }
            .launchIn(processLifecycle.coroutineScope)

        networkRepository.networkAvailable
            .onEach { state ->
                if (state) {
                    startInterface()
                } else if (radioIf is TCPInterface) {
                    stopInterface()
                }
            }
            .launchIn(processLifecycle.coroutineScope)
    }

    companion object {
        private const val TAG = "RadioInterfaceService"
        private const val HEARTBEAT_INTERVAL_MILLIS = 30 * 1000L
    }

    private var lastHeartbeatMillis = 0L

    fun keepAlive(now: Long = System.currentTimeMillis()) {
        if (now - lastHeartbeatMillis > HEARTBEAT_INTERVAL_MILLIS) {
            if (radioIf is SerialInterface) {
                val heartbeat =
                    MeshProtos.ToRadio.newBuilder().setHeartbeat(MeshProtos.Heartbeat.getDefaultInstance()).build()
                handleSendToRadio(heartbeat.toByteArray())
            } else {
                
                radioIf.keepAlive()
            }
            lastHeartbeatMillis = now
        }
    }

    fun toInterfaceAddress(interfaceId: InterfaceId, rest: String): String =
        interfaceFactory.toInterfaceAddress(interfaceId, rest)

    fun getDeviceAddress(): String? = radioPrefs.devAddr

    fun getBondedDeviceAddress(): String? {
        
        val address = getDeviceAddress()
        return if (interfaceFactory.addressValid(address)) {
            address
        } else {
            null
        }
    }

    private fun broadcastConnectionChanged(newState: ConnectionState) {
        processLifecycle.coroutineScope.launch(dispatchers.default) { _connectionState.emit(newState) }
    }

    private fun handleSendToRadio(p: ByteArray) {
        radioIf.handleSendToRadio(p)
        emitSendActivity()
    }

    fun handleFromRadio(p: ByteArray) {
        try {
            processLifecycle.coroutineScope.launch(dispatchers.io) { _receivedData.emit(p) }
            emitReceiveActivity()
        } catch (t: Throwable) {
        }
    }

    fun onConnect() {
        if (_connectionState.value != ConnectionState.Connected) {
            Logger.d(TAG) { "Radio interface connected" }
            broadcastConnectionChanged(ConnectionState.Connected)
        }
    }

    fun onDisconnect(isPermanent: Boolean) {
        val newTargetState = if (isPermanent) ConnectionState.Disconnected else ConnectionState.DeviceSleep
        if (_connectionState.value != newTargetState) {
            Logger.d(TAG) { "Radio interface disconnected, permanent=$isPermanent" }
            broadcastConnectionChanged(newTargetState)
        }
    }

    fun onDisconnect(error: BleError) {
        Logger.w(TAG) { "Radio interface error: ${error.message}, shouldReconnect=${error.shouldReconnect}" }
        processLifecycle.coroutineScope.launch(dispatchers.default) { _connectionError.emit(error) }
        onDisconnect(!error.shouldReconnect)
    }

    private fun startInterface() {
        if (radioIf !is NopInterface) {
        } else {
            val address = getBondedDeviceAddress()
            if (address != null) {
                isStarted = true
                radioIf = interfaceFactory.createInterface(address)
                startHeartbeat()
            }
        }
    }

    private var heartbeatJob: kotlinx.coroutines.Job? = null

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob =
            serviceScope.launch {
                while (true) {
                    delay(HEARTBEAT_INTERVAL_MILLIS)
                    keepAlive()
                }
            }
    }

    private fun stopInterface() {
        val r = radioIf
        isStarted = false
        radioIf = interfaceFactory.nopInterface
        r.close()

        serviceScope.cancel("stopping interface")
        serviceScope = CoroutineScope(dispatchers.io + SupervisorJob())

        if (r !is NopInterface) {
            onDisconnect(isPermanent = true) 
        }
    }

    private fun setBondedDeviceAddress(address: String?): Boolean =
        if (getBondedDeviceAddress() == address && isStarted && _connectionState.value == ConnectionState.Connected) {
            false
        } else {
            analytics.track("mesh_bond")

            ignoreException { stopInterface() }

            radioPrefs.devAddr = address
            _currentDeviceAddressFlow.value = address

            startInterface()
            true
        }

    fun setDeviceAddress(deviceAddr: String?): Boolean = toRemoteExceptions { setBondedDeviceAddress(deviceAddr) }

    fun forceReconnect() {
        Logger.w(TAG) { "Forcing reconnect of radio interface" }
        ignoreException { stopInterface() }
        startInterface()
    }

    fun connect() = toRemoteExceptions {

        startInterface()
        initStateListeners()
    }

    fun sendToRadio(a: ByteArray) {
        
        serviceScope.handledLaunch { handleSendToRadio(a) }
    }

    private val _meshActivity =
        MutableSharedFlow<MeshActivity>(
            replay = 0, 
            extraBufferCapacity = 1, 
            onBufferOverflow = BufferOverflow.DROP_OLDEST, 
        )
    val meshActivity: SharedFlow<MeshActivity> = _meshActivity.asSharedFlow()

    private fun emitSendActivity() {
        _meshActivity.tryEmit(MeshActivity.Send)
    }

    private fun emitReceiveActivity() {
        _meshActivity.tryEmit(MeshActivity.Receive)
    }
}

sealed class MeshActivity {
    data object Send : MeshActivity()

    data object Receive : MeshActivity()
}

