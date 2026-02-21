

package org.meshtastic.core.prefs

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FloatPrefDelegate(
    private val preferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Float,
) : ReadWriteProperty<Any?, Float> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float = preferences.getFloat(key, defaultValue)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        preferences.edit().putFloat(key, value).apply()
    }
}

