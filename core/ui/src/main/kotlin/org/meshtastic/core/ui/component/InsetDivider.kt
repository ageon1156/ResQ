

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun InsetDivider(
    modifier: Modifier = Modifier,
    inset: Dp = 16.dp,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    InsetDivider(modifier = modifier, startInset = inset, endInset = inset, thickness = thickness, color = color)
}

@Composable
fun InsetDivider(
    modifier: Modifier = Modifier,
    startInset: Dp = 0.dp,
    endInset: Dp = 0.dp,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset, end = endInset),
        thickness = thickness,
        color = color,
    )
}

