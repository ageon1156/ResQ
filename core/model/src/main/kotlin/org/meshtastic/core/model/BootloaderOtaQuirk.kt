
package org.meshtastic.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BootloaderOtaQuirk(
    
    @SerialName("hwModel") val hwModel: Int,
    
    @SerialName("hwModelSlug") val hwModelSlug: String? = null,
    
    @SerialName("requiresBootloaderUpgradeForOta") val requiresBootloaderUpgradeForOta: Boolean = false,
    
    @SerialName("infoUrl") val infoUrl: String? = null,
)

