

package com.geeksville.mesh.concurrent

interface Continuation<in T> {
    abstract fun resume(res: Result<T>)

    fun resumeSuccess(res: T) = resume(Result.success(res))

    fun resumeWithException(ex: Throwable) = try {
        resume(Result.failure(ex))
    } catch (ex: Throwable) {
        
        throw ex
    }
}

class CallbackContinuation<in T>(private val cb: (Result<T>) -> Unit) : Continuation<T> {
    override fun resume(res: Result<T>) = cb(res)
}

class SyncContinuation<T> : Continuation<T> {

    private val mbox = java.lang.Object()
    private var result: Result<T>? = null

    override fun resume(res: Result<T>) {
        synchronized(mbox) {
            result = res
            mbox.notify()
        }
    }

    fun await(timeoutMsecs: Long = 0): T {
        synchronized(mbox) {
            val startT = System.currentTimeMillis()
            while (result == null) {
                mbox.wait(timeoutMsecs)

                if (timeoutMsecs > 0 && ((System.currentTimeMillis() - startT) >= timeoutMsecs)) {
                    throw Exception("SyncContinuation timeout")
                }
            }

            val r = result
            if (r != null) {
                return r.getOrThrow()
            } else {
                throw Exception("This shouldn't happen")
            }
        }
    }
}

fun <T> suspend(timeoutMsecs: Long = -1, initfn: (SyncContinuation<T>) -> Unit): T {
    val cont = SyncContinuation<T>()

    initfn(cont)

    return cont.await(timeoutMsecs)
}

