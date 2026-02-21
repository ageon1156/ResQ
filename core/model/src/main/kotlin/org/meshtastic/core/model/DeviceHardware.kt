
package org.meshtastic.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceHardware(
    val activelySupported: Boolean = false,
    val architecture: String = "",
    val displayName: String = "",
    val hasInkHud: Boolean? = null,
    val hasMui: Boolean? = null,
    val hwModel: Int = 0,
    val hwModelSlug: String = "",
    val images: List<String>? = null,
    val partitionScheme: String? = null,
    val platformioTarget: String = "",
    val requiresDfu: Boolean? = null,
    
    val requiresBootloaderUpgradeForOta: Boolean? = null,
    
    val bootloaderInfoUrl: String? = null,
    val supportLevel: Int? = null,
    val tags: List<String>? = null,
) {
    
    val isEsp32Arc: Boolean
        get() = architecture.startsWith("esp32", ignoreCase = true)
}

