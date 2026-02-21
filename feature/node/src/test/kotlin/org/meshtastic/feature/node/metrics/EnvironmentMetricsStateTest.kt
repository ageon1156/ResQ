
package org.meshtastic.feature.node.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.meshtastic.feature.node.model.TimeFrame
import org.meshtastic.proto.TelemetryProtos.EnvironmentMetrics
import org.meshtastic.proto.TelemetryProtos.Telemetry

class EnvironmentMetricsStateTest {

    @Test
    fun `environmentMetricsFiltered correctly calculates times`() {
        val now = (System.currentTimeMillis() / 1000).toInt()
        val metrics =
            listOf(
                Telemetry.newBuilder()
                    .setTime(now - 100)
                    .setEnvironmentMetrics(EnvironmentMetrics.newBuilder().setTemperature(20f))
                    .build(),
                Telemetry.newBuilder()
                    .setTime(now - 50)
                    .setEnvironmentMetrics(EnvironmentMetrics.newBuilder().setTemperature(22f))
                    .build(),
                Telemetry.newBuilder()
                    .setTime(now)
                    .setEnvironmentMetrics(EnvironmentMetrics.newBuilder().setTemperature(21f))
                    .build(),
            )
        val state = EnvironmentMetricsState(metrics)
        val result = state.environmentMetricsFiltered(TimeFrame.TWENTY_FOUR_HOURS)

        assertEquals(now - 100, result.times.first)
        assertEquals(now, result.times.second)
    }

    @Test
    fun `environmentMetricsFiltered ignores invalid timestamps`() {
        val now = (System.currentTimeMillis() / 1000).toInt()
        val metrics =
            listOf(
                Telemetry.newBuilder()
                    .setTime(0)
                    .setEnvironmentMetrics(EnvironmentMetrics.newBuilder().setTemperature(20f))
                    .build(),
                Telemetry.newBuilder()
                    .setTime(now)
                    .setEnvironmentMetrics(EnvironmentMetrics.newBuilder().setTemperature(21f))
                    .build(),
            )
        val state = EnvironmentMetricsState(metrics)
        val result = state.environmentMetricsFiltered(TimeFrame.TWENTY_FOUR_HOURS)

        assertEquals(now, result.times.first)
        assertEquals(now, result.times.second)
        assertEquals(1, result.metrics.size)
    }

    @Test
    fun `environmentMetricsFiltered handles valid zero temperatures`() {
        val now = (System.currentTimeMillis() / 1000).toInt()
        val metrics =
            listOf(
                Telemetry.newBuilder()
                    .setTime(now)
                    .setEnvironmentMetrics(EnvironmentMetrics.newBuilder().setTemperature(0.0f))
                    .build(),
            )
        val state = EnvironmentMetricsState(metrics)
        val result = state.environmentMetricsFiltered(TimeFrame.TWENTY_FOUR_HOURS)

        assertTrue(result.shouldPlot[Environment.TEMPERATURE.ordinal])
        assertEquals(0.0f, result.rightMinMax.first, 0.01f)
        assertEquals(0.0f, result.rightMinMax.second, 0.01f)
    }
}

