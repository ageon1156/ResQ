

package com.geeksville.mesh.navigation

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.geeksville.mesh.ui.connections.ConnectionsScreen
import org.meshtastic.core.navigation.ConnectionsRoutes
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI
import org.meshtastic.core.navigation.NodesRoutes

fun NavGraphBuilder.connectionsGraph(navController: NavHostController) {
    @Suppress("ktlint:standard:max-line-length")
    navigation<ConnectionsRoutes.ConnectionsGraph>(startDestination = ConnectionsRoutes.Connections) {
        composable<ConnectionsRoutes.Connections>(
            deepLinks = listOf(
                navDeepLink<ConnectionsRoutes.Connections>(basePath = "$DEEP_LINK_BASE_URI/connections"),
            ),
        ) { backStackEntry ->
            val parentEntry =
                remember(backStackEntry) { navController.getBackStackEntry(ConnectionsRoutes.ConnectionsGraph) }
            ConnectionsScreen(
                onClickNodeChip = {
                    navController.navigate(NodesRoutes.NodeDetailGraph(it)) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToNodeDetails = { navController.navigate(NodesRoutes.NodeDetailGraph(it)) },
            )
        }

    }
}

