

package com.geeksville.mesh.repository.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import com.geeksville.mesh.util.exceptionReporter
import javax.inject.Inject

class UsbBroadcastReceiver @Inject constructor(private val usbRepository: UsbRepository) : BroadcastReceiver() {
    
    internal val intentFilter
        get() =
            IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            }

    override fun onReceive(context: Context, intent: Intent) = exceptionReporter {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                usbRepository.refreshState()
            }
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                usbRepository.refreshState()
            }
            UsbManager.EXTRA_PERMISSION_GRANTED -> {
                usbRepository.refreshState()
            }
        }
    }
}

