

package org.meshtastic.feature.node.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun InfoCard(icon: ImageVector, text: String, value: String, modifier: Modifier = Modifier, rotateIcon: Float = 0f) {
    Card(modifier = modifier.padding(4.dp).width(100.dp).height(100.dp)) {
        Box(modifier = Modifier.padding(4.dp).width(100.dp).height(100.dp), contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp).thenIf(rotateIcon != 0f) { rotate(rotateIcon) },
                )
                Text(
                    textAlign = TextAlign.Center,
                    text = text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
internal fun DrawableInfoCard(@DrawableRes iconRes: Int, text: String, value: String, rotateIcon: Float = 0f) {
    Card(modifier = Modifier.padding(4.dp).width(100.dp).height(100.dp)) {
        Box(modifier = Modifier.padding(4.dp).width(100.dp).height(100.dp), contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = text,
                    modifier = Modifier.size(24.dp).thenIf(rotateIcon != 0f) { rotate(rotateIcon) },
                )
                Text(
                    textAlign = TextAlign.Center,
                    text = text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

inline fun Modifier.thenIf(precondition: Boolean, action: Modifier.() -> Modifier): Modifier =
    if (precondition) action() else this

