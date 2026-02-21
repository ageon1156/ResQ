

package org.meshtastic.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val OrganicShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val RiverShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 8.dp,
    bottomEnd = 32.dp,
    bottomStart = 8.dp
)

val LeafShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 4.dp,
    bottomEnd = 24.dp,
    bottomStart = 4.dp
)

val PebbleShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 6.dp,
    bottomEnd = 20.dp,
    bottomStart = 6.dp
)

val SoftRectangleShape = RoundedCornerShape(20.dp)

val WaveShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 32.dp,
    bottomEnd = 0.dp,
    bottomStart = 0.dp
)

val HillShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomEnd = 24.dp,
    bottomStart = 24.dp
)
