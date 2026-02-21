

package org.meshtastic.core.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class PrefDelegate<T>(
    private val prefs: SharedPreferences,
    private val key: String,
    private val defaultValue: T,
) : ReadWriteProperty<Any?, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = when (defaultValue) {
        is String -> (prefs.getString(key, defaultValue) ?: defaultValue) as T
        is Int -> prefs.getInt(key, defaultValue) as T
        is Boolean -> prefs.getBoolean(key, defaultValue) as T
        is Float -> prefs.getFloat(key, defaultValue) as T
        is Long -> prefs.getLong(key, defaultValue) as T
        else -> error("Unsupported type for key '$key': $defaultValue")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        prefs.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> error("Unsupported type for key '$key': $value")
            }
        }
    }
}

