
package com.geeksville.mesh.ui.connections.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geeksville.mesh.model.DeviceListEntry
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.bluetooth
import org.meshtastic.core.strings.network
import org.meshtastic.core.strings.serial
import org.meshtastic.core.ui.theme.LeafShape

@Composable
fun OrganicDeviceListItem(
    connectionState: ConnectionState,
    device: DeviceListEntry,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val icon = when (device) {
        is DeviceListEntry.Ble ->
            if (connectionState.isConnected()) Icons.Rounded.BluetoothConnected
            else if (connectionState.isConnecting()) Icons.AutoMirrored.Rounded.BluetoothSearching
            else Icons.Rounded.Bluetooth
        is DeviceListEntry.Usb -> Icons.Rounded.Usb
        is DeviceListEntry.Tcp -> Icons.Rounded.Wifi
    }

    val contentDescription = when (device) {
        is DeviceListEntry.Ble -> stringResource(Res.string.bluetooth)
        is DeviceListEntry.Usb -> stringResource(Res.string.serial)
        is DeviceListEntry.Tcp -> stringResource(Res.string.network)
    }

    val iconColor = when (device) {
        is DeviceListEntry.Ble -> MaterialTheme.colorScheme.primary
        is DeviceListEntry.Usb -> MaterialTheme.colorScheme.secondary
        is DeviceListEntry.Tcp -> MaterialTheme.colorScheme.tertiary
    }

    val isConnected = connectionState.isConnected()
    val isConnecting = connectionState.isConnecting()

    val accentColor = when {
        isConnected -> MaterialTheme.colorScheme.primary
        isConnecting -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onSelect, onLongClick = onDelete)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(72.dp)
                        .background(accentColor)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(LeafShape)
                            .background(iconColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = iconColor,
                                strokeWidth = 2.dp
                            )
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = contentDescription,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        AnimatedVisibility(visible = isConnecting, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = "CONNECTING...",
                                style = MaterialTheme.typography.labelSmall,
                                color = iconColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        AnimatedVisibility(visible = isConnected, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = "CONNECTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isConnected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
