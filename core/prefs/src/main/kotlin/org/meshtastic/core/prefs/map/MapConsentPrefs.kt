

package org.meshtastic.core.prefs.map

import android.content.SharedPreferences
import androidx.core.content.edit
import org.meshtastic.core.prefs.di.MapConsentSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

interface MapConsentPrefs {
    fun shouldReportLocation(nodeNum: Int?): Boolean

    fun setShouldReportLocation(nodeNum: Int?, value: Boolean)
}

@Singleton
class MapConsentPrefsImpl @Inject constructor(@MapConsentSharedPreferences private val prefs: SharedPreferences) :
    MapConsentPrefs {
    override fun shouldReportLocation(nodeNum: Int?) = prefs.getBoolean(nodeNum.toString(), false)

    override fun setShouldReportLocation(nodeNum: Int?, value: Boolean) {
        prefs.edit { putBoolean(nodeNum.toString(), value) }
    }
}

