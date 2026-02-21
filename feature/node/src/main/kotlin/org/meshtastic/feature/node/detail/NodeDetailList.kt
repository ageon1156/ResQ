

package org.meshtastic.feature.node.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.accept
import org.meshtastic.core.strings.are_you_sure
import org.meshtastic.core.strings.cancel
import org.meshtastic.core.strings.device
import org.meshtastic.core.strings.i_know_what_i_m_doing
import org.meshtastic.core.strings.long_name
import org.meshtastic.core.strings.rebroadcast_mode
import org.meshtastic.core.strings.rebroadcast_mode_all_desc
import org.meshtastic.core.strings.rebroadcast_mode_all_skip_decoding_desc
import org.meshtastic.core.strings.rebroadcast_mode_core_portnums_only_desc
import org.meshtastic.core.strings.rebroadcast_mode_known_only_desc
import org.meshtastic.core.strings.rebroadcast_mode_local_only_desc
import org.meshtastic.core.strings.rebroadcast_mode_none_desc
import org.meshtastic.core.strings.role
import org.meshtastic.core.strings.role_client_base_desc
import org.meshtastic.core.strings.role_client_desc
import org.meshtastic.core.strings.role_client_hidden_desc
import org.meshtastic.core.strings.role_client_mute_desc
import org.meshtastic.core.strings.role_lost_and_found_desc
import org.meshtastic.core.strings.role_repeater_desc
import org.meshtastic.core.strings.role_router_client_desc
import org.meshtastic.core.strings.role_router_desc
import org.meshtastic.core.strings.role_router_late_desc
import org.meshtastic.core.strings.role_sensor_desc
import org.meshtastic.core.strings.role_tak_desc
import org.meshtastic.core.strings.role_tak_tracker_desc
import org.meshtastic.core.strings.role_tracker_desc
import org.meshtastic.core.strings.router_role_confirmation_text
import org.meshtastic.core.strings.save
import org.meshtastic.core.strings.short_name
import org.meshtastic.core.strings.unrecognized
import org.meshtastic.core.strings.user
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.SharedContactDialog
import org.meshtastic.core.ui.component.preview.NodePreviewParameterProvider
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.feature.node.component.DeviceActions
import org.meshtastic.feature.node.model.MetricsState
import org.meshtastic.feature.node.model.NodeDetailAction
import org.meshtastic.proto.ConfigProtos.Config.DeviceConfig

@Composable
fun NodeDetailContent(
    node: Node,
    metricsState: MetricsState,
    onAction: (NodeDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showShareDialog by remember { mutableStateOf(false) }
    if (showShareDialog) {
        SharedContactDialog(node) { showShareDialog = false }
    }

    NodeDetailList(
        node = node,
        metricsState = metricsState,
        onAction = { action ->
            if (action is NodeDetailAction.ShareContact) {
                showShareDialog = true
            } else {
                onAction(action)
            }
        },
        modifier = modifier,
    )
}

@Composable
@Suppress("LongMethod")
fun NodeDetailList(
    node: Node,
    metricsState: MetricsState,
    onAction: (NodeDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).focusable(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        NodeNameEditSection(node = node, onAction = onAction)

        DeviceConfigSection(node = node, onAction = onAction)

        DeviceActions(
            isLocal = metricsState.isLocal,
            node = node,
            onAction = onAction,
        )
    }
}

@Composable
private fun NodeNameEditSection(node: Node, onAction: (NodeDetailAction) -> Unit) {
    var longName by remember(node.num) { mutableStateOf(node.user.longName) }
    var shortName by remember(node.num) { mutableStateOf(node.user.shortName) }
    val isDirty = longName != node.user.longName || shortName != node.user.shortName
    val validNames = longName.isNotBlank() && shortName.isNotBlank()
    val focusManager = LocalFocusManager.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.user),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = longName,
                onValueChange = { if (it.toByteArray().size <= 39) longName = it },
                label = { Text(stringResource(Res.string.long_name)) },
                singleLine = true,
                isError = longName.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            )
            HorizontalDivider()
            OutlinedTextField(
                value = shortName,
                onValueChange = { if (it.toByteArray().size <= 4) shortName = it },
                label = { Text(stringResource(Res.string.short_name)) },
                singleLine = true,
                isError = shortName.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            if (isDirty && validNames) {
                Button(
                    onClick = {
                        onAction(NodeDetailAction.SetOwner(node, longName, shortName))
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(Res.string.save))
                }
            }
        }
    }
}

private val DeviceConfig.Role.description: StringResource
    get() = when (this) {
        DeviceConfig.Role.CLIENT -> Res.string.role_client_desc
        DeviceConfig.Role.CLIENT_BASE -> Res.string.role_client_base_desc
        DeviceConfig.Role.CLIENT_MUTE -> Res.string.role_client_mute_desc
        DeviceConfig.Role.ROUTER -> Res.string.role_router_desc
        DeviceConfig.Role.ROUTER_CLIENT -> Res.string.role_router_client_desc
        DeviceConfig.Role.REPEATER -> Res.string.role_repeater_desc
        DeviceConfig.Role.TRACKER -> Res.string.role_tracker_desc
        DeviceConfig.Role.SENSOR -> Res.string.role_sensor_desc
        DeviceConfig.Role.TAK -> Res.string.role_tak_desc
        DeviceConfig.Role.CLIENT_HIDDEN -> Res.string.role_client_hidden_desc
        DeviceConfig.Role.LOST_AND_FOUND -> Res.string.role_lost_and_found_desc
        DeviceConfig.Role.TAK_TRACKER -> Res.string.role_tak_tracker_desc
        DeviceConfig.Role.ROUTER_LATE -> Res.string.role_router_late_desc
        else -> Res.string.unrecognized
    }

private val DeviceConfig.RebroadcastMode.description: StringResource
    get() = when (this) {
        DeviceConfig.RebroadcastMode.ALL -> Res.string.rebroadcast_mode_all_desc
        DeviceConfig.RebroadcastMode.ALL_SKIP_DECODING -> Res.string.rebroadcast_mode_all_skip_decoding_desc
        DeviceConfig.RebroadcastMode.LOCAL_ONLY -> Res.string.rebroadcast_mode_local_only_desc
        DeviceConfig.RebroadcastMode.KNOWN_ONLY -> Res.string.rebroadcast_mode_known_only_desc
        DeviceConfig.RebroadcastMode.NONE -> Res.string.rebroadcast_mode_none_desc
        DeviceConfig.RebroadcastMode.CORE_PORTNUMS_ONLY -> Res.string.rebroadcast_mode_core_portnums_only_desc
        else -> Res.string.unrecognized
    }

@Composable
private fun DeviceConfigSection(node: Node, onAction: (NodeDetailAction) -> Unit) {
    val infrastructureRoles = listOf(DeviceConfig.Role.ROUTER, DeviceConfig.Role.ROUTER_LATE, DeviceConfig.Role.REPEATER)
    var selectedRole by remember(node.num) { mutableStateOf(node.user.role) }
    var selectedRebroadcastMode by rememberSaveable { mutableStateOf(DeviceConfig.RebroadcastMode.ALL) }
    var pendingRole by remember { mutableStateOf<DeviceConfig.Role?>(null) }

    pendingRole?.let { role ->
        RouterRoleConfirmationDialog(
            onDismiss = { pendingRole = null },
            onConfirm = {
                selectedRole = role
                pendingRole = null
            },
        )
    }

    val roleChanged = selectedRole != node.user.role
    val isDirty = roleChanged || selectedRebroadcastMode != DeviceConfig.RebroadcastMode.ALL

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.device),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            DropDownPreference(
                title = stringResource(Res.string.role),
                enabled = true,
                selectedItem = selectedRole,
                onItemSelected = { role ->
                    if (role in infrastructureRoles && role != selectedRole) {
                        pendingRole = role
                    } else {
                        selectedRole = role
                    }
                },
                summary = stringResource(selectedRole.description),
            )

            HorizontalDivider()

            DropDownPreference(
                title = stringResource(Res.string.rebroadcast_mode),
                enabled = true,
                selectedItem = selectedRebroadcastMode,
                onItemSelected = { selectedRebroadcastMode = it },
                summary = stringResource(selectedRebroadcastMode.description),
            )

            if (isDirty) {
                Button(
                    onClick = {
                        onAction(NodeDetailAction.SetDeviceConfig(node, selectedRole, selectedRebroadcastMode))
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(Res.string.save))
                }
            }
        }
    }
}

@Composable
private fun RouterRoleConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val dialogTitle = stringResource(Res.string.are_you_sure)
    val annotatedDialogText = AnnotatedString.fromHtml(
        htmlString = stringResource(Res.string.router_role_confirmation_text),
        linkStyles = TextLinkStyles(style = SpanStyle(color = Color.Blue)),
    )
    var confirmed by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = {
            Column {
                Text(text = annotatedDialogText)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { confirmed = !confirmed },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                    Text(stringResource(Res.string.i_know_what_i_m_doing))
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmed) { Text(stringResource(Res.string.accept)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun NodeDetailsPreview(@PreviewParameter(NodePreviewParameterProvider::class) node: Node) {
    AppTheme {
        NodeDetailList(
            node = node,
            metricsState = MetricsState.Companion.Empty,
            onAction = {},
        )
    }
}

