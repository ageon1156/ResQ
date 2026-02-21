

package org.meshtastic.feature.node.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.add_a_note
import org.meshtastic.core.strings.notes
import org.meshtastic.core.strings.save

@Composable
fun NotesSection(node: Node, onSaveNotes: (Int, String) -> Unit, modifier: Modifier = Modifier) {
    if (node.isFavorite) {
        ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.notes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(16.dp))

                val originalNotes = node.notes
                var notes by remember(node.notes) { mutableStateOf(node.notes) }
                val edited = notes.trim() != originalNotes.trim()
                val keyboardController = LocalSoftwareKeyboardController.current

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.add_a_note)) },
                    shape = MaterialTheme.shapes.large,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onSaveNotes(node.num, notes.trim())
                                keyboardController?.hide()
                            },
                            enabled = edited,
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = stringResource(Res.string.save))
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                    KeyboardActions(
                        onDone = {
                            onSaveNotes(node.num, notes.trim())
                            keyboardController?.hide()
                        },
                    ),
                )
            }
        }
    }
}

