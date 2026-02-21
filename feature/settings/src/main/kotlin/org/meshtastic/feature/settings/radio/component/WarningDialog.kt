

package org.meshtastic.feature.settings.radio.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.cancel
import org.meshtastic.core.strings.send
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun WarningDialog(
    icon: ImageVector? = Icons.Rounded.Warning,
    title: String,
    text: @Composable () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        icon = { icon?.let { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(text = title) },
        text = text,
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

@Preview
@Composable
private fun WarningDialogPreview() {
    AppTheme { WarningDialog(title = "Factory Reset?", onDismiss = {}, onConfirm = {}) }
}

