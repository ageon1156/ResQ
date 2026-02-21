
package org.meshtastic.core.database

import androidx.room.TypeConverter
import co.touchlab.kermit.Logger
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.serialization.json.Json
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.PaxcountProtos
import org.meshtastic.proto.TelemetryProtos

@Suppress("TooManyFunctions")
class Converters {
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter fun dataFromString(value: String): DataPacket = json.decodeFromString(DataPacket.serializer(), value)

    @TypeConverter fun dataToString(value: DataPacket): String = json.encodeToString(DataPacket.serializer(), value)

    @TypeConverter
    fun bytesToFromRadio(bytes: ByteArray): MeshProtos.FromRadio = try {
        MeshProtos.FromRadio.parseFrom(bytes)
    } catch (ex: InvalidProtocolBufferException) {
        Logger.e(ex) { "bytesToFromRadio TypeConverter error" }
        MeshProtos.FromRadio.getDefaultInstance()
    }

    @TypeConverter fun fromRadioToBytes(value: MeshProtos.FromRadio): ByteArray? = value.toByteArray()

    @TypeConverter
    fun bytesToUser(bytes: ByteArray): MeshProtos.User = try {
        MeshProtos.User.parseFrom(bytes)
    } catch (ex: InvalidProtocolBufferException) {
        Logger.e(ex) { "bytesToUser TypeConverter error" }
        MeshProtos.User.getDefaultInstance()
    }

    @TypeConverter fun userToBytes(value: MeshProtos.User): ByteArray? = value.toByteArray()

    @TypeConverter
    fun bytesToPosition(bytes: ByteArray): MeshProtos.Position = try {
        MeshProtos.Position.parseFrom(bytes)
    } catch (ex: InvalidProtocolBufferException) {
        Logger.e(ex) { "bytesToPosition TypeConverter error" }
        MeshProtos.Position.getDefaultInstance()
    }

    @TypeConverter fun positionToBytes(value: MeshProtos.Position): ByteArray? = value.toByteArray()

    @TypeConverter
    fun bytesToTelemetry(bytes: ByteArray): TelemetryProtos.Telemetry = try {
        TelemetryProtos.Telemetry.parseFrom(bytes)
    } catch (ex: InvalidProtocolBufferException) {
        Logger.e(ex) { "bytesToTelemetry TypeConverter error" }
        TelemetryProtos.Telemetry.newBuilder().build() 
    }

    @TypeConverter fun telemetryToBytes(value: TelemetryProtos.Telemetry): ByteArray? = value.toByteArray()

    @TypeConverter
    fun bytesToPaxcounter(bytes: ByteArray): PaxcountProtos.Paxcount = try {
        PaxcountProtos.Paxcount.parseFrom(bytes)
    } catch (ex: InvalidProtocolBufferException) {
        Logger.e(ex) { "bytesToPaxcounter TypeConverter error" }
        PaxcountProtos.Paxcount.getDefaultInstance()
    }

    @TypeConverter fun paxCounterToBytes(value: PaxcountProtos.Paxcount): ByteArray? = value.toByteArray()

    @TypeConverter
    fun bytesToMetadata(bytes: ByteArray): MeshProtos.DeviceMetadata = try {
        MeshProtos.DeviceMetadata.parseFrom(bytes)
    } catch (ex: InvalidProtocolBufferException) {
        Logger.e(ex) { "bytesToMetadata TypeConverter error" }
        MeshProtos.DeviceMetadata.getDefaultInstance()
    }

    @TypeConverter fun metadataToBytes(value: MeshProtos.DeviceMetadata): ByteArray? = value.toByteArray()

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) {
            return null
        }
        return Json.decodeFromString<List<String>>(value)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        if (list == null) {
            return null
        }
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun bytesToByteString(bytes: ByteArray?): ByteString? = if (bytes == null) null else ByteString.copyFrom(bytes)

    @TypeConverter fun byteStringToBytes(value: ByteString?): ByteArray? = value?.toByteArray()

    @TypeConverter fun messageStatusToInt(value: MessageStatus?): Int = value?.ordinal ?: MessageStatus.UNKNOWN.ordinal

    @TypeConverter
    fun intToMessageStatus(value: Int): MessageStatus = MessageStatus.entries.getOrElse(value) { MessageStatus.UNKNOWN }
}

