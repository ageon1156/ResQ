

package com.geeksville.mesh.ui.contact

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geeksville.mesh.model.Contact
import org.meshtastic.core.ui.component.SecurityIcon
import org.meshtastic.core.ui.theme.RiverShape
import org.meshtastic.core.ui.theme.organicSpring
import org.meshtastic.proto.AppOnlyProtos

@Composable
fun OrganicContactItem(
    contact: Contact,
    selected: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onNodeChipClick: () -> Unit = {},
    channels: AppOnlyProtos.ChannelSet? = null,
) = with(contact) {
    val scale by animateFloatAsState(targetValue = if (selected) 0.95f else 1f, animationSpec = organicSpring(), label = "contact_scale")
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isActive -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    Card(
        modifier = modifier.scale(scale).combinedClickable(onClick = onClick, onLongClick = onLongClick).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).semantics { contentDescription = shortName },
        shape = RiverShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OrganicContactAvatar(name = longName, shortName = shortName, seed = contactKey.hashCode().toLong(), onClick = onNodeChipClick, channels = channels, contact = contact)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = longName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, overflow = TextOverflow.Ellipsis, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                    if (!lastMessageTime.isNullOrEmpty()) {
                        Text(text = lastMessageTime.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = lastMessageText.orEmpty(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        MuteIcon(visible = isMuted)
                        UnreadBadge(count = unreadCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganicContactAvatar(name: String, shortName: String, seed: Long, onClick: () -> Unit, channels: AppOnlyProtos.ChannelSet?, contact: Contact) {
    val gradient = remember(seed) {
        val hue = (seed % 360).toFloat()
        val c1 = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.5f, 0.7f))
        val c2 = android.graphics.Color.HSVToColor(floatArrayOf((hue + 60) % 360, 0.6f, 0.8f))
        Brush.linearGradient(colors = listOf(Color(c1), Color(c2)))
    }
    val initials = remember(name) {
        name.split(" ").take(2).map { it.firstOrNull()?.uppercase() ?: "" }.joinToString("").take(2).ifEmpty { shortName.take(2).uppercase() }
    }
    Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(gradient).combinedClickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = initials, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center)
        if (contact.contactKey.isBroadcastContactKey() && channels != null) {
            contact.contactKey[0].digitToIntOrNull()?.let { index ->
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) { SecurityIcon(channels, index) }
            }
        }
    }
}
