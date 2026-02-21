
package org.meshtastic.feature.node.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.channel_1
import org.meshtastic.core.strings.channel_2
import org.meshtastic.core.strings.channel_3
import org.meshtastic.core.ui.theme.LeafShape
import org.meshtastic.feature.node.metrics.CommonCharts.DATE_TIME_FORMAT
import org.meshtastic.feature.node.metrics.CommonCharts.MS_PER_SEC
import org.meshtastic.proto.TelemetryProtos.Telemetry

@Composable
fun OrganicPowerMetricsCard(telemetry: Telemetry) {
    val time = telemetry.time * MS_PER_SEC

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = LeafShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            Text(
                text = DATE_TIME_FORMAT.format(time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (telemetry.powerMetrics.hasCh1Current() || telemetry.powerMetrics.hasCh1Voltage()) {
                    OrganicPowerChannel(
                        titleRes = Res.string.channel_1,
                        voltage = telemetry.powerMetrics.ch1Voltage,
                        current = telemetry.powerMetrics.ch1Current,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (telemetry.powerMetrics.hasCh2Current() || telemetry.powerMetrics.hasCh2Voltage()) {
                    OrganicPowerChannel(
                        titleRes = Res.string.channel_2,
                        voltage = telemetry.powerMetrics.ch2Voltage,
                        current = telemetry.powerMetrics.ch2Current,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (telemetry.powerMetrics.hasCh3Current() || telemetry.powerMetrics.hasCh3Voltage()) {
                    OrganicPowerChannel(
                        titleRes = Res.string.channel_3,
                        voltage = telemetry.powerMetrics.ch3Voltage,
                        current = telemetry.powerMetrics.ch3Current,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrganicPowerChannel(
    titleRes: StringResource,
    voltage: Float,
    current: Float,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        if (voltage > 0f) {
            Text(
                text = "%.2fV".format(voltage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (current > 0f) {
            Text(
                text = "%.0fmA".format(current),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

