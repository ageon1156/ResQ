

package org.meshtastic.core.ui.emoji

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.meshtastic.core.prefs.emoji.CustomEmojiPrefs
import javax.inject.Inject

@HiltViewModel
class EmojiPickerViewModel @Inject constructor(private val customEmojiPrefs: CustomEmojiPrefs) : ViewModel() {

    var customEmojiFrequency: String?
        get() = customEmojiPrefs.customEmojiFrequency
        set(value) {
            customEmojiPrefs.customEmojiFrequency = value
        }
}

