

package org.meshtastic.feature.settings.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FixedUpdateIntervals.toDisplayString(): String = if (pluralRes != null && quantity != null) {
    pluralStringResource(pluralRes, quantity, quantity)
} else if (textRes != null) {
    stringResource(textRes)
} else {
    ""
}

