


package org.meshtastic.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MeshtasticGreen = Color(0xFFE8830A)
val MeshtasticAlt = Color(0xFF0D0F0C)
val HyperlinkBlue = Color(0xFF4E9BC0)
val AnnotationColor = Color(0xFFD4720A)

object TracerouteColors {

    val OutgoingRoute = Color(0xCCE8830A)
    val ReturnRoute = Color(0xCC4E7FA0)
}

object IAQColors {
    val IAQExcellent = Color(0xFF2DB53E)
    val IAQGood = Color(0xFF78A840)
    val IAQLightlyPolluted = Color(0xFFD4A020)
    val IAQModeratelyPolluted = Color(0xFFD47010)
    val IAQHeavilyPolluted = Color(0xFFCC3319)
    val IAQSeverelyPolluted = Color(0xFF8A1A30)
    val IAQExtremelyPolluted = Color(0xFF4A1020)
    val IAQDangerouslyPolluted = Color(0xFF2A0810)
}

object GraphColors {
    val InfantryBlue = Color(0xFF4E7FA0)
    val LightGreen = Color(0xFF8FA864)
    val Purple = Color(0xFF7A5080)
    val Pink = Color(0xFFB06070)
    val Orange = Color(0xFFE8830A)

    val Green = Color(0xFF2DB53E)
    val Red = Color(0xFFCC3319)
    val Blue = Color(0xFF4E7FA0)
    val Yellow = Color(0xFFD4A020)
    val Magenta = Color(0xFFB06070)
    val Cyan = Color(0xFF4E9BC0)
}

object StatusColors {
    val ColorScheme.StatusGreen: Color
        @Composable
        get() =
            if (isSystemInDarkTheme()) {
                Color(0xFF2DB53E)
            } else {
                Color(0xFF3DD454)
            }

    val ColorScheme.StatusYellow: Color
        @Composable
        get() =
            if (isSystemInDarkTheme()) {
                Color(0xFFD4A020)
            } else {
                Color(0xFFE8B830)
            }

    val ColorScheme.StatusOrange: Color
        @Composable
        get() =
            if (isSystemInDarkTheme()) {
                Color(0xFFE8830A)
            } else {
                Color(0xFFD4720A)
            }

    val ColorScheme.StatusRed: Color
        @Composable
        get() =
            if (isSystemInDarkTheme()) {
                Color(0xFFCC3319)
            } else {
                Color(0xFFBF2810)
            }

    val ColorScheme.StatusBlue: Color
        @Composable
        get() =
            if (isSystemInDarkTheme()) {
                Color(0xFF4E7FA0)
            } else {
                Color(0xFF3A6A8A)
            }
}

object MessageItemColors {
    val Red = Color(0x4DCC3319)
}
