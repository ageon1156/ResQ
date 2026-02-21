

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.Channel

@Composable
fun ChannelSelection(
    index: Int,
    title: String,
    enabled: Boolean,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit,
    channel: Channel,
) = ChannelItem(index = index, title = title, enabled = enabled) {
    SecurityIcon(channel)
    Spacer(modifier = Modifier.width(10.dp))
    Checkbox(enabled = enabled, checked = isSelected, onCheckedChange = onSelected)
}

