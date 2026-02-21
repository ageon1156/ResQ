

package org.meshtastic.core.prefs.analytics

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.meshtastic.core.prefs.NullableStringPrefDelegate
import org.meshtastic.core.prefs.PrefDelegate
import org.meshtastic.core.prefs.di.AnalyticsSharedPreferences
import org.meshtastic.core.prefs.di.AppSharedPreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsPrefs {
    
    var analyticsAllowed: Boolean

    fun getAnalyticsAllowedChangesFlow(): Flow<Boolean>

    val installId: String

    companion object {
        
        const val KEY_ANALYTICS_ALLOWED = "allowed"

        const val ANALYTICS_PREFS_NAME = "analytics-prefs"
    }
}

@Singleton
class AnalyticsPrefsImpl
@Inject
constructor(
    @AnalyticsSharedPreferences private val analyticsSharedPreferences: SharedPreferences,
    @AppSharedPreferences appPrefs: SharedPreferences,
) : AnalyticsPrefs {
    override var analyticsAllowed: Boolean by
        PrefDelegate(analyticsSharedPreferences, AnalyticsPrefs.KEY_ANALYTICS_ALLOWED, true)

    private var _installId: String? by NullableStringPrefDelegate(appPrefs, "appPrefs_install_id", null)

    override val installId: String
        get() = _installId ?: UUID.randomUUID().toString().also { _installId = it }

    override fun getAnalyticsAllowedChangesFlow(): Flow<Boolean> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == AnalyticsPrefs.KEY_ANALYTICS_ALLOWED) {
                    trySend(analyticsAllowed)
                }
            }
        
        trySend(analyticsAllowed)
        analyticsSharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { analyticsSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}

