

package org.meshtastic.core.ui.share

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.cancel
import org.meshtastic.core.strings.import_known_shared_contact_text
import org.meshtastic.core.strings.import_label
import org.meshtastic.core.strings.import_shared_contact
import org.meshtastic.core.strings.public_key_changed
import org.meshtastic.core.ui.component.SimpleAlertDialog
import org.meshtastic.core.ui.component.compareUsers
import org.meshtastic.core.ui.component.userFieldsToString
import org.meshtastic.proto.AdminProtos

@Composable
fun SharedContactDialog(
    sharedContact: AdminProtos.SharedContact,
    onDismiss: () -> Unit,
    viewModel: SharedContactViewModel = hiltViewModel(),
) {
    val unfilteredNodes by viewModel.unfilteredNodes.collectAsStateWithLifecycle()

    val nodeNum = sharedContact.nodeNum
    val node = unfilteredNodes.find { it.num == nodeNum }

    SimpleAlertDialog(
        title = Res.string.import_shared_contact,
        text = {
            Column {
                if (node != null) {
                    Text(text = stringResource(Res.string.import_known_shared_contact_text))
                    if (node.user.publicKey.size() > 0 && node.user.publicKey != sharedContact.user?.publicKey) {
                        Text(
                            text = stringResource(Res.string.public_key_changed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    HorizontalDivider()
                    Text(text = compareUsers(node.user, sharedContact.user))
                } else {
                    Text(text = userFieldsToString(sharedContact.user))
                }
            }
        },
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        confirmText = stringResource(Res.string.import_label),
        onConfirm = {
            viewModel.addSharedContact(sharedContact)
            onDismiss()
        },
    )
}

