

@file:Suppress("Wrapping", "SpacingAroundColon")

package com.geeksville.mesh.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.meshtastic.core.navigation.Graph
import org.meshtastic.core.navigation.Route
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import kotlin.reflect.KClass

context(_: NavGraphBuilder)
inline fun <reified R : Route, reified G : Graph> NavHostController.configComposable(
    noinline content: @Composable (RadioConfigViewModel) -> Unit,
) {
    configComposable(route = R::class, parentGraphRoute = G::class, content = content)
}

context(navGraphBuilder: NavGraphBuilder)
fun <R : Route, G : Graph> NavHostController.configComposable(
    route: KClass<R>,
    parentGraphRoute: KClass<G>,
    content: @Composable (RadioConfigViewModel) -> Unit,
) {
    navGraphBuilder.composable(route = route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { getBackStackEntry(parentGraphRoute) }
        content(hiltViewModel(parentEntry))
    }
}
