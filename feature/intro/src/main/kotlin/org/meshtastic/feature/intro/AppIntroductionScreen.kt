

package org.meshtastic.feature.intro

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.communicate_off_the_grid
import org.meshtastic.core.strings.create_your_own_networks
import org.meshtastic.core.strings.easily_set_up_private_mesh_networks
import org.meshtastic.core.strings.get_started
import org.meshtastic.core.strings.intro_welcome
import org.meshtastic.core.strings.meshtastic
import org.meshtastic.core.strings.share_your_location_in_real_time
import org.meshtastic.core.strings.stay_connected_anywhere
import org.meshtastic.core.strings.track_and_share_locations

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppIntroductionScreen(onDone: () -> Unit) {
    
    val allPermissions = remember {
        buildList {
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            
            add(Manifest.permission.CAMERA)
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    val permissionsState = rememberMultiplePermissionsState(permissions = allPermissions)

    var permissionsLaunched by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && permissionsLaunched) {
                onDone()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val features = remember {
        listOf(
            FeatureUIData(
                icon = Icons.Outlined.SettingsInputAntenna,
                titleRes = Res.string.stay_connected_anywhere,
                subtitleRes = Res.string.communicate_off_the_grid,
            ),
            FeatureUIData(
                icon = Icons.Outlined.Hub,
                titleRes = Res.string.create_your_own_networks,
                subtitleRes = Res.string.easily_set_up_private_mesh_networks,
            ),
            FeatureUIData(
                icon = Icons.Outlined.NearMe,
                titleRes = Res.string.track_and_share_locations,
                subtitleRes = Res.string.share_your_location_in_real_time,
            ),
        )
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)) {
                Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
                Button(
                    onClick = {
                        if (permissionsState.allPermissionsGranted || allPermissions.isEmpty()) {
                            
                            onDone()
                        } else {
                            permissionsLaunched = true
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    },
                ) {
                    Text(stringResource(Res.string.get_started))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = org.meshtastic.core.ui.R.drawable.ic_meshtastic),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.intro_welcome),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.meshtastic),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(40.dp))
            features.forEach { feature ->
                FeatureRow(feature = feature)
                Spacer(modifier = Modifier.height(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
private fun AppIntroductionScreenPreview() {
    AppIntroductionScreen(onDone = {})
}
