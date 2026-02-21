
package org.meshtastic.feature.node.component

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.TelemetryType
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.favorite
import org.meshtastic.core.strings.favorite_add
import org.meshtastic.core.strings.favorite_remove
import org.meshtastic.core.strings.ignore
import org.meshtastic.core.strings.ignore_add
import org.meshtastic.core.strings.ignore_remove
import org.meshtastic.core.strings.mute_add
import org.meshtastic.core.strings.mute_notifications
import org.meshtastic.core.strings.mute_remove
import org.meshtastic.core.strings.remove
import org.meshtastic.core.strings.remove_node_text
import org.meshtastic.core.strings.unmute
import org.meshtastic.core.ui.component.SimpleAlertDialog

@Composable
fun NodeActionDialogs(
    node: Node,
    displayFavoriteDialog: Boolean,
    displayIgnoreDialog: Boolean,
    displayMuteDialog: Boolean,
    displayRemoveDialog: Boolean,
    onDismissMenuRequest: () -> Unit,
    onConfirmFavorite: (Node) -> Unit,
    onConfirmIgnore: (Node) -> Unit,
    onConfirmMute: (Node) -> Unit,
    onConfirmRemove: (Node) -> Unit,
) {
    if (displayFavoriteDialog) {
        SimpleAlertDialog(
            title = Res.string.favorite,
            text =
            stringResource(
                if (node.isFavorite) Res.string.favorite_remove else Res.string.favorite_add,
                node.user.longName,
            ),
            onConfirm = {
                onDismissMenuRequest()
                onConfirmFavorite(node)
            },
            onDismiss = onDismissMenuRequest,
        )
    }
    if (displayIgnoreDialog) {
        SimpleAlertDialog(
            title = Res.string.ignore,
            text =
            stringResource(
                if (node.isIgnored) Res.string.ignore_remove else Res.string.ignore_add,
                node.user.longName,
            ),
            onConfirm = {
                onDismissMenuRequest()
                onConfirmIgnore(node)
            },
            onDismiss = onDismissMenuRequest,
        )
    }
    if (displayMuteDialog) {
        SimpleAlertDialog(
            title = if (node.isMuted) Res.string.unmute else Res.string.mute_notifications,
            text =
            stringResource(if (node.isMuted) Res.string.mute_remove else Res.string.mute_add, node.user.longName),
            onConfirm = {
                onDismissMenuRequest()
                onConfirmMute(node)
            },
            onDismiss = onDismissMenuRequest,
        )
    }
    if (displayRemoveDialog) {
        SimpleAlertDialog(
            title = Res.string.remove,
            text = Res.string.remove_node_text,
            onConfirm = {
                onDismissMenuRequest()
                onConfirmRemove(node)
            },
            onDismiss = onDismissMenuRequest,
        )
    }
}

sealed class NodeMenuAction {
    data class Remove(val node: Node) : NodeMenuAction()

    data class Ignore(val node: Node) : NodeMenuAction()

    data class Mute(val node: Node) : NodeMenuAction()

    data class Favorite(val node: Node) : NodeMenuAction()

    data class DirectMessage(val node: Node) : NodeMenuAction()

    data class RequestUserInfo(val node: Node) : NodeMenuAction()

    data class RequestNeighborInfo(val node: Node) : NodeMenuAction()

    data class RequestPosition(val node: Node) : NodeMenuAction()

    data class RequestTelemetry(val node: Node, val type: TelemetryType) : NodeMenuAction()

    data class TraceRoute(val node: Node) : NodeMenuAction()

    data class MoreDetails(val node: Node) : NodeMenuAction()

    data class Share(val node: Node) : NodeMenuAction()
}

