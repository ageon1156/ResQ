
package com.geeksville.mesh.ui.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.geeksville.mesh.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.PacketRepository
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.database.entity.ContactSettings
import org.meshtastic.core.database.entity.Packet
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.util.getChannel
import org.meshtastic.core.model.util.getShortDate
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.channel_name
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.AppOnlyProtos
import org.meshtastic.proto.channelSet
import javax.inject.Inject
import kotlin.collections.map as collectionsMap

@HiltViewModel
class ContactsViewModel
@Inject
constructor(
    private val nodeRepository: NodeRepository,
    private val packetRepository: PacketRepository,
    radioConfigRepository: RadioConfigRepository,
    serviceRepository: ServiceRepository,
) : ViewModel() {
    val ourNodeInfo = nodeRepository.ourNodeInfo
    val connectionState = serviceRepository.connectionState
    val channels = radioConfigRepository.channelSetFlow.stateInWhileSubscribed(initialValue = channelSet {})

    private val identityFlow = combine(nodeRepository.myNodeInfo, nodeRepository.myId) { info, id -> info to id }

    val contactList =
        combine(identityFlow, packetRepository.getContacts(), channels, packetRepository.getContactSettings()) {
                (myNodeInfo, myId), contacts, channelSet, settings ->
            val myNodeNum = myNodeInfo?.myNodeNum ?: return@combine emptyList()
            val placeholder = (0 until channelSet.settingsCount).associate { ch ->
                val contactKey = "$ch${DataPacket.ID_BROADCAST}"
                val data = DataPacket(bytes = null, dataType = 1, time = 0L, channel = ch)
                contactKey to Packet(0L, myNodeNum, 1, contactKey, 0L, true, data)
            }
            (contacts + (placeholder - contacts.keys)).values.collectionsMap { it.toContact(myId, channelSet, settings) }
        }.stateInWhileSubscribed(initialValue = emptyList())

    val contactListPaged: Flow<PagingData<Contact>> =
        combine(identityFlow, channels, packetRepository.getContactSettings()) { (myNodeInfo, myId), channelSet, settings ->
            Triple(myNodeInfo?.myNodeNum, channelSet, Pair(settings, myId))
        }.flatMapLatest { (_, channelSet, extra) ->
            val (settings, myId) = extra
            packetRepository.getContactsPaged().map { pagingData ->
                pagingData.map { it.toContact(myId, channelSet, settings) }
            }
        }.cachedIn(viewModelScope)

    fun getNode(userId: String?) = nodeRepository.getNode(userId ?: DataPacket.ID_BROADCAST)

    fun deleteContacts(contacts: List<String>) =
        viewModelScope.launch(Dispatchers.IO) { packetRepository.deleteContacts(contacts) }

    fun setMuteUntil(contacts: List<String>, until: Long) =
        viewModelScope.launch(Dispatchers.IO) { packetRepository.setMuteUntil(contacts, until) }

    fun getContactSettings() = packetRepository.getContactSettings()

    suspend fun getTotalMessageCount(contactKeys: List<String>): Int =
        contactKeys.sumOf { packetRepository.getMessageCount(it) }

    private fun getUser(userId: String?) = nodeRepository.getUser(userId ?: DataPacket.ID_BROADCAST)

    private suspend fun Packet.toContact(
        myId: String?,
        channelSet: AppOnlyProtos.ChannelSet,
        settings: Map<String, ContactSettings>,
    ): Contact {
        val fromLocal = data.from == DataPacket.ID_LOCAL || (myId != null && data.from == myId)
        val toBroadcast = data.to == DataPacket.ID_BROADCAST
        val user = getUser(if (fromLocal) data.to else data.from)
        val node = getNode(if (fromLocal) data.to else data.from)
        val shortName = user.shortName
        val longName = if (toBroadcast) channelSet.getChannel(data.channel)?.name ?: getString(Res.string.channel_name) else user.longName
        return Contact(
            contactKey = contact_key,
            shortName = if (toBroadcast) "${data.channel}" else shortName,
            longName = longName,
            lastMessageTime = getShortDate(data.time),
            lastMessageText = if (fromLocal) data.text else "$shortName: ${data.text}",
            unreadCount = packetRepository.getUnreadCount(contact_key),
            messageCount = packetRepository.getMessageCount(contact_key),
            isMuted = settings[contact_key]?.isMuted == true,
            isUnmessageable = user.isUnmessagable,
            nodeColors = if (!toBroadcast) node.colors else null,
        )
    }
}
