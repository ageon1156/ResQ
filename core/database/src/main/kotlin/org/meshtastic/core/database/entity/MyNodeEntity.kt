
package org.meshtastic.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.meshtastic.core.model.MyNodeInfo

@Entity(tableName = "my_node")
data class MyNodeEntity(
    @PrimaryKey(autoGenerate = false) val myNodeNum: Int,
    val model: String?,
    val firmwareVersion: String?,
    val couldUpdate: Boolean, 
    val shouldUpdate: Boolean, 
    val currentPacketId: Long,
    val messageTimeoutMsec: Int,
    val minAppVersion: Int,
    val maxChannels: Int,
    val hasWifi: Boolean,
    val deviceId: String? = "unknown",
    val pioEnv: String? = null,
) {
    
    val firmwareString: String
        get() = "$model $firmwareVersion"

    fun toMyNodeInfo() = MyNodeInfo(
        myNodeNum = myNodeNum,
        hasGPS = false,
        model = model,
        firmwareVersion = firmwareVersion,
        couldUpdate = couldUpdate,
        shouldUpdate = shouldUpdate,
        currentPacketId = currentPacketId,
        messageTimeoutMsec = messageTimeoutMsec,
        minAppVersion = minAppVersion,
        maxChannels = maxChannels,
        hasWifi = hasWifi,
        channelUtilization = 0f,
        airUtilTx = 0f,
        deviceId = deviceId,
        pioEnv = pioEnv,
    )
}

