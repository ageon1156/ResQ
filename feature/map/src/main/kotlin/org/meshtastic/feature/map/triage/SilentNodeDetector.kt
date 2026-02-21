package org.meshtastic.feature.map.triage

import org.meshtastic.core.database.model.Node

private const val CONFIDENCE_HEARTBEAT_TIMEOUT   = 40
private const val CONFIDENCE_NEIGHBOR_AGREEMENT  = 25
private const val CONFIDENCE_NO_MOTION           = 20
private const val CONFIDENCE_BATTERY_NOT_CRITICAL = 15

private const val HEARTBEAT_TIMEOUT_SECS = 900L   

private const val NEIGHBOR_RADIUS_METERS = 500

private const val NEIGHBOR_ONLINE_SECS = 600L     

private const val MIN_NEIGHBOR_AGREEMENT = 2

@Suppress("MagicNumber")
fun detectSilentNode(
    candidate: Node,
    allNodes: List<Node>,
    nowSecs: Long = System.currentTimeMillis() / 1000,
    motionDetected: Boolean = false,          
): SilentNodeRecord? {
    val lastHeard = candidate.lastHeard.toLong()
    val battery   = candidate.batteryLevel

    val heartbeatTimedOut = lastHeard > 0 && (nowSecs - lastHeard) >= HEARTBEAT_TIMEOUT_SECS
    if (!heartbeatTimedOut) return null

    if (battery in 1..4) return null

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
