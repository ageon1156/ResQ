package org.meshtastic.feature.map.triage

import org.meshtastic.core.model.triage.TriagePin
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Pins within this distance (metres) are candidates for merging. */
private const val MERGE_DISTANCE_METERS = 5.0

private const val EARTH_RADIUS_METERS = 6_371_000.0

// ── Public API ────────────────────────────────────────────────────────────────

/**
 * Merges [incoming] into [existing] according to the triage merge rules:
 *
 *   1. If a pin exists within [MERGE_DISTANCE_METERS] **at the same triage level**
 *      → accumulate victim counts into the older pin; drop [incoming].
 *
 *   2. If pins exist within [MERGE_DISTANCE_METERS] **at different levels**
 *      → keep all; upgrade any pin to the higher priority level; never downgrade.
 *
 *   3. No conflict → append [incoming] unchanged.
 *
 * Returns the updated list ready to be fully persisted (caller diffs against the
 * previous snapshot to find what actually changed).
 */
fun mergeTriagePin(
    incoming: TriagePin,
    existing: List<TriagePin>,
): List<TriagePin> {
    // Pins near incoming (excluding a round-trip match on the same ID)
    val nearbyPins = existing.filter { candidate ->
        candidate.pinId != incoming.pinId &&
            distanceMeters(incoming.lat, incoming.lon, candidate.lat, candidate.lon) <= MERGE_DISTANCE_METERS
    }

    if (nearbyPins.isEmpty()) return existing + incoming

    // Case 1 — same level within range: accumulate victim count, drop incoming
    val sameLevelNear = nearbyPins.firstOrNull { it.triageLevel == incoming.triageLevel }
    if (sameLevelNear != null) {
        return existing.map { pin ->
            if (pin.pinId == sameLevelNear.pinId) {
                pin.copy(victimCount = pin.victimCount + incoming.victimCount)
            } else {
                pin
            }
        }
    }

    // Case 2 — different levels nearby: apply priority override on existing + append incoming
    val upgraded = existing.map { pin ->
        if (nearbyPins.any { it.pinId == pin.pinId }) {
            val merged = pin.triageLevel.mergeWith(incoming.triageLevel)
            if (merged != pin.triageLevel) pin.copy(triageLevel = merged) else pin
        } else {
            pin
        }
    }
    return upgraded + incoming
}

// ── Haversine ─────────────────────────────────────────────────────────────────

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return EARTH_RADIUS_METERS * 2.0 * asin(sqrt(a))
}
