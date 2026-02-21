

@file:Suppress("TooManyFunctions")

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Channel
import org.meshtastic.core.model.util.getChannel
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.security_icon_badge_warning_description
import org.meshtastic.core.strings.security_icon_description
import org.meshtastic.core.strings.security_icon_help_dismiss
import org.meshtastic.core.strings.security_icon_help_green_lock
import org.meshtastic.core.strings.security_icon_help_red_open_lock
import org.meshtastic.core.strings.security_icon_help_show_all
import org.meshtastic.core.strings.security_icon_help_show_less
import org.meshtastic.core.strings.security_icon_help_title
import org.meshtastic.core.strings.security_icon_help_title_all
import org.meshtastic.core.strings.security_icon_help_warning_precise_mqtt
import org.meshtastic.core.strings.security_icon_help_yellow_open_lock
import org.meshtastic.core.strings.security_icon_insecure_no_precise
import org.meshtastic.core.strings.security_icon_insecure_precise_only
import org.meshtastic.core.strings.security_icon_secure
import org.meshtastic.core.strings.security_icon_warning_precise_mqtt
import org.meshtastic.core.ui.theme.StatusColors.StatusGreen
import org.meshtastic.core.ui.theme.StatusColors.StatusRed
import org.meshtastic.core.ui.theme.StatusColors.StatusYellow
import org.meshtastic.proto.AppOnlyProtos
import org.meshtastic.proto.ChannelProtos.ChannelSettings
import org.meshtastic.proto.ConfigProtos.Config.LoRaConfig

private const val PRECISE_POSITION_BITS = 32

@Immutable
enum class SecurityState(
    @Stable val icon: ImageVector,
    @Stable val color: @Composable () -> Color,
    val descriptionResId: StringResource,
    val helpTextResId: StringResource,
    @Stable val badgeIcon: ImageVector? = null,
    @Stable val badgeIconColor: @Composable () -> Color? = { null },
) {
    
    SECURE(
        icon = Icons.Filled.Lock,
        color = { colorScheme.StatusGreen },
        descriptionResId = Res.string.security_icon_secure,
        helpTextResId = Res.string.security_icon_help_green_lock,
    ),

    INSECURE_NO_PRECISE(
        icon = Icons.Filled.LockOpen,
        color = { colorScheme.StatusYellow },
        descriptionResId = Res.string.security_icon_insecure_no_precise,
        helpTextResId = Res.string.security_icon_help_yellow_open_lock,
    ),

    INSECURE_PRECISE_ONLY(
        icon = Icons.Filled.LockOpen,
        color = { colorScheme.StatusRed },
        descriptionResId = Res.string.security_icon_insecure_precise_only,
        helpTextResId = Res.string.security_icon_help_red_open_lock,
    ),

    INSECURE_PRECISE_MQTT_WARNING(
        icon = Icons.Filled.LockOpen,
        color = { colorScheme.StatusRed },
        descriptionResId = Res.string.security_icon_warning_precise_mqtt,
        helpTextResId = Res.string.security_icon_help_warning_precise_mqtt,
        badgeIcon = Icons.Filled.Warning,
        badgeIconColor = { colorScheme.StatusYellow },
    ),
}

@Composable
private fun SecurityIconDisplay(
    icon: ImageVector,
    mainIconTint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    badgeIcon: ImageVector? = null,
    badgeIconColor: Color? = null,
) {
    BadgedBox(
        badge = {
            if (badgeIcon != null) {
                Badge(
                    containerColor = Color.Transparent, 
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = stringResource(Res.string.security_icon_badge_warning_description),
                        tint = badgeIconColor ?: colorScheme.onError, 
                        modifier = Modifier.size(16.dp), 
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = mainIconTint)
    }
}

private fun determineSecurityState(
    isLowEntropyKey: Boolean,
    isPreciseLocation: Boolean,
    isMqttEnabled: Boolean,
): SecurityState = when {
    !isLowEntropyKey -> SecurityState.SECURE

    isMqttEnabled && isPreciseLocation -> SecurityState.INSECURE_PRECISE_MQTT_WARNING

    isPreciseLocation -> SecurityState.INSECURE_PRECISE_ONLY

    else -> SecurityState.INSECURE_NO_PRECISE
}

@Composable
fun SecurityIcon(
    securityState: SecurityState,
    baseContentDescription: String = stringResource(Res.string.security_icon_description),
    externalOnClick: (() -> Unit)? = null,
) {
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    val fullContentDescription = baseContentDescription + " " + stringResource(securityState.descriptionResId)

    IconButton(
        onClick = {
            showHelpDialog = true
            externalOnClick?.invoke()
        },
    ) {
        SecurityIconDisplay(
            icon = securityState.icon,
            mainIconTint = securityState.color.invoke(),
            contentDescription = fullContentDescription,
            badgeIcon = securityState.badgeIcon,
            badgeIconColor = securityState.badgeIconColor.invoke(),
        )
    }

    if (showHelpDialog) {
        SecurityHelpDialog(securityState = securityState, onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun SecurityIcon(
    isLowEntropyKey: Boolean,
    isPreciseLocation: Boolean = false,
    isMqttEnabled: Boolean = false,
    baseContentDescription: String = stringResource(Res.string.security_icon_description),
    externalOnClick: (() -> Unit)? = null,
) {
    val securityState = determineSecurityState(isLowEntropyKey, isPreciseLocation, isMqttEnabled)
    SecurityIcon(
        securityState = securityState,
        baseContentDescription = baseContentDescription,
        externalOnClick = externalOnClick,
    )
}

val Channel.isLowEntropyKey: Boolean
    get() = settings.psk.size() <= 1

val Channel.isPreciseLocation: Boolean
    get() = settings.moduleSettings.positionPrecision == PRECISE_POSITION_BITS

val Channel.isMqttEnabled: Boolean
    get() = settings.uplinkEnabled

@Composable
fun SecurityIcon(
    channel: Channel,
    baseContentDescription: String = stringResource(Res.string.security_icon_description),
    externalOnClick: (() -> Unit)? = null,
) = SecurityIcon(
    isLowEntropyKey = channel.isLowEntropyKey,
    isPreciseLocation = channel.isPreciseLocation,
    isMqttEnabled = channel.isMqttEnabled,
    baseContentDescription = baseContentDescription,
    externalOnClick = externalOnClick,
)

@Composable
fun SecurityIcon(
    channelSettings: ChannelSettings,
    loraConfig: LoRaConfig,
    baseContentDescription: String = stringResource(Res.string.security_icon_description),
    externalOnClick: (() -> Unit)? = null,
) {
    val channel = Channel(channelSettings, loraConfig)
    SecurityIcon(
        isLowEntropyKey = channel.isLowEntropyKey,
        isPreciseLocation = channel.isPreciseLocation,
        isMqttEnabled = channel.isMqttEnabled,
        baseContentDescription = baseContentDescription,
        externalOnClick = externalOnClick,
    )
}

@Composable
fun SecurityIcon(
    channelSet: AppOnlyProtos.ChannelSet,
    channelIndex: Int,
    baseContentDescription: String = stringResource(Res.string.security_icon_description),
    externalOnClick: (() -> Unit)? = null,
) {
    channelSet.getChannel(channelIndex)?.let { channel ->
        SecurityIcon(
            channel = channel,
            baseContentDescription = baseContentDescription,
            externalOnClick = externalOnClick,
        )
    }
}

@Composable
fun SecurityIcon(
    channelSet: AppOnlyProtos.ChannelSet,
    channelName: String,
    baseContentDescription: String = stringResource(Res.string.security_icon_description),
    externalOnClick: (() -> Unit)? = null,
) {
    val channelByNameMap =
        remember(channelSet) { channelSet.settingsList.associateBy { Channel(it, channelSet.loraConfig).name } }

    channelByNameMap[channelName]?.let { channelSetting ->
        SecurityIcon(
            channel = Channel(channelSetting, channelSet.loraConfig),
            baseContentDescription = baseContentDescription,
            externalOnClick = externalOnClick,
        )
    }
}

@Composable
private fun SecurityHelpDialog(securityState: SecurityState, onDismiss: () -> Unit) {
    var showAll by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        modifier =
        if (showAll) {
            Modifier.fillMaxSize()
        } else {
            Modifier
        },
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (showAll) {
                    stringResource(Res.string.security_icon_help_title_all)
                } else {
                    stringResource(Res.string.security_icon_help_title)
                },
            )
        },
        text = {
            if (showAll) {
                AllSecurityStates()
            } else {
                ContextualSecurityState(securityState)
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(
                        if (showAll) {
                            stringResource(Res.string.security_icon_help_show_less)
                        } else {
                            stringResource(Res.string.security_icon_help_show_all)
                        },
                    )
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.security_icon_help_dismiss)) }
            }
        },
    )
}

@Composable
private fun ContextualSecurityState(securityState: SecurityState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SecurityIconDisplay(
            icon = securityState.icon,
            mainIconTint = securityState.color.invoke(),
            contentDescription = stringResource(securityState.descriptionResId),
            modifier = Modifier.size(48.dp),
            badgeIcon = securityState.badgeIcon,
            badgeIconColor = securityState.badgeIconColor.invoke(),
        )
        Spacer(Modifier.height(16.dp))
        Text(text = stringResource(securityState.helpTextResId), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AllSecurityStates() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        SecurityState.entries.forEach { state ->
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                SecurityIconDisplay(
                    icon = state.icon,
                    mainIconTint = state.color.invoke(),
                    contentDescription = stringResource(state.descriptionResId),
                    modifier = Modifier.size(48.dp),
                    badgeIcon = state.badgeIcon,
                    badgeIconColor = state.badgeIconColor.invoke(),
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = stringResource(state.descriptionResId), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(state.helpTextResId), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state != SecurityState.entries.lastOrNull()) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Preview(name = "Secure Channel Icon")
@Composable
private fun PreviewSecureChannel() {
    SecurityIcon(securityState = SecurityState.SECURE)
}

@Preview(name = "Insecure Precise Icon")
@Composable
private fun PreviewInsecureChannelWithPreciseLocation() {
    SecurityIcon(securityState = SecurityState.INSECURE_PRECISE_ONLY)
}

@Preview(name = "Insecure Channel Icon")
@Composable
private fun PreviewInsecureChannelWithoutPreciseLocation() {
    SecurityIcon(securityState = SecurityState.INSECURE_NO_PRECISE)
}

@Preview(name = "MQTT Enabled Icon")
@Composable
private fun PreviewMqttEnabled() {
    SecurityIcon(securityState = SecurityState.INSECURE_PRECISE_MQTT_WARNING)
}

@Preview(name = "All Security Icons with Dialog")
@Composable
private fun PreviewAllSecurityIconsWithDialog() {
    var showHelpDialogFor by remember { mutableStateOf<SecurityState?>(null) }
    val stateLabels = remember {
        
        mapOf(
            SecurityState.SECURE to "Secure",
            SecurityState.INSECURE_NO_PRECISE to "Insecure (No Precise Location)",
            SecurityState.INSECURE_PRECISE_ONLY to "Insecure (Precise Location Only)",
            SecurityState.INSECURE_PRECISE_MQTT_WARNING to "Insecure (Precise Location + MQTT Warning)",
        )
    }

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Security Icons Preview (Click for Help)", style = MaterialTheme.typography.headlineSmall)

        SecurityState.entries.forEach { state ->
            
            val label = stateLabels[state] ?: "Unknown State (${state.name})" 
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SecurityIcon(securityState = state, externalOnClick = { showHelpDialogFor = state })
                Text(label)
            }
        }
        showHelpDialogFor?.let { SecurityHelpDialog(securityState = it, onDismiss = { showHelpDialogFor = null }) }
    }
}

