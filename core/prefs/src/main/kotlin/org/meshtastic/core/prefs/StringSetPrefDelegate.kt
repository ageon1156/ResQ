

package org.meshtastic.core.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class StringSetPrefDelegate(
    private val prefs: SharedPreferences,
    private val key: String,
    private val defaultValue: Set<String>,
) : ReadWriteProperty<Any?, Set<String>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Set<String> =
        prefs.getStringSet(key, defaultValue) ?: emptySet()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) =
        prefs.edit { putStringSet(key, value) }
}

