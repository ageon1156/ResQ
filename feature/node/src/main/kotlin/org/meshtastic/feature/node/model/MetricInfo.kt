

package org.meshtastic.feature.node.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

internal data class VectorMetricInfo(
    val label: StringResource,
    val value: String,
    val icon: ImageVector,
    val rotateIcon: Float = 0f,
)

internal data class DrawableMetricInfo(
    val label: StringResource,
    val value: String,
    @DrawableRes val icon: Int,
    val rotateIcon: Float = 0f,
)

