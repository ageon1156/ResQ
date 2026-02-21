

package org.meshtastic.core.ui.component

import android.content.ClipData
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.copy

@Composable
fun CopyIconButton(
    valueToCopy: String,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.copy),
) {
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    IconButton(
        modifier = modifier,
        onClick = {
            coroutineScope.launch {
                val clipData = ClipData.newPlainText(label, valueToCopy)
                val clipEntry = ClipEntry(clipData)
                clipboardManager.setClipEntry(clipEntry)
            }
        },
    ) {
        Icon(imageVector = Icons.TwoTone.ContentCopy, contentDescription = label)
    }
}

