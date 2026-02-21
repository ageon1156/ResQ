

package org.meshtastic.feature.intro

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

internal data class FeatureUIData(
    val icon: ImageVector,
    val titleRes: StringResource? = null,
    val subtitleRes: StringResource,
)

