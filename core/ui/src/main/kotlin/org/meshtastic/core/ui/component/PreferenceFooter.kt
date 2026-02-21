

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Deprecated(message = "Use overload that accepts Strings for button text.")
@Composable
fun PreferenceFooter(
    enabled: Boolean,
    negativeText: StringResource,
    onNegativeClicked: () -> Unit,
    positiveText: StringResource,
    onPositiveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceFooter(
        enabled = enabled,
        negativeText = stringResource(negativeText),
        onNegativeClicked = onNegativeClicked,
        positiveText = stringResource(positiveText),
        onPositiveClicked = onPositiveClicked,
        modifier = modifier,
    )
}

@Composable
fun PreferenceFooter(
    enabled: Boolean,
    negativeText: String,
    onNegativeClicked: () -> Unit,
    positiveText: String,
    onPositiveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ElevatedButton(
            modifier = Modifier.height(48.dp).weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(),
            onClick = onNegativeClicked,
        ) {
            Text(text = negativeText)
        }
        ElevatedButton(
            modifier = Modifier.height(48.dp).weight(1f),
            colors = ButtonDefaults.buttonColors(),
            onClick = { if (enabled) onPositiveClicked() },
        ) {
            Text(text = positiveText)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreferenceFooterPreview() {
    PreferenceFooter(
        enabled = true,
        negativeText = "Cancel",
        onNegativeClicked = {},
        positiveText = "Save",
        onPositiveClicked = {},
    )
}

