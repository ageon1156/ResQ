

package com.geeksville.mesh.ui.connections.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geeksville.mesh.model.BTScanModel
import com.geeksville.mesh.model.DeviceListEntry
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.bluetooth_available_devices
import org.meshtastic.core.strings.bluetooth_disabled
import org.meshtastic.core.strings.bluetooth_paired_devices
import org.meshtastic.core.strings.grant_permissions
import org.meshtastic.core.strings.no_ble_devices
import org.meshtastic.core.strings.open_settings
import org.meshtastic.core.strings.permission_missing
import org.meshtastic.core.strings.permission_missing_31
import org.meshtastic.core.strings.scan
import org.meshtastic.core.strings.scanning_bluetooth
import org.meshtastic.core.ui.util.showToast

@OptIn(ExperimentalPermissionsApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun BLEDevices(
    connectionState: ConnectionState,
    bondedDevices: List<DeviceListEntry>,
    availableDevices: List<DeviceListEntry>,
    selectedDevice: String,
    scanModel: BTScanModel,
    bluetoothEnabled: Boolean,
) {
    LocalContext.current 
    val isScanning by scanModel.spinner.collectAsStateWithLifecycle(false)

    val bluetoothPermissionsList = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
    }

    val context = LocalContext.current
    val permsMissing =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stringResource(Res.string.permission_missing_31)
        } else {
            stringResource(Res.string.permission_missing)
        }
    val coroutineScope = rememberCoroutineScope()

    val singlePermissionState =
        rememberPermissionState(
            permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            onPermissionResult = { granted ->
                scanModel.refreshPermissions()
                scanModel.startScan()
            },
        )

    val permissionsState =
        rememberMultiplePermissionsState(
            permissions = bluetoothPermissionsList,
            onPermissionsResult = { permissions ->
                val granted = permissions.values.all { it }
                if (permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
                    coroutineScope.launch { context.showToast(permsMissing) }
                    singlePermissionState.launchPermissionRequest()
                }
                if (granted) {
                    scanModel.refreshPermissions()
                    scanModel.startScan()
                } else {
                    coroutineScope.launch { context.showToast(permsMissing) }
                }
            },
        )

    val settingsLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            scanModel.refreshPermissions()
            scanModel.startScan()
        }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (permissionsState.allPermissionsGranted) {
            when {
                !bluetoothEnabled -> {
                    val context = LocalContext.current
                    EmptyStateContent(
                        imageVector = Icons.Rounded.BluetoothDisabled,
                        text = stringResource(Res.string.bluetooth_disabled),
                        actionButton = {
                            val intent = Intent(ACTION_BLUETOOTH_SETTINGS)
                            if (intent.resolveActivity(context.packageManager) != null) {
                                Button(onClick = { settingsLauncher.launch(intent) }) {
                                    Text(text = stringResource(Res.string.open_settings))
                                }
                            }
                        },
                    )
                }

                else -> {
                    val scanButton: @Composable () -> Unit = {
                        Button(
                            enabled = !isScanning,
                            onClick = { checkPermissionsAndScan(permissionsState, scanModel, true) },
                            shape = MaterialTheme.shapes.extraSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            ),
                        ) {
                            Box {
                                Row(modifier = Modifier.alpha(if (isScanning) 0f else 1f)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = stringResource(Res.string.scan),
                                    )
                                    Text(stringResource(Res.string.scan).uppercase())
                                }
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp).align(Alignment.Center),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    if (bondedDevices.isEmpty() && availableDevices.isEmpty()) {
                        EmptyStateContent(
                            imageVector = Icons.Rounded.BluetoothDisabled,
                            text =
                            if (isScanning) {
                                stringResource(Res.string.scanning_bluetooth)
                            } else {
                                stringResource(Res.string.no_ble_devices)
                            },
                            actionButton = scanButton,
                        )
                    } else {
                        bondedDevices.DeviceListSection(
                            title = stringResource(Res.string.bluetooth_paired_devices),
                            connectionState = connectionState,
                            selectedDevice = selectedDevice,
                            onSelect = scanModel::onSelected,
                        )

                        availableDevices.DeviceListSection(
                            title = stringResource(Res.string.bluetooth_available_devices),
                            connectionState = connectionState,
                            selectedDevice = selectedDevice,
                            onSelect = scanModel::onSelected,
                        )

                        scanButton()
                    }
                }
            }
        } else {
            
            EmptyStateContent(
                text =
                if (permissionsState.shouldShowRationale) {
                    stringResource(Res.string.permission_missing)
                } else {
                    stringResource(Res.string.permission_missing_31)
                },
                actionButton = {
                    Button(onClick = { checkPermissionsAndScan(permissionsState, scanModel, bluetoothEnabled) }) {
                        Text(text = stringResource(Res.string.grant_permissions))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun checkPermissionsAndScan(
    permissionsState: MultiplePermissionsState,
    scanModel: BTScanModel,
    bluetoothEnabled: Boolean,
) {
    if (permissionsState.allPermissionsGranted && bluetoothEnabled) {
        scanModel.startScan()
    } else {
        permissionsState.launchMultiplePermissionRequest()
    }
}

