


package org.meshtastic.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

const val ORGANIC_DURATION_SHORT = 120
const val ORGANIC_DURATION_MEDIUM = 200
const val ORGANIC_DURATION_LONG = 280
const val ORGANIC_DURATION_EXTRA_LONG = 380

const val ORGANIC_STAGGER_DELAY = 20
const val ORGANIC_STAGGER_DELAY_SHORT = 10

val OrganicEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

val OrganicEmphasizedEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)

val OrganicGentleEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

fun <T> organicSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

fun <T> organicGentleSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)

fun <T> organicEmphasizedSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
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
