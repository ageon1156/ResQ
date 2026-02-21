

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.protobuf.ByteString
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Channel
import org.meshtastic.core.model.util.encodeToString
import org.meshtastic.core.model.util.toByteString
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.error
import org.meshtastic.core.strings.reset

@Suppress("LongMethod")
@Composable
fun EditBase64Preference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    value: ByteString,
    enabled: Boolean,
    readOnly: Boolean = false,
    keyboardActions: KeyboardActions,
    onValueChange: (ByteString) -> Unit,
    onGenerateKey: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    var valueState by remember { mutableStateOf(value.encodeToString()) }
    val isError = value.encodeToString() != valueState

    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!isFocused) {
            valueState = value.encodeToString()
        }
    }

    val (icon, description) =
        when {
            isError -> Icons.TwoTone.Close to stringResource(Res.string.error)
            onGenerateKey != null && !isFocused -> Icons.TwoTone.Refresh to stringResource(Res.string.reset)
            else -> null to null
        }
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = valueState,
            onValueChange = {
                valueState = it
                runCatching { it.toByteString() }.onSuccess(onValueChange)
            },
            modifier = Modifier.fillMaxWidth().onFocusChanged { focusState -> isFocused = focusState.isFocused },
            enabled = enabled,
            readOnly = readOnly,
            label = { Text(text = title) },
            isError = isError,
            keyboardOptions =
            KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = keyboardActions,
            trailingIcon = {
                if (icon != null) {
                    IconButton(
                        onClick = {
                            if (isError) {
                                valueState = value.encodeToString()
                                onValueChange(value)
                            } else if (onGenerateKey != null && !isFocused) {
                                onGenerateKey()
                            }
                        },
                        enabled = enabled,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = description,
                            tint =
                            if (isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                LocalContentColor.current
                            },
                        )
                    }
                } else if (trailingIcon != null) {
                    trailingIcon()
                }
            },
        )
        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditBase64PreferencePreview() {
    EditBase64Preference(
        title = "Title",
        summary = "This is a summary",
        value = Channel.getRandomKey(),
        enabled = true,
        keyboardActions = KeyboardActions {},
        onValueChange = {},
        onGenerateKey = {},
        modifier = Modifier.padding(16.dp),
    )
}

