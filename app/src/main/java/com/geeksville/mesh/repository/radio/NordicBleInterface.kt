
package com.geeksville.mesh.repository.radio

import android.annotation.SuppressLint
import com.geeksville.mesh.repository.radio.BleConstants.BTM_FROMNUM_CHARACTER
import com.geeksville.mesh.repository.radio.BleConstants.BTM_FROMRADIO_CHARACTER
import com.geeksville.mesh.repository.radio.BleConstants.BTM_LOGRADIO_CHARACTER
import com.geeksville.mesh.repository.radio.BleConstants.BTM_SERVICE_UUID
import com.geeksville.mesh.repository.radio.BleConstants.BTM_TORADIO_CHARACTER
import com.geeksville.mesh.service.RadioNotConnectedException
import co.touchlab.kermit.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.ConnectionPriority
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.CharacteristicProperty
import no.nordicsemi.kotlin.ble.core.ConnectionState
import no.nordicsemi.kotlin.ble.core.WriteType
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@SuppressLint("MissingPermission")
class NordicBleInterface
@AssistedInject
constructor(
    private val serviceScope: CoroutineScope,
    private val centralManager: CentralManager,
    private val service: RadioInterfaceService,
    @Assisted val address: String,
) : IRadioInterface {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        serviceScope.launch {
            try {
                peripheral?.disconnect()
            } catch (_: Exception) {
            }
        }
        service.onDisconnect(BleError.from(throwable))
    }

    private val connectionScope = CoroutineScope(serviceScope.coroutineContext + SupervisorJob() + exceptionHandler)
    private val drainMutex = Mutex()
    private val writeMutex = Mutex()

    private var peripheral: Peripheral? = null
    private var connectionStartTime: Long = 0
    private var packetsReceived: Int = 0
    private var packetsSent: Int = 0
    private var bytesReceived: Long = 0
    private var bytesSent: Long = 0

    private var toRadioCharacteristic: RemoteCharacteristic? = null
    private var fromNumCharacteristic: RemoteCharacteristic? = null
    private var fromRadioCharacteristic: RemoteCharacteristic? = null
    private var logRadioCharacteristic: RemoteCharacteristic? = null

    init {
        connect()
    }

    private fun fromRadioPacketFlow(): Flow<ByteArray> = channelFlow {
        while (isActive) {
            val packet =
                fromRadioCharacteristic?.read()?.takeIf { it.isNotEmpty() }
                    ?: break
            send(packet)
        }
    }

    private fun dispatchPacket(packet: ByteArray) {
        packetsReceived++
        bytesReceived += packet.size
        try {
            service.handleFromRadio(p = packet)
        } catch (_: Throwable) {
        }
    }

    private suspend fun drainPacketQueueAndDispatch() {
        drainMutex.withLock {
            fromRadioPacketFlow()
                .onEach { packet -> dispatchPacket(packet) }
                .catch { }
                .collect()
        }
    }

    private fun findPeripheral(): Peripheral =
        centralManager.getBondedPeripherals().firstOrNull { it.address == address }
            ?: throw RadioNotConnectedException("Device not found at address $address")

    private fun connect() {
        connectionScope.launch {
            try {
                connectionStartTime = System.currentTimeMillis()
                Logger.d(TAG) { "Connecting to $address" }
                withTimeout(CONNECT_TIMEOUT_MS) { peripheral = retryCall { findAndConnectPeripheral() } }
                Logger.d(TAG) { "GATT connected to $address, starting service discovery" }
                peripheral?.let {
                    onConnected()
                    observePeripheralChanges()
                    discoverServicesAndSetupCharacteristics(it)
                }
            } catch (e: TimeoutCancellationException) {
                Logger.w(TAG) { "Timed out connecting to $address" }
                service.onDisconnect(BleError.ConnectionTimeout(e))
            } catch (e: Exception) {
                Logger.w(TAG, throwable = e) { "Failed to connect to $address" }
                service.onDisconnect(BleError.from(e))
            }
        }
    }

    private suspend fun findAndConnectPeripheral(): Peripheral {
        val p = findPeripheral()
        centralManager.connect(
            peripheral = p,
            options = CentralManager.ConnectionOptions.AutoConnect(automaticallyRequestHighestValueLength = true),
        )
        p.requestConnectionPriority(ConnectionPriority.HIGH)
        return p
    }

    private suspend fun onConnected() {
        try {
            peripheral?.let { p ->
                retryCall { p.readRssi() }
                retryCall { p.readPhy() }
            }
        } catch (e: Exception) {
        }
    }

    private fun observePeripheralChanges() {
        peripheral?.let { p ->
            p.phy.launchIn(connectionScope)

            p.connectionParameters.launchIn(connectionScope)

            p.state
                .onEach { state ->
                    if (state is ConnectionState.Disconnected) {
                        Logger.w(TAG) { "Peripheral $address disconnected: ${state.reason}" }
                        service.onDisconnect(BleError.Disconnected(reason = state.reason))
                    }
                }
                .launchIn(connectionScope)
        }
        centralManager.state.launchIn(connectionScope)
    }

    @Suppress("TooGenericExceptionCaught")
    @OptIn(ExperimentalUuidApi::class)
    private fun discoverServicesAndSetupCharacteristics(peripheral: Peripheral) {
        // The services() flow keeps observing for the life of the connection, so only the FIRST
        // result is timeout-guarded here; discoveryDone signals that first result has arrived.
        val discoveryDone = CompletableDeferred<Unit>()

        connectionScope.launch {
            peripheral
                .services(listOf(BTM_SERVICE_UUID.toKotlinUuid()))
                .onEach { services ->
                    val meshtasticService = services?.find { it.uuid == BTM_SERVICE_UUID.toKotlinUuid() }

                    if (meshtasticService != null) {
                        toRadioCharacteristic =
                            meshtasticService.characteristics.find { it.uuid == BTM_TORADIO_CHARACTER.toKotlinUuid() }
                        fromNumCharacteristic =
                            meshtasticService.characteristics.find { it.uuid == BTM_FROMNUM_CHARACTER.toKotlinUuid() }
                        fromRadioCharacteristic =
                            meshtasticService.characteristics.find { it.uuid == BTM_FROMRADIO_CHARACTER.toKotlinUuid() }
                        logRadioCharacteristic =
                            meshtasticService.characteristics.find { it.uuid == BTM_LOGRADIO_CHARACTER.toKotlinUuid() }

                        if (
                            listOf(toRadioCharacteristic, fromNumCharacteristic, fromRadioCharacteristic).all {
                                it != null
                            }
                        ) {
                            Logger.d(TAG) { "Discovery succeeded for $address, setting up notifications" }
                            setupNotifications()
                            service.onConnect()
                        } else {
                            Logger.w(TAG) { "One or more required characteristics not found for $address" }
                            service.onDisconnect(BleError.DiscoveryFailed("One or more characteristics not found"))
                        }
                    } else {
                        Logger.w(TAG) { "Meshtastic service not found for $address" }
                        service.onDisconnect(BleError.DiscoveryFailed("Meshtastic service not found"))
                    }
                    discoveryDone.complete(Unit)
                }
                .catch { e ->
                    try {
                        peripheral.disconnect()
                    } catch (e2: Exception) {
                    }
                    Logger.w(TAG, throwable = e) { "Discovery failed for $address" }
                    service.onDisconnect(BleError.from(e))
                    discoveryDone.complete(Unit)
                }
                .launchIn(connectionScope)
        }

        connectionScope.launch {
            val completed = withTimeoutOrNull(DISCOVERY_TIMEOUT_MS) { discoveryDone.await() }
            if (completed == null) {
                Logger.w(TAG) { "Timed out discovering services for $address" }
                try {
                    peripheral.disconnect()
                } catch (e: Exception) {
                }
                service.onDisconnect(BleError.ConnectionTimeout(Exception("Discovery timed out for $address")))
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun setupNotifications() {
        retryCall { fromNumCharacteristic?.subscribe() }
            ?.onEach { connectionScope.launch { drainPacketQueueAndDispatch() } }
            ?.catch { e -> service.onDisconnect(BleError.from(e)) }
            ?.launchIn(scope = connectionScope)

        retryCall { logRadioCharacteristic?.subscribe() }
            ?.onEach { notifyBytes -> dispatchPacket(notifyBytes) }
            ?.catch { e -> service.onDisconnect(BleError.from(e)) }
            ?.launchIn(scope = connectionScope)
    }

    private suspend fun <T> retryCall(block: suspend () -> T): T {
        var currentAttempt = 0
        while (true) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                currentAttempt++
                if (currentAttempt >= RETRY_COUNT) {
                    throw e
                }
                delay(RETRY_DELAY_MS)
            }
        }
    }

    override fun handleSendToRadio(p: ByteArray) {
        toRadioCharacteristic?.let { characteristic ->
            if (peripheral == null) {
                return@let
            }
            connectionScope.launch {
                writeMutex.withLock {
                    try {
                        val writeType =
                            if (characteristic.properties.contains(CharacteristicProperty.WRITE_WITHOUT_RESPONSE)) {
                                WriteType.WITHOUT_RESPONSE
                            } else {
                                WriteType.WITH_RESPONSE
                            }
                        retryCall {
                            packetsSent++
                            bytesSent += p.size
                            characteristic.write(p, writeType = writeType)
                        }
                        drainPacketQueueAndDispatch()
                    } catch (e: Exception) {
                        service.onDisconnect(BleError.from(e))
                    }
                }
            }
        }
    }

    override fun keepAlive() {
    }

    override fun close() {
        runBlocking {
            connectionScope.cancel()
            peripheral?.disconnect()
            service.onDisconnect(true)
        }
    }

    companion object {
        private const val TAG = "NordicBleInterface"
        private const val RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 500L
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val DISCOVERY_TIMEOUT_MS = 10_000L
    }
}

object BleConstants {
    const val BLE_NAME_PATTERN = "^.*_([0-9a-fA-F]{4})$"
    val BTM_SERVICE_UUID: UUID = UUID.fromString("6ba1b218-15a8-461f-9fa8-5dcae273eafd")
    val BTM_TORADIO_CHARACTER: UUID = UUID.fromString("f75c76d2-129e-4dad-a1dd-7866124401e7")
    val BTM_FROMNUM_CHARACTER: UUID = UUID.fromString("ed9da18c-a800-4f66-a670-aa7547e34453")
    val BTM_FROMRADIO_CHARACTER: UUID = UUID.fromString("2c55e69e-4993-11ed-b878-0242ac120002")
    val BTM_LOGRADIO_CHARACTER: UUID = UUID.fromString("5a3d6e49-06e6-4423-9944-e9de8cdf9547")
}

