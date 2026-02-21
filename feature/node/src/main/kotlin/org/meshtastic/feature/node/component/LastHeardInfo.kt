
package org.meshtastic.feature.node.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.node_sort_last_heard
import org.meshtastic.core.ui.R
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.formatAgo

@Composable
fun LastHeardInfo(
    modifier: Modifier = Modifier,
    lastHeard: Int,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconInfo(
        modifier = modifier,
        icon = ImageVector.vectorResource(id = R.drawable.ic_antenna_24),
        contentDescription = stringResource(Res.string.node_sort_last_heard),
        text = formatAgo(lastHeard),
        contentColor = contentColor,
    )
}

@PreviewLightDark
@Composable
private fun LastHeardInfoPreview() {
    AppTheme { LastHeardInfo(lastHeard = (System.currentTimeMillis() / 1000).toInt() - 8600) }
}

