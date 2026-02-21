

package org.meshtastic.feature.settings.radio.channel.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.delete
import org.meshtastic.core.ui.component.ChannelItem
import org.meshtastic.core.ui.component.SecurityIcon
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.proto.ChannelProtos.ChannelSettings
import org.meshtastic.proto.ConfigKt.loRaConfig
import org.meshtastic.proto.ConfigProtos.Config.LoRaConfig
import org.meshtastic.proto.channelSettings

@Composable
internal fun ChannelCard(
    index: Int,
    title: String,
    enabled: Boolean,
    channelSettings: ChannelSettings,
    loraConfig: LoRaConfig,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    sharesLocation: Boolean,
) = ChannelItem(index = index, title = title, enabled = enabled, onClick = onEditClick) {
    if (sharesLocation) {
        Icon(
            imageVector = ChannelIcons.LOCATION.icon,
            contentDescription = stringResource(ChannelIcons.LOCATION.descriptionResId),
            modifier = Modifier.wrapContentSize().padding(horizontal = 5.dp),
        )
    }
    if (channelSettings.uplinkEnabled) {
        Icon(
            imageVector = ChannelIcons.UPLINK.icon,
            contentDescription = stringResource(ChannelIcons.UPLINK.descriptionResId),
            modifier = Modifier.wrapContentSize().padding(horizontal = 5.dp),
        )
    }
    if (channelSettings.downlinkEnabled) {
        Icon(
            imageVector = ChannelIcons.DOWNLINK.icon,
            contentDescription = stringResource(ChannelIcons.DOWNLINK.descriptionResId),
            modifier = Modifier.wrapContentSize().padding(horizontal = 5.dp),
        )
    }
    SecurityIcon(channelSettings, loraConfig)
    Spacer(modifier = Modifier.width(10.dp))
    IconButton(onClick = { onDeleteClick() }) {
        Icon(
            imageVector = Icons.TwoTone.Close,
            contentDescription = stringResource(Res.string.delete),
            modifier = Modifier.wrapContentSize(),
        )
    }
}

@Preview
@Composable
private fun ChannelCardPreview() {
    AppTheme {
        ChannelCard(
            index = 0,
            title = "Medium Fast",
            enabled = true,
            channelSettings =
            channelSettings {
                uplinkEnabled = true
                downlinkEnabled = true
            },
            loraConfig = loRaConfig {},
            onEditClick = {},
            onDeleteClick = {},
            sharesLocation = true,
        )
    }
}

