
package com.meshtastic.android.meshserviceexample

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.meshtastic.core.service.testing.FakeIMeshService

class MeshServiceViewModelTest {

    @Test
    fun `test service connection updates status`() {
        val viewModel = MeshServiceViewModel()
        val fakeService = FakeIMeshService()

        viewModel.onServiceConnected(fakeService)

        assertTrue(viewModel.serviceConnectionStatus.value)
        assertEquals("fake_id", viewModel.myId.value)
        assertEquals("CONNECTED", viewModel.connectionState.value)
    }

    @Test
    fun `test service disconnection updates status`() {
        val viewModel = MeshServiceViewModel()
        viewModel.onServiceConnected(FakeIMeshService())

        viewModel.onServiceDisconnected()

        assertEquals(false, viewModel.serviceConnectionStatus.value)
    }
}

