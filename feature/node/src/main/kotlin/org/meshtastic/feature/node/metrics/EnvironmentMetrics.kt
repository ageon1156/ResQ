

package org.meshtastic.feature.node.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.util.UnitConversions.celsiusToFahrenheit
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.current
import org.meshtastic.core.strings.gas_resistance
import org.meshtastic.core.strings.humidity
import org.meshtastic.core.strings.iaq
import org.meshtastic.core.strings.iaq_definition
import org.meshtastic.core.strings.lux
import org.meshtastic.core.strings.radiation
import org.meshtastic.core.strings.soil_moisture
import org.meshtastic.core.strings.soil_temperature
import org.meshtastic.core.strings.temperature
import org.meshtastic.core.strings.uv_lux
import org.meshtastic.core.strings.voltage
import org.meshtastic.core.ui.component.IaqDisplayMode
import org.meshtastic.core.ui.component.IndoorAirQuality
import org.meshtastic.core.ui.component.OptionLabel
import org.meshtastic.core.ui.component.SlidingSelector
import org.meshtastic.feature.node.metrics.CommonCharts.DATE_TIME_FORMAT
import org.meshtastic.feature.node.metrics.CommonCharts.MS_PER_SEC
import org.meshtastic.feature.node.model.TimeFrame
import org.meshtastic.proto.TelemetryProtos
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

@Composable
private fun TemperatureDisplay(envMetrics: TelemetryProtos.EnvironmentMetrics, fahrenheit: Boolean) {
    envMetrics.temperature?.takeIf { !it.isNaN() }?.let { temp ->
        MetricText((if (fahrenheit) "%s %.1f°F" else "%s %.1f°C").format(stringResource(Res.string.temperature), temp))
    }
}

@Composable
private fun HumidityAndBarometricPressureDisplay(envMetrics: TelemetryProtos.EnvironmentMetrics) {
    val humidity = envMetrics.relativeHumidity?.takeIf { !it.isNaN() }
    val pressure = envMetrics.barometricPressure?.takeIf { !it.isNaN() && it > 0 }
    if (humidity != null || pressure != null) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            humidity?.let { MetricText("%s %.2f%%".format(stringResource(Res.string.humidity), it)) }
            pressure?.let { MetricText("%.2f hPa".format(it)) }
        }
    }
}

@Composable
private fun SoilMetricsDisplay(envMetrics: TelemetryProtos.EnvironmentMetrics, fahrenheit: Boolean) {
    val moisture = envMetrics.soilMoisture?.takeIf { it != Int.MIN_VALUE }
    val soilTemp = envMetrics.soilTemperature?.takeIf { !it.isNaN() }
    if (moisture != null || soilTemp != null) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            moisture?.let { MetricText("%s %d%%".format(stringResource(Res.string.soil_moisture), it)) }
            soilTemp?.let { MetricText((if (fahrenheit) "%s %.1f°F" else "%s %.1f°C").format(stringResource(Res.string.soil_temperature), it)) }
        }
    }
}

@Composable
private fun LuxUVLuxDisplay(envMetrics: TelemetryProtos.EnvironmentMetrics) {
    val lux = envMetrics.lux?.takeIf { !it.isNaN() }
    val uvLux = envMetrics.uvLux?.takeIf { !it.isNaN() }
    if (lux != null || uvLux != null) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            lux?.let { MetricText("%s %.0f lx".format(stringResource(Res.string.lux), it)) }
            uvLux?.let { MetricText("%s %.0f UVlx".format(stringResource(Res.string.uv_lux), it)) }
        }
    }
}

@Composable
private fun VoltageCurrentDisplay(envMetrics: TelemetryProtos.EnvironmentMetrics) {
    val voltage = envMetrics.voltage?.takeIf { !it.isNaN() }
    val current = envMetrics.current?.takeIf { !it.isNaN() }
    if (voltage != null || current != null) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            voltage?.let { MetricText("%s %.2f V".format(stringResource(Res.string.voltage), it)) }
            current?.let { MetricText("%s %.2f mA".format(stringResource(Res.string.current), it)) }
        }
    }
}

@Composable
private fun GasCompositionDisplay(envMetrics: TelemetryProtos.EnvironmentMetrics) {
    val iaqValue = envMetrics.iaq?.takeIf { it != Int.MIN_VALUE }
    val gasResistance = envMetrics.gasResistance?.takeIf { it.isFinite() }
    if (iaqValue != null || gasResistance != null) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            iaqValue?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetricText(stringResource(Res.string.iaq))
                    Spacer(modifier = Modifier.width(4.dp))
                    IndoorAirQuality(iaq = it, displayMode = IaqDisplayMode.Dot)
                }
            }
            gasResistance?.let { MetricText("%s %.2f Ohm".format(stringResource(Res.string.gas_resistance), it)) }
        }
    }
}

@Composable
private fun RadiationDisplay(envMetrics: TelemetryProtos.EnvironmentMetrics) {
    envMetrics.radiation?.takeIf { !it.isNaN() && it > 0f }?.let {
        MetricText("%s %.2f µR/h".format(stringResource(Res.string.radiation), it))
    }
}

@Composable
private fun EnvironmentMetricsCard(telemetry: Telemetry, environmentDisplayFahrenheit: Boolean) {
    val envMetrics = telemetry.environmentMetrics
    val time = telemetry.time * MS_PER_SEC
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Surface { SelectionContainer { EnvironmentMetricsContent(telemetry, environmentDisplayFahrenheit) } }
    }
}

@Composable
private fun EnvironmentMetricsContent(telemetry: Telemetry, environmentDisplayFahrenheit: Boolean) {
    val envMetrics = telemetry.environmentMetrics
    val time = telemetry.time * MS_PER_SEC
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp)) {
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = DATE_TIME_FORMAT.format(time),
                style = TextStyle(fontWeight = FontWeight.Bold),
                fontSize = MaterialTheme.typography.labelLarge.fontSize,
            )
            TemperatureDisplay(envMetrics, environmentDisplayFahrenheit)
        }

        HumidityAndBarometricPressureDisplay(envMetrics)

        SoilMetricsDisplay(envMetrics, environmentDisplayFahrenheit)

        GasCompositionDisplay(envMetrics)

        LuxUVLuxDisplay(envMetrics)

        VoltageCurrentDisplay(envMetrics)
        RadiationDisplay(envMetrics)
    }
}

@Suppress("MagicNumber") 
@Preview(showBackground = true)
@Composable
private fun PreviewEnvironmentMetricsContent() {
    
    val fakeEnvMetrics =
        TelemetryProtos.EnvironmentMetrics.newBuilder()
            .setTemperature(22.5f)
            .setRelativeHumidity(55.0f)
            .setBarometricPressure(1013.25f)
            .setSoilMoisture(33)
            .setSoilTemperature(18.0f)
            .setLux(100.0f)
            .setUvLux(100.0f)
            .setVoltage(3.7f)
            .setCurrent(0.12f)
            .setIaq(100)
            .setRadiation(0.15f)
            .setGasResistance(1200.0f)
            .build()
    val fakeTelemetry =
        TelemetryProtos.Telemetry.newBuilder()
            .setTime((System.currentTimeMillis() / 1000).toInt())
            .setEnvironmentMetrics(fakeEnvMetrics)
            .build()
    MaterialTheme {
        Surface { EnvironmentMetricsContent(telemetry = fakeTelemetry, environmentDisplayFahrenheit = false) }
    }
}

