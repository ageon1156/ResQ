

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TextDividerPreference(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    TextDividerPreference(
        title = AnnotatedString(text = title),
        enabled = enabled,
        modifier = modifier,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun TextDividerPreference(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(modifier = modifier.fillMaxWidth().padding(all = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color =
                if (!enabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                } else {
                    Color.Unspecified
                },
            )
            if (trailingIcon != null) {
                Icon(trailingIcon, "trailingIcon", modifier = modifier.fillMaxWidth().wrapContentWidth(Alignment.End))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextDividerPreferencePreview() {
    TextDividerPreference(title = "Advanced settings")
}

