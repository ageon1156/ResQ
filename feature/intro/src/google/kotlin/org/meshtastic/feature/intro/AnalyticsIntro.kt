

package org.meshtastic.feature.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.analytics_notice
import org.meshtastic.core.strings.analytics_platforms
import org.meshtastic.core.strings.datadog_link
import org.meshtastic.core.strings.firebase_link
import org.meshtastic.core.strings.for_more_information_see_our_privacy_policy
import org.meshtastic.core.strings.privacy_url
import org.meshtastic.core.ui.component.AutoLinkText

@Composable
fun AnalyticsIntro(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val textModifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        Text(modifier = textModifier, textAlign = TextAlign.Center, text = stringResource(Res.string.analytics_notice))
        Text(
            modifier = textModifier,
            textAlign = TextAlign.Center,
            text = stringResource(Res.string.analytics_platforms),
        )
        AutoLinkText(stringResource(Res.string.firebase_link))
        AutoLinkText(stringResource(Res.string.datadog_link))

        Text(
            modifier = textModifier,
            textAlign = TextAlign.Center,
            text = stringResource(Res.string.for_more_information_see_our_privacy_policy),
        )
        AutoLinkText(text = stringResource(Res.string.privacy_url))
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsIntroPreview() {
    AnalyticsIntro()
}

