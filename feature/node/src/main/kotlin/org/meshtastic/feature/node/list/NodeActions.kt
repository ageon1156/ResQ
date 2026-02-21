
package org.meshtastic.feature.node.list

import android.os.RemoteException
import co.touchlab.kermit.Logger
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
            Logger.e(ex) { "Favorite node error" }
        }
    }

    suspend fun ignoreNode(node: Node) {
        try {
            serviceRepository.onServiceAction(ServiceAction.Ignore(node))
        } catch (ex: RemoteException) {
            Logger.e(ex) { "Ignore node error" }
        }
    }

    suspend fun muteNode(node: Node) {
        try {
            serviceRepository.onServiceAction(ServiceAction.Mute(node))
        } catch (ex: RemoteException) {
            Logger.e(ex) { "Mute node error" }
        }
    }

    suspend fun removeNode(nodeNum: Int) = withContext(Dispatchers.IO) {
        Logger.i { "Removing node '$nodeNum'" }
        try {
            val packetId = serviceRepository.meshService?.packetId ?: return@withContext
            serviceRepository.meshService?.removeByNodenum(packetId, nodeNum)
            nodeRepository.deleteNode(nodeNum)
        } catch (ex: RemoteException) {
            Logger.e { "Remove node error: ${ex.message}" }
        }
    }
}

