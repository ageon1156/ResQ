
package org.meshtastic.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesTest {

    private fun caps(version: String?) = Capabilities(version, forceEnableAll = false)

    @Test
    fun `canMuteNode requires v2 8 0`() {
        assertFalse(caps("2.7.15").canMuteNode)
        assertFalse(caps("2.7.99").canMuteNode)
        assertTrue(caps("2.8.0").canMuteNode)
        assertTrue(caps("2.8.1").canMuteNode)
    }

    @Test
    fun `canRequestNeighborInfo requires v2 7 15`() {
        assertFalse(caps("2.7.14").canRequestNeighborInfo)
        assertTrue(caps("2.7.15").canRequestNeighborInfo)
        assertTrue(caps("2.8.0").canRequestNeighborInfo)
    }

    @Test
    fun `canSendVerifiedContacts requires v2 7 12`() {
        assertFalse(caps("2.7.11").canSendVerifiedContacts)
        assertTrue(caps("2.7.12").canSendVerifiedContacts)
        assertTrue(caps("2.7.15").canSendVerifiedContacts)
    }

    @Test
    fun `canToggleTelemetryEnabled requires v2 7 12`() {
        assertFalse(caps("2.7.11").canToggleTelemetryEnabled)
        assertTrue(caps("2.7.12").canToggleTelemetryEnabled)
    }

    @Test
    fun `canToggleUnmessageable requires v2 6 9`() {
        assertFalse(caps("2.6.8").canToggleUnmessageable)
        assertTrue(caps("2.6.9").canToggleUnmessageable)
    }

    @Test
    fun `supportsQrCodeSharing requires v2 6 8`() {
        assertFalse(caps("2.6.7").supportsQrCodeSharing)
        assertTrue(caps("2.6.8").supportsQrCodeSharing)
    }

    @Test
    fun `null firmware returns all false`() {
        val c = caps(null)
        assertFalse(c.canMuteNode)
        assertFalse(c.canRequestNeighborInfo)
        assertFalse(c.canSendVerifiedContacts)
        assertFalse(c.canToggleTelemetryEnabled)
        assertFalse(c.canToggleUnmessageable)
        assertFalse(c.supportsQrCodeSharing)
    }

    @Test
    fun `invalid firmware returns all false`() {
        val c = caps("invalid")
        assertFalse(c.canMuteNode)
        assertFalse(c.canRequestNeighborInfo)
        assertFalse(c.canSendVerifiedContacts)
        assertFalse(c.canToggleTelemetryEnabled)
        assertFalse(c.canToggleUnmessageable)
        assertFalse(c.supportsQrCodeSharing)
    }

    @Test
    fun `forceEnableAll returns true for everything regardless of version`() {
        val c = Capabilities(firmwareVersion = null, forceEnableAll = true)
        assertTrue(c.canMuteNode)
        assertTrue(c.canRequestNeighborInfo)
        assertTrue(c.canSendVerifiedContacts)
        assertTrue(c.canToggleTelemetryEnabled)
        assertTrue(c.canToggleUnmessageable)
        assertTrue(c.supportsQrCodeSharing)
    }

    @Test
    fun `forceEnableAll returns true even for invalid versions`() {
        val c = Capabilities(firmwareVersion = "invalid", forceEnableAll = true)
        assertTrue(c.canMuteNode)
        assertTrue(c.canRequestNeighborInfo)
        assertTrue(c.canSendVerifiedContacts)
        assertTrue(c.canToggleTelemetryEnabled)
        assertTrue(c.canToggleUnmessageable)
        assertTrue(c.supportsQrCodeSharing)
    }
}

