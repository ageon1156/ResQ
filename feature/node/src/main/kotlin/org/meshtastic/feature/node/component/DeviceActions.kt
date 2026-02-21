
package org.meshtastic.feature.node.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.service.ServiceAction
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.actions
import org.meshtastic.core.strings.direct_message
import org.meshtastic.core.strings.leave_mesh
import org.meshtastic.core.strings.leave_mesh_text
import org.meshtastic.core.strings.leave_mesh_title
import org.meshtastic.core.strings.remove
import org.meshtastic.core.strings.share_contact
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.SimpleAlertDialog
import org.meshtastic.feature.node.model.NodeDetailAction

private enum class DialogType {
    REMOVE,
    LEAVE_MESH,
}

@Composable
fun DeviceActions(
    node: Node,
    onAction: (NodeDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    isLocal: Boolean = false,
) {
    var displayedDialog by remember { mutableStateOf<DialogType?>(null) }

    NodeActionDialogs(
        node = node,
        displayFavoriteDialog = false,
        displayIgnoreDialog = false,
        displayMuteDialog = false,
        displayRemoveDialog = displayedDialog == DialogType.REMOVE,
        onDismissMenuRequest = { displayedDialog = null },
        onConfirmFavorite = {},
        onConfirmIgnore = {},
        onConfirmMute = {},
        onConfirmRemove = { onAction(NodeDetailAction.HandleNodeMenuAction(NodeMenuAction.Remove(it))) },
    )

    if (displayedDialog == DialogType.LEAVE_MESH) {
        SimpleAlertDialog(
            title = Res.string.leave_mesh_title,
            text = Res.string.leave_mesh_text,
            onConfirm = {
                displayedDialog = null
                onAction(NodeDetailAction.TriggerServiceAction(ServiceAction.LeaveMesh))
            },
            onDismiss = { displayedDialog = null },
        )
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            ActionsHeader()

            PrimaryActionsRow(
                node = node,
                isLocal = isLocal,
                onAction = onAction,
            )

            ActionsDivider()

            ManagementActions(
                node = node,
                isLocal = isLocal,
                onRemoveClick = { displayedDialog = DialogType.REMOVE },
                onLeaveMeshClick = { displayedDialog = DialogType.LEAVE_MESH },
            )
        }
    }
}

@Composable
private fun ActionsHeader() {
    Text(
        text = stringResource(Res.string.actions),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun ActionsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun PrimaryActionsRow(
    node: Node,
    isLocal: Boolean,
    onAction: (NodeDetailAction) -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = { onAction(NodeDetailAction.HandleNodeMenuAction(NodeMenuAction.DirectMessage(node))) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.direct_message))
        }

        OutlinedButton(
            onClick = { onAction(NodeDetailAction.ShareContact) },
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(Icons.Rounded.QrCode2, contentDescription = null)
        }
    }
}

@Composable
private fun ManagementActions(
    node: Node,
    isLocal: Boolean,
    onRemoveClick: () -> Unit,
    onLeaveMeshClick: () -> Unit,
) {
    Column {
        if (isLocal) {
            ListItem(
                text = stringResource(Res.string.leave_mesh),
                leadingIcon = Icons.Rounded.ExitToApp,
                trailingIcon = null,
                textColor = MaterialTheme.colorScheme.error,
                leadingIconTint = MaterialTheme.colorScheme.error,
                onClick = onLeaveMeshClick,
            )
        }
        ListItem(
            text = stringResource(Res.string.remove),
            leadingIcon = Icons.Rounded.Delete,
            trailingIcon = null,
            textColor = MaterialTheme.colorScheme.error,
            leadingIconTint = MaterialTheme.colorScheme.error,
            onClick = onRemoveClick,
        )
    }
}

