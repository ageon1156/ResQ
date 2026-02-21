

package org.meshtastic.feature.messaging

import org.meshtastic.core.database.model.Node

internal sealed interface MessageScreenEvent {
    
    data class SendMessage(val text: String, val replyingToPacketId: Int? = null, val priority: Int = 0) : MessageScreenEvent

    data class SendReaction(val emoji: String, val messageId: Int) : MessageScreenEvent

    data class DeleteMessages(val ids: List<Long>) : MessageScreenEvent

    data class ClearUnreadCount(val messageUuid: Long, val lastReadTimestamp: Long) : MessageScreenEvent

    data class NodeDetails(val node: Node) : MessageScreenEvent

    data class SetTitle(val title: String) : MessageScreenEvent

    data class NavigateToNodeDetails(val nodeNum: Int) : MessageScreenEvent

    data object NavigateBack : MessageScreenEvent

    data class CopyToClipboard(val text: String) : MessageScreenEvent
}

