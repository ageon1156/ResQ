

package org.meshtastic.feature.node.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SocialDistance
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.distance
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun DistanceInfo(
    distance: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconInfo(
        modifier = modifier,
        icon = Icons.Rounded.SocialDistance,
        contentDescription = stringResource(Res.string.distance),
        text = distance,
        contentColor = contentColor,
    )
}

@PreviewLightDark
@Composable
private fun DistanceInfoPreview() {
    AppTheme { DistanceInfo(distance = "423 mi.") }
}

