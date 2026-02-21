
package org.meshtastic.feature.settings.radio

import org.meshtastic.feature.settings.util.UiText

sealed class ResponseState<out T> {
    data object Empty : ResponseState<Nothing>()

    data class Loading(var total: Int = 1, var completed: Int = 0, var status: String? = null) :
        ResponseState<Nothing>()

    data class Success<T>(val result: T) : ResponseState<T>()

    data class Error(val error: UiText) : ResponseState<Nothing>()

    fun isWaiting() = this !is Empty
}

