
package org.meshtastic.feature.messaging.component

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun getMessageBubbleShape(
    cornerRadius: Dp,
    isSender: Boolean,
    hasSamePrev: Boolean = false,
    hasSameNext: Boolean = false,
): CornerBasedShape {
    val square = 0.dp
    val round = cornerRadius

    return if (isSender) {
        RoundedCornerShape(
            topStart = if (hasSamePrev) square else round,
            topEnd = if (hasSamePrev) square else round,
            bottomStart = if (hasSameNext) square else round,
            bottomEnd = square,
        )
    } else {
        RoundedCornerShape(
            topStart = square,
            topEnd = if (hasSamePrev) square else round,
            bottomStart = if (hasSameNext) square else round,
            bottomEnd = if (hasSameNext) square else round,
        )
    }
}

