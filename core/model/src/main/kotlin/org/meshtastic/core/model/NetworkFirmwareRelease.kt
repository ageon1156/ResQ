

package org.meshtastic.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkFirmwareRelease(
    @SerialName("id") val id: String = "",
    @SerialName("page_url") val pageUrl: String = "",
    @SerialName("release_notes") val releaseNotes: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("zip_url") val zipUrl: String = "",
)

@Serializable
data class Releases(
    @SerialName("alpha") val alpha: List<NetworkFirmwareRelease> = listOf(),
    @SerialName("stable") val stable: List<NetworkFirmwareRelease> = listOf(),
)

@Serializable
data class NetworkFirmwareReleases(
    @SerialName("pullRequests") val pullRequests: List<NetworkFirmwareRelease> = listOf(),
    @SerialName("releases") val releases: Releases = Releases(),
)

