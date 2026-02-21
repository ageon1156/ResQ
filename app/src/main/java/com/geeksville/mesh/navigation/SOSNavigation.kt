

package com.geeksville.mesh.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import org.meshtastic.core.navigation.SOSRoutes
import org.meshtastic.feature.sos.SOSScreen

fun NavGraphBuilder.sosGraph(navController: NavHostController) {
    navigation<SOSRoutes.SOSGraph>(startDestination = SOSRoutes.SOSHome) {
        composable<SOSRoutes.SOSHome> {
            SOSScreen()
        }
    }
}

