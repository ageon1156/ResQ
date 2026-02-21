

package org.meshtastic.core.prefs

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class DoublePrefDelegate(
    private val preferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Double,
) : ReadWriteProperty<Any?, Double> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double = preferences
        .getFloat(key, defaultValue.toFloat())
        .toDouble() 

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
        preferences
            .edit()
            .putFloat(key, value.toFloat())
            .apply() 
    }
}

