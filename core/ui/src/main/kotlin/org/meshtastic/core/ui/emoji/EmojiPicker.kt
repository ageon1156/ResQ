
package org.meshtastic.core.ui.emoji

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.RecentEmojiProviderAdapter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.meshtastic.core.ui.component.BottomSheetDialog

@Composable
fun EmojiPicker(
    viewModel: EmojiPickerViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
    onConfirm: (String) -> Unit,
) {
    BackHandler { onDismiss() }
    AndroidView(
        factory = { context ->
            androidx.emoji2.emojipicker.EmojiPickerView(context).apply {
                clipToOutline = true
                setRecentEmojiProvider(
                    RecentEmojiProviderAdapter(
                        CustomRecentEmojiProvider(viewModel.customEmojiFrequency) { updatedValue ->
                            viewModel.customEmojiFrequency = updatedValue
                        },
                    ),
                )
                setOnEmojiPickedListener { emoji ->
                    onDismiss()
                    onConfirm(emoji.emoji)
                }
            }
        },
        modifier = Modifier.fillMaxWidth().wrapContentHeight().verticalScroll(rememberScrollState()),
    )
}

@Composable
fun EmojiPickerDialog(onDismiss: () -> Unit = {}, onConfirm: (String) -> Unit) =
    BottomSheetDialog(onDismiss = onDismiss, modifier = Modifier.fillMaxHeight(fraction = .4f)) {
        EmojiPicker(onConfirm = onConfirm, onDismiss = onDismiss)
    }

