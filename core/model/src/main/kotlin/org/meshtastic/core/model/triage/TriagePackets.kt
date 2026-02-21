package org.meshtastic.core.model.triage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.meshtastic.core.model.DataPacket

const val TRIAGE_APP_PORT = 256

private val triageJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class ManualTriagePinPacket(
    val type: String = "MANUAL_TRIAGE_PIN",
    @SerialName("pin_id")       val pinId: String,
    val lat: Double,
    val lon: Double,
    @SerialName("triage_level") val triageLevel: String,   
    @SerialName("victim_count") val victimCount: Int = 1,
    @SerialName("created_by")   val createdBy: String,
    val timestamp: Long,
)

@Serializable
data class ClaimPinPacket(
    val type: String = "CLAIM_PIN",
    @SerialName("pin_id")     val pinId: String,
    @SerialName("rescuer_id") val rescuerId: String,
    val timestamp: Long,
)

fun ManualTriagePinPacket.toDataPacket(channel: Int = 0): DataPacket = DataPacket(
    to       = DataPacket.ID_BROADCAST,
    bytes    = triageJson.encodeToString(this).encodeToByteArray(),
    dataType = TRIAGE_APP_PORT,
    channel  = channel,
    hopLimit = 2,
    wantAck  = false,
    priority = 100,
)

fun ClaimPinPacket.toDataPacket(channel: Int = 0): DataPacket = DataPacket(
    to       = DataPacket.ID_BROADCAST,
    bytes    = triageJson.encodeToString(this).encodeToByteArray(),
    dataType = TRIAGE_APP_PORT,
    channel  = channel,
    hopLimit = 1,
    wantAck  = false,
    priority = 100,
)

@Serializable
private data class TriagePacketTypeProbe(val type: String)

fun ByteArray.decodeTriagePacket(): Any? = runCatching {
    val text  = decodeToString()
    val probe = triageJson.decodeFromString<TriagePacketTypeProbe>(text)
    when (probe.type) {
        "MANUAL_TRIAGE_PIN" -> triageJson.decodeFromString<ManualTriagePinPacket>(text)
        "CLAIM_PIN"         -> triageJson.decodeFromString<ClaimPinPacket>(text)
        else                -> null
    }
}.getOrNull()
