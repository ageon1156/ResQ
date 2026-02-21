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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.database.model.Node
import org.meshtastic.feature.node.component.NodeMenuAction
import org.meshtastic.proto.ConfigProtos.Config.DeviceConfig
import javax.inject.Inject

@HiltViewModel
class NodeDetailViewModel
@Inject
constructor(
    private val nodeRepository: NodeRepository,
    private val nodeManagementActions: NodeManagementActions,
    private val nodeRequestActions: NodeRequestActions,
) : ViewModel() {

    init {
        nodeManagementActions.start(viewModelScope)
        nodeRequestActions.start(viewModelScope)
    }

    val ourNodeInfo: StateFlow<Node?> = nodeRepository.ourNodeInfo

    private val _lastTraceRouteTime = MutableStateFlow<Long?>(null)
    val lastTraceRouteTime: StateFlow<Long?> = _lastTraceRouteTime.asStateFlow()

    private val _lastRequestNeighborsTime = MutableStateFlow<Long?>(null)
    val lastRequestNeighborsTime: StateFlow<Long?> = _lastRequestNeighborsTime.asStateFlow()

    fun handleNodeMenuAction(action: NodeMenuAction) {
        when (action) {
            is NodeMenuAction.Remove -> nodeManagementActions.removeNode(action.node.num)
            is NodeMenuAction.Ignore -> nodeManagementActions.ignoreNode(action.node)
            is NodeMenuAction.Mute -> nodeManagementActions.muteNode(action.node)
            is NodeMenuAction.Favorite -> nodeManagementActions.favoriteNode(action.node)
            is NodeMenuAction.RequestUserInfo -> nodeRequestActions.requestUserInfo(action.node.num)
            is NodeMenuAction.RequestNeighborInfo -> {
                nodeRequestActions.requestNeighborInfo(action.node.num)
                _lastRequestNeighborsTime.value = System.currentTimeMillis()
            }
            is NodeMenuAction.RequestPosition -> nodeRequestActions.requestPosition(action.node.num)
            is NodeMenuAction.RequestTelemetry -> nodeRequestActions.requestTelemetry(action.node.num, action.type)
            is NodeMenuAction.TraceRoute -> {
                nodeRequestActions.requestTraceroute(action.node.num)
                _lastTraceRouteTime.value = System.currentTimeMillis()
            }
            else -> {}
        }
    }

    fun setNodeNotes(nodeNum: Int, notes: String) {
        nodeManagementActions.setNodeNotes(nodeNum, notes)
    }

    fun setOwner(node: Node, longName: String, shortName: String) {
        nodeManagementActions.setOwner(node, longName, shortName)
    }

    fun setDeviceConfig(node: Node, role: DeviceConfig.Role, rebroadcastMode: DeviceConfig.RebroadcastMode) {
        nodeManagementActions.setDeviceConfig(node, role, rebroadcastMode)
    }
}

