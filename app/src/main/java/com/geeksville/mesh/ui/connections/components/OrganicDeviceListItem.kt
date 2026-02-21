
package com.geeksville.mesh.ui.connections.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geeksville.mesh.model.DeviceListEntry
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.add
import org.meshtastic.core.strings.bluetooth
import org.meshtastic.core.strings.network
import org.meshtastic.core.strings.serial
import org.meshtastic.core.ui.theme.LeafShape
import org.meshtastic.core.ui.theme.organicSpring

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
        is DeviceListEntry.Mock -> Icons.Rounded.Add
    }

    val contentDescription = when (device) {
        is DeviceListEntry.Ble -> stringResource(Res.string.bluetooth)
        is DeviceListEntry.Usb -> stringResource(Res.string.serial)
        is DeviceListEntry.Tcp -> stringResource(Res.string.network)
        is DeviceListEntry.Mock -> stringResource(Res.string.add)
    }

    val iconColor = when (device) {
        is DeviceListEntry.Ble -> MaterialTheme.colorScheme.primary
        is DeviceListEntry.Usb -> MaterialTheme.colorScheme.secondary
        is DeviceListEntry.Tcp -> MaterialTheme.colorScheme.tertiary
        is DeviceListEntry.Mock -> MaterialTheme.colorScheme.outline
    }

    val isSelected = connectionState.isConnected()
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = organicSpring(),
        label = "device_scale"
    )

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Card(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onDelete
            ),
        shape = LeafShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                
                if (connectionState.isConnecting()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = iconColor,
                        strokeWidth = 3.dp
                    )
                }

                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )

                device.address?.let { address ->
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }

                if (connectionState.isConnecting()) {
                    Text(
                        text = "Connecting...",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor
                    )
                } else if (connectionState.isConnected()) {
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isSelected) {
                RadioButton(
                    selected = true,
                    onClick = null
                )
            }
        }
    }
}

