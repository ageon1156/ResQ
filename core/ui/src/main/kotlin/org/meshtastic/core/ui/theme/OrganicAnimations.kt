/*
 * Copyright (c) 2025-2026 Meshtastic LLC
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

package org.meshtastic.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Organic Tech Animation System
 *
 * Inspired by natural motion: slower, more flowing, with gentle spring physics
 * that mimics organic growth and movement patterns.
 */

// Durations - Longer than standard Material (300ms) for calmer feel
const val ORGANIC_DURATION_SHORT = 350
const val ORGANIC_DURATION_MEDIUM = 450
const val ORGANIC_DURATION_LONG = 650
const val ORGANIC_DURATION_EXTRA_LONG = 850

// Stagger delays for list animations
const val ORGANIC_STAGGER_DELAY = 80
const val ORGANIC_STAGGER_DELAY_SHORT = 50

/**
 * Organic easing curve
 * Slower start and end, mimicking natural deceleration
 */
val OrganicEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

/**
 * Emphasized organic easing for important transitions
 * Even more pronounced acceleration and deceleration
 */
val OrganicEmphasizedEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/**
 * Gentle easing for subtle animations
 * Very smooth, barely noticeable acceleration
 */
val OrganicGentleEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.3f, 1.0f)

/**
 * Spring configuration for bounce-like effects
 * Low stiffness creates gentle, flowing motion
 */
fun <T> organicSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

/**
 * Gentle spring for subtle movements
 * More damped, less bouncy
 */
fun <T> organicGentleSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessVeryLow
)

/**
 * Emphasized spring for prominent elements
 * Slightly bouncier for playful feel
 */
fun <T> organicEmphasizedSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Standard organic tween animation
 */
fun <T> organicTween() = tween<T>(
    durationMillis = ORGANIC_DURATION_MEDIUM,
    easing = OrganicEasing
)

/**
 * Short organic tween for quick transitions
 */
fun <T> organicTweenShort() = tween<T>(
    durationMillis = ORGANIC_DURATION_SHORT,
    easing = OrganicEasing
)

/**
 * Long organic tween for dramatic transitions
 */
fun <T> organicTweenLong() = tween<T>(
    durationMillis = ORGANIC_DURATION_LONG,
    easing = OrganicEmphasizedEasing
)

/**
 * Gentle tween for subtle animations
 */
fun <T> organicTweenGentle() = tween<T>(
    durationMillis = ORGANIC_DURATION_SHORT,
    easing = OrganicGentleEasing
)
