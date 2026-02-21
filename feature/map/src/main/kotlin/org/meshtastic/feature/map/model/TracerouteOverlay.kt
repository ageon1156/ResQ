

package org.meshtastic.feature.map.model

data class TracerouteOverlay(
    val requestId: Int,
    val forwardRoute: List<Int> = emptyList(),
    val returnRoute: List<Int> = emptyList(),
) {
    val relatedNodeNums: Set<Int> = (forwardRoute + returnRoute).toSet()

    val hasRoutes: Boolean
        get() = forwardRoute.isNotEmpty() || returnRoute.isNotEmpty()
}

