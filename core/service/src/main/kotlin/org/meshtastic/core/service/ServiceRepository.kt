
package org.meshtastic.core.service

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.MeshProtos.MeshPacket
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

sealed class RetryEvent {
    abstract val packetId: Int
    abstract val attemptNumber: Int
    abstract val maxAttempts: Int

    data class MessageRetry(
        override val packetId: Int,
        val text: String,
        override val attemptNumber: Int,
        override val maxAttempts: Int,
    ) : RetryEvent()

    data class ReactionRetry(
        override val packetId: Int,
        val emoji: String,
        override val attemptNumber: Int,
        override val maxAttempts: Int,
    ) : RetryEvent()
}

data class TracerouteResponse(
    val message: String,
    val destinationNodeNum: Int,
    val requestId: Int,
    val forwardRoute: List<Int> = emptyList(),
    val returnRoute: List<Int> = emptyList(),
    val logUuid: String? = null,
) {
    val hasOverlay: Boolean
        get() = forwardRoute.isNotEmpty() || returnRoute.isNotEmpty()
}

@Suppress("TooManyFunctions")
@Singleton
class ServiceRepository @Inject constructor() {
    var meshService: IMeshService? = null
        private set

    fun setMeshService(service: IMeshService?) {
        meshService = service
    }

    private val _connectionState: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState>
        get() = _connectionState

    fun setConnectionState(connectionState: ConnectionState) {
        _connectionState.value = connectionState
    }

    private val _clientNotification = MutableStateFlow<MeshProtos.ClientNotification?>(null)
    val clientNotification: StateFlow<MeshProtos.ClientNotification?>
        get() = _clientNotification

    fun setClientNotification(notification: MeshProtos.ClientNotification?) {
        Logger.e { notification?.message.orEmpty() }

        _clientNotification.value = notification
    }

    fun clearClientNotification() {
        _clientNotification.value = null
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?>
        get() = _errorMessage

    fun setErrorMessage(text: String) {
        Logger.e { text }
        _errorMessage.value = text
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?>
        get() = _statusMessage

    fun setStatusMessage(text: String) {
        if (connectionState.value != ConnectionState.Connected) {
            _statusMessage.value = text
        }
    }

    private val _meshPacketFlow = MutableSharedFlow<MeshPacket>()
    val meshPacketFlow: SharedFlow<MeshPacket>
        get() = _meshPacketFlow

    suspend fun emitMeshPacket(packet: MeshPacket) {
        _meshPacketFlow.emit(packet)
    }

    private val _tracerouteResponse = MutableStateFlow<TracerouteResponse?>(null)
    val tracerouteResponse: StateFlow<TracerouteResponse?>
        get() = _tracerouteResponse

    fun setTracerouteResponse(value: TracerouteResponse?) {
        _tracerouteResponse.value = value
    }

    fun clearTracerouteResponse() {
        setTracerouteResponse(null)
    }

    private val _neighborInfoResponse = MutableStateFlow<String?>(null)
    val neighborInfoResponse: StateFlow<String?>
        get() = _neighborInfoResponse

    fun setNeighborInfoResponse(value: String?) {
        _neighborInfoResponse.value = value
    }

    fun clearNeighborInfoResponse() {
        setNeighborInfoResponse(null)
    }

    private val _serviceAction = Channel<ServiceAction>()
    val serviceAction = _serviceAction.receiveAsFlow()

    suspend fun onServiceAction(action: ServiceAction) {
        _serviceAction.send(action)
    }

    private val _retryEvents = MutableStateFlow<RetryEvent?>(null)
    val retryEvents: StateFlow<RetryEvent?>
        get() = _retryEvents

    private val pendingRetries = ConcurrentHashMap<Int, CompletableDeferred<Boolean>>()

    suspend fun requestRetry(event: RetryEvent, timeoutMs: Long): Boolean {
        val packetId = event.packetId
        val deferred = CompletableDeferred<Boolean>()
        pendingRetries[packetId] = deferred

        Logger.i { "ServiceRepository: Setting retry event for packet $packetId" }
        _retryEvents.value = event
        Logger.i { "ServiceRepository: Retry event set, waiting for response..." }

        val result = withTimeoutOrNull(timeoutMs) { deferred.await() } ?: true
        Logger.i { "ServiceRepository: Retry result for packet $packetId: $result" }
        return result
    }

    fun respondToRetry(packetId: Int, shouldRetry: Boolean) {
        pendingRetries.remove(packetId)?.complete(shouldRetry)
        _retryEvents.value = null 
    }

    fun cancelPendingRetries() {
        pendingRetries.forEach { (_, deferred) -> deferred.complete(false) }
        pendingRetries.clear()
    }
}

