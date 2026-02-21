

package org.meshtastic.feature.node.model

import org.meshtastic.core.database.model.Node
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.service.ServiceAction
import org.meshtastic.feature.node.component.NodeMenuAction
import org.meshtastic.proto.ConfigProtos.Config.DeviceConfig

sealed interface NodeDetailAction {
    data class Navigate(val route: Route) : NodeDetailAction

    data class TriggerServiceAction(val action: ServiceAction) : NodeDetailAction

    data class HandleNodeMenuAction(val action: NodeMenuAction) : NodeDetailAction

    data object ShareContact : NodeDetailAction

    data class SetOwner(val node: Node, val longName: String, val shortName: String) : NodeDetailAction

    data class SetDeviceConfig(
        val node: Node,
        val role: DeviceConfig.Role,
        val rebroadcastMode: DeviceConfig.RebroadcastMode,
    ) : NodeDetailAction
}

