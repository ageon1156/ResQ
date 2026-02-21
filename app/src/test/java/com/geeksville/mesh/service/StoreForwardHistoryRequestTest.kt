
package com.geeksville.mesh.service

import org.junit.Assert.assertEquals
import org.junit.Test
import org.meshtastic.proto.StoreAndForwardProtos

class StoreForwardHistoryRequestTest {

    @Test
    fun `buildStoreForwardHistoryRequest copies positive parameters`() {
        val request =
            MeshHistoryManager.buildStoreForwardHistoryRequest(
                lastRequest = 42,
                historyReturnWindow = 15,
                historyReturnMax = 25,
            )

        assertEquals(StoreAndForwardProtos.StoreAndForward.RequestResponse.CLIENT_HISTORY, request.rr)
        assertEquals(42, request.history.lastRequest)
        assertEquals(15, request.history.window)
        assertEquals(25, request.history.historyMessages)
    }

    @Test
    fun `buildStoreForwardHistoryRequest omits non-positive parameters`() {
        val request =
            MeshHistoryManager.buildStoreForwardHistoryRequest(
                lastRequest = 0,
                historyReturnWindow = -1,
                historyReturnMax = 0,
            )

        assertEquals(StoreAndForwardProtos.StoreAndForward.RequestResponse.CLIENT_HISTORY, request.rr)
        assertEquals(0, request.history.lastRequest)
        assertEquals(0, request.history.window)
        assertEquals(0, request.history.historyMessages)
    }

    @Test
    fun `resolveHistoryRequestParameters uses config values when positive`() {
        val (window, max) = MeshHistoryManager.resolveHistoryRequestParameters(window = 30, max = 10)

        assertEquals(30, window)
        assertEquals(10, max)
    }

    @Test
    fun `resolveHistoryRequestParameters falls back to defaults when non-positive`() {
        val (window, max) = MeshHistoryManager.resolveHistoryRequestParameters(window = 0, max = -5)

        assertEquals(1440, window)
        assertEquals(100, max)
    }
}

