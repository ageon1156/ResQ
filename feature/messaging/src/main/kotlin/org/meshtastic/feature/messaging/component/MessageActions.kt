
package org.meshtastic.feature.messaging.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.twotone.AddLink
import androidx.compose.material.icons.twotone.Cloud
import androidx.compose.material.icons.twotone.CloudDone
import androidx.compose.material.icons.twotone.CloudOff
import androidx.compose.material.icons.twotone.CloudUpload
import androidx.compose.material.icons.twotone.HowToReg
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.message_delivery_status
import org.meshtastic.core.strings.react
import org.meshtastic.core.strings.reply
import org.meshtastic.core.ui.emoji.EmojiPickerDialog

@Composable
internal fun ReactionButton(onSendReaction: (String) -> Unit = {}) {
    var showEmojiPickerDialog by remember { mutableStateOf(false) }
    if (showEmojiPickerDialog) {
        EmojiPickerDialog(
            onConfirm = { selectedEmoji ->
                showEmojiPickerDialog = false
                onSendReaction(selectedEmoji)
            },
            onDismiss = { showEmojiPickerDialog = false },
        )
    }
    IconButton(onClick = { showEmojiPickerDialog = true }) {
        Icon(imageVector = Icons.Default.AddReaction, contentDescription = stringResource(Res.string.react))
    }
}

@Composable
private fun ReplyButton(onClick: () -> Unit = {}) = IconButton(
    onClick = onClick,
    content = {
        Icon(imageVector = Icons.AutoMirrored.Filled.Reply, contentDescription = stringResource(Res.string.reply))
    },
)

@Composable
internal fun MessageStatusButton(onStatusClick: () -> Unit = {}, status: MessageStatus, fromLocal: Boolean) =
    AnimatedVisibility(visible = fromLocal) {
        IconButton(onClick = onStatusClick) {
            Crossfade(targetState = status, label = "MessageStatusIcon") { currentStatus ->
                Icon(
                    imageVector =
                    when (currentStatus) {
                        MessageStatus.RECEIVED -> Icons.TwoTone.HowToReg
                        MessageStatus.QUEUED -> Icons.TwoTone.CloudUpload
                        MessageStatus.DELIVERED -> Icons.TwoTone.CloudDone
                        MessageStatus.SFPP_ROUTING -> Icons.TwoTone.AddLink
                        MessageStatus.SFPP_CONFIRMED -> Icons.TwoTone.Link
                        MessageStatus.ENROUTE -> Icons.TwoTone.Cloud
                        MessageStatus.ERROR -> Icons.TwoTone.CloudOff
                        else -> Icons.TwoTone.Warning
                    },
                    contentDescription = stringResource(Res.string.message_delivery_status),
                )
            }
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MessageActions(
    modifier: Modifier = Modifier,
    isLocal: Boolean = false,
    status: MessageStatus?,
    onSendReaction: (String) -> Unit = {},
    onSendReply: () -> Unit = {},
    onStatusClick: () -> Unit = {},
) {
    Row(modifier = modifier.wrapContentSize()) {
        ReactionButton { onSendReaction(it) }
        ReplyButton { onSendReply() }
        MessageStatusButton(
            onStatusClick = onStatusClick,
            status = status ?: MessageStatus.UNKNOWN,
            fromLocal = isLocal,
        )
    }
}

