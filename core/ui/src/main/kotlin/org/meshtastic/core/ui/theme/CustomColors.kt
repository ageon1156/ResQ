

package org.meshtastic.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MeshtasticGreen = Color(0xFF1D9BF0) 
val MeshtasticAlt = Color(0xFF020817) 
val HyperlinkBlue = Color(0xFF38BDF8) 
val AnnotationColor = Color(0xFF039BE5)

object TracerouteColors {

    val OutgoingRoute = Color(0xCCE86A00) 
    val ReturnRoute = Color(0xCC0081C7) 
}

object IAQColors {
    val IAQExcellent = Color(0xFF00E400)
    val IAQGood = Color(0xFF92D050)
    val IAQLightlyPolluted = Color(0xFFFFFF00)
    val IAQModeratelyPolluted = Color(0xFFFF7300)
    val IAQHeavilyPolluted = Color(0xFFFF0000)
    val IAQSeverelyPolluted = Color(0xFF99004C)
    val IAQExtremelyPolluted = Color(0xFF663300)
    val IAQDangerouslyPolluted = Color(0xFF663300)
}

object GraphColors {
    val InfantryBlue = Color(red = 75, green = 119, blue = 190)
    val LightGreen = Color(0xFF4BF0BE)
    val Purple = Color(0xFF9C27B0)
    val Pink = Color(red = 255, green = 102, blue = 204)
    val Orange = Color(0xFFFF8800)

    val Green = Color.Green
    val Red = Color.Red
    val Blue = Color.Blue
    val Yellow = Color.Yellow
    val Magenta = Color.Magenta
    val Cyan = Color.Cyan
}

object StatusColors {
    val ColorScheme.StatusGreen: Color
        @Composable
        get() = 
            if (isSystemInDarkTheme()) {
                Color(0xFF28A03B) 
            } else {
                Color(0xFF30C047)
            }

    val ColorScheme.StatusYellow: Color
        @Composable
        get() =
            if (isSystemInDarkTheme()) {
                Color(0xFFFFC107)
            } else {
                Color(0xFFFFD54F)
            }

    val ColorScheme.StatusOrange: Color
        @Composable
        get() =
            if (isSystemInDarkTheme()) {
                Color(0xFFE07000)
            } else {
                Color(0xFFFF8800)
            }

    val ColorScheme.StatusRed: Color
        @Composable
        get() = 
            if (isSystemInDarkTheme()) {
                Color(0xFFB00020)
            } else {
                Color(0xFFF44336)
            }

    val ColorScheme.StatusBlue: Color
        @Composable
        get() = 
            if (isSystemInDarkTheme()) {
                Color(0xFF2196F3)
            } else {
                Color(0xFF42A5F5)
            }
}

object MessageItemColors {
    val Red = Color(0x4DFF0000)
}

