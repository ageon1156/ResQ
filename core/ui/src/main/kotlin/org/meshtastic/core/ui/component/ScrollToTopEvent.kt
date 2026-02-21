

package org.meshtastic.core.ui.component

sealed class ScrollToTopEvent {
    data object NodesTabPressed : ScrollToTopEvent()

    data object ConversationsTabPressed : ScrollToTopEvent()
}

