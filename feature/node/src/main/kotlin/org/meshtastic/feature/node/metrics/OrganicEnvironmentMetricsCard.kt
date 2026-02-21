
package org.meshtastic.feature.node.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import org.meshtastic.core.ui.theme.LeafShape
import org.meshtastic.feature.node.metrics.CommonCharts.DATE_TIME_FORMAT
import org.meshtastic.feature.node.metrics.CommonCharts.MS_PER_SEC
import org.meshtastic.proto.TelemetryProtos.Telemetry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrganicEnvironmentMetricsCard(telemetry: Telemetry, environmentDisplayFahrenheit: Boolean) {
    val envMetrics = telemetry.environmentMetrics
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                
                envMetrics.temperature?.let { temp ->
                    if (!temp.isNaN()) {
                        val displayTemp = if (environmentDisplayFahrenheit) {
                            temp * 9 / 5 + 32
                        } else {
                            temp
                        }
                        val unit = if (environmentDisplayFahrenheit) "°F" else "°C"
                        OrganicEnvMetricChip(
                            label = "TEMP",
                            value = "%.1f%s".format(displayTemp, unit),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                envMetrics.relativeHumidity?.let { humidity ->
                    if (!humidity.isNaN() && humidity > 0f) {
                        OrganicEnvMetricChip(
                            label = "HUMID",
                            value = "%.0f%%".format(humidity),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                envMetrics.barometricPressure?.let { pressure ->
                    if (!pressure.isNaN() && pressure > 0f) {
                        OrganicEnvMetricChip(
                            label = "PRESS",
                            value = "%.1f hPa".format(pressure),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                envMetrics.gasResistance?.let { gas ->
                    if (!gas.isNaN() && gas > 0f) {
                        OrganicEnvMetricChip(
                            label = "GAS",
                            value = "%.0f Ω".format(gas),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                envMetrics.iaq?.let { iaq ->
                    if (iaq > 0) {
                        OrganicEnvMetricChip(
                            label = "IAQ",
                            value = iaq.toString(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                envMetrics.lux?.let { lux ->
                    if (!lux.isNaN() && lux > 0f) {
                        OrganicEnvMetricChip(
                            label = "LIGHT",
                            value = "%.0f lx".format(lux),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganicEnvMetricChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

