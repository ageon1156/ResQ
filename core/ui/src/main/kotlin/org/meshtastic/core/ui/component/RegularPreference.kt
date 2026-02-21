

package org.meshtastic.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
fun RegularPreference(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
    trailingIcon: ImageVector? = null,
    dropdownMenu: @Composable () -> Unit = {},
) {
    RegularPreference(
        title = title,
        subtitle = AnnotatedString(text = subtitle),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        summary = summary,
        trailingIcon = trailingIcon,
        dropdownMenu = dropdownMenu,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegularPreference(
    title: String,
    subtitle: AnnotatedString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
    trailingIcon: ImageVector? = null,
    dropdownMenu: @Composable () -> Unit = {},
) {
    val color =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }

    Column(modifier = modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(all = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                    if (enabled) {
                        Color.Unspecified
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )

                Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = color)
            }
            if (trailingIcon != null) {
                Box {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = "trailingIcon",
                        modifier = Modifier.padding(start = 8.dp).wrapContentWidth(Alignment.End),
                        tint = color,
                    )
                    dropdownMenu()
                }
            }
        }
        if (summary != null) {
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegularPreferencePreview() {
    RegularPreference(title = "Advanced settings", subtitle = "Text2", onClick = {})
}

