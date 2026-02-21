/*
 * Copyright (c) 2025-2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.feature.node.detail

import android.os.RemoteException
import co.touchlab.kermit.Logger
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
            Logger.i { "Removing node '$nodeNum'" }
            try {
                val packetId = serviceRepository.meshService?.packetId ?: return@launch
                serviceRepository.meshService?.removeByNodenum(packetId, nodeNum)
                nodeRepository.deleteNode(nodeNum)
            } catch (ex: RemoteException) {
                Logger.e { "Remove node error: ${ex.message}" }
            }
        } ?: Logger.w { "removeNode: scope not initialised" }
    }

    fun ignoreNode(node: Node) {
        scope?.launch(Dispatchers.IO) {
            try {
                serviceRepository.onServiceAction(ServiceAction.Ignore(node))
            } catch (ex: RemoteException) {
                Logger.e(ex) { "Ignore node error" }
            }
        } ?: Logger.w { "ignoreNode: scope not initialised" }
    }

    fun muteNode(node: Node) {
        scope?.launch(Dispatchers.IO) {
            try {
                serviceRepository.onServiceAction(ServiceAction.Mute(node))
            } catch (ex: RemoteException) {
                Logger.e(ex) { "Mute node error" }
            }
        } ?: Logger.w { "muteNode: scope not initialised" }
    }

    fun favoriteNode(node: Node) {
        scope?.launch(Dispatchers.IO) {
            try {
                serviceRepository.onServiceAction(ServiceAction.Favorite(node))
            } catch (ex: RemoteException) {
                Logger.e(ex) { "Favorite node error" }
            }
        } ?: Logger.w { "favoriteNode: scope not initialised" }
    }

    fun setNodeNotes(nodeNum: Int, notes: String) {
        scope?.launch(Dispatchers.IO) {
            try {
                nodeRepository.setNodeNotes(nodeNum, notes)
            } catch (ex: java.io.IOException) {
                Logger.e { "Set node notes IO error: ${ex.message}" }
            } catch (ex: java.sql.SQLException) {
                Logger.e { "Set node notes SQL error: ${ex.message}" }
            }
        } ?: Logger.w { "setNodeNotes: scope not initialised" }
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
                Logger.e { "Set owner error: ${ex.message}" }
            }
        } ?: Logger.w { "setOwner: scope not initialised" }
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
                Logger.e { "Set device config error: ${ex.message}" }
            }
        } ?: Logger.w { "setDeviceConfig: scope not initialised" }
    }
}

