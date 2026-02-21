

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun TitledCard(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Card(content = content)
    }
}

@PreviewLightDark
@Composable
private fun TitledCardPreview() {
    AppTheme { Surface { TitledCard(title = "Title") { Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {} } } }
}

