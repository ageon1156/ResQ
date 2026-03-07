package org.meshtastic.feature.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.meshtastic.proto.MeshProtos.Waypoint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.meshtastic.core.model.triage.TriageLevel
import org.meshtastic.core.model.triage.TriagePin
import org.meshtastic.feature.map.MapMode
import org.meshtastic.feature.map.triage.SilentNodeRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapModeTabRow(
    activeMode: MapMode,
    onModeSelected: (MapMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        MapMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = MapMode.entries.size,
                ),
                onClick = { onModeSelected(mode) },
                selected = activeMode == mode,
                label = {
                    Text(
                        text = when (mode) {
                            MapMode.CustomMap -> "Custom Map"
                            MapMode.TriageMap -> "Triage Map"
                        },
                        fontSize = 13.sp,
                        fontWeight = if (activeMode == mode) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
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
        title = {
            Text(text = "Add Casualty", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select triage level:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                TriageLevelButton(TriageLevel.RED,    Color(0xFFD32F2F)) { onLevelSelected(TriageLevel.RED) }
                TriageLevelButton(TriageLevel.YELLOW, Color(0xFFF9A825)) { onLevelSelected(TriageLevel.YELLOW) }
                TriageLevelButton(TriageLevel.GREEN,  Color(0xFF2E7D32)) { onLevelSelected(TriageLevel.GREEN) }
                TriageLevelButton(TriageLevel.BLACK,  Color(0xFF212121)) { onLevelSelected(TriageLevel.BLACK) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
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
        title = {
            Text(
                text = "${pin.triageLevel.symbol} ${pin.triageLevel.displayLabel}",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Victims: ${pin.victimCount}", fontSize = 14.sp)
                Text("Added by: ${pin.createdBy}", fontSize = 13.sp)
                pin.claimedBy?.let {
                    Text(
                        text = "Claimed by: $it",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (pin.isSilentNodeConversion) {
                    Text(
                        text = "Source: silent-node detection",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pin.claimedBy == null) {
                    Button(onClick = onClaim) { Text("Claim") }
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
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
        title = { Text(text = "Clear all waypoints?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = if (showDeleteForEveryone) {
                    "Remove all waypoints locally, or broadcast removal to the entire mesh."
                } else {
                    "Remove all waypoints from your local map view."
                },
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            FlowRow(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = onDeleteForMe,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete for me") }
                if (showDeleteForEveryone) {
                    Button(onClick = onDeleteForEveryone) { Text("Delete for everyone") }
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
        title = { Text(text = "Clear All Triage Pins", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Remove all triage pins from the map? This only clears your local view.",
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("Clear All") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
        title = {
            Text(text = "⚠ ${record.node.user.longName}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = record.displayLabel, fontSize = 14.sp)
                Text(
                    text = "No recent packets received from this node.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConvert) { Text("Add Triage Pin") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
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
        title = {
            Text(
                text = "$emoji ${waypoint.name}",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (waypoint.description.isNotEmpty()) {
                    Text(waypoint.description, fontSize = 14.sp)
                }
                Text("Added by: $createdBy", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canEdit) {
                    Button(onClick = onEdit) { Text("Edit") }
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
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
        shape = RoundedCornerShape(8.dp),
    ) {
        Row {
            Text(text = level.symbol, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = level.displayLabel,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}
