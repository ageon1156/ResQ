
package com.geeksville.mesh.service

import com.geeksville.mesh.concurrent.handledLaunch
import com.meshtastic.core.strings.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.TracerouteSnapshotRepository
import org.meshtastic.core.model.fullRouteDiscovery
import org.meshtastic.core.model.getFullTracerouteResponse
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.service.TracerouteResponse
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.traceroute_duration
import org.meshtastic.core.strings.traceroute_route_back_to_us
import org.meshtastic.core.strings.traceroute_route_towards_dest
import org.meshtastic.core.strings.unknown_username
import org.meshtastic.proto.MeshProtos.MeshPacket
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshTracerouteHandler
@Inject
constructor(
    private val nodeManager: MeshNodeManager,
    private val serviceRepository: ServiceRepository,
    private val tracerouteSnapshotRepository: TracerouteSnapshotRepository,
    private val nodeRepository: NodeRepository,
    private val commandSender: MeshCommandSender,
) {
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(scope: CoroutineScope) {
        this.scope = scope
    }

    fun handleTraceroute(packet: MeshPacket, logUuid: String?, logInsertJob: kotlinx.coroutines.Job?) {
        val full =
            packet.getFullTracerouteResponse(
                getUser = { num ->
                    nodeManager.nodeDBbyNodeNum[num]?.let { "${it.longName} (${it.shortName})" }
                        ?: getString(Res.string.unknown_username)
                },
                headerTowards = getString(Res.string.traceroute_route_towards_dest),
                headerBack = getString(Res.string.traceroute_route_back_to_us),
            ) ?: return

        val requestId = packet.decoded.requestId
        if (logUuid != null) {
            scope.handledLaunch {
                logInsertJob?.join()
                val routeDiscovery = packet.fullRouteDiscovery
                val forwardRoute = routeDiscovery?.routeList.orEmpty()
                val returnRoute = routeDiscovery?.routeBackList.orEmpty()
                val routeNodeNums = (forwardRoute + returnRoute).distinct()
                val nodeDbByNum = nodeRepository.nodeDBbyNum.value
                val snapshotPositions =
                    routeNodeNums.mapNotNull { num -> nodeDbByNum[num]?.validPosition?.let { num to it } }.toMap()
                tracerouteSnapshotRepository.upsertSnapshotPositions(logUuid, requestId, snapshotPositions)
            }
        }

        val start = commandSender.tracerouteStartTimes.remove(requestId)
        val responseText =
            if (start != null) {
                val elapsedMs = System.currentTimeMillis() - start
                val seconds = elapsedMs / MILLISECONDS_IN_SECOND
                val durationText = getString(Res.string.traceroute_duration, "%.1f".format(Locale.US, seconds))
                "$full\n\n$durationText"
            } else {
                full
            }

        val routeDiscovery = packet.fullRouteDiscovery
        val destination = routeDiscovery?.routeList?.firstOrNull() ?: routeDiscovery?.routeBackList?.lastOrNull() ?: 0

        serviceRepository.setTracerouteResponse(
            TracerouteResponse(
                message = responseText,
                destinationNodeNum = destination,
                requestId = requestId,
                forwardRoute = routeDiscovery?.routeList.orEmpty(),
                returnRoute = routeDiscovery?.routeBackList.orEmpty(),
                logUuid = logUuid,
            ),
        )
    }

    companion object {
        private const val MILLISECONDS_IN_SECOND = 1000.0
    }
}

