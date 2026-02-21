
package org.meshtastic.core.service

import android.app.Notification
import org.meshtastic.core.database.entity.NodeEntity
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.TelemetryProtos

const val SERVICE_NOTIFY_ID = 101

@Suppress("TooManyFunctions")
interface MeshServiceNotifications {
    fun clearNotifications()

    fun initChannels()

    fun updateServiceStateNotification(summaryString: String?, telemetry: TelemetryProtos.Telemetry?): Notification

    suspend fun updateMessageNotification(
        contactKey: String,
        name: String,
        message: String,
        isBroadcast: Boolean,
        channelName: String?,
        isSilent: Boolean = false,
    )

    suspend fun updateWaypointNotification(
        contactKey: String,
        name: String,
        message: String,
        waypointId: Int,
        isSilent: Boolean = false,
    )

    suspend fun updateReactionNotification(
        contactKey: String,
        name: String,
        emoji: String,
        isBroadcast: Boolean,
        channelName: String?,
        isSilent: Boolean = false,
    )

    fun showAlertNotification(contactKey: String, name: String, alert: String)

    fun showNewNodeSeenNotification(node: NodeEntity)

    fun showOrUpdateLowBatteryNotification(node: NodeEntity, isRemote: Boolean)

    fun showClientNotification(clientNotification: MeshProtos.ClientNotification)

    fun cancelMessageNotification(contactKey: String)

    fun cancelLowBatteryNotification(node: NodeEntity)

    fun clearClientNotification(notification: MeshProtos.ClientNotification)

    fun showSilentNodeNotification(nodeNum: Int, title: String, message: String)
}

