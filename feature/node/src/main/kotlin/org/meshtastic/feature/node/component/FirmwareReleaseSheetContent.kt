

package org.meshtastic.feature.node.component

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.entity.FirmwareRelease
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.download
import org.meshtastic.core.strings.error_no_app_to_handle_link
import org.meshtastic.core.strings.view_release
import org.meshtastic.core.ui.util.showToast

@Composable
fun FirmwareReleaseSheetContent(firmwareRelease: FirmwareRelease, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = firmwareRelease.title, style = MaterialTheme.typography.titleLarge)
        Text(text = "Version: ${firmwareRelease.id}", style = MaterialTheme.typography.bodyMedium)
        Markdown(modifier = Modifier.padding(8.dp), content = firmwareRelease.releaseNotes)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, firmwareRelease.pageUrl.toUri())
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        scope.launch { context.showToast(Res.string.error_no_app_to_handle_link) }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Icons.Default.Link, contentDescription = stringResource(Res.string.view_release))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(Res.string.view_release))
            }
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, firmwareRelease.zipUrl.toUri())
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        scope.launch { context.showToast(Res.string.error_no_app_to_handle_link) }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = stringResource(Res.string.download))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(Res.string.download))
            }
        }
    }
}

