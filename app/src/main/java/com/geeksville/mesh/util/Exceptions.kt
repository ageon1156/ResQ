

package com.geeksville.mesh.util

import android.os.RemoteException

object Exceptions {
    
    var reporter: ((Throwable, String?, String?) -> Unit)? = null

    fun report(exception: Throwable, tag: String? = null, message: String? = null) {
        reporter?.let { r -> r(exception, tag, message) }
    }
}

fun exceptionReporter(inner: () -> Unit) {
    try {
        inner()
    } catch (ex: Throwable) {
        
        Exceptions.report(ex, "exceptionReporter", "Uncaught Exception")
    }
}

fun ignoreException(silent: Boolean = false, inner: () -> Unit) {
    try {
        inner()
    } catch (ex: Throwable) {
    }
}

fun <T> toRemoteExceptions(inner: () -> T): T = try {
    inner()
} catch (ex: Throwable) {
    when (ex) {
        is RemoteException -> throw ex
        else -> throw RemoteException(ex.message)
    }
}

