

package com.geeksville.mesh.concurrent

class DeferredExecution {
    private val queue = mutableListOf<() -> Unit>()

    fun add(fn: () -> Unit) {
        queue.add(fn)
    }

    fun run() {
        queue.forEach { it() }
        queue.clear()
    }
}

