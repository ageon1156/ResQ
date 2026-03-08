
package com.geeksville.mesh.service

import android.annotation.SuppressLint
import android.app.Application
import androidx.core.location.LocationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.meshtastic.core.common.hasLocationPermission
import org.meshtastic.core.data.repository.LocationRepository
import org.meshtastic.core.model.Position
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.position
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class MeshLocationManager
@Inject
constructor(
    private val context: Application,
    private val locationRepository: LocationRepository,
) {
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var locationFlow: Job? = null

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, sendPositionFn: (MeshProtos.Position) -> Unit) {
        this.scope = scope
        if (locationFlow?.isActive == true) return

        if (context.hasLocationPermission()) {
            locationFlow =
                locationRepository
                    .getLocations()
                    .onEach { location ->
                        sendPositionFn(
                            position {
                                latitudeI = Position.degI(location.latitude)
                                longitudeI = Position.degI(location.longitude)
                                if (LocationCompat.hasMslAltitude(location)) {
                                    altitude = LocationCompat.getMslAltitudeMeters(location).toInt()
                                }
                                altitudeHae = location.altitude.toInt()
                                time = (location.time.milliseconds.inWholeSeconds).toInt()
                                groundSpeed = location.speed.toInt()
                                groundTrack = location.bearing.toInt()
                                locationSource = MeshProtos.Position.LocSource.LOC_EXTERNAL
                            },
                        )
                    }
                    .launchIn(scope)
        }
    }

    fun stop() {
        if (locationFlow?.isActive == true) {
            locationFlow?.cancel()
            locationFlow = null
        }
    }
}

