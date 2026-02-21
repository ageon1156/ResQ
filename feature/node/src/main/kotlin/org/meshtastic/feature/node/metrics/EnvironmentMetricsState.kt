

package org.meshtastic.feature.node.metrics

import androidx.compose.ui.graphics.Color
import org.meshtastic.core.model.util.UnitConversions
import org.meshtastic.core.ui.theme.GraphColors.Green
import org.meshtastic.core.ui.theme.GraphColors.InfantryBlue
import org.meshtastic.core.ui.theme.GraphColors.LightGreen
import org.meshtastic.core.ui.theme.GraphColors.Magenta
import org.meshtastic.core.ui.theme.GraphColors.Orange
import org.meshtastic.core.ui.theme.GraphColors.Pink
import org.meshtastic.core.ui.theme.GraphColors.Purple
import org.meshtastic.core.ui.theme.GraphColors.Red
import org.meshtastic.core.ui.theme.GraphColors.Yellow
import org.meshtastic.feature.node.model.TimeFrame
import org.meshtastic.proto.TelemetryProtos

@Suppress("MagicNumber")
enum class Environment(val color: Color) {
    TEMPERATURE(Red) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) = telemetry.environmentMetrics.temperature
    },
    HUMIDITY(InfantryBlue) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) = telemetry.environmentMetrics.relativeHumidity
    },
    SOIL_TEMPERATURE(Pink) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) = telemetry.environmentMetrics.soilTemperature
    },
    SOIL_MOISTURE(Purple) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) =
            telemetry.environmentMetrics.soilMoisture?.toFloat()
    },
    BAROMETRIC_PRESSURE(Green) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) = telemetry.environmentMetrics.barometricPressure
    },
    GAS_RESISTANCE(Yellow) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) = telemetry.environmentMetrics.gasResistance
    },
    IAQ(Magenta) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) = telemetry.environmentMetrics.iaq?.toFloat()
    },
    LUX(LightGreen) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) = telemetry.environmentMetrics.lux
    },
    UV_LUX(Orange) {
        override fun getValue(telemetry: TelemetryProtos.Telemetry) = telemetry.environmentMetrics.uvLux
    }, ;

    abstract fun getValue(telemetry: TelemetryProtos.Telemetry): Float?
}

data class EnvironmentGraphingData(
    val metrics: List<TelemetryProtos.Telemetry>,
    val shouldPlot: List<Boolean>,
    val leftMinMax: Pair<Float, Float> = Pair(0f, 0f),
    val rightMinMax: Pair<Float, Float> = Pair(0f, 0f),
    val times: Pair<Int, Int> = Pair(0, 0),
)

data class EnvironmentMetricsState(val environmentMetrics: List<TelemetryProtos.Telemetry> = emptyList()) {
    fun hasEnvironmentMetrics() = environmentMetrics.isNotEmpty()

    @Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
    fun environmentMetricsFiltered(timeFrame: TimeFrame, useFahrenheit: Boolean = false): EnvironmentGraphingData {
        val oldestTime = timeFrame.calculateOldestTime()
        val telemetries = environmentMetrics.filter { it.time >= oldestTime }
        val shouldPlot = BooleanArray(Environment.entries.size) { false }
        if (telemetries.isEmpty()) {
            return EnvironmentGraphingData(metrics = telemetries, shouldPlot = shouldPlot.toList())
        }

        val minValues = mutableListOf<Float>()
        val maxValues = mutableListOf<Float>()

        val temperatures = telemetries.mapNotNull { it.environmentMetrics.temperature?.takeIf { !it.isNaN() } }
        if (temperatures.isNotEmpty()) {
            var minTempValue = temperatures.minOf { it }
            var maxTempValue = temperatures.maxOf { it }
            if (useFahrenheit) {
                minTempValue = UnitConversions.celsiusToFahrenheit(minTempValue)
                maxTempValue = UnitConversions.celsiusToFahrenheit(maxTempValue)
            }
            minValues.add(minTempValue)
            maxValues.add(maxTempValue)
            shouldPlot[Environment.TEMPERATURE.ordinal] = true
        }

        val humidities =
            telemetries.mapNotNull { it.environmentMetrics.relativeHumidity?.takeIf { !it.isNaN() && it != 0.0f } }
        if (humidities.isNotEmpty()) {
            minValues.add(humidities.minOf { it })
            maxValues.add(humidities.maxOf { it })
            shouldPlot[Environment.HUMIDITY.ordinal] = true
        }

        val soilTemperatures = telemetries.mapNotNull { it.environmentMetrics.soilTemperature?.takeIf { !it.isNaN() } }
        if (soilTemperatures.isNotEmpty()) {
            var minSoilTemperatureValue = soilTemperatures.minOf { it }
            var maxSoilTemperatureValue = soilTemperatures.maxOf { it }
            if (useFahrenheit) {
                minSoilTemperatureValue = UnitConversions.celsiusToFahrenheit(minSoilTemperatureValue)
                maxSoilTemperatureValue = UnitConversions.celsiusToFahrenheit(maxSoilTemperatureValue)
            }
            minValues.add(minSoilTemperatureValue)
            maxValues.add(maxSoilTemperatureValue)
            shouldPlot[Environment.SOIL_TEMPERATURE.ordinal] = true
        }

        val soilMoistures =
            telemetries.mapNotNull { it.environmentMetrics.soilMoisture?.takeIf { it != Int.MIN_VALUE } }
        if (soilMoistures.isNotEmpty()) {
            minValues.add(soilMoistures.minOf { it.toFloat() })
            maxValues.add(soilMoistures.maxOf { it.toFloat() })
            shouldPlot[Environment.SOIL_MOISTURE.ordinal] = true
        }

        val iaqs = telemetries.mapNotNull { it.environmentMetrics.iaq?.takeIf { it != Int.MIN_VALUE } }
        if (iaqs.isNotEmpty()) {
            minValues.add(iaqs.minOf { it.toFloat() })
            maxValues.add(iaqs.maxOf { it.toFloat() })
            shouldPlot[Environment.IAQ.ordinal] = true
        }

        val pressures = telemetries.mapNotNull { it.environmentMetrics.barometricPressure?.takeIf { !it.isNaN() } }
        var minPressureValue = 0f
        var maxPressureValue = 0f
        if (pressures.isNotEmpty()) {
            minPressureValue = pressures.minOf { it }
            maxPressureValue = pressures.maxOf { it }
            shouldPlot[Environment.BAROMETRIC_PRESSURE.ordinal] = true
        }

        val luxValues = telemetries.mapNotNull { it.environmentMetrics.lux?.takeIf { !it.isNaN() } }
        if (luxValues.isNotEmpty()) {
            minValues.add(luxValues.minOf { it })
            maxValues.add(luxValues.maxOf { it })
            shouldPlot[Environment.LUX.ordinal] = true
        }

        val uvLuxValues = telemetries.mapNotNull { it.environmentMetrics.uvLux?.takeIf { !it.isNaN() } }
        if (uvLuxValues.isNotEmpty()) {
            minValues.add(uvLuxValues.minOf { it })
            maxValues.add(uvLuxValues.maxOf { it })
            shouldPlot[Environment.UV_LUX.ordinal] = true
        }

        val min = if (minValues.isEmpty()) 0f else minValues.minOf { it }
        val max = if (maxValues.isEmpty()) 1f else maxValues.maxOf { it }

        val (oldest, newest) = Pair(telemetries.minBy { it.time }, telemetries.maxBy { it.time })

        return EnvironmentGraphingData(
            metrics = telemetries,
            shouldPlot = shouldPlot.toList(),
            leftMinMax = Pair(minPressureValue, maxPressureValue),
            rightMinMax = Pair(min, max),
            times = Pair(oldest.time, newest.time),
        )
    }
}

