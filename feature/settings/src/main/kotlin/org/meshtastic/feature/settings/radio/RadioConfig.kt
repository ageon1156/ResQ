
package org.meshtastic.feature.settings.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.model.Node
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoutes
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.administration
import org.meshtastic.core.strings.advanced_title
import org.meshtastic.core.strings.backup_restore
import org.meshtastic.core.strings.clean_node_database_title
import org.meshtastic.core.strings.debug_panel
import org.meshtastic.core.strings.export_configuration
import org.meshtastic.core.strings.factory_reset
import org.meshtastic.core.strings.firmware_update_title
import org.meshtastic.core.strings.import_configuration
import org.meshtastic.core.strings.message_device_managed
import org.meshtastic.core.strings.module_settings
import org.meshtastic.core.strings.nodedb_reset
import org.meshtastic.core.strings.preserve_favorites
import org.meshtastic.core.strings.reboot
import org.meshtastic.core.strings.shutdown
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.StatusColors.StatusRed
import org.meshtastic.feature.settings.radio.component.ShutdownConfirmationDialog
import org.meshtastic.feature.settings.radio.component.WarningDialog

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun RadioConfigItemList(
    state: RadioConfigState,
    isManaged: Boolean,
    node: Node? = null,
    excludedModulesUnlocked: Boolean = false,
    isOtaCapable: Boolean = false,
    onPreserveFavoritesToggle: (Boolean) -> Unit = {},
    onRouteClick: (Enum<*>) -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
    onNavigate: (Route) -> Unit,
) {
}

enum class AdminRoute(val icon: ImageVector, val title: StringResource) {
    REBOOT(Icons.Rounded.RestartAlt, Res.string.reboot),
    SHUTDOWN(Icons.Rounded.PowerSettingsNew, Res.string.shutdown),
    FACTORY_RESET(Icons.Rounded.Restore, Res.string.factory_reset),
    NODEDB_RESET(Icons.Rounded.Storage, Res.string.nodedb_reset),
}

@Preview(showBackground = true)
@Composable
private fun RadioSettingsScreenPreview() = AppTheme {
    RadioConfigItemList(
        state = RadioConfigState(isLocal = true, connected = true),
        isManaged = false,
        onNavigate = { _ -> },
    )
}

@Composable
private fun ManagedMessage() {
    Text(
        text = stringResource(Res.string.message_device_managed),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.StatusRed,
    )
}

@Preview(showBackground = true)
@Composable
private fun RadioSettingsScreenManagedPreview() = AppTheme {
    RadioConfigItemList(
        state = RadioConfigState(isLocal = true, connected = true),
        isManaged = true,
        onNavigate = { _ -> },
    )
}

