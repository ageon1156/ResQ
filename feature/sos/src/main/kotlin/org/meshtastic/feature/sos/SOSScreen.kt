

package org.meshtastic.feature.sos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.service.ConnectionState
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.sos_cancel
import org.meshtastic.core.strings.sos_confirm_message
import org.meshtastic.core.strings.sos_confirm_send
import org.meshtastic.core.strings.sos_confirm_title
import org.meshtastic.core.strings.sos_gps_unavailable
import org.meshtastic.core.strings.sos_info_text
import org.meshtastic.core.strings.sos_last_sent
import org.meshtastic.core.strings.sos_not_connected
import org.meshtastic.core.strings.sos_screen_title
import org.meshtastic.core.strings.sos_send_button
import org.meshtastic.core.strings.sos_send_failed
import org.meshtastic.core.strings.sos_sending
import org.meshtastic.core.strings.sos_sent_location_included
import org.meshtastic.core.strings.sos_sent_location_unavailable
import org.meshtastic.core.strings.sos_sent_success
import org.meshtastic.core.strings.sos_tap_to_retry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SOSRed = Color(0xFFD32F2F)
private val SOSRedDark = Color(0xFF9A0007)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSScreen(
    viewModel: SOSViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsStateWithLifecycle()
    val lastSentTime by viewModel.lastSentTime.collectAsStateWithLifecycle()

    if (showConfirmDialog) {
        SOSConfirmationDialog(
            onConfirm = viewModel::confirmAndSendSOS,
            onDismiss = viewModel::dismissConfirmation,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.sos_screen_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { innerPadding ->
        SOSContent(
            uiState = uiState,
            connectionState = connectionState,
            lastSentTime = lastSentTime,
            locationInfo = viewModel.getLocationText(),
            onSendSOS = viewModel::requestSOS,
            onReset = viewModel::resetState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun SOSContent(
    uiState: SOSUiState,
    connectionState: ConnectionState,
    lastSentTime: Long?,
    locationInfo: String?,
    onSendSOS: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState == ConnectionState.Connected
    val isSending = uiState is SOSUiState.Sending

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        
        Text(
            text = if (isConnected) {
                "\u2022 Connected"
            } else {
                stringResource(Res.string.sos_not_connected)
            },
            color = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            Button(
                onClick = {
                    when (uiState) {
                        is SOSUiState.Sent, is SOSUiState.Error -> onReset()
                        else -> onSendSOS()
                    }
                },
                enabled = isConnected && !isSending,
                modifier = Modifier.size(200.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SOSRed,
                    contentColor = Color.White,
                    disabledContainerColor = if (isSending) SOSRedDark else SOSRed.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White,
                        strokeWidth = 4.dp,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.sos_send_button),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (uiState) {
            is SOSUiState.Ready -> {}
            is SOSUiState.Sending -> {
                Text(
                    text = stringResource(Res.string.sos_sending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            is SOSUiState.Sent -> {
                Text(
                    text = stringResource(Res.string.sos_sent_success),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.locationIncluded) {
                        stringResource(Res.string.sos_sent_location_included)
                    } else {
                        stringResource(Res.string.sos_sent_location_unavailable)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.sos_tap_to_retry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is SOSUiState.Error -> {
                Text(
                    text = stringResource(Res.string.sos_send_failed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.sos_tap_to_retry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (locationInfo != null) {
                "GPS: $locationInfo"
            } else {
                stringResource(Res.string.sos_gps_unavailable)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.sos_info_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        if (lastSentTime != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val timeText = SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(Date(lastSentTime))
            Text(
                text = stringResource(Res.string.sos_last_sent, timeText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SOSConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.sos_confirm_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(text = stringResource(Res.string.sos_confirm_message))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SOSRed,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.sos_confirm_send),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.sos_cancel))
            }
        },
    )
}

