

package org.meshtastic.feature.settings.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoutes
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.channels

enum class ConfigRoute(val title: StringResource, val route: Route, val icon: ImageVector?, val type: Int = 0) {
    CHANNELS(Res.string.channels, SettingsRoutes.ChannelConfig, Icons.AutoMirrored.Default.List, 0),
    ;
}

