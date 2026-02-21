
package com.geeksville.mesh.ui.connections.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun EmptyStateContent(
    text: String,
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    actionButton: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        imageVector?.let { Icon(imageVector = imageVector, contentDescription = text, modifier = Modifier.size(96.dp)) }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
        )

        actionButton?.invoke()
    }
}

@PreviewLightDark
@Composable
fun EmptyStateContentPreview() {
    AppTheme {
        Surface {
            EmptyStateContent(text = "No devices found", imageVector = Icons.Rounded.BluetoothDisabled) {
                Button(onClick = {}) { Text("Button") }
            }
        }
    }
}

