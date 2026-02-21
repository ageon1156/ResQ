

package com.geeksville.mesh.repository.usb

interface SerialConnection : AutoCloseable {
    
    fun connect()

    fun sendBytes(bytes: ByteArray)

    fun close(waitForStopped: Boolean)

    override fun close()
}
