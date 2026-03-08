

package org.meshtastic.feature.node.metrics

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.util.UnitConversions.celsiusToFahrenheit
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.iaq
import org.meshtastic.core.strings.iaq_definition
import org.meshtastic.core.ui.component.OptionLabel
import org.meshtastic.core.ui.component.SlidingSelector
import org.meshtastic.feature.node.model.TimeFrame
import org.meshtastic.proto.TelemetryProtos.Telemetry
import org.meshtastic.proto.copy

@Composable
fun EnvironmentMetricsScreen(viewModel: MetricsViewModel = hiltViewModel(), onNavigateUp: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val environmentState by viewModel.environmentState.collectAsStateWithLifecycle()
    val selectedTimeFrame by viewModel.timeFrame.collectAsState()
    val graphData = environmentState.environmentMetricsFiltered(selectedTimeFrame, state.isFahrenheit)
    val data = graphData.metrics
    val processedTelemetries = if (state.isFahrenheit) {
        data.map { telemetry ->
            telemetry.copy {
                environmentMetrics = telemetry.environmentMetrics.copy {
                    temperature = celsiusToFahrenheit(telemetry.environmentMetrics.temperature)
                    soilTemperature = celsiusToFahrenheit(telemetry.environmentMetrics.soilTemperature)
                }
            }
        }
    } else data
    var displayInfoDialog by remember { mutableStateOf(false) }

    MetricsScaffold(title = state.node?.user?.longName ?: "", onNavigateUp = onNavigateUp) {
        if (displayInfoDialog) {
            LegendInfoDialog(pairedRes = listOf(Pair(Res.string.iaq, Res.string.iaq_definition)), onDismiss = { displayInfoDialog = false })
        }
        EnvironmentMetricsChart(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.33f), telemetries = processedTelemetries.reversed(), graphData = graphData, selectedTime = selectedTimeFrame, promptInfoDialog = { displayInfoDialog = true })
        SlidingSelector(TimeFrame.entries.toList(), selectedTimeFrame, onOptionSelected = { viewModel.setTimeFrame(it) }) { OptionLabel(stringResource(it.strRes)) }
        LazyColumn(modifier = Modifier.fillMaxSize()) { items(processedTelemetries) { OrganicEnvironmentMetricsCard(it, state.isFahrenheit) } }
    }
}


