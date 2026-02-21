

package com.geeksville.mesh.util

import android.os.RemoteException
import android.util.Log
import co.touchlab.kermit.Logger

object Exceptions {
    
    var reporter: ((Throwable, String?, String?) -> Unit)? = null

    fun report(exception: Throwable, tag: String? = null, message: String? = null) {
        Logger.e(exception) {
            "Exceptions.report: $tag $message"
        } 
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
        
        if (!silent) Logger.e(ex) { "ignoring exception" }
    }
}

fun <T> toRemoteExceptions(inner: () -> T): T = try {
    inner()
} catch (ex: Throwable) {
    Log.e("toRemoteExceptions", "Uncaught exception, returning to remote client", ex)
    when (ex) { 
        is RemoteException -> throw ex
        else -> throw RemoteException(ex.message)
    }
}

