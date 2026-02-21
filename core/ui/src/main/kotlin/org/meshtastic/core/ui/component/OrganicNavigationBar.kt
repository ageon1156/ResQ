

package org.meshtastic.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.theme.organicSpring
import org.meshtastic.core.ui.theme.organicTweenShort

@Composable
fun OrganicNavigationBar(
    destinations: List<NavigationDestination>,
    selectedIndex: Int,
    onDestinationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)  
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                OrganicNavigationItem(
                    destination = destination,
                    selected = selectedIndex == index,
                    onClick = { onDestinationSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OrganicNavigationItem(
    destination: NavigationDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = organicTweenShort(),
        label = "nav_icon_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = organicSpring(),
        label = "nav_icon_scale"
    )

    OrganicTabItem(
        icon = destination.icon,
        label = destination.label,
        selected = selected,
        onClick = onClick,
        modifier = modifier
    )
}

data class NavigationDestination(
    val label: String,
    val icon: ImageVector,
    val badge: (@Composable () -> Unit)? = null
)

