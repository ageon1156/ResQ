package org.meshtastic.feature.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.meshtastic.proto.MeshProtos.Waypoint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.meshtastic.core.model.triage.TriageLevel
import org.meshtastic.core.model.triage.TriagePin
import org.meshtastic.feature.map.MapMode
import org.meshtastic.feature.map.triage.SilentNodeRecord

@Composable
fun MapModeTabRow(
    activeMode: MapMode,
    onModeSelected: (MapMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .height(40.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        MapMode.entries.forEachIndexed { index, mode ->
            val selected = activeMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onModeSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        MapMode.CustomMap -> "CUSTOM MAP"
                        MapMode.TriageMap -> "TRIAGE MAP"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            if (index < MapMode.entries.size - 1) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

@Composable
fun TriageLevelPickerDialog(
    onLevelSelected: (TriageLevel) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraSmall,
        title = {
            Text(text = "ADD CASUALTY", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Select triage level:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                TriageLevelButton(TriageLevel.RED,    Color(0xFFD32F2F)) { onLevelSelected(TriageLevel.RED) }
                TriageLevelButton(TriageLevel.YELLOW, Color(0xFFB8860B)) { onLevelSelected(TriageLevel.YELLOW) }
                TriageLevelButton(TriageLevel.GREEN,  Color(0xFF2E6B2E)) { onLevelSelected(TriageLevel.GREEN) }
                TriageLevelButton(TriageLevel.BLACK,  Color(0xFF1A1A1A)) { onLevelSelected(TriageLevel.BLACK) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("CANCEL", style = MaterialTheme.typography.labelMedium) }
        },
    )
}

@Composable
fun TriagePinInfoDialog(
    pin: TriagePin,
    onClaim: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = {
            Text(
                text = "${pin.triageLevel.symbol} ${pin.triageLevel.displayLabel.uppercase()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("VICTIMS: ${pin.victimCount}", style = MaterialTheme.typography.labelLarge)
                Text("ADDED BY: ${pin.createdBy}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                pin.claimedBy?.let {
                    Text(
                        text = "CLAIMED BY: $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (pin.isSilentNodeConversion) {
                    Text(
                        text = "SRC: silent-node detection",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pin.claimedBy == null) {
                    Button(
                        onClick = onClaim,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) { Text("CLAIM", style = MaterialTheme.typography.labelMedium) }
                }
                OutlinedButton(
                    onClick = onDelete,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("DELETE", style = MaterialTheme.typography.labelMedium) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE", style = MaterialTheme.typography.labelMedium) }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClearWaypointsDialog(
    showDeleteForEveryone: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { Text(text = "CLEAR ALL WAYPOINTS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = if (showDeleteForEveryone) {
                    "Remove all waypoints locally, or broadcast removal to the entire mesh."
                } else {
                    "Remove all waypoints from your local map view."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        },
        confirmButton = {
            FlowRow(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onDismiss) { Text("CANCEL", style = MaterialTheme.typography.labelMedium) }
                Button(
                    onClick = onDeleteForMe,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("DELETE FOR ME", style = MaterialTheme.typography.labelMedium) }
                if (showDeleteForEveryone) {
                    Button(
                        onClick = onDeleteForEveryone,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) { Text("DELETE FOR ALL", style = MaterialTheme.typography.labelMedium) }
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
fun ClearTriagePinsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { Text(text = "CLEAR ALL TRIAGE PINS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Remove all triage pins from the map? This only clears your local view.",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("CLEAR ALL", style = MaterialTheme.typography.labelMedium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", style = MaterialTheme.typography.labelMedium) }
        },
    )
}

@Composable
fun SilentNodeTriageDialog(
    record: SilentNodeRecord,
    onConvert: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = {
            Text(text = "⚠ ${record.node.user.longName.uppercase()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = record.displayLabel, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "No recent packets received from this node.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConvert,
                shape = MaterialTheme.shapes.extraSmall,
            ) { Text("ADD TRIAGE PIN", style = MaterialTheme.typography.labelMedium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("DISMISS", style = MaterialTheme.typography.labelMedium) }
        },
    )
}

@Composable
fun WaypointInfoDialog(
    waypoint: Waypoint,
    createdBy: String,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    @Suppress("MagicNumber")
    val emoji = if (waypoint.icon == 0) "\uD83D\uDCCD" else String(Character.toChars(waypoint.icon))
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = {
            Text(
                text = "$emoji ${waypoint.name.uppercase()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (waypoint.description.isNotEmpty()) {
                    Text(waypoint.description, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "ADDED BY: $createdBy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canEdit) {
                    Button(
                        onClick = onEdit,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) { Text("EDIT", style = MaterialTheme.typography.labelMedium) }
                }
                OutlinedButton(
                    onClick = onDelete,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("DELETE", style = MaterialTheme.typography.labelMedium) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE", style = MaterialTheme.typography.labelMedium) }
        },
    )
}

@Composable
private fun TriageLevelButton(
    level: TriageLevel,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = level.symbol, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
            Text(
                text = level.displayLabel.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
