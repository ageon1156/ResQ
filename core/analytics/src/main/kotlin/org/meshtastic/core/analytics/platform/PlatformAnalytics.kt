

package org.meshtastic.core.analytics.platform

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import org.meshtastic.core.analytics.DataPair

interface PlatformAnalytics {

    fun track(event: String, vararg properties: DataPair)

    fun setDeviceAttributes(firmwareVersion: String, model: String)

    @Composable fun AddNavigationTrackingEffect(navController: NavHostController)

    val isPlatformServicesAvailable: Boolean
}

