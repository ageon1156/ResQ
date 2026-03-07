
package org.meshtastic.feature.messaging

import android.os.RemoteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import co.touchlab.kermit.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.audio.AudioPlayer
import org.meshtastic.core.audio.AudioRecorder
import org.meshtastic.core.audio.Codec2Wrapper
import org.meshtastic.core.model.voice.VoiceFragment
import org.meshtastic.core.model.voice.encode
import org.meshtastic.proto.MeshProtos
import org.meshtastic.proto.Portnums
import java.util.UUID
import org.meshtastic.core.data.repository.NodeRepository
import org.meshtastic.core.data.repository.PacketRepository
import org.meshtastic.core.data.repository.VoiceMessageRepository
import org.meshtastic.core.data.repository.QuickChatActionRepository
import org.meshtastic.core.data.repository.RadioConfigRepository
import org.meshtastic.core.database.entity.ContactSettings
import org.meshtastic.core.database.entity.Packet
import org.meshtastic.core.database.model.Message
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.Capabilities
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.prefs.emoji.CustomEmojiPrefs
import org.meshtastic.core.prefs.ui.UiPrefs
import org.meshtastic.core.service.MeshServiceNotifications
import org.meshtastic.core.service.ServiceAction
import org.meshtastic.core.service.ServiceRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.ConfigProtos.Config.DeviceConfig.Role
import org.meshtastic.proto.channelSet
import org.meshtastic.proto.sharedContact
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class MessageViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val nodeRepository: NodeRepository,
    radioConfigRepository: RadioConfigRepository,
    quickChatActionRepository: QuickChatActionRepository,
    private val serviceRepository: ServiceRepository,
    private val packetRepository: PacketRepository,
    private val uiPrefs: UiPrefs,
    private val customEmojiPrefs: CustomEmojiPrefs,
    private val meshServiceNotifications: MeshServiceNotifications,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val codec2: Codec2Wrapper,
    private val voiceMessageRepository: VoiceMessageRepository,
) : ViewModel() {

    private val _pttRecording = MutableStateFlow(false)
    val pttRecording: StateFlow<Boolean> = _pttRecording.asStateFlow()
    private val recordedPcm = mutableListOf<ShortArray>()
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    val ourNodeInfo = nodeRepository.ourNodeInfo

    val connectionState = serviceRepository.connectionState

    val nodeList: StateFlow<List<Node>> = nodeRepository.getNodes().stateInWhileSubscribed(initialValue = emptyList())

    val channels = radioConfigRepository.channelSetFlow.stateInWhileSubscribed(channelSet {})

    private val _showQuickChat = MutableStateFlow(uiPrefs.showQuickChat)
    val showQuickChat: StateFlow<Boolean> = _showQuickChat

    val quickChatActions = quickChatActionRepository.getAllActions().stateInWhileSubscribed(initialValue = emptyList())

    val contactSettings: StateFlow<Map<String, ContactSettings>> =
        packetRepository.getContactSettings().stateInWhileSubscribed(initialValue = emptyMap())

    val retryEvents = serviceRepository.retryEvents

    private val contactKeyForPagedMessages: MutableStateFlow<String?> = MutableStateFlow(null)
    private val pagedMessagesForContactKey: Flow<PagingData<Message>> =
        contactKeyForPagedMessages
            .filterNotNull()
            .flatMapLatest { contactKey -> packetRepository.getMessagesFromPaged(contactKey, ::getNode) }
            .cachedIn(viewModelScope)

    val frequentEmojis: List<String>
        get() =
            customEmojiPrefs.customEmojiFrequency
                ?.split(",")
                ?.associate { entry ->
                    entry.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1].toInt() } ?: ("" to 0)
                }
                ?.toList()
                ?.sortedByDescending { it.second }
                ?.map { it.first }
                ?.take(6) ?: listOf("👍", "👎", "😂", "🔥", "❤️", "😮")

    init {
        val contactKey = savedStateHandle.get<String>("contactKey")
        if (contactKey != null) {
            contactKeyForPagedMessages.value = contactKey
        }
    }

    fun setTitle(title: String) {
        viewModelScope.launch { _title.value = title }
    }

    fun getMessagesFromPaged(contactKey: String): Flow<PagingData<Message>> {
        if (contactKeyForPagedMessages.value != contactKey) {
            contactKeyForPagedMessages.value = contactKey
        }
        return pagedMessagesForContactKey
    }

    fun getFirstUnreadMessageUuid(contactKey: String): Flow<Long?> =
        packetRepository.getFirstUnreadMessageUuid(contactKey)

    fun hasUnreadMessages(contactKey: String): Flow<Boolean> = packetRepository.hasUnreadMessages(contactKey)

    fun toggleShowQuickChat() = toggle(_showQuickChat) { uiPrefs.showQuickChat = it }

    private fun toggle(state: MutableStateFlow<Boolean>, onChanged: (newValue: Boolean) -> Unit) {
        (!state.value).let { toggled ->
            state.update { toggled }
            onChanged(toggled)
        }
    }

    fun getNode(userId: String?) = nodeRepository.getNode(userId ?: DataPacket.ID_BROADCAST)

    fun getUser(userId: String?) = nodeRepository.getUser(userId ?: DataPacket.ID_BROADCAST)

    @Suppress("NestedBlockDepth")
    fun sendMessage(str: String, contactKey: String = "0${DataPacket.ID_BROADCAST}", replyId: Int? = null, priority: Int = 0) {
        
        val channel = contactKey[0].digitToIntOrNull()
        val dest = if (channel != null) contactKey.substring(1) else contactKey

        if (channel == null) { 
            val fwVersion = ourNodeInfo.value?.metadata?.firmwareVersion
            val destNode = nodeRepository.getNode(dest)
            val isClientBase = ourNodeInfo.value?.user?.role == Role.CLIENT_BASE

            val capabilities = Capabilities(fwVersion)

            if (capabilities.canSendVerifiedContacts) {
                sendSharedContact(destNode)
            } else {
                if (!destNode.isFavorite && !isClientBase) {
                    favoriteNode(destNode)
                }
            }
        }
        val p =
            DataPacket(dest, channel ?: 0, str, replyId).apply {
                from = ourNodeInfo.value?.user?.id ?: DataPacket.ID_LOCAL
                this.priority = priority
            }
        sendDataPacket(p)
    }

    fun sendReaction(emoji: String, replyId: Int, contactKey: String) =
        viewModelScope.launch { serviceRepository.onServiceAction(ServiceAction.Reaction(emoji, replyId, contactKey)) }

    fun deleteMessages(uuidList: List<Long>) =
        viewModelScope.launch(Dispatchers.IO) { packetRepository.deleteMessages(uuidList) }

    fun clearUnreadCount(contact: String, messageUuid: Long, lastReadTimestamp: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            val existingTimestamp = contactSettings.value[contact]?.lastReadMessageTimestamp ?: Long.MIN_VALUE
            if (lastReadTimestamp <= existingTimestamp) {
                return@launch
            }
            packetRepository.clearUnreadCount(contact, lastReadTimestamp)
            packetRepository.updateLastReadMessage(contact, messageUuid, lastReadTimestamp)
            val unreadCount = packetRepository.getUnreadCount(contact)
            if (unreadCount == 0) meshServiceNotifications.cancelMessageNotification(contact)
        }

    private fun favoriteNode(node: Node) = viewModelScope.launch {
        try {
            serviceRepository.onServiceAction(ServiceAction.Favorite(node))
        } catch (ex: RemoteException) {
            Logger.e(ex) { "Favorite node error" }
        }
    }

    private fun sendSharedContact(node: Node) = viewModelScope.launch {
        try {
            val contact = sharedContact {
                nodeNum = node.num
                user = node.user
                manuallyVerified = node.manuallyVerified
            }
            serviceRepository.onServiceAction(ServiceAction.SendContact(contact = contact))
        } catch (ex: RemoteException) {
            Logger.e(ex) { "Send shared contact error" }
        }
    }

    fun startPttRecording() {
        if (_pttRecording.value) return
        recordedPcm.clear()
        _pttRecording.value = true
        audioRecorder.start { samples -> recordedPcm.add(samples) }
    }

    fun stopPttAndSend(contactKey: String) {
        if (!_pttRecording.value) return
        _pttRecording.value = false
        audioRecorder.stop()
        viewModelScope.launch { encodePttAndSend(contactKey) }
    }

    fun playVoiceMessage(sessionId: String) {
        val message = voiceMessageRepository.getRecent(sessionId) ?: return
        viewModelScope.launch {
            runCatching { audioPlayer.play(message.codec2Bytes) }
                .onFailure { Logger.e(it) { "Voice replay failed" } }
        }
    }

    private suspend fun encodePttAndSend(contactKey: String) = withContext(Dispatchers.Default) {
        if (recordedPcm.isEmpty()) return@withContext
        val allPcm = ShortArray(recordedPcm.sumOf { it.size })
        var offset = 0
        for (chunk in recordedPcm) { chunk.copyInto(allPcm, offset); offset += chunk.size }
        recordedPcm.clear()
        val codec2Bytes = runCatching { codec2.encode(allPcm, Codec2Wrapper.DEFAULT_MODE) }
            .getOrElse { Logger.e(it) { "PTT encode failed" }; return@withContext }
        val sessionId = UUID.randomUUID().toString().replace("-", "").take(16)
            .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val timestamp = (System.currentTimeMillis() / 1000L).toInt()
        val channel = contactKey[0].digitToIntOrNull() ?: 0
        val dest = if (contactKey[0].isDigit()) contactKey.substring(1) else contactKey
        val chunks = codec2Bytes.toList().chunked(216)
        chunks.forEachIndexed { index, chunk ->
            val fragment = VoiceFragment(index, chunks.size, sessionId, timestamp, chunk.toByteArray())
            val packet = DataPacket(
                to = dest,
                bytes = fragment.encode(),
                dataType = Portnums.PortNum.AUDIO_APP_VALUE,
                channel = channel,
                priority = MeshProtos.MeshPacket.Priority.HIGH_VALUE,
                wantAck = false,
            )
            sendDataPacket(packet)
            if (index < chunks.lastIndex) delay(500)
        }

        val localUserId = ourNodeInfo.value?.user?.id ?: DataPacket.ID_LOCAL
        val myNodeNum = ourNodeInfo.value?.num ?: 0
        val placeholderData = DataPacket(
            to = dest,
            bytes = "🎤 Voice message".encodeToByteArray(),
            dataType = Portnums.PortNum.TEXT_MESSAGE_APP_VALUE,
            from = localUserId,
            channel = channel,
        )
        packetRepository.insert(
            Packet(
                uuid = 0L,
                myNodeNum = myNodeNum,
                port_num = Portnums.PortNum.TEXT_MESSAGE_APP_VALUE,
                contact_key = contactKey,
                received_time = System.currentTimeMillis(),
                read = true,
                data = placeholderData,
            )
        )
    }

    private fun sendDataPacket(p: DataPacket) {
        try {
            serviceRepository.meshService?.send(p)
        } catch (ex: RemoteException) {
            Logger.e { "Send DataPacket error: ${ex.message}" }
        }
    }

    fun respondToRetry(packetId: Int, shouldRetry: Boolean) {
        serviceRepository.respondToRetry(packetId, shouldRetry)
    }
}

