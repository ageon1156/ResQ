

package com.geeksville.mesh.concurrent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalCoroutinesApi
class SequentialJobTest {

    private val sequentialJob = SequentialJob()

    @Test
    fun `launch cancels previous job`() = runTest {
        var job1Active = false
        var job1Cancelled = false

        sequentialJob.launch(this) {
            try {
                job1Active = true
                delay(1000)
            } finally {
                job1Cancelled = true
            }
        }

        advanceTimeBy(100)
        assertTrue("Job 1 should be active", job1Active)

        sequentialJob.launch(this) {
            
        }

        advanceTimeBy(100)
        assertTrue("Job 1 should be cancelled", job1Cancelled)
    }

    @Test
    fun `cancel stops the job`() = runTest {
        var jobActive = false
        var jobCancelled = false

        sequentialJob.launch(this) {
            try {
                jobActive = true
                delay(1000)
            } finally {
                jobCancelled = true
            }
        }

        advanceTimeBy(100)
        assertTrue("Job should be active", jobActive)

        sequentialJob.cancel()

        advanceTimeBy(100)
        assertTrue("Job should be cancelled", jobCancelled)
    }
}

