

package com.geeksville.mesh.ui.connections.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.geeksville.mesh.ui.connections.DeviceType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.bluetooth
import org.meshtastic.core.strings.network
import org.meshtastic.core.strings.serial
import org.meshtastic.core.ui.theme.AppTheme

@Suppress("LambdaParameterEventTrailing")
@Composable
fun ConnectionsSegmentedBar(
    selectedDeviceType: DeviceType,
    modifier: Modifier = Modifier,
    onClickDeviceType: (DeviceType) -> Unit,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .height(40.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Item.entries.forEachIndexed { index, item ->
            val text = stringResource(item.textRes)
            val selected = item.deviceType == selectedDeviceType
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onClickDeviceType(item.deviceType) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = item.imageVector,
                        contentDescription = text,
                        modifier = Modifier.height(16.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = text.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            if (index < Item.entries.size - 1) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

private enum class Item(val imageVector: ImageVector, val textRes: StringResource, val deviceType: DeviceType) {
    BLUETOOTH(imageVector = Icons.Rounded.Bluetooth, textRes = Res.string.bluetooth, deviceType = DeviceType.BLE),
    NETWORK(imageVector = Icons.Rounded.Wifi, textRes = Res.string.network, deviceType = DeviceType.TCP),
    SERIAL(imageVector = Icons.Rounded.Usb, textRes = Res.string.serial, deviceType = DeviceType.USB),
}

@Preview(showBackground = true)
@Composable
private fun ConnectionsSegmentedBarPreview() {
    AppTheme { ConnectionsSegmentedBar(selectedDeviceType = DeviceType.BLE) {} }
}

