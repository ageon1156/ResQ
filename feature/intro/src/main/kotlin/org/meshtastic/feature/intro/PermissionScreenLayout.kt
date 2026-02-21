

package org.meshtastic.feature.intro

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.skip

@Composable
internal fun PermissionScreenLayout(
    headlineRes: StringResource,
    annotatedDescription: AnnotatedString,
    features: List<FeatureUIData>,
    additionalContent: (@Composable () -> Unit)? = null,
    onSkip: () -> Unit,
    onConfigure: () -> Unit,
    configureButtonTextRes: StringResource,
    onAnnotationClick: (String) -> Unit,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val pressIndicator =
        Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                textLayoutResult?.let { layoutResult ->
                    val position = layoutResult.getOffsetForPosition(offset)
                    annotatedDescription.getStringAnnotations(
                        SETTINGS_TAG,
                        position,
                        position,
                    ).firstOrNull()?.let { annotation ->
                        onAnnotationClick(annotation.item)
                    }
                }
            }
        }

    Scaffold(
        bottomBar = {
            IntroBottomBar(
                onSkip = onSkip,
                onConfigure = onConfigure,
                configureButtonText = stringResource(configureButtonTextRes),
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
                text = stringResource(headlineRes),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = annotatedDescription,
                style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(horizontal = 16.dp).then(pressIndicator),
                onTextLayout = { textLayoutResult = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            features.forEach { feature ->
                FeatureRow(feature = feature)
                Spacer(modifier = Modifier.height(16.dp))
            }
            additionalContent?.invoke()
        }
    }
}

