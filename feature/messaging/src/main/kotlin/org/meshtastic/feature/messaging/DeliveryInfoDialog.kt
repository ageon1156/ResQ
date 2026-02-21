
package org.meshtastic.feature.messaging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.close
import org.meshtastic.core.strings.message_retry_count
import org.meshtastic.core.strings.relays
import org.meshtastic.core.strings.resend

@Suppress("UnusedParameter")
@Composable
fun DeliveryInfo(
    title: StringResource,
    resendOption: Boolean,
    text: StringResource? = null,
    relayNodeName: String? = null,
    relays: Int = 0,
    retryCount: Int = 0,
    maxRetries: Int = 0,
    onConfirm: (() -> Unit) = {},
    onDismiss: () -> Unit = {},
) = AlertDialog(
    onDismissRequest = onDismiss,
    dismissButton = {
        FilledTonalButton(onClick = onDismiss, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = stringResource(Res.string.close))
        }
    },
    confirmButton = {
        if (resendOption) {
            FilledTonalButton(onClick = onConfirm, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = stringResource(Res.string.resend))
            }
        }
    },
    title = {
        Text(
            text = stringResource(title),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
        )
    },
    text = {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            text?.let {
                Text(
                    text = stringResource(it),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (maxRetries > 0) {
                Text(
                    text = stringResource(Res.string.message_retry_count, retryCount, maxRetries),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (relays != 0) {
                Text(
                    text = pluralStringResource(Res.plurals.relays, relays, relays),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    },
    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    containerColor = MaterialTheme.colorScheme.surface,
)

