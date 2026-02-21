

package org.meshtastic.feature.settings.radio.channel.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.channels
import org.meshtastic.core.strings.freq
import org.meshtastic.core.strings.slot
import org.meshtastic.core.ui.component.PreferenceCategory
import org.meshtastic.core.ui.theme.AppTheme

@Composable
internal fun ChannelConfigHeader(frequency: Float, slot: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreferenceCategory(text = stringResource(Res.string.channels))
        Column {
            Text(text = "${stringResource(Res.string.freq)}: ${frequency}MHz", fontSize = 11.sp)
            Text(text = "${stringResource(Res.string.slot)}: $slot", fontSize = 11.sp)
        }
    }
}

@Preview
@Composable
private fun ChannelConfigHeaderPreview() {
    AppTheme { ChannelConfigHeader(frequency = 913.125f, slot = 45) }
}

