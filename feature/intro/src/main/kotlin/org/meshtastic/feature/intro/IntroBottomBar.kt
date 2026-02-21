

package org.meshtastic.feature.intro

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun IntroBottomBar(
    onSkip: () -> Unit,
    onConfigure: () -> Unit,
    skipButtonText: String,
    configureButtonText: String,
    showSkipButton: Boolean = true,
) {
    BottomAppBar(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)) {
        if (showSkipButton) {
            Button(onClick = onSkip) { Text(skipButtonText) }
        }

        Spacer(modifier = Modifier.fillMaxWidth().weight(1f))

        Button(onClick = onConfigure) { Text(configureButtonText) }
    }
}

