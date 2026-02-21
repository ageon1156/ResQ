

package com.geeksville.mesh.ui.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.geeksville.mesh.model.DeviceListEntry
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.ui.component.TitledCard

@Composable
fun List<DeviceListEntry>.DeviceListSection(
    title: String,
    connectionState: ConnectionState,
    selectedDevice: String,
    onSelect: (DeviceListEntry) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: ((DeviceListEntry) -> Unit)? = null,
) {
    if (isNotEmpty()) {
        TitledCard(title = title, modifier = modifier) {
            forEach { device ->
                OrganicDeviceListItem(
                    connectionState =
                    connectionState.takeIf { device.fullAddress == selectedDevice } ?: ConnectionState.Disconnected,
                    device = device,
                    onSelect = { onSelect(device) },
                    onDelete = onDelete?.let { delete -> { delete(device) } },
                    modifier = Modifier.Companion,
                )
            }
        }
    }
}

