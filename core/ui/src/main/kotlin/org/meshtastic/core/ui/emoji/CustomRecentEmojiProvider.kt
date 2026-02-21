

package org.meshtastic.core.ui.emoji

import androidx.emoji2.emojipicker.RecentEmojiAsyncProvider
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class CustomRecentEmojiProvider(
    private val customEmojiFrequency: String?,
    private val onUpdateCustomEmojiFrequency: (updatedValue: String) -> Unit,
) : RecentEmojiAsyncProvider {

    private val emoji2Frequency: MutableMap<String, Int> by lazy {
        customEmojiFrequency
            ?.split(SPLIT_CHAR)
            ?.associate { entry ->
                entry.split(KEY_VALUE_DELIMITER, limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1].toInt() }
                    ?: ("" to 0)
            }
            ?.toMutableMap() ?: mutableMapOf()
    }

    override fun getRecentEmojiListAsync(): ListenableFuture<List<String>> =
        Futures.immediateFuture(emoji2Frequency.toList().sortedByDescending { it.second }.map { it.first })

    override fun recordSelection(emoji: String) {
        emoji2Frequency[emoji] = (emoji2Frequency[emoji] ?: 0) + 1
        onUpdateCustomEmojiFrequency(emoji2Frequency.entries.joinToString(SPLIT_CHAR))
    }

    companion object {
        private const val SPLIT_CHAR = ","
        private const val KEY_VALUE_DELIMITER = "="
    }
}

