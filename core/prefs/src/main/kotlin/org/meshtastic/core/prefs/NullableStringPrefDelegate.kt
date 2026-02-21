

package org.meshtastic.core.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class NullableStringPrefDelegate(
    private val prefs: SharedPreferences,
    private val key: String,
    private val defaultValue: String?,
) : ReadWriteProperty<Any?, String?> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): String? = prefs.getString(key, defaultValue)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        prefs.edit {
            when (value) {
                null -> remove(key)
                else -> putString(key, value)
            }
        }
    }
}

