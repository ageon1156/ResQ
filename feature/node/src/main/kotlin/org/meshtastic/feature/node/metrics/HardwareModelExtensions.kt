

package org.meshtastic.feature.node.metrics

import org.meshtastic.proto.MeshProtos

@Suppress("detekt:SwallowedException")
fun MeshProtos.HardwareModel.safeNumber(fallbackValue: Int = -1): Int = try {
    this.number
} catch (e: IllegalArgumentException) {
    fallbackValue
}

