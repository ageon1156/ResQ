
package org.meshtastic.feature.node.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.feature.node.component.NodeMenuAction
import org.meshtastic.feature.node.metrics.MetricsViewModel
import org.meshtastic.feature.node.model.NodeDetailAction

@Suppress("LongMethod")
@Composable
fun NodeDetailScreen(
    nodeId: Int,
    modifier: Modifier = Modifier,
    metricsViewModel: MetricsViewModel = hiltViewModel(),
    nodeDetailViewModel: NodeDetailViewModel = hiltViewModel(),
    navigateToMessages: (String) -> Unit = {},
    onNavigate: (Route) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    LaunchedEffect(nodeId) { metricsViewModel.setNodeId(nodeId) }

    val metricsState by metricsViewModel.state.collectAsStateWithLifecycle()
    val ourNode by nodeDetailViewModel.ourNodeInfo.collectAsStateWithLifecycle()

    val node = metricsState.node

    @Suppress("ModifierNotUsedAtRoot")
    Scaffold(
        topBar = {
            MainAppBar(
                title = node?.user?.longName ?: "",
                ourNode = ourNode,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        if (node != null) {
            NodeDetailContent(
                node = node,
                metricsState = metricsState,
                onAction = { action ->
                    handleNodeAction(
                        action = action,
                        ourNode = ourNode,
                        node = node,
                        navigateToMessages = navigateToMessages,
                        onNavigateUp = onNavigateUp,
                        onNavigate = onNavigate,
                        metricsViewModel = metricsViewModel,
                        nodeDetailViewModel = nodeDetailViewModel,
                    )
                },
                modifier = modifier.padding(paddingValues),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

private fun handleNodeAction(
    action: NodeDetailAction,
    ourNode: Node?,
    node: Node,
    navigateToMessages: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigate: (Route) -> Unit,
    metricsViewModel: MetricsViewModel,
    nodeDetailViewModel: NodeDetailViewModel,
) {
    when (action) {
        is NodeDetailAction.Navigate -> onNavigate(action.route)
        is NodeDetailAction.TriggerServiceAction -> metricsViewModel.onServiceAction(action.action)
        is NodeDetailAction.HandleNodeMenuAction -> {
            when (val menuAction = action.action) {
                is NodeMenuAction.DirectMessage -> {
                    val hasPKC = ourNode?.hasPKC == true
                    val channel = if (hasPKC) DataPacket.PKC_CHANNEL_INDEX else node.channel
                    navigateToMessages("${channel}${node.user.id}")
                }

                is NodeMenuAction.Remove -> {
                    nodeDetailViewModel.handleNodeMenuAction(menuAction)
                    onNavigateUp()
                }

                else -> nodeDetailViewModel.handleNodeMenuAction(menuAction)
            }
        }

        is NodeDetailAction.ShareContact -> {
            
        }

        is NodeDetailAction.SetOwner -> nodeDetailViewModel.setOwner(action.node, action.longName, action.shortName)
        is NodeDetailAction.SetDeviceConfig -> nodeDetailViewModel.setDeviceConfig(action.node, action.role, action.rebroadcastMode)
    }
}

