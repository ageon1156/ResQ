

package org.meshtastic.core.analytics

class DataPair(val name: String, val valueIn: Any?) {
    val value: Any = valueIn ?: "null"
}

