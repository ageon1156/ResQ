
package com.geeksville.mesh.repository.radio

import com.geeksville.mesh.concurrent.handledLaunch
import com.geeksville.mesh.repository.network.NetworkRepository
import com.geeksville.mesh.util.Exceptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.meshtastic.core.di.CoroutineDispatchers
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.SocketTimeoutException

open class TCPInterface
@AssistedInject
constructor(
    service: RadioInterfaceService,
    private val dispatchers: CoroutineDispatchers,
    @Assisted private val address: String,
) : StreamInterface(service) {

    companion object {
        const val MAX_RETRIES_ALLOWED = Int.MAX_VALUE
        const val MIN_BACKOFF_MILLIS = 1 * 1000L 
        const val MAX_BACKOFF_MILLIS = 5 * 60 * 1000L 
        const val SOCKET_TIMEOUT = 5000
        const val SOCKET_RETRIES = 18
        const val SERVICE_PORT = NetworkRepository.SERVICE_PORT
    }

    private var retryCount = 1
    private var backoffDelay = MIN_BACKOFF_MILLIS

    private var socket: Socket? = null
    private var outStream: OutputStream? = null

    init {
        connect()
    }

    override fun sendBytes(p: ByteArray) {
        val stream = outStream ?: return
        try {
            stream.write(p)
        } catch (ex: IOException) {
            onDeviceDisconnect(false)
        }
    }

    override fun flushBytes() {
        val stream = outStream ?: return
        try {
            stream.flush()
        } catch (ex: IOException) {
            onDeviceDisconnect(false)
        }
    }

    override fun onDeviceDisconnect(waitForStopped: Boolean) {
        val s = socket
        if (s != null) {
            s.close()
            socket = null
            outStream = null
        }
        super.onDeviceDisconnect(waitForStopped)
    }

    override fun connect() {
        service.serviceScope.handledLaunch {
            while (true) {
                try {
                    startConnect()
                } catch (ex: IOException) {
                    onDeviceDisconnect(false)
                } catch (ex: Throwable) {
                    Exceptions.report(ex, "Exception in TCP reader")
                    onDeviceDisconnect(false)
                }

                if (retryCount > MAX_RETRIES_ALLOWED) {
                    break
                }

                delay(backoffDelay)

                retryCount++
                backoffDelay = minOf(backoffDelay * 2, MAX_BACKOFF_MILLIS)
            }
        }
    }

    override fun keepAlive() {
        val heartbeat =
            org.meshtastic.proto.MeshProtos.ToRadio.newBuilder()
                .setHeartbeat(org.meshtastic.proto.MeshProtos.Heartbeat.getDefaultInstance())
                .build()
        handleSendToRadio(heartbeat.toByteArray())
    }

    private suspend fun startConnect() = withContext(dispatchers.io) {
        val parts = address.split(":", limit = 2)
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: SERVICE_PORT

        Socket(InetAddress.getByName(host), port).use { socket ->
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.soTimeout = SOCKET_TIMEOUT
            this@TCPInterface.socket = socket

            BufferedOutputStream(socket.getOutputStream()).use { outputStream ->
                outStream = outputStream

                BufferedInputStream(socket.getInputStream()).use { inputStream ->
                    super.connect()

                    retryCount = 1
                    backoffDelay = MIN_BACKOFF_MILLIS

                    var timeoutCount = 0
                    while (timeoutCount < SOCKET_RETRIES) {
                        try {
                            val c = inputStream.read()
                            if (c == -1) {
                                break
                            } else {
                                timeoutCount = 0
                                readChar(c.toByte())
                            }
                        } catch (ex: SocketTimeoutException) {
                            timeoutCount++
                        }
                    }
                }
            }
            onDeviceDisconnect(false)
        }
    }
}

