
package org.meshtastic.core.prefs.meshlog

import android.content.SharedPreferences
import org.meshtastic.core.prefs.PrefDelegate
import org.meshtastic.core.prefs.di.MeshLogSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

interface MeshLogPrefs {
    var retentionDays: Int
    var loggingEnabled: Boolean

    companion object {
        const val RETENTION_DAYS_KEY = "meshlog_retention_days"
        const val LOGGING_ENABLED_KEY = "meshlog_logging_enabled"
        const val DEFAULT_RETENTION_DAYS = 30
        const val DEFAULT_LOGGING_ENABLED = true
        const val MIN_RETENTION_DAYS = -1 
        const val MAX_RETENTION_DAYS = 365
        const val NEVER_CLEAR_RETENTION_DAYS = 0
        const val ONE_HOUR_RETENTION_DAYS = -1
    }
}

@Singleton
class MeshLogPrefsImpl @Inject constructor(@MeshLogSharedPreferences private val prefs: SharedPreferences) :
    MeshLogPrefs {
    override var retentionDays: Int by
        PrefDelegate(
            prefs = prefs,
            key = MeshLogPrefs.RETENTION_DAYS_KEY,
            defaultValue = MeshLogPrefs.DEFAULT_RETENTION_DAYS,
        )
    override var loggingEnabled: Boolean by
        PrefDelegate(
            prefs = prefs,
            key = MeshLogPrefs.LOGGING_ENABLED_KEY,
            defaultValue = MeshLogPrefs.DEFAULT_LOGGING_ENABLED,
        )
}

