
package org.meshtastic.core.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MyNodeInfo(
    val myNodeNum: Int,
    val hasGPS: Boolean,
    val model: String?,
    val firmwareVersion: String?,
    val couldUpdate: Boolean, 
    val shouldUpdate: Boolean, 
    val currentPacketId: Long,
    val messageTimeoutMsec: Int,
    val minAppVersion: Int,
    val maxChannels: Int,
    val hasWifi: Boolean,
    val channelUtilization: Float,
    val airUtilTx: Float,
    val deviceId: String?,
    val pioEnv: String? = null,
) : Parcelable {
    
    val firmwareString: String
        get() = "$model $firmwareVersion"
}

