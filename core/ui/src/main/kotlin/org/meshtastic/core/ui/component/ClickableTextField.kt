

package org.meshtastic.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClickableTextField(
    label: StringResource,
    enabled: Boolean,
    trailingIcon: ImageVector,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    if (isPressed) onClick()

    OutlinedTextField(
        value,
        onValueChange = {},
        enabled = enabled,
        readOnly = true,
        label = { Text(stringResource(label)) },
        trailingIcon = { Icon(trailingIcon, null) },
        isError = isError,
        interactionSource = source,
        modifier = modifier,
    )
}

