

package org.meshtastic.core.prefs.emoji

import android.content.SharedPreferences
import org.meshtastic.core.prefs.NullableStringPrefDelegate
import org.meshtastic.core.prefs.di.CustomEmojiSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

interface CustomEmojiPrefs {
    var customEmojiFrequency: String?
}

@Singleton
class CustomEmojiPrefsImpl @Inject constructor(@CustomEmojiSharedPreferences prefs: SharedPreferences) :
    CustomEmojiPrefs {
    override var customEmojiFrequency: String? by NullableStringPrefDelegate(prefs, "pref_key_custom_emoji_freq", null)
}

