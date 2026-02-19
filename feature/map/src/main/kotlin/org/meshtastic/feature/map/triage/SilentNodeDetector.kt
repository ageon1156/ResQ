package org.meshtastic.feature.map.triage

import org.meshtastic.core.database.model.Node

// ── Confidence weights ────────────────────────────────────────────────────────
private const val CONFIDENCE_HEARTBEAT_TIMEOUT   = 40
private const val CONFIDENCE_NEIGHBOR_AGREEMENT  = 25
private const val CONFIDENCE_NO_MOTION           = 20
private const val CONFIDENCE_BATTERY_NOT_CRITICAL = 15

// ── Thresholds ────────────────────────────────────────────────────────────────
/** Seconds without a heartbeat before considering a node silent. */
private const val HEARTBEAT_TIMEOUT_SECS = 900L   // 15 min

/** Radius within which online neighbours are expected to have relayed packets. */
private const val NEIGHBOR_RADIUS_METERS = 500

/** Neighbour is considered "online" if heard within this many seconds. */
private const val NEIGHBOR_ONLINE_SECS = 600L     // 10 min

/** Minimum neighbours that must agree to add the NEIGHBOR_AGREEMENT weight. */
private const val MIN_NEIGHBOR_AGREEMENT = 2

/**
 * Evaluates [candidate] for silent-node / potential-casualty status.
 *
 * Returns null when:
 *   - Node has not yet timed out (too early to flag)
 *   - Battery is in the 1–4 % range (device likely powered off, not a casualty)
 *   - Node has no valid GPS position (cannot be placed on the triage map)
 *
 * Hook [motionDetected] into your IMU/motion sensor feed before shipping.
 */
@Suppress("MagicNumber")
fun detectSilentNode(
    candidate: Node,
    allNodes: List<Node>,
    nowSecs: Long = System.currentTimeMillis() / 1000,
    motionDetected: Boolean = false,          // wire to sensor feed
): SilentNodeRecord? {
    val lastHeard = candidate.lastHeard.toLong()
    val battery   = candidate.batteryLevel

    // Must have a prior heartbeat that has now timed out
    val heartbeatTimedOut = lastHeard > 0 && (nowSecs - lastHeard) >= HEARTBEAT_TIMEOUT_SECS
    if (!heartbeatTimedOut) return null

    // Battery 1–4 % → device likely off, suppress
    if (battery in 1..4) return null

    // No GPS → cannot place on map
    if (candidate.validPosition == null) return null

    val neighborAgreementCount = countOnlineNeighborsWithNoHeartbeat(candidate, allNodes, nowSecs)

    var confidence = 0
    if (heartbeatTimedOut)                       confidence += CONFIDENCE_HEARTBEAT_TIMEOUT
    if (neighborAgreementCount >= MIN_NEIGHBOR_AGREEMENT) confidence += CONFIDENCE_NEIGHBOR_AGREEMENT
    if (!motionDetected)                         confidence += CONFIDENCE_NO_MOTION
    if (battery !in 5..15)                       confidence += CONFIDENCE_BATTERY_NOT_CRITICAL

    return SilentNodeRecord(
        node                  = candidate,
        confidence            = confidence.coerceIn(0, 100),
        detectedAt            = nowSecs * 1000L,
        heartbeatTimedOut     = heartbeatTimedOut,
        neighborAgreementCount = neighborAgreementCount,
        motionDetected        = motionDetected,
        batteryLevel          = battery,
    )
}

/**
 * Counts online neighbours within [NEIGHBOR_RADIUS_METERS] of [candidate].
 * An online neighbour that has not recently received a packet from [candidate]
 * is treated as indirect evidence of silence.
 */
private fun countOnlineNeighborsWithNoHeartbeat(
    candidate: Node,
    allNodes: List<Node>,
    nowSecs: Long,
): Int = allNodes.count { neighbor ->
    neighbor.num != candidate.num &&
        neighbor.validPosition != null &&
        (nowSecs - neighbor.lastHeard.toLong()) < NEIGHBOR_ONLINE_SECS &&
        (neighbor.distance(candidate) ?: Int.MAX_VALUE) < NEIGHBOR_RADIUS_METERS
}
