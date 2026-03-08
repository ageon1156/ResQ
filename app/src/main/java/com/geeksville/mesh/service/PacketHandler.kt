
package com.geeksville.mesh.service

import com.geeksville.mesh.concurrent.handledLaunch
import com.geeksville.mesh.repository.radio.RadioInterfaceService
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.meshtastic.core.data.repository.MeshLogRepository
import org.meshtastic.core.data.repository.PacketRepository
import org.meshtastic.core.database.entity.MeshLog
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.MeshProtos.MeshPacket
import org.meshtastic.proto.MeshProtos.ToRadio
import org.meshtastic.proto.fromRadio
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class PacketHandler
@Inject
constructor(
    private val packetRepository: Lazy<PacketRepository>,
    private val serviceBroadcasts: MeshServiceBroadcasts,
    private val radioInterfaceService: RadioInterfaceService,
    private val meshLogRepository: Lazy<MeshLogRepository>,
    private val connectionStateHolder: ConnectionStateHandler,
) {

    companion object {
        private const val TIMEOUT_MS = 5000L 
    }

    private var queueJob: Job? = null
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private val queuedPackets = ConcurrentLinkedQueue<MeshPacket>()
    private val queueResponse = ConcurrentHashMap<Int, CompletableDeferred<Boolean>>()

    fun start(scope: CoroutineScope) {
        this.scope = scope
    }

    fun sendToRadio(p: ToRadio.Builder) {
        val built = p.build()
        val b = built.toByteArray()

        radioInterfaceService.sendToRadio(b)
        changeStatus(p.packet.id, MessageStatus.ENROUTE)

        if (p.packet.hasDecoded()) {
            val packetToSave =
                MeshLog(
                    uuid = UUID.randomUUID().toString(),
                    message_type = "Packet",
                    received_date = System.currentTimeMillis(),
                    raw_message = p.packet.toString(),
                    fromNum = p.packet.from,
                    portNum = p.packet.decoded.portnumValue,
                    fromRadio = fromRadio { packet = p.packet },
                )
            insertMeshLog(packetToSave)
        }
    }

    fun sendToRadio(packet: MeshPacket) {
        queuedPackets.add(packet)
        startPacketQueue()
    }

    fun stopPacketQueue() {
        if (queueJob?.isActive == true) {
            queueJob?.cancel()
            queueJob = null
            queuedPackets.clear()
            queueResponse.entries.lastOrNull { !it.value.isCompleted }?.value?.complete(false)
            queueResponse.clear()
        }
    }

    fun handleQueueStatus(queueStatus: MeshProtos.QueueStatus) {
        val (success, isFull, requestId) = with(queueStatus) { Triple(res == 0, free == 0, meshPacketId) }
        if (success && isFull) return 
        if (requestId != 0) {
            queueResponse.remove(requestId)?.complete(success)
        } else {
            
            queueResponse.values.firstOrNull { !it.isCompleted }?.complete(success)
        }
    }

    fun removeResponse(dataRequestId: Int, complete: Boolean) {
        queueResponse.remove(dataRequestId)?.complete(complete)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun startPacketQueue() {
        if (queueJob?.isActive == true) return
        queueJob =
            scope.handledLaunch {
                while (connectionStateHolder.connectionState.value == ConnectionState.Connected) {
                    val packet = queuedPackets.poll() ?: break
                    try {
                        val response = sendPacket(packet)
                        withTimeout(TIMEOUT_MS) { response.await() }
                    } catch (e: TimeoutCancellationException) {
                    } catch (e: Exception) {
                    } finally {
                        queueResponse.remove(packet.id)
                    }
                }
            }
    }

    private fun changeStatus(packetId: Int, m: MessageStatus) = scope.handledLaunch {
        if (packetId != 0) {
            getDataPacketById(packetId)?.let { p ->
                if (p.status == m) return@handledLaunch
                packetRepository.get().updateMessageStatus(p, m)
                serviceBroadcasts.broadcastMessageStatus(packetId, m)
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun getDataPacketById(packetId: Int): DataPacket? = withTimeoutOrNull(1000) {
        var dataPacket: DataPacket? = null
        while (dataPacket == null) {
            dataPacket = packetRepository.get().getPacketById(packetId)?.data
            if (dataPacket == null) delay(100)
        }
        dataPacket
    }

    @Suppress("TooGenericExceptionCaught")
    private fun sendPacket(packet: MeshPacket): CompletableDeferred<Boolean> {

        val deferred = CompletableDeferred<Boolean>()
        queueResponse[packet.id] = deferred
        try {
            if (connectionStateHolder.connectionState.value != ConnectionState.Connected) {
                throw RadioNotConnectedException()
            }
            sendToRadio(ToRadio.newBuilder().apply { this.packet = packet })
        } catch (ex: Exception) {
            deferred.complete(false)
        }
        return deferred
    }

    private fun insertMeshLog(packetToSave: MeshLog) {
        scope.handledLaunch {

            meshLogRepository.get().insert(packetToSave)
        }
    }
}

