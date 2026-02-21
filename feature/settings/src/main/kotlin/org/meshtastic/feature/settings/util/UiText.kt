

package org.meshtastic.feature.settings.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Suppress("SpreadOperator")
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    class StringResource(val resId: org.jetbrains.compose.resources.StringResource, vararg val args: Any) : UiText()

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(resId, *args)
    }
}

