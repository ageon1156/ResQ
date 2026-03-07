package com.geeksville.mesh.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import org.meshtastic.core.navigation.VoiceMessageRoutes
import org.meshtastic.feature.voicemessage.VoiceMessageScreen

fun NavGraphBuilder.voiceMessageGraph(navController: NavHostController) {
    navigation<VoiceMessageRoutes.VoiceMessageGraph>(startDestination = VoiceMessageRoutes.VoiceMessageHome) {
        composable<VoiceMessageRoutes.VoiceMessageHome> {
            VoiceMessageScreen()
        }
    }
}
