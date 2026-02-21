
package org.meshtastic.core.service

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServiceRepositoryRetryTest {

    private lateinit var serviceRepository: ServiceRepository

    @Before
    fun setUp() {
        serviceRepository = ServiceRepository()
    }

    @Test
    fun `requestRetry returns true when user confirms`() = runTest {
        val testEvent =
            RetryEvent.MessageRetry(packetId = 123, text = "Test message", attemptNumber = 1, maxAttempts = 3)

        val retryDeferred = async { serviceRepository.requestRetry(testEvent, timeoutMs = 5000) }

        val emittedEvent = serviceRepository.retryEvents.first { it != null }
        assertEquals(testEvent, emittedEvent)

        serviceRepository.respondToRetry(testEvent.packetId, shouldRetry = true)

        val result = retryDeferred.await()
        assertTrue("Expected retry to proceed", result)
    }

    @Test
    fun `requestRetry returns false when user cancels`() = runTest {
        val testEvent = RetryEvent.ReactionRetry(packetId = 456, emoji = "👍", attemptNumber = 2, maxAttempts = 3)

        val retryDeferred = async { serviceRepository.requestRetry(testEvent, timeoutMs = 5000) }

        val emittedEvent = serviceRepository.retryEvents.first { it != null }
        assertEquals(testEvent, emittedEvent)

        serviceRepository.respondToRetry(testEvent.packetId, shouldRetry = false)

        val result = retryDeferred.await()
        assertFalse("Expected retry to be cancelled", result)
    }

    @Test
    fun `requestRetry returns true on timeout when user does not respond`() = runTest {
        val testEvent =
            RetryEvent.MessageRetry(packetId = 789, text = "Timeout test", attemptNumber = 1, maxAttempts = 3)

        val result = serviceRepository.requestRetry(testEvent, timeoutMs = 100)

        assertTrue("Expected auto-retry on timeout", result)
    }

    @Test
    fun `multiple simultaneous retry requests handled independently`() = runTest {
        val event1 = RetryEvent.MessageRetry(packetId = 100, text = "Message 1", attemptNumber = 1, maxAttempts = 3)
        val event2 = RetryEvent.MessageRetry(packetId = 200, text = "Message 2", attemptNumber = 1, maxAttempts = 3)

        val retry1 = async { serviceRepository.requestRetry(event1, timeoutMs = 5000) }
        val retry2 = async { serviceRepository.requestRetry(event2, timeoutMs = 5000) }

        delay(50)

        serviceRepository.respondToRetry(event1.packetId, shouldRetry = true)
        serviceRepository.respondToRetry(event2.packetId, shouldRetry = false)

        val result1 = retry1.await()
        val result2 = retry2.await()

        assertTrue("First retry should proceed", result1)
        assertFalse("Second retry should be cancelled", result2)
    }

    @Test
    fun `cancelPendingRetries completes all pending requests with false`() = runTest {
        val event1 = RetryEvent.MessageRetry(packetId = 111, text = "Message 1", attemptNumber = 1, maxAttempts = 3)
        val event2 = RetryEvent.MessageRetry(packetId = 222, text = "Message 2", attemptNumber = 1, maxAttempts = 3)

        val retry1 = async { serviceRepository.requestRetry(event1, timeoutMs = 10000) }
        val retry2 = async { serviceRepository.requestRetry(event2, timeoutMs = 10000) }

        delay(50)

        serviceRepository.cancelPendingRetries()

        val result1 = retry1.await()
        val result2 = retry2.await()

        assertFalse("First retry should be cancelled", result1)
        assertFalse("Second retry should be cancelled", result2)
    }

    @Test
    fun `retryEvents are cleared after user responds`() = runTest {
        val testEvent = RetryEvent.MessageRetry(packetId = 333, text = "Clear test", attemptNumber = 1, maxAttempts = 3)

        val retryDeferred = async { serviceRepository.requestRetry(testEvent, timeoutMs = 5000) }

        val emittedEvent = serviceRepository.retryEvents.first { it != null }
        assertEquals("Should receive event", testEvent, emittedEvent)

        serviceRepository.respondToRetry(testEvent.packetId, shouldRetry = true)

        retryDeferred.await()

        assertEquals("Event should be cleared after responding", null, serviceRepository.retryEvents.value)
    }

    @Test
    fun `respondToRetry does nothing for unknown packetId`() = runTest {
        
        serviceRepository.respondToRetry(999, shouldRetry = true)
        
    }
}

