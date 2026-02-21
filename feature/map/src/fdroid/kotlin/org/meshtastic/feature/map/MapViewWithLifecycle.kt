

package org.meshtastic.feature.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import co.touchlab.kermit.Logger
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

@SuppressLint("WakelockTimeout")
private fun PowerManager.WakeLock.safeAcquire() {
    if (!isHeld) {
        try {
            acquire()
        } catch (e: SecurityException) {
            Logger.e { "WakeLock permission exception: ${e.message}" }
        } catch (e: IllegalStateException) {
            Logger.e { "WakeLock acquire() exception: ${e.message}" }
        }
    }
}

private fun PowerManager.WakeLock.safeRelease() {
    if (isHeld) {
        try {
            release()
        } catch (e: IllegalStateException) {
            Logger.e { "WakeLock release() exception: ${e.message}" }
        }
    }
}

private const val MIN_ZOOM_LEVEL = 1.5
private const val MAX_ZOOM_LEVEL = 20.0
private const val DEFAULT_ZOOM_LEVEL = 17.0

@Suppress("MagicNumber")
@Composable
fun rememberMapViewWithLifecycle(
    applicationId: String,
    box: BoundingBox,
    tileSource: ITileSource = TileSourceFactory.DEFAULT_TILE_SOURCE,
): MapView {
    val zoom =
        if (box.requiredZoomLevel().isFinite()) {
            (box.requiredZoomLevel() - 0.5).coerceAtLeast(MIN_ZOOM_LEVEL)
        } else {
            DEFAULT_ZOOM_LEVEL
        }
    val center = GeoPoint(box.centerLatitude, box.centerLongitude)
    return rememberMapViewWithLifecycle(
        applicationId = applicationId,
        zoomLevel = zoom,
        mapCenter = center,
        tileSource = tileSource,
    )
}

@Suppress("LongMethod")
@Composable
internal fun rememberMapViewWithLifecycle(
    applicationId: String,
    zoomLevel: Double = MIN_ZOOM_LEVEL,
    mapCenter: GeoPoint = GeoPoint(0.0, 0.0),
    tileSource: ITileSource = TileSourceFactory.DEFAULT_TILE_SOURCE,
): MapView {
    var savedZoom by rememberSaveable { mutableDoubleStateOf(zoomLevel) }
    var savedCenter by
        rememberSaveable(
            stateSaver =
            Saver(
                save = { mapOf("latitude" to it.latitude, "longitude" to it.longitude) },
                restore = { GeoPoint(it["latitude"] ?: 0.0, it["longitude"] ?: .0) },
            ),
        ) {
            mutableStateOf(mapCenter)
        }

    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            clipToOutline = true

            Configuration.getInstance().userAgentValue = applicationId
            setTileSource(tileSource)
            isVerticalMapRepetitionEnabled = false 
            setMultiTouchControls(true)
            val bounds = overlayManager.tilesOverlay.bounds 
            setScrollableAreaLimitLatitude(bounds.actualNorth, bounds.actualSouth, 0)
            
            isTilesScaledToDpi = true
            
            minZoomLevel = MIN_ZOOM_LEVEL
            maxZoomLevel = MAX_ZOOM_LEVEL
            
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)

            controller.setZoom(savedZoom)
            controller.setCenter(savedCenter)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Meshtastic:MapViewLock")

        wakeLock.safeAcquire()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wakeLock.safeRelease()
                    mapView.onPause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    wakeLock.safeAcquire()
                    mapView.onResume()
                }

                Lifecycle.Event.ON_STOP -> {
                    savedCenter = mapView.projection.currentCenter
                    savedZoom = mapView.zoomLevelDouble
                }

                else -> {}
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            wakeLock.safeRelease()
        }
    }
    return mapView
}

