

package com.geeksville.mesh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun organicNavigationSuiteColors(): NavigationSuiteItemColors {
    val scheme = MaterialTheme.colorScheme

    return NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = scheme.primary,
            selectedTextColor = scheme.primary,
            indicatorColor = scheme.primary.copy(alpha = 0.12f),
            unselectedIconColor = scheme.onSurfaceVariant.copy(alpha = 0.5f),
            unselectedTextColor = scheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = scheme.primary,
            selectedTextColor = scheme.primary,
            indicatorColor = scheme.primary.copy(alpha = 0.12f),
            unselectedIconColor = scheme.onSurfaceVariant.copy(alpha = 0.5f),
            unselectedTextColor = scheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedIconColor = scheme.primary,
            selectedTextColor = scheme.primary,
            selectedContainerColor = scheme.primary.copy(alpha = 0.12f),
            unselectedIconColor = scheme.onSurfaceVariant.copy(alpha = 0.5f),
            unselectedTextColor = scheme.onSurfaceVariant.copy(alpha = 0.5f),
            unselectedContainerColor = Color.Transparent,
        )
    )
}

