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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Organic Tech Shape System
 *
 * Features more rounded corners than standard Material Design
 * to create a softer, more natural feel inspired by organic forms
 */
val OrganicShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Custom organic shapes for specific use cases
 */

/**
 * Asymmetric shape inspired by flowing water
 * Large radius on opposite corners creates dynamic visual interest
 * Use for: Cards, containers, prominent UI elements
 */
val RiverShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 8.dp,
    bottomEnd = 32.dp,
    bottomStart = 8.dp
)

/**
 * Asymmetric shape inspired by leaf forms
 * Subtle variation for smaller components
 * Use for: Chips, badges, compact containers
 */
val LeafShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 4.dp,
    bottomEnd = 24.dp,
    bottomStart = 4.dp
)

/**
 * Asymmetric shape with gentle curves
 * For smaller UI elements with organic feel
 * Use for: Small cards, list items, buttons
 */
val PebbleShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 6.dp,
    bottomEnd = 20.dp,
    bottomStart = 6.dp
)

/**
 * Highly rounded shape for soft, approachable elements
 * Use for: FABs, prominent buttons, avatars
 */
val SoftRectangleShape = RoundedCornerShape(20.dp)

/**
 * Top-heavy curve for bottom sheets and modals
 * Creates natural pull-up visual metaphor
 * Use for: Bottom sheets, modal dialogs
 */
val WaveShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 32.dp,
    bottomEnd = 0.dp,
    bottomStart = 0.dp
)

/**
 * Gentle top curve for header elements
 * Use for: App bars, header containers
 */
val HillShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomEnd = 24.dp,
    bottomStart = 24.dp
)
