

package org.meshtastic.core.ui.component

import android.content.ClipData
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun ListItem(
    text: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    textColor: Color = LocalContentColor.current,
    supportingTextColor: Color = LocalContentColor.current,
    copyable: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = LocalContentColor.current,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
    trailingIconTint: Color = LocalContentColor.current,
    onClick: (() -> Unit)? = null,
) {
    val clipboard: Clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    BasicListItem(
        text = text,
        modifier = modifier,
        textColor = textColor,
        supportingText = supportingText,
        supportingTextColor = supportingTextColor,
        enabled = enabled,
        leadingIcon = leadingIcon,
        leadingIconTint = leadingIconTint,
        trailingContent = trailingIcon.icon(trailingIconTint),
        onClick = onClick,
        onLongClick =
        if (!supportingText.isNullOrBlank() && copyable) {
            {
                coroutineScope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", supportingText)))
                }
            }
        } else {
            null
        },
    )
}

@Composable
fun SwitchListItem(
    checked: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = LocalContentColor.current,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = LocalContentColor.current,
) {
    BasicListItem(
        text = text,
        modifier = modifier,
        textColor = textColor,
        enabled = enabled,
        leadingIcon = leadingIcon,
        leadingIconTint = leadingIconTint,
        trailingContent = { Switch(checked = checked, enabled = enabled, onCheckedChange = null) },
        onClick = onClick,
    )
}

@Suppress("UnusedParameter")
@Composable
fun BasicListItem(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = LocalContentColor.current,
    supportingText: String? = null,
    supportingTextColor: Color = LocalContentColor.current,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = LocalContentColor.current,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier =
        if (onLongClick != null || onClick != null) {
            modifier.combinedClickable(onLongClick = onLongClick, onClick = onClick ?: {})
        } else {
            modifier
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(text = text, color = textColor) },
        supportingContent = supportingText?.let { { Text(text = it, color = supportingTextColor) } },
        leadingContent = leadingIcon.icon(leadingIconTint),
        trailingContent = trailingContent,
    )
}

@Composable
fun ImageVector?.icon(tint: Color = LocalContentColor.current): @Composable (() -> Unit)? =
    this?.let { { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(24.dp), tint = tint) } }

@Preview(showBackground = true)
@Composable
private fun ListItemPreview() {
    AppTheme { ListItem(text = "Text", leadingIcon = Icons.Rounded.Android, enabled = true) {} }
}

@Preview(showBackground = true)
@Composable
private fun ListItemDisabledPreview() {
    AppTheme { ListItem(text = "Text", leadingIcon = Icons.Rounded.Android, enabled = false) {} }
}

@Preview(showBackground = true)
@Composable
private fun SwitchListItemPreview() {
    AppTheme { SwitchListItem(text = "Text", leadingIcon = Icons.Rounded.Android, checked = true, onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun ListItemPreviewSupportingText() {
    AppTheme {
        ListItem(text = "Text 1", leadingIcon = Icons.Rounded.Android, supportingText = "Text2", trailingIcon = null)
    }
}

