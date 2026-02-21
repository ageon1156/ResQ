package org.meshtastic.feature.map.triage

import org.meshtastic.core.database.model.Node

data class SilentNodeRecord(
    val node: Node,
    val confidence: Int,                   
    val detectedAt: Long,                  
    val heartbeatTimedOut: Boolean,
    val neighborAgreementCount: Int,
    val motionDetected: Boolean,
    val batteryLevel: Int,                 
) {
    
    val displayLabel: String
        get() = "Possible casualty — $confidence%"

    val isSuppressed: Boolean
        get() = batteryLevel in 1..4

    fun directionLabel(fromNode: Node): String? {
        val distanceMeters = fromNode.distance(node) ?: return null
        val bearingDeg = fromNode.bearing(node) ?: return null
        return "→ ${distanceMeters}m ${bearingToCardinal(bearingDeg)}"
    }
}

fun bearingToCardinal(bearing: Int): String {
    val n = ((bearing % 360) + 360) % 360
    return when {
        n < 23 || n >= 338 -> "North"
        n < 68             -> "North-East"
        n < 113            -> "East"
        n < 158            -> "South-East"
        n < 203            -> "South"
        n < 248            -> "South-West"
        n < 293            -> "West"
        else               -> "North-West"
    }
}
