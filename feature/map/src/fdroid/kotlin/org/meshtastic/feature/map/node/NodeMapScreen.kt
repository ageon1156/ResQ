

package org.meshtastic.feature.map.node

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.meshtastic.feature.map.addCopyright
import org.meshtastic.feature.map.addPolyline
import org.meshtastic.feature.map.addPositionMarkers
import org.meshtastic.feature.map.addScaleBarOverlay
import org.meshtastic.feature.map.rememberMapViewWithLifecycle
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

private const val DEG_D = 1e-7

@Composable
fun NodeMapScreen(nodeMapViewModel: NodeMapViewModel, onNavigateUp: () -> Unit) {
    val density = LocalDensity.current
    val positionLogs by nodeMapViewModel.positionLogs.collectAsStateWithLifecycle()
    val geoPoints = positionLogs.map { GeoPoint(it.latitudeI * DEG_D, it.longitudeI * DEG_D) }
    val cameraView = remember { BoundingBox.fromGeoPoints(geoPoints) }
    val mapView =
        rememberMapViewWithLifecycle(
            applicationId = nodeMapViewModel.applicationId,
            box = cameraView,
            tileSource = nodeMapViewModel.tileSource,
        )

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            map.overlays.clear()
            map.addCopyright()
            map.addScaleBarOverlay(density)

            map.addPolyline(density, geoPoints) {}
            map.addPositionMarkers(positionLogs) {}
        },
    )
}

