

package org.meshtastic.feature.node.metrics

import co.touchlab.kermit.Logger
import org.meshtastic.proto.MeshProtos

@Suppress("detekt:SwallowedException")
fun MeshProtos.HardwareModel.safeNumber(fallbackValue: Int = -1): Int = try {
    this.number
} catch (e: IllegalArgumentException) {
    Logger.w { "Unknown hardware model enum value: $this, using fallback value: $fallbackValue" }
    fallbackValue
}

