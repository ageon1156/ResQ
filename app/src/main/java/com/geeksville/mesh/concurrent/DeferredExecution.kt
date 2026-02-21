

package com.geeksville.mesh.concurrent

import co.touchlab.kermit.Logger

class DeferredExecution {
    private val queue = mutableListOf<() -> Unit>()

    fun add(fn: () -> Unit) {
        queue.add(fn)
    }

    fun run() {
        Logger.d { "Running deferred execution numjobs=${queue.size}" }
        queue.forEach { it() }
        queue.clear()
    }
}

