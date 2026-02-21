

package org.meshtastic.feature.node.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Wind Dir -359°")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirectionn359() {
    PreviewWindDirectionItem(-359f)
}

@Preview(name = "Wind Dir 0°")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirection0() {
    PreviewWindDirectionItem(0f)
}

@Preview(name = "Wind Dir 45°")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirection45() {
    PreviewWindDirectionItem(45f)
}

@Preview(name = "Wind Dir 90°")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirection90() {
    PreviewWindDirectionItem(90f)
}

@Preview(name = "Wind Dir 180°")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirection180() {
    PreviewWindDirectionItem(180f)
}

@Preview(name = "Wind Dir 225°")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirection225() {
    PreviewWindDirectionItem(225f)
}

@Preview(name = "Wind Dir 270°")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirection270() {
    PreviewWindDirectionItem(270f)
}

@Preview(name = "Wind Dir 315°")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirection315() {
    PreviewWindDirectionItem(315f)
}

@Preview(name = "Wind Dir -45")
@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirectionN45() {
    PreviewWindDirectionItem(-45f)
}

@Suppress("detekt:MagicNumber")
@Composable
private fun PreviewWindDirectionItem(windDirection: Float, windSpeed: String = "5 m/s") {
    val normalizedBearing = (windDirection + 180) % 360
    InfoCard(icon = Icons.Outlined.Navigation, text = "Wind", value = windSpeed, rotateIcon = normalizedBearing)
}

