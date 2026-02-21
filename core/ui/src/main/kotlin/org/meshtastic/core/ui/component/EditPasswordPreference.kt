

package org.meshtastic.core.ui.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.hide_password
import org.meshtastic.core.strings.show_password

@Composable
fun EditPasswordPreference(
    title: String,
    value: String,
    maxSize: Int,
    enabled: Boolean,
    keyboardActions: KeyboardActions,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    EditTextPreference(
        title = title,
        value = value,
        maxSize = maxSize,
        enabled = enabled,
        isError = false,
        keyboardOptions =
        KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = keyboardActions,
        onValueChanged = { onValueChanged(it) },
        onFocusChanged = {},
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.TwoTone.VisibilityOff else Icons.TwoTone.VisibilityOff,
                    contentDescription =
                    if (isPasswordVisible) {
                        stringResource(Res.string.hide_password)
                    } else {
                        stringResource(Res.string.show_password)
                    },
                )
            }
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun EditPasswordPreferencePreview() {
    EditPasswordPreference(
        title = "Password",
        value = "top secret",
        maxSize = 63,
        enabled = true,
        keyboardActions = KeyboardActions {},
        onValueChanged = {},
    )
}

