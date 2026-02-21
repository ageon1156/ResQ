

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceCategory(
    text: String,
    modifier: Modifier = Modifier,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Text(
        text,
        modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp),
        style = MaterialTheme.typography.titleLarge,
    )
    if (content != null) {
        Card(modifier = modifier.padding(bottom = 8.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge) { content() }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreferenceCategoryPreview() {
    PreferenceCategory(text = "Advanced settings")
}

