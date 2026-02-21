

package com.geeksville.mesh.ui.contact

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.navigation.ChannelsRoutes
import org.meshtastic.core.navigation.ContactsRoutes
import org.meshtastic.core.navigation.NodesRoutes
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.conversations
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.icon.Conversations
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.feature.messaging.MessageScreen

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveContactsScreen(
    navController: NavHostController,
    scrollToTopEvents: Flow<ScrollToTopEvent>,
    initialContactKey: String? = null,
    initialMessage: String = "",
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val backNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange

    val handleBack: () -> Unit = {
        val currentEntry = navController.currentBackStackEntry
        val isContactsRoute = currentEntry?.destination?.hasRoute<ContactsRoutes.Contacts>() == true

        val previousEntry = navController.previousBackStackEntry
        val isFromDifferentGraph = previousEntry?.destination?.hasRoute<ContactsRoutes.ContactsGraph>() == false

        if (isFromDifferentGraph && !isContactsRoute) {
            
            navController.navigateUp()
        } else {
            
            scope.launch { navigator.navigateBack(backNavigationBehavior) }
        }
    }

    BackHandler(enabled = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) { handleBack() }

    LaunchedEffect(initialContactKey) {
        if (initialContactKey != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, initialContactKey)
        }
    }

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect { event ->
            if (
                event is ScrollToTopEvent.ConversationsTabPressed &&
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
                ContactsScreen(
                    onNavigateToShare = { navController.navigate(ChannelsRoutes.ChannelsGraph) },
                    onClickNodeChip = {
                        navController.navigate(NodesRoutes.NodeDetailGraph(it)) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMessages = { contactKey ->
                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, contactKey) }
                    },
                    onNavigateToNodeDetails = { navController.navigate(NodesRoutes.NodeDetailGraph(it)) },
                    scrollToTopEvents = scrollToTopEvents,
                    activeContactKey = navigator.currentDestination?.contentKey,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.contentKey?.let { contactKey ->
                    key(contactKey) {
                        MessageScreen(
                            contactKey = contactKey,
                            message = if (contactKey == initialContactKey) initialMessage else "",
                            navigateToNodeDetails = { navController.navigate(NodesRoutes.NodeDetailGraph(it)) },
                            navigateToQuickChatOptions = { navController.navigate(ContactsRoutes.QuickChat) },
                            navigateToChannelSettings = { navController.navigate(ChannelsRoutes.ChannelsGraph) },
                            onNavigateBack = handleBack,
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
                imageVector = MeshtasticIcons.Conversations,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.conversations),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

