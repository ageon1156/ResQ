

package org.meshtastic.core.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
data class NetworkDeviceHardware(
    @SerialName("activelySupported") val activelySupported: Boolean = false,
    @SerialName("architecture") val architecture: String = "",
    @SerialName("displayName") val displayName: String = "",
    @SerialName("hasInkHud") val hasInkHud: Boolean? = null,
    @SerialName("hasMui") val hasMui: Boolean? = null,
    @SerialName("hwModel") val hwModel: Int = 0,
    @SerialName("hwModelSlug") val hwModelSlug: String = "",
    @SerialName("images") val images: List<String>? = null,
    @SerialName("key") val key: String? = null,
    @SerialName("partitionScheme") val partitionScheme: String? = null,
    @SerialName("platformioTarget") val platformioTarget: String = "",
    @SerialName("requiresDfu") val requiresDfu: Boolean? = null,
    @SerialName("supportLevel") val supportLevel: Int? = null,
    @SerialName("tags") val tags: List<String>? = null,
    @SerialName("variant") val variant: String? = null,
)

