
package com.geeksville.mesh.repository.radio

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class StreamInterface(protected val service: RadioInterfaceService) : IRadioInterface {
    companion object {
        private const val START1 = 0x94.toByte()
        private const val START2 = 0xc3.toByte()
        private const val MAX_TO_FROM_RADIO_SIZE = 512
    }

    private val writeMutex = Mutex()

    private var ptr = 0

    private var msb = 0
    private var lsb = 0
    private var packetLen = 0

    override fun close() {
        onDeviceDisconnect(true)
    }

    protected open fun onDeviceDisconnect(waitForStopped: Boolean) {
        service.onDisconnect(
            isPermanent = true,
        ) 
    }

    protected open fun connect() {
        
        val wakeBytes = byteArrayOf(START1, START1, START1, START1)
        sendBytes(wakeBytes)

        service.onConnect()
    }

    abstract fun sendBytes(p: ByteArray)

    open fun flushBytes() {}

    override fun handleSendToRadio(p: ByteArray) {

        service.serviceScope.launch {
            writeMutex.withLock {
                val header = ByteArray(4)
                header[0] = START1
                header[1] = START2
                header[2] = (p.size shr 8).toByte()
                header[3] = (p.size and 0xff).toByte()

                sendBytes(header)
                sendBytes(p)
                flushBytes()
            }
        }
    }

    private val rxPacket = ByteArray(MAX_TO_FROM_RADIO_SIZE)

    protected fun readChar(c: Byte) {
        
        var nextPtr = ptr + 1

        fun lostSync() {
            nextPtr = 0
        }

        fun deliverPacket() {
            val buf = rxPacket.copyOf(packetLen)
            service.handleFromRadio(buf)

            nextPtr = 0 
        }

        when (ptr) {
            0 ->
                if (c != START1) {
                    nextPtr = 0
                }
            1 -> 
                if (c != START2) {
                    lostSync() 
                }
            2 -> 
                msb = c.toInt() and 0xff
            3 -> { 
                lsb = c.toInt() and 0xff

                packetLen = (msb shl 8) or lsb
                if (packetLen > MAX_TO_FROM_RADIO_SIZE) {
                    lostSync() 
                    
                } else if (packetLen == 0) {
                    deliverPacket() 
                    
                }
            }
            else -> {
                
                rxPacket[ptr - 4] = c

                if (ptr - 4 + 1 == packetLen) {
                    deliverPacket()
                }
            }
        }
        ptr = nextPtr
    }
}

