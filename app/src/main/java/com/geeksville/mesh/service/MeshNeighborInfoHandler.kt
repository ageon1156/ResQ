
package com.geeksville.mesh.service

import co.touchlab.kermit.Logger
import com.meshtastic.core.strings.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.unknown_username
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.MeshProtos.MeshPacket
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshNeighborInfoHandler
@Inject
constructor(
    private val nodeManager: MeshNodeManager,
    private val serviceRepository: ServiceRepository,
    private val commandSender: MeshCommandSender,
    private val serviceBroadcasts: MeshServiceBroadcasts,
) {
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(scope: CoroutineScope) {
        this.scope = scope
    }

    fun handleNeighborInfo(packet: MeshPacket) {
        val ni = MeshProtos.NeighborInfo.parseFrom(packet.decoded.payload)

        if (packet.from == nodeManager.myNodeNum) {
            commandSender.lastNeighborInfo = ni
            Logger.d { "Stored last neighbor info from connected radio" }
        }

        nodeManager.nodeDBbyNodeNum[packet.from]?.let { serviceBroadcasts.broadcastNodeChange(it.toNodeInfo()) }

        val requestId = packet.decoded.requestId
        val start = commandSender.neighborInfoStartTimes.remove(requestId)

        val neighbors =
            ni.neighborsList.joinToString("\n") { n ->
                val node = nodeManager.nodeDBbyNodeNum[n.nodeId]
                val name = node?.let { "${it.longName} (${it.shortName})" } ?: getString(Res.string.unknown_username)
                "• $name (SNR: ${n.snr})"
            }

        val formatted = "Neighbors of ${nodeManager.nodeDBbyNodeNum[packet.from]?.longName ?: "Unknown"}:\n$neighbors"

        val responseText =
            if (start != null) {
                val elapsedMs = System.currentTimeMillis() - start
                val seconds = elapsedMs / MILLIS_PER_SECOND
                Logger.i { "Neighbor info $requestId complete in $seconds s" }
                String.format(Locale.US, "%s\n\nDuration: %.1f s", formatted, seconds)
            } else {
                formatted
            }

        serviceRepository.setNeighborInfoResponse(responseText)
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1000.0
    }
}

