

package com.geeksville.mesh.repository.usb

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.Reusable
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class ProbeTableProvider @Inject constructor() : Provider<ProbeTable> {
    override fun get(): ProbeTable {
        return UsbSerialProber.getDefaultProbeTable().apply {
            
            addProduct(9114, 32809, CdcAcmSerialDriver::class.java)
            
            addProduct(6790, 21972, CdcAcmSerialDriver::class.java)
        }
    }
}
