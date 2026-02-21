

package com.geeksville.mesh.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI
import org.meshtastic.core.navigation.MapRoutes
import org.meshtastic.core.navigation.NodesRoutes
import org.meshtastic.feature.map.MapScreen

fun NavGraphBuilder.mapGraph(navController: NavHostController) {
    composable<MapRoutes.Map>(deepLinks = listOf(navDeepLink<MapRoutes.Map>(basePath = "$DEEP_LINK_BASE_URI/map"))) {
        MapScreen(
            onClickNodeChip = {
                navController.navigate(NodesRoutes.NodeDetailGraph(it)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            navigateToNodeDetails = { navController.navigate(NodesRoutes.NodeDetailGraph(it)) },
        )
    }
}
