

package com.geeksville.mesh.ui.connections.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UsbOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.geeksville.mesh.model.BTScanModel
import com.geeksville.mesh.model.DeviceListEntry
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.no_usb_devices
import org.meshtastic.core.strings.usb_devices
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun UsbDevices(
    connectionState: ConnectionState,
    usbDevices: List<DeviceListEntry>,
    selectedDevice: String,
    scanModel: BTScanModel,
) {
    UsbDevicesInternal(
        connectionState = connectionState,
        usbDevices = usbDevices,
        selectedDevice = selectedDevice,
        onDeviceSelected = scanModel::onSelected,
    )
}

@Composable
private fun UsbDevicesInternal(
    connectionState: ConnectionState,
    usbDevices: List<DeviceListEntry>,
    selectedDevice: String,
    onDeviceSelected: (DeviceListEntry) -> Unit,
) {
    when {
        usbDevices.isEmpty() ->
            EmptyStateContent(imageVector = Icons.Rounded.UsbOff, text = stringResource(Res.string.no_usb_devices))

        else ->
            usbDevices.DeviceListSection(
                title = stringResource(Res.string.usb_devices),
                connectionState = connectionState,
                selectedDevice = selectedDevice,
                onSelect = onDeviceSelected,
            )
    }
}

@PreviewLightDark
@Composable
private fun UsbDevicesPreview() {
    AppTheme {
        UsbDevicesInternal(
            connectionState = ConnectionState.Connected,
            usbDevices = emptyList(),
            selectedDevice = "",
            onDeviceSelected = {},
        )
    }
}

