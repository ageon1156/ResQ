

package org.meshtastic.feature.intro

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal const val SETTINGS_TAG = "settings_link_tag"

@Composable
internal fun FeatureRow(feature: FeatureUIData) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = feature.icon,
            contentDescription = feature.titleRes?.let { stringResource(it) } ?: stringResource(feature.subtitleRes),
            modifier = Modifier.padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column {
            feature.titleRes?.let { titleRes ->
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Text(
                text = stringResource(feature.subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun Context.createClickableAnnotatedString(
    fullTextRes: StringResource,
    linkTextRes: StringResource,
    tag: String,
): AnnotatedString {
    val fullText = stringResource(fullTextRes)
    val linkText = stringResource(linkTextRes)
    val startIndex = fullText.indexOf(linkText)

    return buildAnnotatedString {
        append(fullText)
        if (startIndex != -1) {
            val endIndex = startIndex + linkText.length
            addStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
                start = startIndex,
                end = endIndex,
            )
            addStringAnnotation(tag = tag, annotation = linkText, start = startIndex, end = endIndex)
        }
    }
}

