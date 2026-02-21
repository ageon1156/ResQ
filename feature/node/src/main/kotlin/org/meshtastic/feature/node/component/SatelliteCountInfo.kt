

package org.meshtastic.feature.node.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.SatelliteAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.sats
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun SatelliteCountInfo(
    modifier: Modifier = Modifier,
    satCount: Int,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconInfo(
        modifier = modifier,
        icon = Icons.TwoTone.SatelliteAlt,
        contentDescription = stringResource(Res.string.sats),
        text = "$satCount",
        contentColor = contentColor,
    )
}

@PreviewLightDark
@Composable
private fun SatelliteCountInfoPreview() {
    AppTheme { SatelliteCountInfo(satCount = 5) }
}

