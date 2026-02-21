

package org.meshtastic.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val organicLightScheme = lightColorScheme(
    primary = primaryLight_Organic,
    onPrimary = onPrimaryLight_Organic,
    primaryContainer = primaryContainerLight_Organic,
    onPrimaryContainer = onPrimaryContainerLight_Organic,
    secondary = secondaryLight_Organic,
    onSecondary = onSecondaryLight_Organic,
    secondaryContainer = secondaryContainerLight_Organic,
    onSecondaryContainer = onSecondaryContainerLight_Organic,
    tertiary = tertiaryLight_Organic,
    onTertiary = onTertiaryLight_Organic,
    tertiaryContainer = tertiaryContainerLight_Organic,
    onTertiaryContainer = onTertiaryContainerLight_Organic,
    error = errorLight_Organic,
    onError = onErrorLight_Organic,
    errorContainer = errorContainerLight_Organic,
    onErrorContainer = onErrorContainerLight_Organic,
    background = backgroundLight_Organic,
    onBackground = onBackgroundLight_Organic,
    surface = surfaceLight_Organic,
    onSurface = onSurfaceLight_Organic,
    surfaceVariant = surfaceVariantLight_Organic,
    onSurfaceVariant = onSurfaceVariantLight_Organic,
    outline = outlineLight_Organic,
    outlineVariant = outlineVariantLight_Organic,
    scrim = scrimLight_Organic,
    inverseSurface = inverseSurfaceLight_Organic,
    inverseOnSurface = inverseOnSurfaceLight_Organic,
    inversePrimary = inversePrimaryLight_Organic,
    surfaceDim = surfaceDimLight_Organic,
    surfaceBright = surfaceBrightLight_Organic,
    surfaceContainerLowest = surfaceContainerLowestLight_Organic,
    surfaceContainerLow = surfaceContainerLowLight_Organic,
    surfaceContainer = surfaceContainerLight_Organic,
    surfaceContainerHigh = surfaceContainerHighLight_Organic,
    surfaceContainerHighest = surfaceContainerHighestLight_Organic,
)

private val organicDarkScheme = darkColorScheme(
    primary = primaryDark_Organic,
    onPrimary = onPrimaryDark_Organic,
    primaryContainer = primaryContainerDark_Organic,
    onPrimaryContainer = onPrimaryContainerDark_Organic,
    secondary = secondaryDark_Organic,
    onSecondary = onSecondaryDark_Organic,
    secondaryContainer = secondaryContainerDark_Organic,
    onSecondaryContainer = onSecondaryContainerDark_Organic,
    tertiary = tertiaryDark_Organic,
    onTertiary = onTertiaryDark_Organic,
    tertiaryContainer = tertiaryContainerDark_Organic,
    onTertiaryContainer = onTertiaryContainerDark_Organic,
    error = errorDark_Organic,
    onError = onErrorDark_Organic,
    errorContainer = errorContainerDark_Organic,
    onErrorContainer = onErrorContainerDark_Organic,
    background = backgroundDark_Organic,
    onBackground = onBackgroundDark_Organic,
    surface = surfaceDark_Organic,
    onSurface = onSurfaceDark_Organic,
    surfaceVariant = surfaceVariantDark_Organic,
    onSurfaceVariant = onSurfaceVariantDark_Organic,
    outline = outlineDark_Organic,
    outlineVariant = outlineVariantDark_Organic,
    scrim = scrimDark_Organic,
    inverseSurface = inverseSurfaceDark_Organic,
    inverseOnSurface = inverseOnSurfaceDark_Organic,
    inversePrimary = inversePrimaryDark_Organic,
    surfaceDim = surfaceDimDark_Organic,
    surfaceBright = surfaceBrightDark_Organic,
    surfaceContainerLowest = surfaceContainerLowestDark_Organic,
    surfaceContainerLow = surfaceContainerLowDark_Organic,
    surfaceContainer = surfaceContainerDark_Organic,
    surfaceContainerHigh = surfaceContainerHighDark_Organic,
    surfaceContainerHighest = surfaceContainerHighestDark_Organic,
)

private val organicMediumContrastLightColorScheme = lightColorScheme(
    primary = primaryLightMediumContrast_Organic,
    onPrimary = onPrimaryLightMediumContrast_Organic,
    primaryContainer = primaryContainerLightMediumContrast_Organic,
    onPrimaryContainer = onPrimaryContainerLightMediumContrast_Organic,
    secondary = secondaryLightMediumContrast_Organic,
    onSecondary = onSecondaryLightMediumContrast_Organic,
    secondaryContainer = secondaryContainerLightMediumContrast_Organic,
    onSecondaryContainer = onSecondaryContainerLightMediumContrast_Organic,
    tertiary = tertiaryLightMediumContrast_Organic,
    onTertiary = onTertiaryLightMediumContrast_Organic,
    tertiaryContainer = tertiaryContainerLightMediumContrast_Organic,
    onTertiaryContainer = onTertiaryContainerLightMediumContrast_Organic,
    error = errorLightMediumContrast_Organic,
    onError = onErrorLightMediumContrast_Organic,
    errorContainer = errorContainerLightMediumContrast_Organic,
    onErrorContainer = onErrorContainerLightMediumContrast_Organic,
)

private val organicHighContrastLightColorScheme = lightColorScheme(
    primary = primaryLightHighContrast_Organic,
    onPrimary = onPrimaryLightHighContrast_Organic,
    primaryContainer = primaryContainerLightHighContrast_Organic,
    onPrimaryContainer = onPrimaryContainerLightHighContrast_Organic,
    secondary = secondaryLightHighContrast_Organic,
    onSecondary = onSecondaryLightHighContrast_Organic,
    secondaryContainer = secondaryContainerLightHighContrast_Organic,
    onSecondaryContainer = onSecondaryContainerLightHighContrast_Organic,
    tertiary = tertiaryLightHighContrast_Organic,
    onTertiary = onTertiaryLightHighContrast_Organic,
    tertiaryContainer = tertiaryContainerLightHighContrast_Organic,
    onTertiaryContainer = onTertiaryContainerLightHighContrast_Organic,
    error = errorLightHighContrast_Organic,
    onError = onErrorLightHighContrast_Organic,
    errorContainer = errorContainerLightHighContrast_Organic,
    onErrorContainer = onErrorContainerLightHighContrast_Organic,
)

private val organicMediumContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkMediumContrast_Organic,
    onPrimary = onPrimaryDarkMediumContrast_Organic,
    primaryContainer = primaryContainerDarkMediumContrast_Organic,
    onPrimaryContainer = onPrimaryContainerDarkMediumContrast_Organic,
    secondary = secondaryDarkMediumContrast_Organic,
    onSecondary = onSecondaryDarkMediumContrast_Organic,
    secondaryContainer = secondaryContainerDarkMediumContrast_Organic,
    onSecondaryContainer = onSecondaryContainerDarkMediumContrast_Organic,
    tertiary = tertiaryDarkMediumContrast_Organic,
    onTertiary = onTertiaryDarkMediumContrast_Organic,
    tertiaryContainer = tertiaryContainerDarkMediumContrast_Organic,
    onTertiaryContainer = onTertiaryContainerDarkMediumContrast_Organic,
    error = errorDarkMediumContrast_Organic,
    onError = onErrorDarkMediumContrast_Organic,
    errorContainer = errorContainerDarkMediumContrast_Organic,
    onErrorContainer = onErrorContainerDarkMediumContrast_Organic,
)

private val organicHighContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkHighContrast_Organic,
    onPrimary = onPrimaryDarkHighContrast_Organic,
    primaryContainer = primaryContainerDarkHighContrast_Organic,
    onPrimaryContainer = onPrimaryContainerDarkHighContrast_Organic,
    secondary = secondaryDarkHighContrast_Organic,
    onSecondary = onSecondaryDarkHighContrast_Organic,
    secondaryContainer = secondaryContainerDarkHighContrast_Organic,
    onSecondaryContainer = onSecondaryContainerDarkHighContrast_Organic,
    tertiary = tertiaryDarkHighContrast_Organic,
    onTertiary = onTertiaryDarkHighContrast_Organic,
    tertiaryContainer = tertiaryContainerDarkHighContrast_Organic,
    onTertiaryContainer = onTertiaryContainerDarkHighContrast_Organic,
    error = errorDarkHighContrast_Organic,
    onError = onErrorDarkHighContrast_Organic,
    errorContainer = errorContainerDarkHighContrast_Organic,
    onErrorContainer = onErrorContainerDarkHighContrast_Organic,
)

enum class ContrastLevel {
    NORMAL,
    MEDIUM,
    HIGH
}

@Composable
fun OrganicMeshtasticTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  
    contrastLevel: ContrastLevel = ContrastLevel.NORMAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        
        darkTheme -> {
            when (contrastLevel) {
                ContrastLevel.NORMAL -> organicDarkScheme
                ContrastLevel.MEDIUM -> organicMediumContrastDarkColorScheme
                ContrastLevel.HIGH -> organicHighContrastDarkColorScheme
            }
        }
        else -> {
            when (contrastLevel) {
                ContrastLevel.NORMAL -> organicLightScheme
                ContrastLevel.MEDIUM -> organicMediumContrastLightColorScheme
                ContrastLevel.HIGH -> organicHighContrastLightColorScheme
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()

            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OrganicTypography,
        shapes = OrganicShapes,
        content = content
    )
}

