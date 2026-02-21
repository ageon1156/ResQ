
package org.meshtastic.core.data.repository

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Application
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.location.LocationCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.location.altitude.AltitudeConverterCompat
import co.touchlab.kermit.Logger
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.meshtastic.core.analytics.platform.PlatformAnalytics
import org.meshtastic.core.di.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository
@Inject
constructor(
    private val context: Application,
    private val locationManager: dagger.Lazy<LocationManager>,
    private val analytics: PlatformAnalytics,
    private val dispatchers: CoroutineDispatchers,
) {

    private val _receivingLocationUpdates: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val receivingLocationUpdates: StateFlow<Boolean>
        get() = _receivingLocationUpdates

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    private fun LocationManager.requestLocationUpdates() = callbackFlow {
        val intervalMs = 30 * 1000L 
        val minDistanceM = 0f

        val locationRequest =
            LocationRequestCompat.Builder(intervalMs)
                .setMinUpdateDistanceMeters(minDistanceM)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .build()

        val locationListener = LocationListenerCompat { location ->
            if (location.hasAltitude() && !LocationCompat.hasMslAltitude(location)) {
                try {
                    AltitudeConverterCompat.addMslAltitudeToLocation(context, location)
                } catch (e: Exception) {
                    Logger.e(e) { "addMslAltitudeToLocation() failed" }
                }
            }
            
            trySend(location)
        }

        val providerList = buildList {
            val providers = allProviders
            if (android.os.Build.VERSION.SDK_INT >= 31 && LocationManager.FUSED_PROVIDER in providers) {
                add(LocationManager.FUSED_PROVIDER)
            } else {
                if (LocationManager.GPS_PROVIDER in providers) add(LocationManager.GPS_PROVIDER)
                if (LocationManager.NETWORK_PROVIDER in providers) add(LocationManager.NETWORK_PROVIDER)
            }
        }

        Logger.i {
            "Starting location updates with $providerList intervalMs=${intervalMs}ms and minDistanceM=${minDistanceM}m"
        }
        _receivingLocationUpdates.value = true
        analytics.track("location_start") 

        try {
            providerList.forEach { provider ->
                LocationManagerCompat.requestLocationUpdates(
                    this@requestLocationUpdates,
                    provider,
                    locationRequest,
                    dispatchers.io.asExecutor(),
                    locationListener,
                )
            }
        } catch (e: Exception) {
            close(e) 
        }

        awaitClose {
            Logger.i { "Stopping location requests" }
            _receivingLocationUpdates.value = false
            analytics.track("location_stop")

            LocationManagerCompat.removeUpdates(this@requestLocationUpdates, locationListener)
        }
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun getLocations() = locationManager.get().requestLocationUpdates()
}

