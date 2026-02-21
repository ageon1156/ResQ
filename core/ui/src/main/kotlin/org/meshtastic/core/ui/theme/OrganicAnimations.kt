

package org.meshtastic.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

const val ORGANIC_DURATION_SHORT = 350
const val ORGANIC_DURATION_MEDIUM = 450
const val ORGANIC_DURATION_LONG = 650
const val ORGANIC_DURATION_EXTRA_LONG = 850

const val ORGANIC_STAGGER_DELAY = 80
const val ORGANIC_STAGGER_DELAY_SHORT = 50

val OrganicEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

val OrganicEmphasizedEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

val OrganicGentleEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.3f, 1.0f)

fun <T> organicSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

fun <T> organicGentleSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessVeryLow
)

fun <T> organicEmphasizedSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

fun <T> organicTween() = tween<T>(
    durationMillis = ORGANIC_DURATION_MEDIUM,
    easing = OrganicEasing
)

fun <T> organicTweenShort() = tween<T>(
    durationMillis = ORGANIC_DURATION_SHORT,
    easing = OrganicEasing
)

fun <T> organicTweenLong() = tween<T>(
    durationMillis = ORGANIC_DURATION_LONG,
    easing = OrganicEmphasizedEasing
)

fun <T> organicTweenGentle() = tween<T>(
    durationMillis = ORGANIC_DURATION_SHORT,
    easing = OrganicGentleEasing
)
