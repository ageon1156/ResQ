

package org.meshtastic.core.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CustomTileProviderConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val urlTemplate: String,
)

