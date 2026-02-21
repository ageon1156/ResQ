
package org.meshtastic.core.model

data class Capabilities(val firmwareVersion: String?, internal val forceEnableAll: Boolean = BuildConfig.DEBUG) {
    private val version = firmwareVersion?.let { DeviceVersion(it) }

    private fun isSupported(minVersion: String): Boolean =
        forceEnableAll || (version != null && version >= DeviceVersion(minVersion))

    val canMuteNode: Boolean
        get() = isSupported("2.8.0")

    val canRequestNeighborInfo: Boolean
        get() = isSupported("2.7.15")

    val canSendVerifiedContacts: Boolean
        get() = isSupported("2.7.12")

    val canToggleTelemetryEnabled: Boolean
        get() = isSupported("2.7.12")

    val canToggleUnmessageable: Boolean
        get() = isSupported("2.6.9")

    val supportsQrCodeSharing: Boolean
        get() = isSupported("2.6.8")

    val supportsEsp32Ota: Boolean
        get() = isSupported("2.7.18")
}

