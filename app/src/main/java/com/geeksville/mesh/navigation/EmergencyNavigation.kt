

package com.geeksville.mesh.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import org.meshtastic.core.navigation.EmergencyRoutes
import org.meshtastic.feature.emergency.EmergencyHelpScreen
import org.meshtastic.feature.emergency.EmergencyTopicDetailScreen

fun NavGraphBuilder.emergencyGraph(navController: NavHostController) {
    navigation<EmergencyRoutes.EmergencyGraph>(startDestination = EmergencyRoutes.EmergencyHome) {
        composable<EmergencyRoutes.EmergencyHome> {
            EmergencyHelpScreen(
                onNavigateToTopic = { section, topicId ->
                    navController.navigate(EmergencyRoutes.EmergencyTopic(section, topicId))
                },
            )
        }
        composable<EmergencyRoutes.EmergencyTopic> { backStackEntry ->
            val route = backStackEntry.toRoute<EmergencyRoutes.EmergencyTopic>()
            EmergencyTopicDetailScreen(
                section = route.section,
                topicId = route.topicId,
                onNavigateUp = navController::navigateUp,
            )
        }
    }
}

