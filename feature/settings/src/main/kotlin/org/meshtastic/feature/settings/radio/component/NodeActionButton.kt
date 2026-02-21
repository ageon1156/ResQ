

package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun NodeActionButton(
    modifier: Modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(48.dp),
    title: String,
    enabled: Boolean,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    onClick: () -> Unit,
) {
    Button(onClick = { onClick() }, enabled = enabled, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint ?: LocalContentColor.current,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}

