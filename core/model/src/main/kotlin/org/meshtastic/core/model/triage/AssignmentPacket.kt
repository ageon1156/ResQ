package org.meshtastic.core.model.triage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.meshtastic.core.model.DataPacket

private val assignmentJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class AssignmentPacket(
    val type: String = "ASSIGNMENT",
    @SerialName("rescuer_id") val rescuerId: String,
    @SerialName("pin_id")     val pinId: String,
    val timestamp: Long,
    val locked: Boolean = false,
)

fun AssignmentPacket.toDataPacket(toNodeId: String, channel: Int = 0): DataPacket = DataPacket(
    to       = toNodeId,
    bytes    = assignmentJson.encodeToString(this).encodeToByteArray(),
    dataType = TRIAGE_APP_PORT,
    channel  = channel,
    hopLimit = 3,
    wantAck  = false,
    priority = 100,
)
