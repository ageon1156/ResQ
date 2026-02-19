package org.meshtastic.feature.map.triage

import org.meshtastic.core.database.model.Node

/**
 * Result of silent-node detection for a single node.
 *
 * [confidence] is 0–100, composed of:
 *   +40  heartbeat timed out
 *   +25  ≥2 online neighbours have not heard this node
 *   +20  no motion detected
 *   +15  battery not in the critical low range (5–15 %)
 *
 * The record is suppressed (and should not be displayed) when battery is 1–4 %
 * because the device is likely powered off, not a casualty.
 */
data class SilentNodeRecord(
    val node: Node,
    val confidence: Int,                   // 0–100
    val detectedAt: Long,                  // epoch ms
    val heartbeatTimedOut: Boolean,
    val neighborAgreementCount: Int,
    val motionDetected: Boolean,
    val batteryLevel: Int,                 // 0 = unknown
) {
    /** Human-readable confidence label shown on the triage map marker. */
    val displayLabel: String
        get() = "Possible casualty — $confidence%"

    /**
     * True when the device is likely powered off (battery 1–4 %).
     * Suppressed records must not be shown as potential casualties.
     * Battery = 0 means unknown — do NOT suppress in that case.
     */
    val isSuppressed: Boolean
        get() = batteryLevel in 1..4

    /**
     * Returns a human-readable direction + distance string relative to [fromNode],
     * computed from GPS positions. Returns null if either position is unavailable.
     */
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
