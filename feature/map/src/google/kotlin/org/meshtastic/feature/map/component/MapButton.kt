

package org.meshtastic.feature.map.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.map_style_selection
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun MapButton(
    icon: ImageVector,
    contentDescription: StringResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    MapButton(
        icon = icon,
        contentDescription = stringResource(contentDescription),
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun MapButton(icon: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
}

@PreviewLightDark
@Composable
private fun MapButtonPreview() {
    AppTheme { MapButton(icon = Icons.Outlined.Layers, contentDescription = Res.string.map_style_selection) }
}
