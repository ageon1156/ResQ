
package com.geeksville.mesh.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.meshtastic.core.data.repository.PacketRepository
import org.meshtastic.core.database.entity.ReactionEntity
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.service.MeshServiceNotifications
import org.meshtastic.proto.Portnums
import javax.inject.Inject

@AndroidEntryPoint
class ReactionReceiver : BroadcastReceiver() {
    @Inject lateinit var commandSender: MeshCommandSender

    @Inject lateinit var meshServiceNotifications: MeshServiceNotifications

    @Inject lateinit var packetRepository: PacketRepository

    @Inject lateinit var nodeManager: MeshNodeManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val REACT_ACTION = "com.geeksville.mesh.REACT_ACTION"
        const val EXTRA_PACKET_ID = "packetId"
        const val EXTRA_EMOJI = "emoji"
        const val EXTRA_CONTACT_KEY = "contactKey"
        const val EXTRA_TO_ID = "toId"
        const val EXTRA_CHANNEL_INDEX = "channelIndex"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != REACT_ACTION) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val packetId = intent.getIntExtra(EXTRA_PACKET_ID, 0)
                val emoji = intent.getStringExtra(EXTRA_EMOJI)
                val toId = intent.getStringExtra(EXTRA_TO_ID)
                val channelIndex = intent.getIntExtra(EXTRA_CHANNEL_INDEX, 0)
                val contactKey = intent.getStringExtra(EXTRA_CONTACT_KEY)

                @Suppress("ComplexCondition")
                if (packetId == 0 || emoji.isNullOrEmpty() || toId.isNullOrEmpty() || contactKey.isNullOrEmpty()) {
                    return@launch
                }

                val reactionPacket =
                    DataPacket(
                        to = toId,
                        channel = channelIndex,
                        bytes = emoji.toByteArray(Charsets.UTF_8),
                        dataType = Portnums.PortNum.TEXT_MESSAGE_APP_VALUE,
                        replyId = packetId,
                        wantAck = true,
                        emoji = emoji.codePointAt(0),
                    )
                commandSender.sendData(reactionPacket)

                val reaction =
                    ReactionEntity(
                        myNodeNum = nodeManager.myNodeNum ?: 0,
                        replyId = packetId,
                        userId = nodeManager.getMyId().takeIf { it.isNotEmpty() } ?: DataPacket.ID_LOCAL,
                        emoji = emoji,
                        timestamp = System.currentTimeMillis(),
                        packetId = reactionPacket.id,
                        status = org.meshtastic.core.model.MessageStatus.QUEUED,
                        to = toId,
                        channel = channelIndex,
                    )
                packetRepository.insertReaction(reaction)

                meshServiceNotifications.cancelMessageNotification(contactKey)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

