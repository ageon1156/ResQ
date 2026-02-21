

package org.meshtastic.feature.map.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.map_download_region

@Composable
fun DownloadButton(enabled: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = enabled,
        enter =
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        ),
        exit =
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        ),
    ) {
        FloatingActionButton(onClick = onClick, contentColor = MaterialTheme.colorScheme.primary) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = stringResource(Res.string.map_download_region),
                modifier = Modifier.scale(1.25f),
            )
        }
    }
}
