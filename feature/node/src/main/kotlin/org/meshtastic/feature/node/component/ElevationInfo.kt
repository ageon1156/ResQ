

package org.meshtastic.feature.node.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.util.metersIn
import org.meshtastic.core.model.util.toString
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.altitude
import org.meshtastic.core.strings.elevation_suffix
import org.meshtastic.core.ui.icon.Elevation
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.proto.ConfigProtos.Config.DisplayConfig.DisplayUnits

@Composable
fun ElevationInfo(
    modifier: Modifier = Modifier,
    altitude: Int,
    system: DisplayUnits,
    suffix: String = stringResource(Res.string.elevation_suffix),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconInfo(
        modifier = modifier,
        icon = MeshtasticIcons.Elevation,
        contentDescription = stringResource(Res.string.altitude),
        text = altitude.metersIn(system).toString(system) + " " + suffix,
        contentColor = contentColor,
    )
}

@Composable
@Preview
private fun ElevationInfoPreview() {
    MaterialTheme { ElevationInfo(altitude = 100, system = DisplayUnits.METRIC, suffix = "ASL") }
}

