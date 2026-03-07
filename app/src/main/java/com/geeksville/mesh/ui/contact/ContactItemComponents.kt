package com.geeksville.mesh.ui.contact

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal fun String.isBroadcastContactKey() =
    getOrNull(1) == '^' || endsWith("^all") || endsWith("^broadcast")

@Composable
internal fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = count > 0, modifier = modifier, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        val text = if (count > 99) "99+" else count.toString()
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)) {
            Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun MuteIcon(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        Icon(imageVector = Icons.AutoMirrored.TwoTone.VolumeOff, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
