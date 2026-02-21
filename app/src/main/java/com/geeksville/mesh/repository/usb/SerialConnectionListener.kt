

package com.geeksville.mesh.repository.usb

interface SerialConnectionListener {
    
    fun onMissingPermission() {}

    fun onConnected() {}

    fun onDataReceived(bytes: ByteArray) {}

    fun onDisconnected(thrown: Exception?) {}
}
