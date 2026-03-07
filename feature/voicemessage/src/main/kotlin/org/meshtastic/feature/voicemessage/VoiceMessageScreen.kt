package org.meshtastic.feature.voicemessage

import android.Manifest
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.meshtastic.core.model.voice.VoiceMessage
import org.meshtastic.core.service.ConnectionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PttRed = Color(0xFFD32F2F)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceMessageScreen(viewModel: VoiceMessageViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val incomingMessages by viewModel.incomingMessages.collectAsStateWithLifecycle()

    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val isConnected = connectionState == ConnectionState.Connected
    val isRecording = uiState is VoiceUiState.Recording
    val isSending = uiState is VoiceUiState.Sending

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Push-to-Talk",
                style = MaterialTheme.typography.headlineSmall,
            )

            when {
                !isConnected -> Text(
                    text = "Not connected to mesh",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                isSending -> {
                    val state = uiState as VoiceUiState.Sending
                    Text(
                        text = "Sending ${state.fragmentsSent}/${state.totalFragments}…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { state.fragmentsSent.toFloat() / state.totalFragments },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                uiState is VoiceUiState.Error -> Text(
                    text = (uiState as VoiceUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                else -> Text(
                    text = if (isRecording) "Recording… release to send" else "Hold button to record",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) PttRed else MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val pttColor = when {
                !isConnected || isSending -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                isRecording -> PttRed
                else -> MaterialTheme.colorScheme.primary
            }

            Surface(
                shape = CircleShape,
                color = pttColor,
                modifier = Modifier
                    .size(120.dp)
                    .pointerInput(isConnected, isSending) {
                        detectTapGestures(
                            onPress = {
                                if (!isConnected || isSending) return@detectTapGestures
                                if (!micPermission.status.isGranted) {
                                    micPermission.launchPermissionRequest()
                                    return@detectTapGestures
                                }
                                viewModel.startRecording()
                                tryAwaitRelease()
                                viewModel.stopRecordingAndSend()
                            },
                        )
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Push to Talk",
                        modifier = Modifier.size(48.dp),
                        tint = if (!isConnected || isSending)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            if (incomingMessages.isNotEmpty()) {
                Text(
                    text = "Received Messages",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(incomingMessages.asReversed(), key = { it.sessionId }) { msg ->
                        VoiceMessageCard(msg, onPlay = { viewModel.playMessage(it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceMessageCard(message: VoiceMessage, onPlay: (VoiceMessage) -> Unit) {
    val timeStr = remember(message.timestampMs) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestampMs))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = "Node !%08x".format(message.fromNodeNum),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "$timeStr · %.1fs".format(message.durationSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { onPlay(message) },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
