
package org.meshtastic.core.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.meshtastic.core.service.testing.FakeIMeshService

class IMeshServiceContractTest {

    @Test
    fun `verify fake implementation matches aidl contract`() {
        val service: IMeshService = FakeIMeshService()

        assertEquals("fake_id", service.myId)
        assertEquals(1234, service.packetId)
        assertEquals("CONNECTED", service.connectionState())
        assertNotNull(service.nodes)
    }
}

