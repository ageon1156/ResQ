

package org.meshtastic.feature.node.compass

import androidx.compose.ui.graphics.Color
import org.meshtastic.proto.ConfigProtos.Config.DisplayConfig.DisplayUnits

private const val DEFAULT_TARGET_COLOR_HEX = 0xFFFF9800

enum class CompassWarning {
    NO_MAGNETOMETER,
    NO_LOCATION_PERMISSION,
    LOCATION_DISABLED,
    NO_LOCATION_FIX,
}

data class CompassUiState(
    val targetName: String = "",
    val targetColor: Color = Color(DEFAULT_TARGET_COLOR_HEX),
    val heading: Float? = null,
    val bearing: Float? = null,
    val distanceText: String? = null,
    val bearingText: String? = null,
    val lastUpdateText: String? = null,
    val positionTimeSec: Long? = null, 
    val warnings: List<CompassWarning> = emptyList(),
    val errorRadiusText: String? = null,
    val angularErrorDeg: Float? = null,
    val isAligned: Boolean = false,
    val hasTargetPosition: Boolean = true,
    val displayUnits: DisplayUnits = DisplayUnits.METRIC,
    val targetAltitude: Int? = null,
)

