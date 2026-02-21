

package com.geeksville.mesh.ui.connections.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        Item.entries.forEachIndexed { index, item ->
            val text = stringResource(item.textRes)
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, Item.entries.size),
                onClick = { onClickDeviceType(item.deviceType) },
                selected = item.deviceType == selectedDeviceType,
                icon = { Icon(imageVector = item.imageVector, contentDescription = text) },
                label = { Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
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

