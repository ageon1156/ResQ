
package com.geeksville.mesh.repository.radio

import co.touchlab.kermit.Logger
import com.geeksville.mesh.repository.usb.SerialConnection
import com.geeksville.mesh.repository.usb.SerialConnectionListener
import com.geeksville.mesh.repository.usb.UsbRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.atomic.AtomicReference

class SerialInterface
@AssistedInject
constructor(
    service: RadioInterfaceService,
    private val serialInterfaceSpec: SerialInterfaceSpec,
    private val usbRepository: UsbRepository,
    @Assisted private val address: String,
) : StreamInterface(service) {
    private var connRef = AtomicReference<SerialConnection?>()

    init {
        connect()
    }

    override fun onDeviceDisconnect(waitForStopped: Boolean) {
        connRef.get()?.close(waitForStopped)
        super.onDeviceDisconnect(waitForStopped)
    }

    override fun connect() {
        val device = serialInterfaceSpec.findSerial(address)
        if (device == null) {
            Logger.e { "[$address] Serial device not found at address" }
        } else {
            val connectStart = System.currentTimeMillis()
            Logger.i { "[$address] Opening serial device: $device" }

            var packetsReceived = 0
            var bytesReceived = 0L
            var connectionStartTime = 0L

            val onConnect: () -> Unit = {
                connectionStartTime = System.currentTimeMillis()
                val connectionTime = connectionStartTime - connectStart
                Logger.i { "[$address] Serial device connected in ${connectionTime}ms" }
                super.connect()
            }

            usbRepository
                .createSerialConnection(
                    device,
                    object : SerialConnectionListener {
                        override fun onMissingPermission() {
                            Logger.e {
                                "[$address] Serial connection failed - missing USB permissions for device: $device"
                            }
                        }

                        override fun onConnected() {
                            onConnect.invoke()
                        }

                        override fun onDataReceived(bytes: ByteArray) {
                            packetsReceived++
                            bytesReceived += bytes.size
                            Logger.d {
                                "[$address] Serial received packet #$packetsReceived - " +
                                    "${bytes.size} byte(s) (Total RX: $bytesReceived bytes)"
                            }
                            bytes.forEach(::readChar)
                        }

                        override fun onDisconnected(thrown: Exception?) {
                            val uptime =
                                if (connectionStartTime > 0) {
                                    System.currentTimeMillis() - connectionStartTime
                                } else {
                                    0
                                }
                            thrown?.let { e ->
                                Logger.e(e) { "[$address] Serial error after ${uptime}ms: ${e.message}" }
                            }
                            Logger.w {
                                "[$address] Serial device disconnected - " +
                                    "Device: $device, " +
                                    "Uptime: ${uptime}ms, " +
                                    "Packets RX: $packetsReceived ($bytesReceived bytes)"
                            }
                            onDeviceDisconnect(false)
                        }
                    },
                )
                .also { conn ->
                    connRef.set(conn)
                    conn.connect()
                }
        }
    }

    override fun keepAlive() {
        Logger.d { "[$address] Serial keepAlive" }
    }

    override fun sendBytes(p: ByteArray) {
        val conn = connRef.get()
        if (conn != null) {
            Logger.d { "[$address] Serial sending ${p.size} bytes" }
            conn.sendBytes(p)
        } else {
            Logger.w { "[$address] Serial connection not available, cannot send ${p.size} bytes" }
        }
    }
}

