

package org.meshtastic.feature.settings.radio.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.protobuf.MessageLite

class ConfigState<T : MessageLite>(private val initialValue: T) {
    var value by mutableStateOf(initialValue)

    val isDirty: Boolean
        get() = value != initialValue

    fun reset() {
        value = initialValue
    }

    companion object {
        fun <T : MessageLite> saver(initialValue: T): Saver<ConfigState<T>, ByteArray> = Saver(
            save = { it.value.toByteArray() },
            restore = {
                ConfigState(initialValue).apply {
                    @Suppress("UNCHECKED_CAST")
                    value = initialValue.parserForType.parseFrom(it) as T
                }
            },
        )
    }
}

@Composable
fun <T : MessageLite> rememberConfigState(initialValue: T): ConfigState<T> =
    rememberSaveable(initialValue, saver = ConfigState.saver(initialValue)) { ConfigState(initialValue) }

