
package com.geeksville.mesh.service

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import dagger.hilt.android.qualifiers.ApplicationContext
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.NodeInfo
import org.meshtastic.core.service.ServiceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshServiceBroadcasts
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val connectionStateHolder: ConnectionStateHandler,
    private val serviceRepository: ServiceRepository,
) {
    
    private val clientPackages = mutableMapOf<String, String>()

    fun subscribeReceiver(receiverName: String, packageName: String) {
        clientPackages[receiverName] = packageName
    }

    fun broadcastReceivedData(payload: DataPacket) {
        explicitBroadcast(Intent(MeshService.actionReceived(payload.dataType)).putExtra(EXTRA_PAYLOAD, payload))
    }

    fun broadcastNodeChange(info: NodeInfo) {
        val intent = Intent(ACTION_NODE_CHANGE).putExtra(EXTRA_NODEINFO, info)
        explicitBroadcast(intent)
    }

    fun broadcastMessageStatus(p: DataPacket) = broadcastMessageStatus(p.id, p.status)

    fun broadcastMessageStatus(id: Int, status: MessageStatus?) {
        if (id != 0) {
            val intent =
                Intent(ACTION_MESSAGE_STATUS).apply {
                    putExtra(EXTRA_PACKET_ID, id)
                    putExtra(EXTRA_STATUS, status as Parcelable)
                }
            explicitBroadcast(intent)
        }
    }

    fun broadcastConnection() {
        val connectionState = connectionStateHolder.connectionState.value
        val intent = Intent(ACTION_MESH_CONNECTED).putExtra(EXTRA_CONNECTED, connectionState.toString())
        serviceRepository.setConnectionState(connectionState)
        explicitBroadcast(intent)
    }

    private fun explicitBroadcast(intent: Intent) {
        context.sendBroadcast(
            intent,
        ) 
        clientPackages.forEach {
            intent.setClassName(it.value, it.key)
            context.sendBroadcast(intent)
        }
    }
}

