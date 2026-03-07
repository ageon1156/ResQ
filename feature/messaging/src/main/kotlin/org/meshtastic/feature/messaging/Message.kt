
@file:Suppress("TooManyFunctions")

package org.meshtastic.feature.messaging

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.SpeakerNotesOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.entity.QuickChatAction
import org.meshtastic.core.database.model.Message
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.util.getChannel
import org.meshtastic.core.service.RetryEvent
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.alert_bell_text
import org.meshtastic.core.strings.cancel
import org.meshtastic.core.strings.channels
import org.meshtastic.core.strings.cancel_reply
import org.meshtastic.core.strings.clear_selection
import org.meshtastic.core.strings.copy
import org.meshtastic.core.strings.delete
import org.meshtastic.core.strings.delete_messages
import org.meshtastic.core.strings.delete_messages_title
import org.meshtastic.core.strings.message_input_label
import org.meshtastic.core.strings.navigate_back
import org.meshtastic.core.strings.overflow_menu
import org.meshtastic.core.strings.quick_chat
import org.meshtastic.core.strings.quick_chat_hide
import org.meshtastic.core.strings.quick_chat_show
import org.meshtastic.core.strings.reply
import org.meshtastic.core.strings.replying_to
import org.meshtastic.core.strings.scroll_to_bottom
import org.meshtastic.core.strings.select_all
import org.meshtastic.core.strings.send
import org.meshtastic.core.strings.type_a_message
import org.meshtastic.core.strings.unknown
import org.meshtastic.core.strings.unknown_channel
import org.meshtastic.core.ui.component.NodeKeyStatusIcon
import org.meshtastic.core.ui.component.SecurityIcon
import org.meshtastic.core.ui.component.SharedContactDialog
import org.meshtastic.core.ui.component.smartScrollToIndex
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.feature.messaging.component.RetryConfirmationDialog
import org.meshtastic.proto.AppOnlyProtos
import org.meshtastic.proto.MeshProtos.MeshPacket
import androidx.core.content.ContextCompat
import java.nio.charset.StandardCharsets

private const val MESSAGE_CHARACTER_LIMIT_BYTES = 200
private const val SNIPPET_CHARACTER_LIMIT = 50
private const val ROUNDED_CORNER_PERCENT = 100

@Suppress("LongMethod", "CyclomaticComplexMethod") 
@Composable
fun MessageScreen(
    contactKey: String,
    message: String,
    viewModel: MessageViewModel = hiltViewModel(),
    navigateToNodeDetails: (Int) -> Unit,
    navigateToQuickChatOptions: () -> Unit,
    navigateToChannelSettings: () -> Unit = {},
    onNavigateBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboard.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val nodes by viewModel.nodeList.collectAsStateWithLifecycle()
    val ourNode by viewModel.ourNodeInfo.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val quickChatActions by viewModel.quickChatActions.collectAsStateWithLifecycle(initialValue = emptyList())
    val pagedMessages = viewModel.getMessagesFromPaged(contactKey).collectAsLazyPagingItems()
    val contactSettings by viewModel.contactSettings.collectAsStateWithLifecycle(initialValue = emptyMap())

    var replyingToPacketId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var sharedContact by rememberSaveable { mutableStateOf<Node?>(null) }
    val selectedMessageIds = rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val messageInputState = rememberTextFieldState(message)
    var selectedPriority by rememberSaveable { mutableStateOf(MeshPacket.Priority.DEFAULT_VALUE) }
    val showQuickChat by viewModel.showQuickChat.collectAsStateWithLifecycle()
    val pttRecording by viewModel.pttRecording.collectAsStateWithLifecycle()

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    var currentRetryEvent by remember { mutableStateOf<RetryEvent?>(null) }

    LaunchedEffect(contactKey) {
        viewModel.retryEvents.collect { event ->
            currentRetryEvent = event
        }
    }

    LaunchedEffect(contactKey) { focusManager.clearFocus() }

    val channelInfo =
        remember(contactKey, channels) {
            val index = contactKey.firstOrNull()?.digitToIntOrNull()
            val id = contactKey.substring(1)
            val name = index?.let { channels.getChannel(it)?.name } 
            Triple(index, id, name)
        }
    val (channelIndex, nodeId, rawChannelName) = channelInfo
    val unknownChannelText = stringResource(Res.string.unknown_channel)
    val channelName = rawChannelName ?: unknownChannelText

    val title =
        remember(nodeId, channelName, viewModel) {
            when (nodeId) {
                DataPacket.ID_BROADCAST -> channelName
                else -> viewModel.getUser(nodeId).longName
            }
        }

    val isMismatchKey =
        remember(channelIndex, nodeId, viewModel) {
            channelIndex == DataPacket.PKC_CHANNEL_INDEX && viewModel.getNode(nodeId).mismatchKey
        }

    val inSelectionMode by remember { derivedStateOf { selectedMessageIds.value.isNotEmpty() } }

    val listState = rememberLazyListState()

    val lastReadMessageTimestamp by
        remember(contactKey, contactSettings) {
            derivedStateOf { contactSettings[contactKey]?.lastReadMessageTimestamp }
        }

    val hasUnreadMessages by viewModel.hasUnreadMessages(contactKey).collectAsStateWithLifecycle(initialValue = false)
    val firstUnreadMessageUuid by
        viewModel.getFirstUnreadMessageUuid(contactKey).collectAsStateWithLifecycle(initialValue = null)

    var hasPerformedInitialScroll by rememberSaveable(contactKey) { mutableStateOf(false) }

    val firstUnreadIndex by
        remember(pagedMessages.itemCount, firstUnreadMessageUuid) {
            derivedStateOf {
                firstUnreadMessageUuid?.let { uuid ->
                    (0 until pagedMessages.itemCount).firstOrNull { index -> pagedMessages[index]?.uuid == uuid }
                }
            }
        }

    LaunchedEffect(hasPerformedInitialScroll, firstUnreadIndex, pagedMessages.itemCount) {
        if (hasPerformedInitialScroll || pagedMessages.itemCount == 0) return@LaunchedEffect

        val shouldScrollToUnread = hasUnreadMessages && firstUnreadIndex != null
        if (shouldScrollToUnread) {
            val targetIndex = (firstUnreadIndex!! - (UnreadUiDefaults.VISIBLE_CONTEXT_COUNT - 1)).coerceAtLeast(0)
            listState.smartScrollToIndex(coroutineScope = coroutineScope, targetIndex = targetIndex)
            hasPerformedInitialScroll = true
        } else if (!hasUnreadMessages) {
            
            listState.scrollToItem(0)
            hasPerformedInitialScroll = true
        }
    }

    val onEvent: (MessageScreenEvent) -> Unit =
        remember(viewModel, contactKey, messageInputState, ourNode) {
            fun handle(event: MessageScreenEvent) {
                when (event) {
                    is MessageScreenEvent.SendMessage -> {
                        viewModel.sendMessage(event.text, contactKey, event.replyingToPacketId, event.priority)
                        if (event.replyingToPacketId != null) replyingToPacketId = null
                        messageInputState.clearText()
                    }

                    is MessageScreenEvent.SendReaction ->
                        viewModel.sendReaction(event.emoji, event.messageId, contactKey)

                    is MessageScreenEvent.DeleteMessages -> {
                        viewModel.deleteMessages(event.ids)
                        selectedMessageIds.value = emptySet()
                        showDeleteDialog = false
                    }

                    is MessageScreenEvent.ClearUnreadCount ->
                        viewModel.clearUnreadCount(contactKey, event.messageUuid, event.lastReadTimestamp)

                    is MessageScreenEvent.NodeDetails -> navigateToNodeDetails(event.node.num)

                    is MessageScreenEvent.SetTitle -> viewModel.setTitle(event.title)
                    is MessageScreenEvent.NavigateToNodeDetails -> navigateToNodeDetails(event.nodeNum)
                    MessageScreenEvent.NavigateBack -> onNavigateBack()
                    is MessageScreenEvent.CopyToClipboard -> {
                        clipboardManager.nativeClipboard.setPrimaryClip(ClipData.newPlainText(event.text, event.text))
                        selectedMessageIds.value = emptySet()
                    }
                }
            }

            ::handle
        }

    if (showDeleteDialog) {
        DeleteMessageDialog(
            count = selectedMessageIds.value.size,
            onConfirm = { onEvent(MessageScreenEvent.DeleteMessages(selectedMessageIds.value.toList())) },
            onDismiss = { showDeleteDialog = false },
        )
    }

    sharedContact?.let { contact -> SharedContactDialog(contact = contact, onDismiss = { sharedContact = null }) }

    currentRetryEvent?.let { event ->
        RetryConfirmationDialog(
            retryEvent = event,
            countdownSeconds = 5,
            onConfirm = {
                
                viewModel.respondToRetry(event.packetId, shouldRetry = true)
                currentRetryEvent = null
            },
            onCancel = {
                
                viewModel.respondToRetry(event.packetId, shouldRetry = false)
                currentRetryEvent = null
            },
            onTimeout = {
                
                viewModel.respondToRetry(event.packetId, shouldRetry = true)
                currentRetryEvent = null
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (inSelectionMode) {
                ActionModeTopBar(
                    selectedCount = selectedMessageIds.value.size,
                    onAction = { action ->
                        when (action) {
                            MessageMenuAction.ClipboardCopy -> {
                                val copiedText =
                                    (0 until pagedMessages.itemCount)
                                        .mapNotNull { pagedMessages[it] }
                                        .filter { it.uuid in selectedMessageIds.value }
                                        .joinToString("\n") { it.text }
                                onEvent(MessageScreenEvent.CopyToClipboard(copiedText))
                            }

                            MessageMenuAction.Delete -> showDeleteDialog = true
                            MessageMenuAction.Dismiss -> selectedMessageIds.value = emptySet()
                            MessageMenuAction.SelectAll -> {

                                selectedMessageIds.value =
                                    if (selectedMessageIds.value.size == pagedMessages.itemCount) {
                                        emptySet()
                                    } else {
                                        (0 until pagedMessages.itemCount).mapNotNull { pagedMessages[it]?.uuid }.toSet()
                                    }
                            }
                        }
                    },
                )
            } else {
                MessageTopBar(
                    title = title,
                    channelIndex = channelIndex,
                    mismatchKey = isMismatchKey,
                    onNavigateBack = { onEvent(MessageScreenEvent.NavigateBack) },
                    channels = channels,
                    channelIndexParam = channelIndex,
                    showQuickChat = showQuickChat,
                    onToggleQuickChat = viewModel::toggleShowQuickChat,
                    onNavigateToQuickChatOptions = navigateToQuickChatOptions,
                    onNavigateToChannelSettings = navigateToChannelSettings,
                )
            }
        },
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).focusable()) {
            Box(modifier = Modifier.weight(1f)) {
                MessageListPaged(
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                    state =
                    MessageListPagedState(
                        nodes = nodes,
                        ourNode = ourNode,
                        messages = pagedMessages,
                        selectedIds = selectedMessageIds,
                        contactKey = contactKey,
                        firstUnreadMessageUuid = firstUnreadMessageUuid,
                        hasUnreadMessages = hasUnreadMessages,
                    ),
                    handlers =
                    MessageListHandlers(
                        onUnreadChanged = { messageUuid, timestamp ->
                            onEvent(MessageScreenEvent.ClearUnreadCount(messageUuid, timestamp))
                        },
                        onSendReaction = { emoji, id -> onEvent(MessageScreenEvent.SendReaction(emoji, id)) },
                        onClickChip = { onEvent(MessageScreenEvent.NodeDetails(it)) },
                        onDeleteMessages = { viewModel.deleteMessages(it) },
                        onSendMessage = { text, key -> viewModel.sendMessage(text, key) },
                        onReply = { message -> replyingToPacketId = message?.packetId },
                        onPlayVoice = viewModel::playVoiceMessage,
                    ),
                    quickEmojis = viewModel.frequentEmojis,
                )
                
                if (listState.canScrollBackward) {
                    ScrollToBottomFab(coroutineScope, listState)
                }
            }
            AnimatedVisibility(visible = showQuickChat) {
                QuickChatRow(
                    enabled = connectionState.isConnected(),
                    actions = quickChatActions,
                    onClick = { action ->
                        handleQuickChatAction(
                            action = action,
                            messageInputState = messageInputState,
                            onSendMessage = { text -> onEvent(MessageScreenEvent.SendMessage(text)) },
                        )
                    },
                )
            }
            val originalMessage by
                remember(replyingToPacketId, pagedMessages.itemCount) {
                    derivedStateOf {
                        replyingToPacketId?.let { id ->
                            (0 until pagedMessages.itemCount).firstNotNullOfOrNull { index ->
                                pagedMessages[index]?.takeIf { it.packetId == id }
                            }
                        }
                    }
                }
            ReplySnippet(
                originalMessage = originalMessage,
                onClearReply = { replyingToPacketId = null },
                ourNode = ourNode,
            )
            MessagePrioritySelector(
                selectedPriority = selectedPriority,
                onPrioritySelected = { selectedPriority = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MessageInput(
                    modifier = Modifier.weight(1f),
                    isEnabled = connectionState.isConnected(),
                    textFieldState = messageInputState,
                    onSendMessage = {
                        val messageText = messageInputState.text.toString().trim()
                        if (messageText.isNotEmpty()) {
                            onEvent(MessageScreenEvent.SendMessage(messageText, replyingToPacketId, selectedPriority))
                            selectedPriority = MeshPacket.Priority.DEFAULT_VALUE
                        }
                    },
                )
                val isConnected = connectionState.isConnected()
                Surface(
                    shape = CircleShape,
                    color = if (pttRecording) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(48.dp)
                        .pointerInput(isConnected) {
                            detectTapGestures(onPress = {
                                if (!isConnected) return@detectTapGestures
                                val hasMic = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasMic) {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@detectTapGestures
                                }
                                viewModel.startPttRecording()
                                tryAwaitRelease()
                                viewModel.stopPttAndSend(contactKey)
                            })
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Push to talk",
                            tint = if (pttRecording) MaterialTheme.colorScheme.onError
                                   else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ScrollToBottomFab(coroutineScope: CoroutineScope, listState: LazyListState) {
    FloatingActionButton(
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        onClick = {
            coroutineScope.launch {
                
                listState.animateScrollToItem(0)
            }
        },
    ) {
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = stringResource(Res.string.scroll_to_bottom),
        )
    }
}

@Composable
private fun ReplySnippet(originalMessage: Message?, onClearReply: () -> Unit, ourNode: Node?) {
    AnimatedVisibility(visible = originalMessage != null) {
        originalMessage?.let { message ->
            val isFromLocalUser = message.fromLocal
            val replyingToNodeUser = if (isFromLocalUser) ourNode?.user else message.node.user
            val unknownUserText = stringResource(Res.string.unknown)

            Row(
                modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Reply,
                    contentDescription = stringResource(Res.string.reply), 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.replying_to, replyingToNodeUser?.shortName ?: unknownUserText),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = message.text.ellipsize(SNIPPET_CHARACTER_LIMIT),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onClearReply) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.cancel_reply), 
                    )
                }
            }
        }
    }
}

private fun String.ellipsize(maxLength: Int): String = if (length > maxLength) "${take(maxLength)}…" else this

private fun handleQuickChatAction(
    action: QuickChatAction,
    messageInputState: TextFieldState,
    onSendMessage: (String) -> Unit,
) {
    when (action.mode) {
        QuickChatAction.Mode.Append -> {
            val originalText = messageInputState.text.toString()
            
            if (!originalText.contains(action.message)) {
                val newText =
                    buildString {
                        append(originalText)
                        if (originalText.isNotEmpty() && !originalText.endsWith(' ')) {
                            append(' ')
                        }
                        append(action.message)
                    }
                        .limitBytes(MESSAGE_CHARACTER_LIMIT_BYTES)
                messageInputState.setTextAndPlaceCursorAtEnd(newText)
            }
        }

        QuickChatAction.Mode.Instant -> {
            
            onSendMessage(action.message)
        }
    }
}

private fun String.limitBytes(maxBytes: Int): String {
    val bytes = this.toByteArray(StandardCharsets.UTF_8)
    if (bytes.size <= maxBytes) {
        return this
    }

    var currentBytesSum = 0
    var validCharCount = 0
    for (charIndex in this.indices) {
        val charToTest = this[charIndex]
        val charBytes = charToTest.toString().toByteArray(StandardCharsets.UTF_8).size
        if (currentBytesSum + charBytes > maxBytes) {
            break
        }
        currentBytesSum += charBytes
        validCharCount++
    }
    return this.substring(0, validCharCount)
}

@Composable
private fun DeleteMessageDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val deleteMessagesString = pluralStringResource(Res.plurals.delete_messages, count, count)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text(stringResource(Res.string.delete_messages_title)) },
        text = { Text(text = deleteMessagesString) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

internal sealed class MessageMenuAction {
    data object ClipboardCopy : MessageMenuAction()

    data object Delete : MessageMenuAction()

    data object Dismiss : MessageMenuAction()

    data object SelectAll : MessageMenuAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionModeTopBar(selectedCount: Int, onAction: (MessageMenuAction) -> Unit) = TopAppBar(
    title = { Text(text = selectedCount.toString()) },
    navigationIcon = {
        IconButton(onClick = { onAction(MessageMenuAction.Dismiss) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.clear_selection),
            )
        }
    },
    actions = {
        IconButton(onClick = { onAction(MessageMenuAction.ClipboardCopy) }) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = stringResource(Res.string.copy))
        }
        IconButton(onClick = { onAction(MessageMenuAction.Delete) }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
        }
        IconButton(onClick = { onAction(MessageMenuAction.SelectAll) }) {
            Icon(imageVector = Icons.Default.SelectAll, contentDescription = stringResource(Res.string.select_all))
        }
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageTopBar(
    title: String,
    channelIndex: Int?,
    mismatchKey: Boolean,
    onNavigateBack: () -> Unit,
    channels: AppOnlyProtos.ChannelSet?,
    channelIndexParam: Int?,
    showQuickChat: Boolean,
    onToggleQuickChat: () -> Unit,
    onNavigateToQuickChatOptions: () -> Unit = {},
    onNavigateToChannelSettings: () -> Unit = {},
) = TopAppBar(
    title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(10.dp))

            if (channels != null && channelIndexParam != null) {
                SecurityIcon(channels, channelIndexParam)
            }
        }
    },
    navigationIcon = {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.navigate_back),
            )
        }
    },
    actions = {
        MessageTopBarActions(
            showQuickChat,
            onToggleQuickChat,
            onNavigateToQuickChatOptions,
            channelIndex,
            mismatchKey,
            onNavigateToChannelSettings,
        )
    },
)

@Composable
private fun MessageTopBarActions(
    showQuickChat: Boolean,
    onToggleQuickChat: () -> Unit,
    onNavigateToQuickChatOptions: () -> Unit,
    channelIndex: Int?,
    mismatchKey: Boolean,
    onNavigateToChannelSettings: () -> Unit = {},
) {
    if (channelIndex == DataPacket.PKC_CHANNEL_INDEX) {
        NodeKeyStatusIcon(hasPKC = true, mismatchKey = mismatchKey)
    }
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = true) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(Res.string.overflow_menu))
        }
        OverFlowMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            showQuickChat = showQuickChat,
            onToggleQuickChat = onToggleQuickChat,
            onNavigateToQuickChatOptions = onNavigateToQuickChatOptions,
            onNavigateToChannelSettings = onNavigateToChannelSettings,
        )
    }
}

@Composable
private fun OverFlowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    showQuickChat: Boolean,
    onToggleQuickChat: () -> Unit,
    onNavigateToQuickChatOptions: () -> Unit,
    onNavigateToChannelSettings: () -> Unit = {},
) {
    if (expanded) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            val quickChatToggleTitle =
                if (showQuickChat) {
                    stringResource(Res.string.quick_chat_hide)
                } else {
                    stringResource(Res.string.quick_chat_show)
                }
            DropdownMenuItem(
                text = { Text(quickChatToggleTitle) },
                onClick = {
                    onDismiss()
                    onToggleQuickChat()
                },
                leadingIcon = {
                    Icon(
                        imageVector =
                        if (showQuickChat) {
                            Icons.Default.SpeakerNotesOff
                        } else {
                            Icons.Default.SpeakerNotes
                        },
                        contentDescription = quickChatToggleTitle,
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.quick_chat)) },
                onClick = {
                    onDismiss()
                    onNavigateToQuickChatOptions()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = stringResource(Res.string.quick_chat),
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.channels)) },
                onClick = {
                    onDismiss()
                    onNavigateToChannelSettings()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(Res.string.channels),
                    )
                },
            )
        }
    }
}

@Composable
private fun QuickChatRow(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    actions: List<QuickChatAction>,
    onClick: (QuickChatAction) -> Unit,
) {
    val alertActionMessage = stringResource(Res.string.alert_bell_text)
    val alertAction =
        remember(alertActionMessage) {
            
            QuickChatAction(
                name = "🔔",
                message = "🔔 $alertActionMessage  ", 
                mode = QuickChatAction.Mode.Append,
                position = -1, 
            )
        }

    val allActions = remember(alertAction, actions) { listOf(alertAction) + actions }

    LazyRow(modifier = modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(allActions, key = { it.uuid }) { action ->
            Button(onClick = { onClick(action) }, enabled = enabled) { Text(text = action.name) }
        }
    }
}

@Composable
private fun MessagePrioritySelector(
    selectedPriority: Int,
    onPrioritySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember {
        listOf(
            "Normal" to MeshPacket.Priority.DEFAULT_VALUE,
            "High" to MeshPacket.Priority.HIGH_VALUE,
            "Critical Alert" to MeshPacket.Priority.ALERT_VALUE,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Priority:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        options.forEach { (label, value) ->
            val isSelected = selectedPriority == value
            val isCritical = value == MeshPacket.Priority.ALERT_VALUE
            FilterChip(
                selected = isSelected,
                onClick = { onPrioritySelected(value) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = if (isCritical && isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    null
                },
                colors = if (isCritical) {
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    FilterChipDefaults.filterChipColors()
                },
            )
        }
    }
}

private const val MAX_LINES = 3

@Suppress("LongMethod") 
@Composable
private fun MessageInput(
    isEnabled: Boolean,
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    maxByteSize: Int = MESSAGE_CHARACTER_LIMIT_BYTES,
    onSendMessage: () -> Unit,
) {
    val currentText = textFieldState.text.toString()
    val currentByteLength =
        remember(currentText) {
            
            currentText.toByteArray(StandardCharsets.UTF_8).size
        }

    val isOverLimit = currentByteLength > maxByteSize
    val canSend = !isOverLimit && currentText.isNotEmpty() && isEnabled

    OutlinedTextField(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        state = textFieldState,
        lineLimits = TextFieldLineLimits.MultiLine(1, MAX_LINES),
        label = { Text(stringResource(Res.string.message_input_label)) },
        enabled = isEnabled,
        shape = RoundedCornerShape(ROUNDED_CORNER_PERCENT.toFloat()),
        isError = isOverLimit,
        placeholder = { Text(stringResource(Res.string.type_a_message)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        supportingText = {
            if (isEnabled) { 
                Text(
                    text = "$currentByteLength/$maxByteSize",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                    if (isOverLimit) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        },

        trailingIcon = {
            IconButton(onClick = { if (canSend) onSendMessage() }, enabled = canSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = stringResource(Res.string.send),
                )
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun MessageInputPreview() {
    AppTheme {
        Surface {
            Column(modifier = Modifier.padding(8.dp)) {
                MessageInput(isEnabled = true, textFieldState = rememberTextFieldState("Hello"), onSendMessage = {})
                Spacer(Modifier.size(16.dp))
                MessageInput(isEnabled = false, textFieldState = rememberTextFieldState("Disabled"), onSendMessage = {})
                Spacer(Modifier.size(16.dp))
                MessageInput(
                    isEnabled = true,
                    textFieldState =
                    rememberTextFieldState(
                        "A very long message that might exceed the byte limit " +
                            "and cause an error state display for the user to see clearly.",
                    ),
                    onSendMessage = {},
                    maxByteSize = 50, 
                )
                Spacer(Modifier.size(16.dp))
                
                MessageInput(
                    isEnabled = true,
                    textFieldState = rememberTextFieldState("こんにちは世界"), 
                    onSendMessage = {},
                    maxByteSize = 10,

                )
            }
        }
    }
}

