
package org.meshtastic.core.ui.qr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Channel
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.accept
import org.meshtastic.core.strings.add
import org.meshtastic.core.strings.add_channels_description
import org.meshtastic.core.strings.cancel
import org.meshtastic.core.strings.new_channel_rcvd
import org.meshtastic.core.strings.replace
import org.meshtastic.core.strings.replace_channels_and_settings_description
import org.meshtastic.core.ui.component.ChannelSelection
import org.meshtastic.proto.AppOnlyProtos.ChannelSet
import org.meshtastic.proto.ConfigProtos.Config.LoRaConfig.ModemPreset
import org.meshtastic.proto.channelSet
import org.meshtastic.proto.copy

@Composable
fun ScannedQrCodeDialog(
    incoming: ChannelSet,
    onDismiss: () -> Unit,
    viewModel: ScannedQrCodeViewModel = hiltViewModel(),
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()

    ScannedQrCodeDialog(
        channels = channels,
        incoming = incoming,
        onDismiss = onDismiss,
        onConfirm = viewModel::setChannels,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun ScannedQrCodeDialog(
    channels: ChannelSet,
    incoming: ChannelSet,
    onDismiss: () -> Unit,
    onConfirm: (ChannelSet) -> Unit,
) {
    var shouldReplace by remember { mutableStateOf(incoming.hasLoraConfig()) }

    val channelSet =
        remember(shouldReplace) {
            if (shouldReplace) {

                incoming.copy {
                    loraConfig =
                        loraConfig.copy {
                            configOkToMqtt = channels.loraConfig.configOkToMqtt
                            txPower = channels.loraConfig.txPower
                        }
                }
            } else {
                channels.copy {

                    val result = LinkedHashSet(settings + incoming.settingsList)
                    settings.clear()
                    settings.addAll(result)
                }
            }
        }

    val modemPresetName = Channel(loraConfig = channelSet.loraConfig).name

    val channelSelections =
        remember(channelSet) { mutableStateListOf(elements = Array(size = channelSet.settingsCount, init = { true })) }

    val selectedChannelSet =
        channelSet.copy {

            val result =
                settings.filterIndexed { i, _ ->
                    val isExisting = !shouldReplace && i < channels.settingsCount
                    isExisting || channelSelections.getOrNull(i) == true
                }
            settings.clear()
            settings.addAll(result)
        }

    val loraChanges =
        remember(shouldReplace, channels, incoming) {
            if (shouldReplace && incoming.hasLoraConfig()) {
                val current = channels.loraConfig
                val new = incoming.loraConfig
                val changes = mutableListOf<String>()

                if (current.hopLimit != new.hopLimit) {
                    changes.add("Hop Limit: ${current.hopLimit} -> ${new.hopLimit}")
                }
                if (current.getRegion() != new.getRegion()) {
                    val currentRegionDesc = current.getRegion()?.name ?: "Unknown"
                    val newRegionDesc = new.getRegion()?.name ?: "Unknown"
                    changes.add("Region: $currentRegionDesc -> $newRegionDesc")
                }
                if (current.modemPreset != new.modemPreset) {
                    val currentPresetDesc = ModemPreset.forNumber(current.modemPreset.number)?.name ?: "Unknown"
                    val newPresetDesc = ModemPreset.forNumber(new.modemPreset.number)?.name ?: "Unknown"
                    changes.add("Modem Preset: $currentPresetDesc -> $newPresetDesc")
                }
                if (current.usePreset != new.usePreset) {
                    changes.add("Use Preset: ${current.usePreset} -> ${new.usePreset}")
                }

                changes
            } else {
                emptyList()
            }
        }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Text(
                        text = stringResource(Res.string.new_channel_rcvd),
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                item {
                    Text(
                        text =
                        stringResource(
                            if (shouldReplace) {
                                Res.string.replace_channels_and_settings_description
                            } else {
                                Res.string.add_channels_description
                            },
                        ),
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                itemsIndexed(channelSet.settingsList) { index, channel ->
                    val isExisting = !shouldReplace && index < channels.settingsCount
                    val channelObj = Channel(channel, channelSet.loraConfig)
                    ChannelSelection(
                        index = index,
                        title = channel.name.ifEmpty { modemPresetName },
                        enabled = !isExisting,
                        isSelected = if (isExisting) true else channelSelections[index],
                        onSelected = {
                            if (it || selectedChannelSet.settingsCount > 1) {
                                channelSelections[index] = it
                            }
                        },
                        channel = channelObj,
                    )
                }

                if (shouldReplace && loraChanges.isNotEmpty()) {
                    item {
                        Text(
                            text = "LoRa Configuration Changes:",
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        loraChanges.forEach { change ->
                            Text(
                                text = "• $change",
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.padding(vertical = 20.dp)) {
                        val selectedColors = ButtonDefaults.buttonColors()
                        val unselectedColors =
                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)

                        OutlinedButton(
                            onClick = { shouldReplace = false },
                            modifier = Modifier.height(48.dp).weight(1f),
                            colors = if (!shouldReplace) selectedColors else unselectedColors,
                        ) {
                            Text(text = stringResource(Res.string.add))
                        }

                        OutlinedButton(
                            onClick = { shouldReplace = true },
                            modifier = Modifier.height(48.dp).weight(1f),
                            enabled = incoming.hasLoraConfig(),
                            colors = if (shouldReplace) selectedColors else unselectedColors,
                        ) {
                            Text(text = stringResource(Res.string.replace))
                        }
                    }
                }

                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    ) {
                        TextButton(onClick = { onDismiss() }) {
                            Text(
                                text = stringResource(Res.string.cancel),
                                color = MaterialTheme.colorScheme.onSurface,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                        TextButton(
                            onClick = {
                                onDismiss()
                                onConfirm(selectedChannelSet)
                            },
                            enabled = selectedChannelSet.settingsCount in 1..8,
                        ) {
                            Text(
                                text = stringResource(Res.string.accept),
                                color = MaterialTheme.colorScheme.onSurface,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun ScannedQrCodeDialogPreview() {
    ScannedQrCodeDialog(
        channels =
        channelSet {
            settings.add(Channel.default.settings)
            loraConfig = Channel.default.loraConfig
        },
        incoming =
        channelSet {
            settings.add(Channel.default.settings)
            loraConfig = Channel.default.loraConfig
        },
        onDismiss = {},
        onConfirm = {},
    )
}

