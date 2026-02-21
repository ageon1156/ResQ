

package com.geeksville.mesh.model

data class Contact(
    val contactKey: String,
    val shortName: String,
    val longName: String,
    val lastMessageTime: String?,
    val lastMessageText: String?,
    val unreadCount: Int,
    val messageCount: Int,
    val isMuted: Boolean,
    val isUnmessageable: Boolean,
    val nodeColors: Pair<Int, Int>? = null,
)

