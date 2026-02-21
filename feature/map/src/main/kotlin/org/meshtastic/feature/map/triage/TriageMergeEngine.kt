package org.meshtastic.feature.map.triage

import org.meshtastic.core.model.triage.TriagePin
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val MERGE_DISTANCE_METERS = 5.0

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun mergeTriagePin(
    incoming: TriagePin,
    existing: List<TriagePin>,
): List<TriagePin> {
    
    val nearbyPins = existing.filter { candidate ->
        candidate.pinId != incoming.pinId &&
            distanceMeters(incoming.lat, incoming.lon, candidate.lat, candidate.lon) <= MERGE_DISTANCE_METERS
    }

    if (nearbyPins.isEmpty()) return existing + incoming

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

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return EARTH_RADIUS_METERS * 2.0 * asin(sqrt(a))
}
