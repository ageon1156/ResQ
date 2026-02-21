
package com.geeksville.mesh.service

import org.meshtastic.core.model.DataPacket
import org.meshtastic.proto.MeshProtos.MeshPacket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshDataMapper @Inject constructor(private val nodeManager: MeshNodeManager) {
    fun toNodeID(n: Int): String = if (n == DataPacket.NODENUM_BROADCAST) {
        DataPacket.ID_BROADCAST
    } else {
        nodeManager.nodeDBbyNodeNum[n]?.user?.id ?: DataPacket.nodeNumToDefaultId(n)
    }

    fun toDataPacket(packet: MeshPacket): DataPacket? = if (!packet.hasDecoded()) {
        null
    } else {
        val data = packet.decoded
        DataPacket(
            from = toNodeID(packet.from),
            to = toNodeID(packet.to),
            time = packet.rxTime * 1000L,
            id = packet.id,
            dataType = data.portnumValue,
            bytes = data.payload.toByteArray(),
            hopLimit = packet.hopLimit,
            channel = if (packet.pkiEncrypted) DataPacket.PKC_CHANNEL_INDEX else packet.channel,
            wantAck = packet.wantAck,
            hopStart = packet.hopStart,
            snr = packet.rxSnr,
            rssi = packet.rxRssi,
            replyId = data.replyId,
            relayNode = packet.relayNode,
            viaMqtt = packet.viaMqtt,
            emoji = data.emoji,
        )
    }
}

