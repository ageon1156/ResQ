

package org.meshtastic.core.prefs.ui

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.meshtastic.core.prefs.PrefDelegate
import org.meshtastic.core.prefs.di.UiSharedPreferences
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface UiPrefs {
    var hasShownNotPairedWarning: Boolean
    var showQuickChat: Boolean

    fun shouldProvideNodeLocation(nodeNum: Int): StateFlow<Boolean>

    fun setShouldProvideNodeLocation(nodeNum: Int, value: Boolean)
}

@Singleton
class UiPrefsImpl @Inject constructor(@UiSharedPreferences private val prefs: SharedPreferences) : UiPrefs {

    private val provideNodeLocationFlows = ConcurrentHashMap<Int, MutableStateFlow<Boolean>>()

    private val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                
                else ->
                    provideNodeLocationFlows.keys.forEach { nodeNum ->
                        if (key == provideLocationKey(nodeNum)) {
                            val newValue = sharedPreferences.getBoolean(key, true)
                            provideNodeLocationFlows[nodeNum]?.tryEmit(newValue)
                        }
                    }
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

    override var hasShownNotPairedWarning: Boolean by PrefDelegate(prefs, "has_shown_not_paired_warning", false)
    override var showQuickChat: Boolean by PrefDelegate(prefs, "show-quick-chat", false)

    override fun shouldProvideNodeLocation(nodeNum: Int): StateFlow<Boolean> = provideNodeLocationFlows
        .getOrPut(nodeNum) { MutableStateFlow(prefs.getBoolean(provideLocationKey(nodeNum), true)) }
        .asStateFlow()

    override fun setShouldProvideNodeLocation(nodeNum: Int, value: Boolean) {
        prefs.edit { putBoolean(provideLocationKey(nodeNum), value) }
        provideNodeLocationFlows[nodeNum]?.tryEmit(value)
    }

    private fun provideLocationKey(nodeNum: Int) = "provide-location-$nodeNum"
}

