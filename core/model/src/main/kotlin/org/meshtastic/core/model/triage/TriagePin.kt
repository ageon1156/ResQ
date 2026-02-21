package org.meshtastic.core.model.triage

import java.util.UUID

data class TriagePin(
    val pinId: String = UUID.randomUUID().toString(),
    val lat: Double,
    val lon: Double,
    val triageLevel: TriageLevel,
    val victimCount: Int = 1,
    val createdBy: String,            
    val timestamp: Long = System.currentTimeMillis(),
    val claimedBy: String? = null,    
    val claimTimestamp: Long? = null,
    val isSilentNodeConversion: Boolean = false,
    val sourceNodeNum: Int? = null,   
) {
    
    val claimLabel: String
        get() = claimedBy
            ?.let { "Handled by ${it.takeLast(2).uppercase()}" }
            ?: "Claim"

    val summaryLabel: String
        get() = "${triageLevel.symbol} ${triageLevel.displayLabel}" +
            if (victimCount > 1) " ×$victimCount" else ""
}
