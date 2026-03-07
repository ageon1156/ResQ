package org.meshtastic.feature.map.triage

import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.triage.TriageLevel
import org.meshtastic.core.model.triage.TriagePin
import org.meshtastic.core.model.util.latLongToMeter

data class RescuerNode(
    val nodeId: String,
    val lat: Double,
    val lon: Double,
)

data class AssignmentResult(
    val assignments: Map<String, String>,
    val unassignedRescuers: List<String>,
    val unassignedPins: List<String>,
)

object AssignmentEngine {

    private val PRIORITY_WEIGHT = mapOf(
        TriageLevel.RED    to 0.25,
        TriageLevel.YELLOW to 1.0,
        TriageLevel.GREEN  to 2.0,
        TriageLevel.BLACK  to 4.0,
    )

    private const val RESCUER_ONLINE_WINDOW_SECONDS = 7200

    fun eligibleRescuers(
        nodes: List<Node>,
        ourNodeId: String?,
        nowSeconds: Int = (System.currentTimeMillis() / 1000).toInt(),
    ): List<RescuerNode> = nodes
        .filter { node ->
            node.user.id != ourNodeId &&
                !node.isIgnored &&
                (nowSeconds - node.lastHeard) <= RESCUER_ONLINE_WINDOW_SECONDS
        }
        .map { node -> RescuerNode(nodeId = node.user.id, lat = node.latitude, lon = node.longitude) }

    fun computeAssignments(
        pins: List<TriagePin>,
        rescuers: List<RescuerNode>,
        locked: Map<String, String> = emptyMap(),
    ): AssignmentResult {
        if (pins.isEmpty() || rescuers.isEmpty()) {
            return AssignmentResult(
                assignments      = locked,
                unassignedRescuers = rescuers.map { it.nodeId }.filter { it !in locked },
                unassignedPins   = pins.map { it.pinId }.filter { it !in locked.values },
            )
        }

        val freeRescuers = rescuers.filter { it.nodeId !in locked }
        val freePins     = pins.filter { it.pinId !in locked.values }

        if (freeRescuers.isEmpty() || freePins.isEmpty()) {
            val assignedPinIds = locked.values.toSet()
            return AssignmentResult(
                assignments      = locked,
                unassignedRescuers = freeRescuers.map { it.nodeId },
                unassignedPins   = freePins.map { it.pinId },
            )
        }

        val sortedPins = freePins.sortedBy { PRIORITY_WEIGHT[it.triageLevel] ?: 1.0 }

        val effectivePins = if (sortedPins.size > freeRescuers.size) {
            sortedPins.take(freeRescuers.size)
        } else {
            sortedPins
        }

        val costMatrix = Array(freeRescuers.size) { i ->
            val hasPosition = freeRescuers[i].lat != 0.0 || freeRescuers[i].lon != 0.0
            DoubleArray(effectivePins.size) { j ->
                val weight = PRIORITY_WEIGHT[effectivePins[j].triageLevel] ?: 1.0
                if (hasPosition) {
                    val dist = latLongToMeter(
                        freeRescuers[i].lat, freeRescuers[i].lon,
                        effectivePins[j].lat, effectivePins[j].lon,
                    )
                    dist * weight
                } else {
                    weight
                }
            }
        }

        val assignment = HungarianSolver.solve(costMatrix)

        val newAssignments = mutableMapOf<String, String>()

        for (i in freeRescuers.indices) {
            val jobIdx = assignment.getOrElse(i) { -1 }
            if (jobIdx >= 0 && jobIdx < effectivePins.size) {
                newAssignments[freeRescuers[i].nodeId] = effectivePins[jobIdx].pinId
            }
        }

        val finalAssignments = locked + newAssignments

        val assignedPinIds = finalAssignments.values.toSet()
        val unassignedRescuers = freeRescuers.map { it.nodeId }.filter { it !in newAssignments }
        val unassignedPins = sortedPins.drop(effectivePins.size).map { it.pinId } +
            freePins.filter { it.pinId !in assignedPinIds }.map { it.pinId }

        return AssignmentResult(
            assignments        = finalAssignments,
            unassignedRescuers = unassignedRescuers,
            unassignedPins     = unassignedPins.distinct(),
        )
    }
}
