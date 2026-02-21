

package org.meshtastic.core.ui.util

import android.content.Context
import android.widget.Toast
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

suspend fun Context.showToast(stringResource: StringResource) {
    showToast(getString(stringResource))
}

suspend fun Context.showToast(stringResource: StringResource, vararg formatArgs: Any) {
    Toast.makeText(this, getString(stringResource, formatArgs), Toast.LENGTH_SHORT).show()
}

suspend fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

