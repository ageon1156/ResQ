

package com.geeksville.mesh.ui.node

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.navigation.NodesRoutes
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.nodes
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Nodes
import org.meshtastic.feature.node.detail.NodeDetailScreen
import org.meshtastic.feature.node.list.NodeListScreen

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveNodeListScreen(
    navController: NavHostController,
    scrollToTopEvents: Flow<ScrollToTopEvent>,
    initialNodeId: Int? = null,
    onNavigateToMessages: (String) -> Unit = {},
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()
    val scope = rememberCoroutineScope()
    val backNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange

    val handleBack: () -> Unit = {
        val currentEntry = navController.currentBackStackEntry
        val isNodesRoute = currentEntry?.destination?.hasRoute<NodesRoutes.Nodes>() == true

        val previousEntry = navController.previousBackStackEntry
        val isFromDifferentGraph = previousEntry?.destination?.hasRoute<NodesRoutes.NodesGraph>() == false

        if (isFromDifferentGraph && !isNodesRoute) {
            
            navController.navigateUp()
        } else {
            
            scope.launch { navigator.navigateBack(backNavigationBehavior) }
        }
    }

    BackHandler(enabled = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) { handleBack() }

    LaunchedEffect(initialNodeId) {
        if (initialNodeId != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, initialNodeId)
        }
    }

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect { event ->
            if (
                event is ScrollToTopEvent.NodesTabPressed &&
                navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
            ) {
                if (navigator.canNavigateBack(backNavigationBehavior)) {
                    navigator.navigateBack(backNavigationBehavior)
                } else {
                    navigator.navigateTo(ListDetailPaneScaffoldRole.List)
                }
            }
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                val focusManager = LocalFocusManager.current
                
                LaunchedEffect(Unit) { focusManager.clearFocus() }
                NodeListScreen(
                    navigateToNodeDetails = { nodeId ->
                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, nodeId) }
                    },
                    scrollToTopEvents = scrollToTopEvents,
                    activeNodeId = navigator.currentDestination?.contentKey,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val focusManager = LocalFocusManager.current
                
                navigator.currentDestination?.contentKey?.let { nodeId ->
                    key(nodeId) {
                        LaunchedEffect(nodeId) { focusManager.clearFocus() }
                        NodeDetailScreen(
                            nodeId = nodeId,
                            navigateToMessages = onNavigateToMessages,
                            onNavigate = { route -> navController.navigate(route) },
                            onNavigateUp = handleBack,
                        )
                    }
                } ?: PlaceholderScreen()
            }
        },
    )
}

@Composable
private fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = MeshtasticIcons.Nodes,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.nodes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

