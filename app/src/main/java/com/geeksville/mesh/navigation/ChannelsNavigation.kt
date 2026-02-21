

package com.geeksville.mesh.navigation

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.geeksville.mesh.ui.sharing.ChannelScreen
import org.meshtastic.core.navigation.ChannelsRoutes
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI
import org.meshtastic.core.navigation.SettingsRoutes
import org.meshtastic.feature.settings.radio.channel.ChannelConfigScreen

fun NavGraphBuilder.channelsGraph(navController: NavHostController) {
    navigation<ChannelsRoutes.ChannelsGraph>(startDestination = SettingsRoutes.ChannelConfig) {
        composable<ChannelsRoutes.Channels>(
            deepLinks = listOf(navDeepLink<ChannelsRoutes.Channels>(basePath = "$DEEP_LINK_BASE_URI/channels")),
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(ChannelsRoutes.ChannelsGraph) }
            ChannelScreen(
                radioConfigViewModel = hiltViewModel(parentEntry),
                onNavigate = { route -> navController.navigate(route) },
                onNavigateUp = { navController.navigateUp() },
            )
        }

        navController.configComposable<SettingsRoutes.ChannelConfig, ChannelsRoutes.ChannelsGraph> {
            ChannelConfigScreen(viewModel = it, onBack = navController::popBackStack)
        }
    }
}

