
package org.meshtastic.core.database.model

import android.graphics.Color
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.isNotEmpty
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.core.model.Capabilities
import org.meshtastic.core.model.util.GPSFormat
import org.meshtastic.core.model.util.UnitConversions.celsiusToFahrenheit
import org.meshtastic.core.model.util.latLongToMeter
import org.meshtastic.core.model.util.toDistanceString
import org.meshtastic.proto.ConfigProtos
import org.meshtastic.proto.ConfigProtos.Config.DisplayConfig
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.PaxcountProtos
import org.meshtastic.proto.TelemetryProtos.DeviceMetrics
import org.meshtastic.proto.TelemetryProtos.EnvironmentMetrics
import org.meshtastic.proto.TelemetryProtos.PowerMetrics

@Suppress("MagicNumber")
data class Node(
    val num: Int,
    val metadata: MeshProtos.DeviceMetadata? = null,
    val user: MeshProtos.User = MeshProtos.User.getDefaultInstance(),
    val position: MeshProtos.Position = MeshProtos.Position.getDefaultInstance(),
    val snr: Float = Float.MAX_VALUE,
    val rssi: Int = Int.MAX_VALUE,
    val lastHeard: Int = 0, 
    val deviceMetrics: DeviceMetrics = DeviceMetrics.getDefaultInstance(),
    val channel: Int = 0,
    val viaMqtt: Boolean = false,
    val hopsAway: Int = -1,
    val isFavorite: Boolean = false,
    val isIgnored: Boolean = false,
    val isMuted: Boolean = false,
    val environmentMetrics: EnvironmentMetrics = EnvironmentMetrics.getDefaultInstance(),
    val powerMetrics: PowerMetrics = PowerMetrics.getDefaultInstance(),
    val paxcounter: PaxcountProtos.Paxcount = PaxcountProtos.Paxcount.getDefaultInstance(),
    val publicKey: ByteString? = null,
    val notes: String = "",
    val manuallyVerified: Boolean = false,
) {
    val capabilities: Capabilities by lazy { Capabilities(metadata?.firmwareVersion) }

    val colors: Pair<Int, Int>
        get() { 
            val r = (num and 0xFF0000) shr 16
            val g = (num and 0x00FF00) shr 8
            val b = num and 0x0000FF
            val brightness = ((r * 0.299) + (g * 0.587) + (b * 0.114)) / 255
            return (if (brightness > 0.5) Color.BLACK else Color.WHITE) to Color.rgb(r, g, b)
        }

    val isUnknownUser
        get() = user.hwModel == MeshProtos.HardwareModel.UNSET

    val hasPKC
        get() = (publicKey ?: user.publicKey).isNotEmpty()

    val mismatchKey
        get() = (publicKey ?: user.publicKey) == NodeEntity.ERROR_BYTE_STRING

    val hasEnvironmentMetrics: Boolean
        get() = environmentMetrics != EnvironmentMetrics.getDefaultInstance()

    val hasPowerMetrics: Boolean
        get() = powerMetrics != PowerMetrics.getDefaultInstance()

    val batteryLevel
        get() = deviceMetrics.batteryLevel

    val voltage
        get() = deviceMetrics.voltage

    val batteryStr
        get() = if (batteryLevel in 1..100) "$batteryLevel%" else ""

    val latitude
        get() = position.latitudeI * 1e-7

    val longitude
        get() = position.longitudeI * 1e-7

    private fun hasValidPosition(): Boolean = latitude != 0.0 &&
        longitude != 0.0 &&
        (latitude >= -90 && latitude <= 90.0) &&
        (longitude >= -180 && longitude <= 180)

    val validPosition: MeshProtos.Position?
        get() = position.takeIf { hasValidPosition() }

    fun distance(o: Node): Int? = when {
        validPosition == null || o.validPosition == null -> null
        else -> latLongToMeter(latitude, longitude, o.latitude, o.longitude).toInt()
    }

    fun distanceStr(o: Node, displayUnits: DisplayConfig.DisplayUnits): String? =
        distance(o)?.toDistanceString(displayUnits)

    fun bearing(o: Node?): Int? = when {
        validPosition == null || o?.validPosition == null -> null
        else -> org.meshtastic.core.model.util.bearing(latitude, longitude, o.latitude, o.longitude).toInt()
    }

    fun gpsString(): String = GPSFormat.toDec(latitude, longitude)

    private fun EnvironmentMetrics.getDisplayStrings(isFahrenheit: Boolean): List<String> {
        val temp =
            if (temperature != 0f) {
                if (isFahrenheit) {
                    "%.1f°F".format(celsiusToFahrenheit(temperature))
                } else {
                    "%.1f°C".format(temperature)
                }
            } else {
                null
            }
        val humidity = if (relativeHumidity != 0f) "%.0f%%".format(relativeHumidity) else null
        val soilTemperatureStr =
            if (soilTemperature != 0f) {
                if (isFahrenheit) {
                    "%.1f°F".format(celsiusToFahrenheit(soilTemperature))
                } else {
                    "%.1f°C".format(soilTemperature)
                }
            } else {
                null
            }
        val soilMoistureRange = 0..100
        val soilMoisture =
            if (soilMoisture in soilMoistureRange && soilTemperature != 0f) {
                "%d%%".format(soilMoisture)
            } else {
                null
            }
        val voltage = if (this.voltage != 0f) "%.2fV".format(this.voltage) else null
        val current = if (current != 0f) "%.1fmA".format(current) else null
        val iaq = if (iaq != 0) "IAQ: $iaq" else null

        return listOfNotNull(
            paxcounter.getDisplayString(),
            temp,
            humidity,
            soilTemperatureStr,
            soilMoisture,
            voltage,
            current,
            iaq,
        )
    }

    private fun PaxcountProtos.Paxcount.getDisplayString() =
        "PAX: ${ble + wifi} (B:$ble/W:$wifi)".takeIf { ble != 0 || wifi != 0 }

    fun getTelemetryStrings(isFahrenheit: Boolean = false): List<String> =
        environmentMetrics.getDisplayStrings(isFahrenheit)
}

fun ConfigProtos.Config.DeviceConfig.Role?.isUnmessageableRole(): Boolean = this in
    listOf(
        ConfigProtos.Config.DeviceConfig.Role.REPEATER,
        ConfigProtos.Config.DeviceConfig.Role.ROUTER,
        ConfigProtos.Config.DeviceConfig.Role.ROUTER_LATE,
        ConfigProtos.Config.DeviceConfig.Role.SENSOR,
        ConfigProtos.Config.DeviceConfig.Role.TRACKER,
        ConfigProtos.Config.DeviceConfig.Role.TAK_TRACKER,
    )

