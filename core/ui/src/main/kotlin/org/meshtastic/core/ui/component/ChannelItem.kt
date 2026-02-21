

package org.meshtastic.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun ChannelItem(
    index: Int,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    val fontColor = if (index == 0) MaterialTheme.colorScheme.primary else Color.Unspecified
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable(enabled = enabled) { onClick() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
        ) {
            AssistChip(onClick = onClick, label = { Text(text = "$index", color = fontColor) })
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
                color = fontColor,
            )
            content()
        }
    }
}

@Preview
@Composable
private fun ChannelItemPreview() {
    AppTheme { ChannelItem(index = 0, title = "Medium Fast", enabled = true) {} }
}

