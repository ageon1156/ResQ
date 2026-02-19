package org.meshtastic.core.model.triage

import java.util.UUID

/**
 * A triage incident marker placed by a rescuer on the triage map.
 *
 * Custom map pins are a completely separate concept and are never merged here.
 */
data class TriagePin(
    val pinId: String = UUID.randomUUID().toString(),
    val lat: Double,
    val lon: Double,
    val triageLevel: TriageLevel,
    val victimCount: Int = 1,
    val createdBy: String,            // nodeID string, e.g. "!deadbeef"
    val timestamp: Long = System.currentTimeMillis(),
    val claimedBy: String? = null,    // rescuer nodeID; null = unclaimed
    val claimTimestamp: Long? = null,
    val isSilentNodeConversion: Boolean = false,
    val sourceNodeNum: Int? = null,   // set when converted from a silent node
) {
    /** Label shown on the claim button or status chip. */
    val claimLabel: String
        get() = claimedBy
            ?.let { "Handled by ${it.takeLast(2).uppercase()}" }
            ?: "Claim"

    /** Full display string (level + victim count). */
    val summaryLabel: String
        get() = "${triageLevel.symbol} ${triageLevel.displayLabel}" +
            if (victimCount > 1) " ×$victimCount" else ""
}
