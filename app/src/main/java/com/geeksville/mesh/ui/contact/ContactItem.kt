

package com.geeksville.mesh.ui.contact

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.geeksville.mesh.model.Contact
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.sample_message
import org.meshtastic.core.strings.some_username
import org.meshtastic.core.strings.unknown_username
import org.meshtastic.core.ui.component.SecurityIcon
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.proto.AppOnlyProtos

@Composable
fun ContactItem(
    contact: Contact,
    selected: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onNodeChipClick: () -> Unit = {},
    channels: AppOnlyProtos.ChannelSet? = null,
) = with(contact) {
    val isOutlined = !selected && !isActive
    val colors = if (isOutlined) {
        CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    } else {
        CardDefaults.cardColors(containerColor = if (selected) Color.Gray else MaterialTheme.colorScheme.surfaceVariant)
    }
    Card(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = shortName },
        shape = RoundedCornerShape(4.dp),
        colors = colors,
        border = if (isOutlined) CardDefaults.outlinedCardBorder() else null,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            ContactHeader(contact = contact, channels = channels, onNodeChipClick = onNodeChipClick)
            ChatMetadata(modifier = Modifier.padding(top = 4.dp), contact = contact)
        }
    }
}

@Composable
private fun ContactHeader(
    contact: Contact,
    channels: AppOnlyProtos.ChannelSet?,
    modifier: Modifier = Modifier,
    onNodeChipClick: () -> Unit = {},
) {
    val chipColors = if (contact.nodeColors != null) {
        AssistChipDefaults.assistChipColors(
            labelColor = Color(contact.nodeColors.first),
            containerColor = Color(contact.nodeColors.second),
        )
    } else AssistChipDefaults.assistChipColors()

    Row(modifier = modifier.padding(0.dp), verticalAlignment = Alignment.CenterVertically) {
        AssistChip(
            onClick = onNodeChipClick,
            modifier = Modifier.width(IntrinsicSize.Min).height(32.dp).semantics { contentDescription = contact.shortName },
            label = {
                Text(text = contact.shortName, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            },
            colors = chipColors,
        )
        if (contact.contactKey.isBroadcastContactKey() && channels != null) {
            contact.contactKey[0].digitToIntOrNull()?.let { SecurityIcon(channels, it) }
        }
        Text(modifier = Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, overflow = TextOverflow.Ellipsis, maxLines = 1, text = contact.longName)
        Text(text = contact.lastMessageTime.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ChatMetadata(contact: Contact, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = contact.lastMessageText.orEmpty(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis, maxLines = 2)
        MuteIcon(visible = contact.isMuted, modifier = Modifier.padding(start = 4.dp))
        UnreadBadge(count = contact.unreadCount, modifier = Modifier.padding(start = 4.dp))
    }
}

@PreviewLightDark
@Composable
private fun ContactItemPreview() {
    val sample = Contact(contactKey = "0^all", shortName = stringResource(Res.string.some_username), longName = stringResource(Res.string.unknown_username), lastMessageTime = "Mon", lastMessageText = stringResource(Res.string.sample_message), unreadCount = 2, messageCount = 10, isMuted = true, isUnmessageable = false)
    AppTheme { Column { listOf(sample, sample.copy(shortName = "0", longName = "A very long contact name that should be truncated.", lastMessageTime = "15 minutes ago")).forEach { ContactItem(contact = it, selected = false) } } }
}
