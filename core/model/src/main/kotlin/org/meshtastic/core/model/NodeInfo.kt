

package org.meshtastic.core.model

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.meshtastic.core.model.util.anonymize
import org.meshtastic.core.model.util.bearing
import org.meshtastic.core.model.util.latLongToMeter
import org.meshtastic.core.model.util.onlineTimeThreshold
import org.meshtastic.proto.ConfigProtos
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.TelemetryProtos

@Parcelize
data class MeshUser(
    val id: String,
    val longName: String,
    val shortName: String,
    val hwModel: MeshProtos.HardwareModel,
    val isLicensed: Boolean = false,
    val role: Int = 0,
) : Parcelable {

    override fun toString(): String = "MeshUser(id=${id.anonymize}, " +
        "longName=${longName.anonymize}, " +
        "shortName=${shortName.anonymize}, " +
        "hwModel=$hwModelString, " +
        "isLicensed=$isLicensed, " +
        "role=$role)"

    constructor(p: MeshProtos.User) : this(p.id, p.longName, p.shortName, p.hwModel, p.isLicensed, p.roleValue)

    val hwModelString: String?
        get() =
            if (hwModel == MeshProtos.HardwareModel.UNSET) {
                null
            } else {
                hwModel.name.replace('_', '-').replace('p', '.').lowercase()
            }
}

@Parcelize
data class Position(
    val latitude: Double,
    val longitude: Double,
    val altitude: Int,
    val time: Int = currentTime(), 
    val satellitesInView: Int = 0,
    val groundSpeed: Int = 0,
    val groundTrack: Int = 0, 
    val precisionBits: Int = 0,
) : Parcelable {

    @Suppress("MagicNumber")
    companion object {
        
        fun degD(i: Int) = i * 1e-7

        fun degI(d: Double) = (d * 1e7).toInt()

        fun currentTime() = (System.currentTimeMillis() / 1000).toInt()
    }

    constructor(
        position: MeshProtos.Position,
        defaultTime: Int = currentTime(),
    ) : this(
        
        degD(position.latitudeI),
        degD(position.longitudeI),
        position.altitude,
        if (position.time != 0) position.time else defaultTime,
        position.satsInView,
        position.groundSpeed,
        position.groundTrack,
        position.precisionBits,
    )

    fun distance(o: Position) = latLongToMeter(latitude, longitude, o.latitude, o.longitude)

    fun bearing(o: Position) = bearing(latitude, longitude, o.latitude, o.longitude)

    @Suppress("MagicNumber")
    fun isValid(): Boolean = latitude != 0.0 &&
        longitude != 0.0 &&
        (latitude >= -90 && latitude <= 90.0) &&
        (longitude >= -180 && longitude <= 180)

    override fun toString(): String =
        "Position(lat=${latitude.anonymize}, lon=${longitude.anonymize}, alt=${altitude.anonymize}, time=$time)"
}

@Parcelize
data class DeviceMetrics(
    val time: Int = currentTime(), 
    val batteryLevel: Int = 0,
    val voltage: Float,
    val channelUtilization: Float,
    val airUtilTx: Float,
    val uptimeSeconds: Int,
) : Parcelable {
    companion object {
        @Suppress("MagicNumber")
        fun currentTime() = (System.currentTimeMillis() / 1000).toInt()
    }

    constructor(
        p: TelemetryProtos.DeviceMetrics,
        telemetryTime: Int = currentTime(),
    ) : this(telemetryTime, p.batteryLevel, p.voltage, p.channelUtilization, p.airUtilTx, p.uptimeSeconds)
}

@Parcelize
data class EnvironmentMetrics(
    val time: Int = currentTime(), 
    val temperature: Float?,
    val relativeHumidity: Float?,
    val soilTemperature: Float?,
    val soilMoisture: Int?,
    val barometricPressure: Float?,
    val gasResistance: Float?,
    val voltage: Float?,
    val current: Float?,
    val iaq: Int?,
    val lux: Float? = null,
    val uvLux: Float? = null,
) : Parcelable {
    @Suppress("MagicNumber")
    companion object {
        fun currentTime() = (System.currentTimeMillis() / 1000).toInt()

        fun fromTelemetryProto(proto: TelemetryProtos.EnvironmentMetrics, time: Int): EnvironmentMetrics =
            EnvironmentMetrics(
                temperature = proto.temperature.takeIf { proto.hasTemperature() && !it.isNaN() },
                relativeHumidity =
                proto.relativeHumidity.takeIf { proto.hasRelativeHumidity() && !it.isNaN() && it != 0.0f },
                soilTemperature = proto.soilTemperature.takeIf { proto.hasSoilTemperature() && !it.isNaN() },
                soilMoisture = proto.soilMoisture.takeIf { proto.hasSoilMoisture() && it != Int.MIN_VALUE },
                barometricPressure = proto.barometricPressure.takeIf { proto.hasBarometricPressure() && !it.isNaN() },
                gasResistance = proto.gasResistance.takeIf { proto.hasGasResistance() && !it.isNaN() },
                voltage = proto.voltage.takeIf { proto.hasVoltage() && !it.isNaN() },
                current = proto.current.takeIf { proto.hasCurrent() && !it.isNaN() },
                iaq = proto.iaq.takeIf { proto.hasIaq() && it != Int.MIN_VALUE },
                lux = proto.lux.takeIf { proto.hasLux() && !it.isNaN() },
                uvLux = proto.uvLux.takeIf { proto.hasUvLux() && !it.isNaN() },
                time = time,
            )
    }
}

@Parcelize
data class NodeInfo(
    val num: Int, 
    var user: MeshUser? = null,
    var position: Position? = null,
    var snr: Float = Float.MAX_VALUE,
    var rssi: Int = Int.MAX_VALUE,
    var lastHeard: Int = 0, 
    var deviceMetrics: DeviceMetrics? = null,
    var channel: Int = 0,
    var environmentMetrics: EnvironmentMetrics? = null,
    var hopsAway: Int = 0,
) : Parcelable {

    @Suppress("MagicNumber")
    val colors: Pair<Int, Int>
        get() { 
            val r = (num and 0xFF0000) shr 16
            val g = (num and 0x00FF00) shr 8
            val b = num and 0x0000FF
            val brightness = ((r * 0.299) + (g * 0.587) + (b * 0.114)) / 255
            return (if (brightness > 0.5) Color.BLACK else Color.WHITE) to Color.rgb(r, g, b)
        }

    val batteryLevel
        get() = deviceMetrics?.batteryLevel

    val voltage
        get() = deviceMetrics?.voltage

    @Suppress("ImplicitDefaultLocale")
    val batteryStr
        get() = if (batteryLevel in 1..100) String.format("%d%%", batteryLevel) else ""

    val isOnline: Boolean
        get() {
            return lastHeard > onlineTimeThreshold()
        }

    val validPosition: Position?
        get() {
            return position?.takeIf { it.isValid() }
        }

    fun distance(o: NodeInfo?): Int? {
        val p = validPosition
        val op = o?.validPosition
        return if (p != null && op != null) p.distance(op).toInt() else null
    }

    fun bearing(o: NodeInfo?): Int? {
        val p = validPosition
        val op = o?.validPosition
        return if (p != null && op != null) p.bearing(op).toInt() else null
    }

    @Suppress("MagicNumber")
    fun distanceStr(o: NodeInfo?, prefUnits: Int = 0) = distance(o)?.let { dist ->
        when {
            dist == 0 -> null 
            prefUnits == ConfigProtos.Config.DisplayConfig.DisplayUnits.METRIC_VALUE && dist < 1000 ->
                "%.0f m".format(dist.toDouble())
            prefUnits == ConfigProtos.Config.DisplayConfig.DisplayUnits.METRIC_VALUE && dist >= 1000 ->
                "%.1f km".format(dist / 1000.0)
            prefUnits == ConfigProtos.Config.DisplayConfig.DisplayUnits.IMPERIAL_VALUE && dist < 1609 ->
                "%.0f ft".format(dist.toDouble() * 3.281)
            prefUnits == ConfigProtos.Config.DisplayConfig.DisplayUnits.IMPERIAL_VALUE && dist >= 1609 ->
                "%.1f mi".format(dist / 1609.34)
            else -> null
        }
    }
}

