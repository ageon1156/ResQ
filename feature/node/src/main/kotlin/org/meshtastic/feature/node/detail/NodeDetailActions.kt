
package org.meshtastic.feature.node.detail

import kotlinx.coroutines.CoroutineScope
import org.meshtastic.core.model.Position
import org.meshtastic.core.model.TelemetryType
import org.meshtastic.feature.node.component.NodeMenuAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeDetailActions
@Inject
constructor(
    private val nodeManagementActions: NodeManagementActions,
    private val nodeRequestActions: NodeRequestActions,
) {
    private var scope: CoroutineScope? = null

    fun start(coroutineScope: CoroutineScope) {
        scope = coroutineScope
        nodeManagementActions.start(coroutineScope)
        nodeRequestActions.start(coroutineScope)
    }

    fun handleNodeMenuAction(action: NodeMenuAction) {
        when (action) {
            is NodeMenuAction.Remove -> nodeManagementActions.removeNode(action.node.num)
            is NodeMenuAction.Ignore -> nodeManagementActions.ignoreNode(action.node)
            is NodeMenuAction.Mute -> nodeManagementActions.muteNode(action.node)
            is NodeMenuAction.Favorite -> nodeManagementActions.favoriteNode(action.node)
            is NodeMenuAction.RequestUserInfo -> nodeRequestActions.requestUserInfo(action.node.num)
            is NodeMenuAction.RequestNeighborInfo -> nodeRequestActions.requestNeighborInfo(action.node.num)
            is NodeMenuAction.RequestPosition -> nodeRequestActions.requestPosition(action.node.num)
            is NodeMenuAction.RequestTelemetry -> nodeRequestActions.requestTelemetry(action.node.num, action.type)
            is NodeMenuAction.TraceRoute -> nodeRequestActions.requestTraceroute(action.node.num)
            else -> {}
        }
    }

    fun setNodeNotes(nodeNum: Int, notes: String) {
        nodeManagementActions.setNodeNotes(nodeNum, notes)
    }

    fun requestPosition(destNum: Int, position: Position) {
        nodeRequestActions.requestPosition(destNum, position)
    }

    fun requestUserInfo(destNum: Int) {
        nodeRequestActions.requestUserInfo(destNum)
    }

    fun requestNeighborInfo(destNum: Int) {
        nodeRequestActions.requestNeighborInfo(destNum)
    }

    fun requestTelemetry(destNum: Int, type: TelemetryType) {
        nodeRequestActions.requestTelemetry(destNum, type)
    }

    fun requestTraceroute(destNum: Int) {
        nodeRequestActions.requestTraceroute(destNum)
    }
}

