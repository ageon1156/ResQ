
package org.meshtastic.feature.settings.radio.component

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.close
import org.meshtastic.core.strings.delivery_confirmed
import org.meshtastic.core.strings.error
import org.meshtastic.feature.settings.radio.ResponseState

private const val AUTO_DISMISS_DELAY_MS = 1500L

@Composable
fun <T> PacketResponseStateDialog(state: ResponseState<T>, onDismiss: () -> Unit = {}, onComplete: () -> Unit = {}) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(state) {
        if (state is ResponseState.Success) {
            delay(AUTO_DISMISS_DELAY_MS)
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = {},
        shape = MaterialTheme.shapes.extraSmall,
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state is ResponseState.Loading) {
                    val progress by
                        animateFloatAsState(
                            targetValue = state.completed.toFloat() / state.total.toFloat(),
                            label = "progress",
                        )
                    Text(
                        text = "%.0f%%".format(progress * 100),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    state.status?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.completed >= state.total) onComplete()
                }
                if (state is ResponseState.Success) {
                    Text(
                        text = stringResource(Res.string.delivery_confirmed).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (state is ResponseState.Error) {
                    Text(
                        text = stringResource(Res.string.error).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        minLines = 2,
                    )
                    Text(text = state.error.asString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        if (state is ResponseState.Success || state is ResponseState.Error) {
                            backDispatcher?.onBackPressed()
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Text(stringResource(Res.string.close).uppercase(), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun PacketResponseStateDialogPreview() {
    PacketResponseStateDialog(state = ResponseState.Loading(total = 17, completed = 5))
}

