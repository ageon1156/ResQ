

package org.meshtastic.feature.intro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.configure_critical_alerts
import org.meshtastic.core.strings.critical_alerts
import org.meshtastic.core.strings.critical_alerts_dnd_request_text
import org.meshtastic.core.strings.skip

@Composable
internal fun CriticalAlertsScreen(onSkip: () -> Unit, onConfigure: () -> Unit) {
    Scaffold(
        bottomBar = {
            IntroBottomBar(
                onSkip = onSkip,
                onConfigure = onConfigure,
                configureButtonText = stringResource(Res.string.configure_critical_alerts),
                skipButtonText = stringResource(Res.string.skip),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.critical_alerts),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.critical_alerts_dnd_request_text),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

