
package com.geeksville.mesh.repository.radio

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
        } else {
            usbRepository
                .createSerialConnection(
                    device,
                    object : SerialConnectionListener {
                        override fun onMissingPermission() {
                        }

                        override fun onConnected() {
                            super@SerialInterface.connect()
                        }

                        override fun onDataReceived(bytes: ByteArray) {
                            bytes.forEach(::readChar)
                        }

                        override fun onDisconnected(thrown: Exception?) {
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
    }

    override fun sendBytes(p: ByteArray) {
        val conn = connRef.get()
        if (conn != null) {
            conn.sendBytes(p)
        }
    }
}

