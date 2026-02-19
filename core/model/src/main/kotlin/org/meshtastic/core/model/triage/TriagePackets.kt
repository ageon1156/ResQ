package org.meshtastic.core.model.triage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.meshtastic.core.model.DataPacket

/**
 * Port number in the meshtastic private-use range (256–511).
 * Both MANUAL_TRIAGE_PIN and CLAIM_PIN share this port;
 * the payload JSON "type" field disambiguates them.
 */
const val TRIAGE_APP_PORT = 256

private val triageJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// ── Wire packets ─────────────────────────────────────────────────────────────

@Serializable
data class ManualTriagePinPacket(
    val type: String = "MANUAL_TRIAGE_PIN",
    @SerialName("pin_id")       val pinId: String,
    val lat: Double,
    val lon: Double,
    @SerialName("triage_level") val triageLevel: String,   // TriageLevel.name
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

// ── Encoding ─────────────────────────────────────────────────────────────────

/**
 * Wraps a [ManualTriagePinPacket] as a [DataPacket] ready to send via the mesh.
 *   hopLimit = 2   (rescuers are close; 1–2 hops is sufficient)
 *   priority = 100 (HIGH)
 */
fun ManualTriagePinPacket.toDataPacket(channel: Int = 0): DataPacket = DataPacket(
    to       = DataPacket.ID_BROADCAST,
    bytes    = triageJson.encodeToString(this).encodeToByteArray(),
    dataType = TRIAGE_APP_PORT,
    channel  = channel,
    hopLimit = 2,
    wantAck  = false,
    priority = 100,
)

/**
 * Wraps a [ClaimPinPacket] as a [DataPacket] ready to send via the mesh.
 *   hopLimit = 1   (claim is local coordination only)
 *   priority = 100 (HIGH)
 */
fun ClaimPinPacket.toDataPacket(channel: Int = 0): DataPacket = DataPacket(
    to       = DataPacket.ID_BROADCAST,
    bytes    = triageJson.encodeToString(this).encodeToByteArray(),
    dataType = TRIAGE_APP_PORT,
    channel  = channel,
    hopLimit = 1,
    wantAck  = false,
    priority = 100,
)

// ── Decoding ─────────────────────────────────────────────────────────────────

@Serializable
private data class TriagePacketTypeProbe(val type: String)

/**
 * Decodes a raw TRIAGE_APP_PORT payload into either a [ManualTriagePinPacket]
 * or [ClaimPinPacket]. Returns null on unknown type or malformed JSON.
 *
 * Usage in your mesh packet receive handler:
 * ```
 * if (dataPacket.dataType == TRIAGE_APP_PORT) {
 *     dataPacket.bytes?.decodeTriagePacket()?.let { triageMapViewModel.handleIncoming(it) }
 * }
 * ```
 */
fun ByteArray.decodeTriagePacket(): Any? = runCatching {
    val text  = decodeToString()
    val probe = triageJson.decodeFromString<TriagePacketTypeProbe>(text)
    when (probe.type) {
        "MANUAL_TRIAGE_PIN" -> triageJson.decodeFromString<ManualTriagePinPacket>(text)
        "CLAIM_PIN"         -> triageJson.decodeFromString<ClaimPinPacket>(text)
        else                -> null
    }
}.getOrNull()
