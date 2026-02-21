

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

