
package org.meshtastic.core.model.util

import kotlin.math.ln
import kotlin.math.roundToInt

object UnitConversions {

    @Suppress("MagicNumber")
    fun celsiusToFahrenheit(celsius: Float): Float = (celsius * 1.8F) + 32

    fun Float.toTempString(isFahrenheit: Boolean): String {
        val temp = if (isFahrenheit) celsiusToFahrenheit(this) else this
        val unit = if (isFahrenheit) "F" else "C"

        val absoluteTemp: Float = kotlin.math.abs(temp)
        val roundedAbsoluteTemp: Int = absoluteTemp.roundToInt()

        val isZero = roundedAbsoluteTemp == 0
        val isPositive = kotlin.math.sign(temp) > 0
        val sign: String = if (isPositive || isZero) "" else "-"

        return "$sign$roundedAbsoluteTemp°$unit"
    }

    @Suppress("MagicNumber")
    fun calculateDewPoint(tempCelsius: Float, humidity: Float): Float {
        val (a, b) = 17.27f to 237.7f
        val alpha = (a * tempCelsius) / (b + tempCelsius) + ln(humidity / 100f)
        return (b * alpha) / (a - alpha)
    }
}

