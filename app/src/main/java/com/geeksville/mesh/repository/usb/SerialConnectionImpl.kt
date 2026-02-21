

package com.geeksville.mesh.repository.usb

import android.hardware.usb.UsbManager
import co.touchlab.kermit.Logger
import com.geeksville.mesh.util.ignoreException
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class SerialConnectionImpl(
    private val usbManagerLazy: dagger.Lazy<UsbManager?>,
    private val device: UsbSerialDriver,
    private val listener: SerialConnectionListener,
) : SerialConnection {
    private val port = device.ports[0] 
    private val closedLatch = CountDownLatch(1)
    private val closed = AtomicBoolean(false)
    private val ioRef = AtomicReference<SerialInputOutputManager>()

    override fun sendBytes(bytes: ByteArray) {
        ioRef.get()?.let {
            Logger.d { "writing ${bytes.size} byte(s }" }
            it.writeAsync(bytes)
        }
    }

    override fun close(waitForStopped: Boolean) {
        ignoreException {
            if (closed.compareAndSet(false, true)) {
                ioRef.get()?.stop()
                port.close() 
            }

            if (waitForStopped) {
                Logger.d { "Waiting for USB manager to stop..." }
                closedLatch.await(1, TimeUnit.SECONDS)
            }
        }
    }

    override fun close() {
        close(true)
    }

    override fun connect() {
        
        val usbManager = usbManagerLazy.get()!!

        val usbDeviceConnection = usbManager.openDevice(device.device)
        if (usbDeviceConnection == null) {
            listener.onMissingPermission()
            closed.set(true)
            return
        }

        port.open(usbDeviceConnection)
        port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        port.dtr = true
        port.rts = true

        Logger.d { "Starting serial reader thread" }
        val io =
            SerialInputOutputManager(
                port,
                object : SerialInputOutputManager.Listener {
                    override fun onNewData(data: ByteArray) {
                        listener.onDataReceived(data)
                    }

                    override fun onRunError(e: Exception?) {
                        closed.set(true)
                        ignoreException {
                            port.dtr = false
                            port.rts = false
                            port.close()
                        }
                        closedLatch.countDown()
                        listener.onDisconnected(e)
                    }
                },
            )
                .apply {
                    readTimeout = 200 
                    ioRef.set(this)
                }

        io.start()
        listener.onConnected()
    }
}

