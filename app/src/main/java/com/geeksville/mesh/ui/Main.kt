
@file:Suppress("MatchingDeclarationName")

package com.geeksville.mesh.ui

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.recalculateWindowInsets
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import com.geeksville.mesh.BuildConfig
import com.geeksville.mesh.model.BTScanModel
import com.geeksville.mesh.model.UIViewModel
import com.geeksville.mesh.navigation.channelsGraph
import com.geeksville.mesh.navigation.connectionsGraph
import com.geeksville.mesh.navigation.contactsGraph
import com.geeksville.mesh.navigation.emergencyGraph
import com.geeksville.mesh.navigation.mapGraph
import com.geeksville.mesh.navigation.sosGraph
import com.geeksville.mesh.navigation.nodesGraph
import com.geeksville.mesh.repository.radio.MeshActivity
import com.geeksville.mesh.service.MeshService
import com.geeksville.mesh.ui.connections.DeviceType
import com.geeksville.mesh.ui.connections.components.ConnectionsNavIcon
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.DeviceVersion
import org.meshtastic.core.navigation.ConnectionsRoutes
import org.meshtastic.core.navigation.ContactsRoutes
import org.meshtastic.core.navigation.EmergencyRoutes
import org.meshtastic.core.navigation.SOSRoutes
import org.meshtastic.core.navigation.MapRoutes
import org.meshtastic.core.navigation.NodesRoutes
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.app_too_old
import org.meshtastic.core.strings.client_notification
import org.meshtastic.core.strings.compromised_keys
import org.meshtastic.core.strings.connected
import org.meshtastic.core.strings.connecting
import org.meshtastic.core.strings.connections
import org.meshtastic.core.strings.conversations
import org.meshtastic.core.strings.device_sleeping
import org.meshtastic.core.strings.disconnected
import org.meshtastic.core.strings.emergency_help
import org.meshtastic.core.strings.firmware_old
import org.meshtastic.core.strings.map
import org.meshtastic.core.strings.firmware_too_old
import org.meshtastic.core.strings.must_update
import org.meshtastic.core.strings.neighbor_info
import org.meshtastic.core.strings.nodes
import org.meshtastic.core.strings.okay
import org.meshtastic.core.strings.should_update
import org.meshtastic.core.strings.should_update_firmware
import org.meshtastic.core.strings.sos
import org.meshtastic.core.strings.traceroute
import org.meshtastic.core.ui.component.MultipleChoiceAlertDialog
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.component.SimpleAlertDialog
import org.meshtastic.core.ui.qr.ScannedQrCodeDialog
import org.meshtastic.core.ui.share.SharedContactDialog
import org.meshtastic.core.ui.theme.StatusColors.StatusBlue
import org.meshtastic.core.ui.theme.StatusColors.StatusGreen
import org.meshtastic.feature.node.metrics.annotateTraceroute
import org.meshtastic.proto.MeshProtos

enum class TopLevelDestination(val label: StringResource, val icon: ImageVector, val route: Route) {
    Nodes(Res.string.nodes, Icons.Outlined.Hub, NodesRoutes.NodesGraph),
    Map(Res.string.map, Icons.Outlined.Map, MapRoutes.Map()),
    Conversations(Res.string.conversations, Icons.Outlined.ChatBubbleOutline, ContactsRoutes.ContactsGraph),
    Emergency(Res.string.emergency_help, Icons.Outlined.HealthAndSafety, EmergencyRoutes.EmergencyGraph),
    SOS(Res.string.sos, Icons.Outlined.Sos, SOSRoutes.SOSGraph),
    Connections(Res.string.connections, Icons.Rounded.Wifi, ConnectionsRoutes.ConnectionsGraph),
    ;

    companion object {
        fun fromNavDestination(destination: NavDestination?): TopLevelDestination? =
            entries.find { dest -> destination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MainScreen(uIViewModel: UIViewModel = hiltViewModel(), scanModel: BTScanModel = hiltViewModel()) {
    val navController = rememberNavController()
    val connectionState by uIViewModel.connectionState.collectAsStateWithLifecycle()
    val requestChannelSet by uIViewModel.requestChannelSet.collectAsStateWithLifecycle()
    val sharedContactRequested by uIViewModel.sharedContactRequested.collectAsStateWithLifecycle()
    val unreadMessageCount by uIViewModel.unreadMessageCount.collectAsStateWithLifecycle()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(connectionState, notificationPermissionState) {
            if (connectionState == ConnectionState.Connected && !notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    if (connectionState == ConnectionState.Connected) {
        sharedContactRequested?.let {
            SharedContactDialog(sharedContact = it, onDismiss = { uIViewModel.clearSharedContactRequested() })
        }

        requestChannelSet?.let { newChannelSet ->
            ScannedQrCodeDialog(newChannelSet, onDismiss = { uIViewModel.clearRequestChannelUrl() })
        }
    }

    uIViewModel.AddNavigationTrackingEffect(navController)

    VersionChecks(uIViewModel)
    val alertDialogState by uIViewModel.currentAlert.collectAsStateWithLifecycle()
    alertDialogState?.let { state ->
        if (state.choices.isNotEmpty()) {
            MultipleChoiceAlertDialog(
                title = state.title,
                message = state.message,
                choices = state.choices,
                onDismissRequest = { state.onDismiss?.let { it() } },
            )
        } else {
            SimpleAlertDialog(
                title = state.title,
                message = state.message,
                html = state.html,
                onConfirmRequest = { state.onConfirm?.let { it() } },
                onDismissRequest = { state.onDismiss?.let { it() } },
            )
        }
    }

    val clientNotification by uIViewModel.clientNotification.collectAsStateWithLifecycle()
    clientNotification?.let { notification ->
        var message = notification.message
        val compromisedKeys =
            if (notification.hasLowEntropyKey() || notification.hasDuplicatedPublicKey()) {
                message = stringResource(Res.string.compromised_keys)
                true
            } else {
                false
            }
        SimpleAlertDialog(
            title = Res.string.client_notification,
            text = { Text(text = message) },
            onConfirm = {
                uIViewModel.clearClientNotification(notification)
            },
            onDismiss = { uIViewModel.clearClientNotification(notification) },
        )
    }

    val traceRouteResponse by uIViewModel.tracerouteResponse.observeAsState()
    var dismissedTracerouteRequestId by remember { mutableStateOf<Int?>(null) }
    traceRouteResponse
        ?.takeIf { it.requestId != dismissedTracerouteRequestId }
        ?.let { response ->
            SimpleAlertDialog(
                title = Res.string.traceroute,
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(text = annotateTraceroute(response.message))
                    }
                },
                dismissText = stringResource(Res.string.okay),
                onDismiss = {
                    uIViewModel.clearTracerouteResponse()
                    dismissedTracerouteRequestId = null
                },
            )
        }
    val neighborInfoResponse by uIViewModel.neighborInfoResponse.observeAsState()
    neighborInfoResponse?.let { response ->
        SimpleAlertDialog(
            title = Res.string.neighbor_info,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    fun tryParseNeighborInfo(input: String): MeshProtos.NeighborInfo? {
                        
                        var neighborInfo: MeshProtos.NeighborInfo? =
                            runCatching { MeshProtos.NeighborInfo.parseFrom(input.toByteArray()) }.getOrNull()

                        if (neighborInfo == null) {
                            
                            val hexPairs = Regex("""\b[0-9A-Fa-f]{2}\b""").findAll(input).map { it.value }.toList()
                            @Suppress("detekt:MagicNumber") 
                            if (hexPairs.size >= 4) {
                                val bytes = hexPairs.map { it.toInt(16).toByte() }.toByteArray()
                                neighborInfo = runCatching { MeshProtos.NeighborInfo.parseFrom(bytes) }.getOrNull()
                            }
                        }

                        return neighborInfo
                    }

                    val parsed = tryParseNeighborInfo(response)
                    if (parsed != null) {
                        fun fmtNode(nodeNum: Int): String = "!%08x".format(nodeNum)
                        Text(text = "NeighborInfo:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "node_id: ${fmtNode(parsed.nodeId)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = "last_sent_by_id: ${fmtNode(parsed.lastSentById)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            text = "node_broadcast_interval_secs: ${parsed.nodeBroadcastIntervalSecs}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        if (parsed.neighborsCount > 0) {
                            Text(
                                text = "neighbors:",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            parsed.neighborsList.forEach { n ->
                                Text(
                                    text = "  - node_id: ${fmtNode(n.nodeId)} snr: ${n.snr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    } else {
                        val rawBytes = response.toByteArray()

                        @Suppress("detekt:MagicNumber") 
                        val isBinary = response.any { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }
                        if (isBinary) {
                            val hexString = rawBytes.joinToString(" ") { "%02X".format(it) }
                            Text(
                                text = "Binary data (hex view):",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            Text(
                                text = hexString,
                                style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                ),
                                modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                            )
                        } else {
                            Text(
                                text = response,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            dismissText = stringResource(Res.string.okay),
            onDismiss = { uIViewModel.clearNeighborInfoResponse() },
        )
    }
    val navSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val topLevelDestination = TopLevelDestination.fromNavDestination(currentDestination)

    val selectedDevice by scanModel.selectedNotNullFlow.collectAsStateWithLifecycle()

    var currentGlowColor by remember { mutableStateOf(Color.Transparent) }
    val animatedGlowAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val capturedColorScheme = colorScheme 

    val sendColor = capturedColorScheme.StatusGreen
    val receiveColor = capturedColorScheme.StatusBlue
    LaunchedEffect(uIViewModel.meshActivity, capturedColorScheme) {
        uIViewModel.meshActivity.collectLatest { activity ->
            val newTargetColor =
                when (activity) {
                    is MeshActivity.Send -> sendColor
                    is MeshActivity.Receive -> receiveColor
                }

            currentGlowColor = newTargetColor

            coroutineScope.launch {
                animatedGlowAlpha.stop() 
                animatedGlowAlpha.snapTo(1.0f) 
                animatedGlowAlpha.animateTo(
                    targetValue = 0.0f, 
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                )
            }
        }
    }

    val navSelColor = colorScheme.primary
    val navUnselColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val navIndicatorColor = colorScheme.primary.copy(alpha = 0.12f)
    val minimalItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(selectedIconColor = navSelColor, selectedTextColor = navSelColor, indicatorColor = navIndicatorColor, unselectedIconColor = navUnselColor, unselectedTextColor = navUnselColor),
        navigationRailItemColors = NavigationRailItemDefaults.colors(selectedIconColor = navSelColor, selectedTextColor = navSelColor, indicatorColor = navIndicatorColor, unselectedIconColor = navUnselColor, unselectedTextColor = navUnselColor),
    )

    NavigationSuiteScaffold(
        modifier = Modifier.fillMaxSize(),
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == topLevelDestination
                val isConnectionsRoute = destination == TopLevelDestination.Connections
                item(
                    colors = minimalItemColors,
                    icon = {
                        TooltipBox(
                            positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                            tooltip = {
                                PlainTooltip {
                                    Text(
                                        if (isConnectionsRoute) {
                                            when (connectionState) {
                                                ConnectionState.Connected -> stringResource(Res.string.connected)
                                                ConnectionState.Connecting -> stringResource(Res.string.connecting)
                                                ConnectionState.DeviceSleep ->
                                                    stringResource(Res.string.device_sleeping)
                                                ConnectionState.Disconnected -> stringResource(Res.string.disconnected)
                                            }
                                        } else {
                                            stringResource(destination.label)
                                        },
                                    )
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            if (isConnectionsRoute) {
                                Box(
                                    modifier =
                                    Modifier.drawWithCache {
                                        val glowRadius = size.minDimension
                                        val glowBrush =
                                            Brush.radialGradient(
                                                colors =
                                                listOf(
                                                    currentGlowColor.copy(alpha = 0.8f),
                                                    currentGlowColor.copy(alpha = 0.4f),
                                                    Color.Transparent,
                                                ),
                                                center =
                                                androidx.compose.ui.geometry.Offset(
                                                    size.width / 2,
                                                    size.height / 2,
                                                ),
                                                radius = glowRadius,
                                            )
                                        onDrawWithContent {
                                            drawContent()
                                            val alpha = animatedGlowAlpha.value
                                            if (alpha > 0f) {
                                                drawCircle(
                                                    brush = glowBrush,
                                                    radius = glowRadius,
                                                    alpha = alpha,
                                                    blendMode = BlendMode.Screen,
                                                )
                                            }
                                        }
                                    },
                                ) {
                                    ConnectionsNavIcon(
                                        connectionState = connectionState,
                                        deviceType = DeviceType.fromAddress(selectedDevice),
                                    )
                                }
                            } else {
                                BadgedBox(
                                    badge = {
                                        if (destination == TopLevelDestination.Conversations) {
                                            
                                            var lastNonZeroCount by remember { mutableIntStateOf(unreadMessageCount) }
                                            if (unreadMessageCount > 0) {
                                                lastNonZeroCount = unreadMessageCount
                                            }
                                            AnimatedVisibility(
                                                visible = unreadMessageCount > 0,
                                                enter = scaleIn() + fadeIn(),
                                                exit = scaleOut() + fadeOut(),
                                            ) {
                                                Badge { Text(lastNonZeroCount.toString()) }
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = stringResource(destination.label),
                                    )
                                }
                            }
                        }
                    },
                    selected = isSelected,
                    label = {
                        Text(
                            text = stringResource(destination.label),
                            modifier =
                            if (navSuiteType == NavigationSuiteType.ShortNavigationBarCompact) {
                                Modifier.width(1.dp)
                                    .height(1.dp) 
                            } else {
                                Modifier
                            },
                        )
                    },
                    onClick = {
                        val isRepress = destination == topLevelDestination
                        if (isRepress) {
                            when (destination) {
                                TopLevelDestination.Nodes -> {
                                    val onNodesList = currentDestination?.hasRoute(NodesRoutes.Nodes::class) == true
                                    if (!onNodesList) {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    }
                                    uIViewModel.emitScrollToTopEvent(ScrollToTopEvent.NodesTabPressed)
                                }
                                TopLevelDestination.Conversations -> {
                                    val onConversationsList =
                                        currentDestination?.hasRoute(ContactsRoutes.Contacts::class) == true
                                    if (!onConversationsList) {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    }
                                    uIViewModel.emitScrollToTopEvent(ScrollToTopEvent.ConversationsTabPressed)
                                }
                                else -> Unit
                            }
                        } else {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = NodesRoutes.NodesGraph,
            modifier = Modifier.fillMaxSize().recalculateWindowInsets().safeDrawingPadding().imePadding(),
        ) {
            contactsGraph(navController, uIViewModel.scrollToTopEventFlow)
            nodesGraph(navController, uIViewModel.scrollToTopEventFlow)
            mapGraph(navController)
            channelsGraph(navController)
            connectionsGraph(navController)
            emergencyGraph(navController)
            sosGraph(navController)
        }
    }
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun VersionChecks(viewModel: UIViewModel) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val myNodeInfo by viewModel.myNodeInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val myFirmwareVersion = myNodeInfo?.firmwareVersion

    val firmwareEdition by viewModel.firmwareEdition.collectAsStateWithLifecycle(null)

    val latestStableFirmwareRelease by
        viewModel.latestStableFirmwareRelease.collectAsStateWithLifecycle(DeviceVersion("2.6.4"))
    LaunchedEffect(connectionState, firmwareEdition) {
        if (connectionState == ConnectionState.Connected) {
            firmwareEdition?.let { edition ->
                Logger.d { "FirmwareEdition: ${edition.name}" }
                when (edition) {
                    MeshProtos.FirmwareEdition.VANILLA -> {
                        
                    }

                    else -> {
                        
                    }
                }
            }
        }
    }

    LaunchedEffect(connectionState, myNodeInfo) {
        if (connectionState == ConnectionState.Connected) {
            Logger.i {
                "[FW_CHECK] Connection state: $connectionState, " +
                    "myNodeInfo: ${if (myNodeInfo != null) "present" else "null"}, " +
                    "firmwareVersion: ${myFirmwareVersion ?: "null"}"
            }

            myNodeInfo?.let { info ->
                val isOld = info.minAppVersion > BuildConfig.VERSION_CODE && BuildConfig.DEBUG.not()
                Logger.d {
                    "[FW_CHECK] App version check - minAppVersion: ${info.minAppVersion}, " +
                        "currentVersion: ${BuildConfig.VERSION_CODE}, isOld: $isOld"
                }

                if (isOld) {
                    Logger.w { "[FW_CHECK] App too old - showing update prompt" }
                    viewModel.showAlert(
                        getString(Res.string.app_too_old),
                        getString(Res.string.must_update),
                        dismissable = false,
                        onConfirm = {
                            val service = viewModel.meshService ?: return@showAlert
                            MeshService.changeDeviceAddress(context, service, "n")
                        },
                    )
                } else {
                    myFirmwareVersion?.let { fwVersion ->
                        val curVer = DeviceVersion(fwVersion)
                        Logger.i {
                            "[FW_CHECK] Firmware version comparison - " +
                                "device: $curVer (raw: $fwVersion), " +
                                "absoluteMin: ${MeshService.absoluteMinDeviceVersion}, " +
                                "min: ${MeshService.minDeviceVersion}"
                        }

                        if (curVer < MeshService.absoluteMinDeviceVersion) {
                            Logger.w {
                                "[FW_CHECK] Firmware too old - " +
                                    "device: $curVer < absoluteMin: ${MeshService.absoluteMinDeviceVersion}"
                            }
                            val title = getString(Res.string.firmware_too_old)
                            val message = getString(Res.string.firmware_old)
                            viewModel.showAlert(
                                title = title,
                                html = message,
                                dismissable = false,
                                onConfirm = {
                                    val service = viewModel.meshService ?: return@showAlert
                                    MeshService.changeDeviceAddress(context, service, "n")
                                },
                            )
                        } else if (curVer < MeshService.minDeviceVersion) {
                            Logger.w {
                                "[FW_CHECK] Firmware should update - " +
                                    "device: $curVer < min: ${MeshService.minDeviceVersion}"
                            }
                            val title = getString(Res.string.should_update_firmware)
                            val message = getString(Res.string.should_update, latestStableFirmwareRelease.asString)
                            viewModel.showAlert(title = title, message = message, dismissable = false, onConfirm = {})
                        } else {
                            Logger.i { "[FW_CHECK] Firmware version OK - device: $curVer meets requirements" }
                        }
                    } ?: run { Logger.w { "[FW_CHECK] Firmware version is null despite myNodeInfo being present" } }
                }
            } ?: run { Logger.d { "[FW_CHECK] myNodeInfo is null, skipping firmware check" } }
        } else {
            Logger.d { "[FW_CHECK] Not connected (state: $connectionState), skipping firmware check" }
        }
    }
}

