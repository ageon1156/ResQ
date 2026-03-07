
package org.meshtastic.feature.messaging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.entity.Packet
import org.meshtastic.core.database.entity.Reaction
import org.meshtastic.core.database.model.Message
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.new_messages_below
import org.meshtastic.feature.messaging.component.MessageItem
import org.meshtastic.feature.messaging.component.ReactionDialog

internal data class MessageListHandlers(
    val onUnreadChanged: (Long, Long) -> Unit,
    val onSendReaction: (String, Int) -> Unit,
    val onClickChip: (Node) -> Unit,
    val onDeleteMessages: (List<Long>) -> Unit,
    val onSendMessage: (String, String) -> Unit,
    val onReply: (Message?) -> Unit,
    val onPlayVoice: (String) -> Unit = {},
)

internal data class MessageListPagedState(
    val nodes: List<Node>,
    val ourNode: Node?,
    val messages: LazyPagingItems<Message>,
    val selectedIds: MutableState<Set<Long>>,
    val contactKey: String,
    val firstUnreadMessageUuid: Long? = null,
    val hasUnreadMessages: Boolean = false,
)

private fun MutableState<Set<Long>>.toggle(uuid: Long) {
    value =
        if (value.contains(uuid)) {
            value - uuid
        } else {
            value + uuid
        }
}

@Composable
internal fun MessageListPaged(
    state: MessageListPagedState,
    handlers: MessageListHandlers,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    quickEmojis: List<String> = emptyList(),
) {
    val haptics = LocalHapticFeedback.current
    val inSelectionMode by remember { derivedStateOf { state.selectedIds.value.isNotEmpty() } }

    val nodeMap = remember(state.nodes) { state.nodes.associateBy { it.num } }

    var showStatusDialog by remember { mutableStateOf<Message?>(null) }
    showStatusDialog?.let { message ->
        MessageStatusDialog(
            message = message,
            nodes = state.nodes,
            ourNode = state.ourNode,
            resendOption = message.status?.equals(MessageStatus.ERROR) ?: false,
            retryCount = message.retryCount,
            maxRetries = 2,
            onResend = {
                handlers.onDeleteMessages(listOf(message.uuid))
                handlers.onSendMessage(message.text, state.contactKey)
                showStatusDialog = null
            },
            onDismiss = { showStatusDialog = null },
        )
    }

    var showReactionDialog by remember { mutableStateOf<List<Reaction>?>(null) }
    showReactionDialog?.let { reactions ->
        ReactionDialog(
            reactions = reactions,
            myId = state.ourNode?.user?.id,
            onDismiss = { showReactionDialog = null },
            onResend = { reaction ->
                handlers.onSendReaction(reaction.emoji, reaction.replyId)
                showReactionDialog = null
            },
            nodes = state.nodes,
            ourNode = state.ourNode,
        )
    }

    val coroutineScope = rememberCoroutineScope()

    val hasDialogOpen = showStatusDialog != null || showReactionDialog != null

    UpdateUnreadCountPaged(listState = listState, messages = state.messages, onUnreadChange = handlers.onUnreadChanged)

    AutoScrollToBottomPaged(
        listState = listState,
        messages = state.messages,
        hasUnreadMessages = state.hasUnreadMessages,
        hasDialogOpen = hasDialogOpen,
    )

    MessageListPagedContent(
        listState = listState,
        state = state,
        nodeMap = nodeMap,
        handlers = handlers,
        inSelectionMode = inSelectionMode,
        coroutineScope = coroutineScope,
        haptics = haptics,
        onShowStatusDialog = { showStatusDialog = it },
        onShowReactions = { showReactionDialog = it },
        modifier = modifier,
        quickEmojis = quickEmojis,
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun MessageListPagedContent(
    listState: LazyListState,
    state: MessageListPagedState,
    nodeMap: Map<Int, Node>,
    handlers: MessageListHandlers,
    inSelectionMode: Boolean,
    coroutineScope: CoroutineScope,
    haptics: HapticFeedback,
    onShowStatusDialog: (Message) -> Unit,
    onShowReactions: (List<Reaction>) -> Unit,
    modifier: Modifier = Modifier,
    quickEmojis: List<String>,
) {
    
    val unreadDividerIndex by
        remember(state.messages.itemCount, state.firstUnreadMessageUuid) {
            derivedStateOf {
                state.firstUnreadMessageUuid?.let { uuid ->
                    (0 until state.messages.itemCount).firstOrNull { index -> state.messages[index]?.uuid == uuid }
                }
            }
        }

    val enableAnimations by remember { derivedStateOf { !listState.isScrollInProgress } }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(count = state.messages.itemCount, key = state.messages.itemKey { it.uuid }) { index ->
                val message = state.messages[index]
                val visuallyPrevMessage = if (index < state.messages.itemCount - 1) state.messages[index + 1] else null
                val visuallyNextMessage = if (index > 0) state.messages[index - 1] else null

                val hasSamePrev =
                    if (message != null && visuallyPrevMessage != null) {
                        visuallyPrevMessage.fromLocal == message.fromLocal &&
                            (message.fromLocal || visuallyPrevMessage.node.num == message.node.num)
                    } else {
                        false
                    }

                val hasSameNext =
                    if (message != null && visuallyNextMessage != null) {
                        visuallyNextMessage.fromLocal == message.fromLocal &&
                            (message.fromLocal || visuallyNextMessage.node.num == message.node.num)
                    } else {
                        false
                    }

                if (message != null) {
                    renderPagedChatMessageRow(
                        message = message,
                        state = state,
                        nodeMap = nodeMap,
                        handlers = handlers,
                        inSelectionMode = inSelectionMode,
                        coroutineScope = coroutineScope,
                        haptics = haptics,
                        listState = listState,
                        onShowStatusDialog = onShowStatusDialog,
                        onShowReactions = onShowReactions,
                        enableAnimations = enableAnimations,
                        showUserName = !hasSamePrev,
                        hasSamePrev = hasSamePrev,
                        hasSameNext = hasSameNext,
                        quickEmojis = quickEmojis,
                    )

                    if (state.hasUnreadMessages && unreadDividerIndex == index) {
                        UnreadMessagesDivider(modifier = if (enableAnimations) Modifier.animateItem() else Modifier)
                    }
                }
            }

            state.messages.apply {
                when {
                    loadState.append is LoadState.Loading -> {
                        item(key = "append_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LazyItemScope.renderPagedChatMessageRow(
    message: Message,
    state: MessageListPagedState,
    nodeMap: Map<Int, Node>,
    handlers: MessageListHandlers,
    inSelectionMode: Boolean,
    coroutineScope: CoroutineScope,
    haptics: HapticFeedback,
    listState: LazyListState,
    onShowStatusDialog: (Message) -> Unit,
    onShowReactions: (List<Reaction>) -> Unit,
    enableAnimations: Boolean,
    showUserName: Boolean,
    hasSamePrev: Boolean,
    hasSameNext: Boolean,
    quickEmojis: List<String>,
) {
    val ourNode = state.ourNode ?: return
    val selected by
        remember(message.uuid, state.selectedIds.value) {
            derivedStateOf { state.selectedIds.value.contains(message.uuid) }
        }
    val node = nodeMap[message.node.num] ?: message.node

    MessageItem(
        modifier = if (enableAnimations) Modifier.animateItem() else Modifier,
        node = node,
        ourNode = ourNode,
        message = message,
        selected = selected,
        inSelectionMode = inSelectionMode,
        onClick = {
            if (inSelectionMode) {
                state.selectedIds.toggle(message.uuid)
            } else {
                val sessionId = message.text.substringAfter('\u0000', "")
                if (sessionId.isNotEmpty()) handlers.onPlayVoice(sessionId)
            }
        },
        onLongClick = {
            if (inSelectionMode) {
                state.selectedIds.toggle(message.uuid)
            }
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onSelect = { state.selectedIds.toggle(message.uuid) },
        onDelete = { handlers.onDeleteMessages(listOf(message.uuid)) },
        onClickChip = handlers.onClickChip,
        onStatusClick = { onShowStatusDialog(message) },
        onReply = { handlers.onReply(message) },
        emojis = message.emojis,
        showUserName = showUserName,
        sendReaction = { emoji ->
            val hasReacted =
                message.emojis.any { reaction ->
                    (
                        reaction.user.id == ourNode.user.id ||
                            reaction.user.id == org.meshtastic.core.model.DataPacket.ID_LOCAL
                        ) && reaction.emoji == emoji
                }
            if (!hasReacted) {
                handlers.onSendReaction(emoji, message.packetId)
            }
        },
        onShowReactions = { onShowReactions(message.emojis) },
        onNavigateToOriginalMessage = {
            coroutineScope.launch {

                val targetIndex =
                    (0 until state.messages.itemCount).firstOrNull { index ->
                        state.messages[index]?.packetId == message.replyId
                    }
                if (targetIndex != null) {
                    listState.animateScrollToItem(index = targetIndex)
                }
            }
        },
        hasSamePrev = hasSamePrev,
        hasSameNext = hasSameNext,
        quickEmojis = quickEmojis,
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun AutoScrollToBottomPaged(
    listState: LazyListState,
    messages: LazyPagingItems<Message>,
    hasUnreadMessages: Boolean,
    hasDialogOpen: Boolean = false,
    itemThreshold: Int = 3,
) = with(listState) {

    var cachedAtBottom by remember { mutableStateOf(true) }

    val isCurrentlyAtBottom by
        remember(hasUnreadMessages, hasDialogOpen) {
            derivedStateOf {
                if (hasDialogOpen) {
                    false
                } else {
                    val isAtBottom =
                        firstVisibleItemIndex == 0 &&
                            firstVisibleItemScrollOffset <= UnreadUiDefaults.AUTO_SCROLL_BOTTOM_OFFSET_TOLERANCE
                    val isNearBottom = firstVisibleItemIndex <= itemThreshold
                    isAtBottom || (!hasUnreadMessages && isNearBottom)
                }
            }
        }

    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            cachedAtBottom = isCurrentlyAtBottom
        }
    }

    LaunchedEffect(messages.itemCount) {

        if (cachedAtBottom && messages.itemCount > 0) {
            scrollToItem(0)
            
            cachedAtBottom = true
        }
    }
}

private fun findFirstVisibleUnreadMessage(messages: LazyPagingItems<Message>, visibleIndex: Int): Message? {
    val firstVisibleUnreadIndex =
        (visibleIndex until messages.itemCount).firstOrNull { i ->
            val msg = messages[i]
            msg != null && !msg.read && !msg.fromLocal
        }
    return firstVisibleUnreadIndex?.let { messages[it] }
}

private fun findLastUnreadMessageIndex(messages: LazyPagingItems<Message>): Int? =
    (0 until messages.itemCount).lastOrNull { i ->
        val msg = messages[i]
        msg != null && !msg.read && !msg.fromLocal
    }

@OptIn(FlowPreview::class)
@Composable
private fun UpdateUnreadCountPaged(
    listState: LazyListState,
    messages: LazyPagingItems<Message>,
    onUnreadChange: (Long, Long) -> Unit,
) {
    val currentOnUnreadChange by rememberUpdatedState(onUnreadChange)
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> isResumed = true
                    Lifecycle.Event.ON_PAUSE -> isResumed = false
                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val remoteMessageCount by
        remember(messages.itemCount) { derivedStateOf { messages.itemSnapshotList.items.count { !it.fromLocal } } }

    LaunchedEffect(remoteMessageCount, listState, isResumed) {
        snapshotFlow {

            if (listState.isScrollInProgress || !isResumed) {
                null 
            } else {
                listState.firstVisibleItemIndex 
            }
        }
            .debounce(timeoutMillis = UnreadUiDefaults.SCROLL_DEBOUNCE_MILLIS)
            .collectLatest { index ->
                
                if (index != null) {
                    val lastUnreadIndex = findLastUnreadMessageIndex(messages)

                    if (lastUnreadIndex != null && index <= lastUnreadIndex) {
                        val firstVisibleUnread = findFirstVisibleUnreadMessage(messages, index)
                        firstVisibleUnread?.let { currentOnUnreadChange(it.uuid, it.receivedTime) }
                    }
                }
            }
    }
}

@Composable
internal fun UnreadMessagesDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.new_messages_below),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun MessageStatusDialog(
    message: Message,
    nodes: List<Node>,
    ourNode: Node?,
    resendOption: Boolean,
    retryCount: Int,
    maxRetries: Int,
    onResend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, text) = message.getStatusStringRes()
    val relayNodeName by
        remember(message.relayNode, nodes, ourNode) {
            derivedStateOf {
                message.relayNode?.let { relayNodeId ->
                    Packet.getRelayNode(relayNodeId, nodes, ourNode?.num)?.user?.longName
                }
            }
        }
    DeliveryInfo(
        title = title,
        resendOption = resendOption,
        text = text,
        relayNodeName = relayNodeName,
        relays = message.relays,
        retryCount = retryCount,
        maxRetries = maxRetries,
        onConfirm = onResend,
        onDismiss = onDismiss,
    )
}

