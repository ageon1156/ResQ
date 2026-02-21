
package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.model.isUnmessageableRole
import org.meshtastic.core.model.Capabilities
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.hardware_model
import org.meshtastic.core.strings.licensed_amateur_radio
import org.meshtastic.core.strings.licensed_amateur_radio_text
import org.meshtastic.core.strings.long_name
import org.meshtastic.core.strings.node_id
import org.meshtastic.core.strings.short_name
import org.meshtastic.core.strings.unmessageable
import org.meshtastic.core.strings.unmonitored_or_infrastructure
import org.meshtastic.core.strings.user
import org.meshtastic.core.strings.user_config
import org.meshtastic.core.ui.component.EditTextPreference
import org.meshtastic.core.ui.component.RegularPreference
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.component.TitledCard
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.proto.copy

@Composable
fun UserConfigScreen(viewModel: RadioConfigViewModel = hiltViewModel(), onBack: () -> Unit) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()
    val userConfig = state.userConfig
    val formState = rememberConfigState(initialValue = userConfig)
    val capabilities = remember(state.metadata?.firmwareVersion) { Capabilities(state.metadata?.firmwareVersion) }

    val validLongName = formState.value.longName.isNotBlank()
    val validShortName = formState.value.shortName.isNotBlank()
    val validNames = validLongName && validShortName
    val focusManager = LocalFocusManager.current

    RadioConfigScreenList(
        title = stringResource(Res.string.user),
        onBack = onBack,
        configState = formState,
        enabled = state.connected && validNames,
        responseState = state.responseState,
        onDismissPacketResponse = viewModel::clearPacketResponse,
        onSave = viewModel::setOwner,
    ) {
        item {
            TitledCard(title = stringResource(Res.string.user_config)) {
                RegularPreference(
                    title = stringResource(Res.string.node_id),
                    subtitle = formState.value.id,
                    onClick = {},
                )
                HorizontalDivider()
                EditTextPreference(
                    title = stringResource(Res.string.long_name),
                    value = formState.value.longName,
                    maxSize = 39, 
                    enabled = state.connected,
                    isError = !validLongName,
                    keyboardOptions =
                    KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    onValueChanged = { formState.value = formState.value.copy { longName = it } },
                )
                HorizontalDivider()
                EditTextPreference(
                    title = stringResource(Res.string.short_name),
                    value = formState.value.shortName,
                    maxSize = 4, 
                    enabled = state.connected,
                    isError = !validShortName,
                    keyboardOptions =
                    KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    onValueChanged = { formState.value = formState.value.copy { shortName = it } },
                )
                HorizontalDivider()
                RegularPreference(
                    title = stringResource(Res.string.hardware_model),
                    subtitle = formState.value.hwModel.name,
                    onClick = {},
                )
                HorizontalDivider()
                SwitchPreference(
                    title = stringResource(Res.string.unmessageable),
                    summary = stringResource(Res.string.unmonitored_or_infrastructure),
                    checked =
                    formState.value.isUnmessagable ||
                        (!capabilities.canToggleUnmessageable && formState.value.role.isUnmessageableRole()),
                    enabled = formState.value.hasIsUnmessagable() || capabilities.canToggleUnmessageable,
                    onCheckedChange = { formState.value = formState.value.copy { isUnmessagable = it } },
                    containerColor = CardDefaults.cardColors().containerColor,
                )
                HorizontalDivider()
                SwitchPreference(
                    title = stringResource(Res.string.licensed_amateur_radio),
                    summary = stringResource(Res.string.licensed_amateur_radio_text),
                    checked = formState.value.isLicensed,
                    enabled = state.connected,
                    onCheckedChange = { formState.value = formState.value.copy { isLicensed = it } },
                    containerColor = CardDefaults.cardColors().containerColor,
                )
            }
        }
    }
}

