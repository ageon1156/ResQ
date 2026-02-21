/*
 * Copyright (c) 2025 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
