

package org.meshtastic.core.analytics.platform

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import org.meshtastic.core.analytics.BuildConfig
import org.meshtastic.core.analytics.DataPair
import javax.inject.Inject

class FdroidPlatformAnalytics @Inject constructor() : PlatformAnalytics {
    init {

        if (BuildConfig.DEBUG) {
            Logger.setMinSeverity(Severity.Debug)
            Logger.i { "F-Droid platform no-op analytics initialized (Debug mode }." }
        } else {
            Logger.setMinSeverity(Severity.Info)
            Logger.i { "F-Droid platform no-op analytics initialized." }
        }
    }

    override fun setDeviceAttributes(firmwareVersion: String, model: String) {
        
        Logger.d { "Set device attributes called: firmwareVersion=$firmwareVersion, deviceHardware=$model" }
    }

    @Composable
    override fun AddNavigationTrackingEffect(navController: NavHostController) {
        
        if (BuildConfig.DEBUG) {
            navController.addOnDestinationChangedListener { _, destination, _ ->
                Logger.d { "Navigation changed to: ${destination.route}" }
            }
        }
    }

    override val isPlatformServicesAvailable: Boolean
        get() = false

    override fun track(event: String, vararg properties: DataPair) {
        Logger.d { "Track called: event=$event, properties=${properties.toList()}" }
    }
}

