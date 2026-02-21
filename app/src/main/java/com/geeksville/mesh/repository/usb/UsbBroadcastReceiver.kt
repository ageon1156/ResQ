

package com.geeksville.mesh.repository.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import co.touchlab.kermit.Logger
import com.geeksville.mesh.util.exceptionReporter
import com.geeksville.mesh.util.getParcelableExtraCompat
import javax.inject.Inject

class UsbBroadcastReceiver @Inject constructor(private val usbRepository: UsbRepository) : BroadcastReceiver() {
    
    internal val intentFilter
        get() =
            IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            }

    override fun onReceive(context: Context, intent: Intent) = exceptionReporter {
        val device: UsbDevice? = intent.getParcelableExtraCompat(UsbManager.EXTRA_DEVICE)
        val deviceName: String = device?.deviceName ?: "unknown"

        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                Logger.d { "USB device '$deviceName' was detached" }
                usbRepository.refreshState()
            }
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                Logger.d { "USB device '$deviceName' was attached" }
                usbRepository.refreshState()
            }
            UsbManager.EXTRA_PERMISSION_GRANTED -> {
                Logger.d { "USB device '$deviceName' was granted permission" }
                usbRepository.refreshState()
            }
        }
    }
}

