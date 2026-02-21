

package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.cancel
import org.meshtastic.core.strings.send
import org.meshtastic.core.strings.shutdown_node_name
import org.meshtastic.core.strings.shutdown_warning
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.proto.MeshProtos

@Composable
fun ShutdownConfirmationDialog(
    title: String,
    node: Node?,
    onDismiss: () -> Unit,
    isShutdown: Boolean = true,
    icon: ImageVector? = Icons.Rounded.Warning,
    onConfirm: () -> Unit,
) {
    val nodeLongName = node?.user?.longName ?: "Unknown Node"

    AlertDialog(
        onDismissRequest = {},
        icon = { icon?.let { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(text = title) },
        text = { ShutdownDialogContent(nodeLongName = nodeLongName, isShutdown = isShutdown) },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text(stringResource(Res.string.cancel)) } },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
            ) {
                Text(stringResource(Res.string.send))
            }
        },
    )
}

@Composable
private fun ShutdownDialogContent(nodeLongName: String, isShutdown: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.shutdown_node_name, nodeLongName),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        if (isShutdown) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.shutdown_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun ShutdownConfirmationDialogPreview() {
    val mockNode =
        Node(
            num = 123,
            user = MeshProtos.User.newBuilder().setLongName("Rooftop Router Node").setShortName("ROOF").build(),
        )

    AppTheme { ShutdownConfirmationDialog(title = "Shutdown?", node = mockNode, onDismiss = {}, onConfirm = {}) }
}

