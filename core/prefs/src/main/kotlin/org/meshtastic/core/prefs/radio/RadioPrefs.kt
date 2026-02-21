

package org.meshtastic.core.prefs.radio

import android.content.SharedPreferences
import org.meshtastic.core.prefs.NullableStringPrefDelegate
import org.meshtastic.core.prefs.di.RadioSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

interface RadioPrefs {
    var devAddr: String?
}

fun RadioPrefs.isBle() = devAddr?.startsWith("x") == true

fun RadioPrefs.isSerial() = devAddr?.startsWith("s") == true

fun RadioPrefs.isMock() = devAddr?.startsWith("m") == true

fun RadioPrefs.isTcp() = devAddr?.startsWith("t") == true

fun RadioPrefs.isNoop() = devAddr?.startsWith("n") == true

@Singleton
class RadioPrefsImpl @Inject constructor(@RadioSharedPreferences prefs: SharedPreferences) : RadioPrefs {
    override var devAddr: String? by NullableStringPrefDelegate(prefs, "devAddr2", null)
}

