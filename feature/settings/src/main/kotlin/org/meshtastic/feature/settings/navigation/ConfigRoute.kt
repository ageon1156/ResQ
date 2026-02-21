/*
 * Copyright (c) 2025 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

