
package com.geeksville.mesh.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IInterface
import com.geeksville.mesh.util.exceptionReporter
import kotlinx.coroutines.delay
import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BindFailedException : Exception("bindService failed")

open class ServiceClient<T : IInterface>(private val stubFactory: (IBinder) -> T) : Closeable {

    var serviceP: T? = null

    val service: T
        get() {
            waitConnect() 
            return serviceP ?: throw Exception("Service not bound")
        }

    private var context: Context? = null

    private var isClosed = true

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    fun waitConnect() {
        
        lock.withLock {
            if (context == null) {
                throw Exception("Haven't called connect")
            }

            if (serviceP == null) {
                condition.await()
            }
        }
    }

    suspend fun connect(c: Context, intent: Intent, flags: Int) {
        context = c
        if (isClosed) {
            isClosed = false
            if (!c.bindService(intent, connection, flags)) {

                delay(500)
                if (!c.bindService(intent, connection, flags)) {
                    throw BindFailedException()
                }
            }
        }
    }

    override fun close() {
        isClosed = true
        try {
            context?.unbindService(connection)
        } catch (_: IllegalArgumentException) {
        }
        serviceP = null
        context = null
    }

    open fun onConnected(service: T) {}

    open fun onDisconnected() {}

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) = exceptionReporter {
                if (!isClosed) {
                    val s = stubFactory(binder)
                    serviceP = s
                    onConnected(s)

                    lock.withLock { condition.signalAll() }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) = exceptionReporter {
                serviceP = null
                onDisconnected()
            }
        }
}

