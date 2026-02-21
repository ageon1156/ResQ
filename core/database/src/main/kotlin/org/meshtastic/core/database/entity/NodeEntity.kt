
package org.meshtastic.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.isNotEmpty
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.DeviceMetrics
import org.meshtastic.core.model.EnvironmentMetrics
import org.meshtastic.core.model.MeshUser
import org.meshtastic.core.model.NodeInfo
import org.meshtastic.core.model.Position
import org.meshtastic.core.model.util.onlineTimeThreshold
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.PaxcountProtos
import org.meshtastic.proto.TelemetryProtos
import org.meshtastic.proto.copy

data class NodeWithRelations(
    @Embedded val node: NodeEntity,
    @Relation(entity = MetadataEntity::class, parentColumn = "num", entityColumn = "num")
    val metadata: MetadataEntity? = null,
) {
    fun toModel() = with(node) {
        Node(
            num = num,
            metadata = metadata?.proto,
            user = user,
            position = position,
            snr = snr,
            rssi = rssi,
            lastHeard = lastHeard,
            deviceMetrics = deviceTelemetry.deviceMetrics,
            channel = channel,
            viaMqtt = viaMqtt,
            hopsAway = hopsAway,
            isFavorite = isFavorite,
            isIgnored = isIgnored,
            isMuted = isMuted,
            environmentMetrics = environmentTelemetry.environmentMetrics,
            powerMetrics = powerTelemetry.powerMetrics,
            paxcounter = paxcounter,
            notes = notes,
            manuallyVerified = manuallyVerified,
        )
    }

    fun toEntity() = with(node) {
        NodeEntity(
            num = num,
            user = user,
            position = position,
            snr = snr,
            rssi = rssi,
            lastHeard = lastHeard,
            deviceTelemetry = deviceTelemetry,
            channel = channel,
            viaMqtt = viaMqtt,
            hopsAway = hopsAway,
            isFavorite = isFavorite,
            isIgnored = isIgnored,
            isMuted = isMuted,
            environmentTelemetry = environmentTelemetry,
            powerTelemetry = powerTelemetry,
            paxcounter = paxcounter,
            notes = notes,
            manuallyVerified = manuallyVerified,
        )
    }
}

@Entity(tableName = "metadata", indices = [Index(value = ["num"])])
data class MetadataEntity(
    @PrimaryKey val num: Int,
    @ColumnInfo(name = "proto", typeAffinity = ColumnInfo.BLOB) val proto: MeshProtos.DeviceMetadata,
    val timestamp: Long = System.currentTimeMillis(),
)

@Suppress("MagicNumber")
@Entity(
    tableName = "nodes",
    indices =
    [
        Index(value = ["last_heard"]),
        Index(value = ["short_name"]),
        Index(value = ["long_name"]),
        Index(value = ["hops_away"]),
        Index(value = ["is_favorite"]),
        Index(value = ["last_heard", "is_favorite"]),
    ],
)
data class NodeEntity(
    @PrimaryKey(autoGenerate = false) val num: Int, 
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) var user: MeshProtos.User = MeshProtos.User.getDefaultInstance(),
    @ColumnInfo(name = "long_name") var longName: String? = null,
    @ColumnInfo(name = "short_name") var shortName: String? = null, 
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var position: MeshProtos.Position = MeshProtos.Position.getDefaultInstance(),
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var snr: Float = Float.MAX_VALUE,
    var rssi: Int = Int.MAX_VALUE,
    @ColumnInfo(name = "last_heard") var lastHeard: Int = 0, 
    @ColumnInfo(name = "device_metrics", typeAffinity = ColumnInfo.BLOB)
    var deviceTelemetry: TelemetryProtos.Telemetry = TelemetryProtos.Telemetry.getDefaultInstance(),
    var channel: Int = 0,
    @ColumnInfo(name = "via_mqtt") var viaMqtt: Boolean = false,
    @ColumnInfo(name = "hops_away") var hopsAway: Int = -1,
    @ColumnInfo(name = "is_favorite") var isFavorite: Boolean = false,
    @ColumnInfo(name = "is_ignored", defaultValue = "0") var isIgnored: Boolean = false,
    @ColumnInfo(name = "is_muted", defaultValue = "0") var isMuted: Boolean = false,
    @ColumnInfo(name = "environment_metrics", typeAffinity = ColumnInfo.BLOB)
    var environmentTelemetry: TelemetryProtos.Telemetry = TelemetryProtos.Telemetry.newBuilder().build(),
    @ColumnInfo(name = "power_metrics", typeAffinity = ColumnInfo.BLOB)
    var powerTelemetry: TelemetryProtos.Telemetry = TelemetryProtos.Telemetry.getDefaultInstance(),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var paxcounter: PaxcountProtos.Paxcount = PaxcountProtos.Paxcount.getDefaultInstance(),
    @ColumnInfo(name = "public_key") var publicKey: ByteString? = null,
    @ColumnInfo(name = "notes", defaultValue = "") var notes: String = "",
    @ColumnInfo(name = "manually_verified", defaultValue = "0")
    var manuallyVerified: Boolean = false, 
) {
    val deviceMetrics: TelemetryProtos.DeviceMetrics
        get() = deviceTelemetry.deviceMetrics

    val environmentMetrics: TelemetryProtos.EnvironmentMetrics
        get() = environmentTelemetry.environmentMetrics

    val isUnknownUser
        get() = user.hwModel == MeshProtos.HardwareModel.UNSET

    val hasPKC
        get() = (publicKey ?: user.publicKey).isNotEmpty()

    fun setPosition(p: MeshProtos.Position, defaultTime: Int = currentTime()) {
        position = p.copy { time = if (p.time != 0) p.time else defaultTime }
        latitude = degD(p.latitudeI)
        longitude = degD(p.longitudeI)
    }

    val isOnline: Boolean
        get() {
            return lastHeard > onlineTimeThreshold()
        }

    companion object {
        
        fun degD(i: Int) = i * 1e-7

        fun degI(d: Double) = (d * 1e7).toInt()

        val ERROR_BYTE_STRING: ByteString = ByteString.copyFrom(ByteArray(32) { 0 })

        fun currentTime() = (System.currentTimeMillis() / 1000).toInt()
    }

    fun toModel() = Node(
        num = num,
        user = user,
        position = position,
        snr = snr,
        rssi = rssi,
        lastHeard = lastHeard,
        deviceMetrics = deviceTelemetry.deviceMetrics,
        channel = channel,
        viaMqtt = viaMqtt,
        hopsAway = hopsAway,
        isFavorite = isFavorite,
        isIgnored = isIgnored,
        isMuted = isMuted,
        environmentMetrics = environmentTelemetry.environmentMetrics,
        powerMetrics = powerTelemetry.powerMetrics,
        paxcounter = paxcounter,
        publicKey = publicKey ?: user.publicKey,
        notes = notes,
    )

    fun toNodeInfo() = NodeInfo(
        num = num,
        user =
        MeshUser(
            id = user.id,
            longName = user.longName,
            shortName = user.shortName,
            hwModel = user.hwModel,
            role = user.roleValue,
        )
            .takeIf { user.id.isNotEmpty() },
        position =
        Position(
            latitude = latitude,
            longitude = longitude,
            altitude = position.altitude,
            time = position.time,
            satellitesInView = position.satsInView,
            groundSpeed = position.groundSpeed,
            groundTrack = position.groundTrack,
            precisionBits = position.precisionBits,
        )
            .takeIf { it.isValid() },
        snr = snr,
        rssi = rssi,
        lastHeard = lastHeard,
        deviceMetrics =
        DeviceMetrics(
            time = deviceTelemetry.time,
            batteryLevel = deviceMetrics.batteryLevel,
            voltage = deviceMetrics.voltage,
            channelUtilization = deviceMetrics.channelUtilization,
            airUtilTx = deviceMetrics.airUtilTx,
            uptimeSeconds = deviceMetrics.uptimeSeconds,
        ),
        channel = channel,
        environmentMetrics =
        EnvironmentMetrics.fromTelemetryProto(
            environmentTelemetry.environmentMetrics,
            environmentTelemetry.time,
        ),
        hopsAway = hopsAway,
    )
}

