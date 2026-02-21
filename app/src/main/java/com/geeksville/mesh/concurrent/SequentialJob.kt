

package com.geeksville.mesh.concurrent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class SequentialJob @Inject constructor() {
    private val job = AtomicReference<Job?>(null)

    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        cancel()
        val newJob = scope.handledLaunch(block = block)
        job.set(newJob)

        newJob.invokeOnCompletion { job.compareAndSet(newJob, null) }
    }

    fun cancel() {
        job.getAndSet(null)?.cancel()
    }
}

