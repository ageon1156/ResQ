

package org.meshtastic.feature.node.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.icon.Elevation
import org.meshtastic.core.ui.icon.MeshtasticIcons

private const val SIZE_ICON = 20

@Composable
fun IconInfo(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            modifier = Modifier.size(SIZE_ICON.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
        )
        text?.let { Text(text = it, style = MaterialTheme.typography.labelMedium, color = contentColor) }
        content()
    }
}

@Composable
@Preview
private fun IconInfoPreview() {
    MaterialTheme {
        IconInfo(icon = MeshtasticIcons.Elevation, contentDescription = "Elevation", content = { Text(text = "100") })
    }
}

