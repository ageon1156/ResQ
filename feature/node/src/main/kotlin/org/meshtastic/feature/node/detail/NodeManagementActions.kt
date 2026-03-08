
package org.meshtastic.feature.node.detail

import android.os.RemoteException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.service.ServiceAction
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.proto.ConfigProtos
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeManagementActions
@Inject
constructor(
    private val nodeRepository: NodeRepository,
    private val serviceRepository: ServiceRepository,
) {
    private var scope: CoroutineScope? = null

    fun start(coroutineScope: CoroutineScope) {
        scope = coroutineScope
    }

    fun removeNode(nodeNum: Int) {
        scope?.launch(Dispatchers.IO) {
            try {
                val packetId = serviceRepository.meshService?.packetId ?: return@launch
                serviceRepository.meshService?.removeByNodenum(packetId, nodeNum)
                nodeRepository.deleteNode(nodeNum)
            } catch (ex: RemoteException) {
            }
        }
    }

    fun ignoreNode(node: Node) {
        scope?.launch(Dispatchers.IO) {
            try {
                serviceRepository.onServiceAction(ServiceAction.Ignore(node))
            } catch (ex: RemoteException) {
            }
        }
    }

    fun muteNode(node: Node) {
        scope?.launch(Dispatchers.IO) {
            try {
                serviceRepository.onServiceAction(ServiceAction.Mute(node))
            } catch (ex: RemoteException) {
            }
        }
    }

    fun favoriteNode(node: Node) {
        scope?.launch(Dispatchers.IO) {
            try {
                serviceRepository.onServiceAction(ServiceAction.Favorite(node))
            } catch (ex: RemoteException) {
            }
        }
    }

    fun setNodeNotes(nodeNum: Int, notes: String) {
        scope?.launch(Dispatchers.IO) {
            try {
                nodeRepository.setNodeNotes(nodeNum, notes)
            } catch (ex: java.io.IOException) {
            } catch (ex: java.sql.SQLException) {
            }
        }
    }

    fun setOwner(node: Node, longName: String, shortName: String) {
        scope?.launch(Dispatchers.IO) {
            try {
                val service = serviceRepository.meshService ?: return@launch
                val packetId = service.packetId
                val updatedUser = node.user.toBuilder()
                    .setLongName(longName)
                    .setShortName(shortName)
                    .build()
                service.setRemoteOwner(packetId, node.num, updatedUser.toByteArray())
            } catch (ex: RemoteException) {
            }
        }
    }

    fun setDeviceConfig(
        node: Node,
        role: ConfigProtos.Config.DeviceConfig.Role,
        rebroadcastMode: ConfigProtos.Config.DeviceConfig.RebroadcastMode,
    ) {
        scope?.launch(Dispatchers.IO) {
            try {
                val service = serviceRepository.meshService ?: return@launch
                val packetId = service.packetId
                val cfg = ConfigProtos.Config.newBuilder()
                    .setDevice(
                        ConfigProtos.Config.DeviceConfig.newBuilder()
                            .setRole(role)
                            .setRebroadcastMode(rebroadcastMode)
                            .build()
                    )
                    .build()
                service.setRemoteConfig(packetId, node.num, cfg.toByteArray())
            } catch (ex: RemoteException) {
            }
        }
    }
}

