

package com.meshtastic.core.strings

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource

fun getString(stringResource: StringResource): String = runBlocking {
    org.jetbrains.compose.resources.getString(stringResource)
}

fun getString(stringResource: StringResource, vararg formatArgs: Any): String = runBlocking {
    org.jetbrains.compose.resources.getString(stringResource, *formatArgs)
}

