
package org.meshtastic.feature.node.list

import android.os.RemoteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.service.ServiceAction
import org.meshtastic.core.service.ServiceRepository
import javax.inject.Inject

class NodeActions
@Inject
constructor(
    private val serviceRepository: ServiceRepository,
    private val nodeRepository: NodeRepository,
) {
    suspend fun favoriteNode(node: Node) {
        try {
            serviceRepository.onServiceAction(ServiceAction.Favorite(node))
        } catch (ex: RemoteException) {
        }
    }

    suspend fun ignoreNode(node: Node) {
        try {
            serviceRepository.onServiceAction(ServiceAction.Ignore(node))
        } catch (ex: RemoteException) {
        }
    }

    suspend fun muteNode(node: Node) {
        try {
            serviceRepository.onServiceAction(ServiceAction.Mute(node))
        } catch (ex: RemoteException) {
        }
    }

    suspend fun removeNode(nodeNum: Int) = withContext(Dispatchers.IO) {
        try {
            val packetId = serviceRepository.meshService?.packetId ?: return@withContext
            serviceRepository.meshService?.removeByNodenum(packetId, nodeNum)
            nodeRepository.deleteNode(nodeNum)
        } catch (ex: RemoteException) {
        }
    }
}

