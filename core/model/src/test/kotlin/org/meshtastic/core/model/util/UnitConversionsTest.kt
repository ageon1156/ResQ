
package org.meshtastic.core.model.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.meshtastic.core.model.util.UnitConversions.toTempString

class UnitConversionsTest {

    private val tempTestCases =
        listOf(
            
            Triple(-0.1f, false, "0°C"),
            Triple(-0.2f, false, "0°C"),
            Triple(-0.4f, false, "0°C"),
            Triple(-0.49f, false, "0°C"),
            
            Triple(-0.5f, false, "-1°C"),
            Triple(-0.9f, false, "-1°C"),
            Triple(-1.0f, false, "-1°C"),
            
            Triple(0.0f, false, "0°C"),
            Triple(0.1f, false, "0°C"),
            Triple(0.4f, false, "0°C"),
            
            Triple(1.0f, false, "1°C"),
            Triple(20.0f, false, "20°C"),
            Triple(25.4f, false, "25°C"),
            Triple(25.5f, false, "26°C"),
            
            Triple(-5.0f, false, "-5°C"),
            Triple(-10.0f, false, "-10°C"),
            Triple(-20.4f, false, "-20°C"),
            
            Triple(0.0f, true, "32°F"),
            Triple(20.0f, true, "68°F"),
            Triple(25.0f, true, "77°F"),
            Triple(100.0f, true, "212°F"),
            Triple(-40.0f, true, "-40°F"), 
            
            Triple(-0.1f, true, "32°F"),
            Triple(-17.78f, true, "0°F"),
        )

    @Test
    fun `toTempString formats all temperatures correctly`() {
        tempTestCases.forEach { (celsius, isFahrenheit, expected) ->
            assertEquals(
                "Failed for $celsius°C (Fahrenheit=$isFahrenheit)",
                expected,
                celsius.toTempString(isFahrenheit),
            )
        }
    }

    @Test
    fun `toTempString handles extreme temperatures`() {
        assertEquals("100°C", 100.0f.toTempString(false))
        assertEquals("-40°C", (-40.0f).toTempString(false))
        assertEquals("-40°F", (-40.0f).toTempString(true))
    }

    @Test
    fun `celsiusToFahrenheit converts correctly`() {
        mapOf(
            0.0f to 32.0f,
            20.0f to 68.0f,
            100.0f to 212.0f,
            -40.0f to -40.0f,
        ).forEach { (celsius, expectedFahrenheit) ->
            assertEquals(expectedFahrenheit, UnitConversions.celsiusToFahrenheit(celsius), 0.01f)
        }
    }

    @Test
    fun `calculateDewPoint returns expected values`() {
        
        assertEquals(20.0f, UnitConversions.calculateDewPoint(20.0f, 100.0f), 0.1f)

        assertEquals(12.0f, UnitConversions.calculateDewPoint(20.0f, 60.0f), 0.5f)

        val highHumidity = UnitConversions.calculateDewPoint(25.0f, 80.0f)
        val lowHumidity = UnitConversions.calculateDewPoint(25.0f, 40.0f)
        assertTrue("Dew point should be higher at higher humidity", highHumidity > lowHumidity)
    }

    @Test
    fun `calculateDewPoint handles edge cases`() {
        
        val zeroHumidity = UnitConversions.calculateDewPoint(20.0f, 0.0f)
        assertTrue("Expected NaN for 0% humidity", zeroHumidity.isNaN())
    }
}

