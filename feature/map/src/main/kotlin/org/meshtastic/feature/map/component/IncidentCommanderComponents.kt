package org.meshtastic.feature.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.meshtastic.core.model.triage.TriageLevel
import org.meshtastic.core.model.triage.TriagePin

fun levelColor(level: TriageLevel): Color = when (level) {
    TriageLevel.RED    -> Color(0xFFD32F2F)
    TriageLevel.YELLOW -> Color(0xFFF9A825)
    TriageLevel.GREEN  -> Color(0xFF2E7D32)
    TriageLevel.BLACK  -> Color(0xFF212121)
}

@Composable
fun IcModeFab(
    isIcMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        containerColor = if (isIcMode) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Icon(
            imageVector = if (isIcMode) Icons.Filled.Shield else Icons.Outlined.Shield,
            contentDescription = if (isIcMode) "Disable IC Mode" else "Enable IC Mode",
            tint = if (isIcMode) {
                MaterialTheme.colorScheme.onError
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
fun IncidentCommanderPanel(
    assignments: Map<String, String>,
    rescuerNames: Map<String, String>,
    pins: List<TriagePin>,
    lockedAssignments: Set<String>,
    onLock: (rescuerId: String) -> Unit,
    onUnlock: (rescuerId: String) -> Unit,
    onRecompute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Incident Commander",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                TextButton(onClick = onRecompute) {
                    Text("Recompute")
                }
            }
            HorizontalDivider()
            if (assignments.isEmpty()) {
                Text(
                    text = "No rescuers with GPS online",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(assignments.entries.toList(), key = { it.key }) { (rescuerId, pinId) ->
                        val pin = pins.firstOrNull { it.pinId == pinId }
                        val name = rescuerNames[rescuerId] ?: rescuerId.takeLast(6)
                        val isLocked = rescuerId in lockedAssignments
                        AssignmentRow(
                            rescuerName    = name,
                            pin            = pin,
                            isLocked       = isLocked,
                            onToggleLock   = {
                                if (isLocked) onUnlock(rescuerId) else onLock(rescuerId)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentRow(
    rescuerName: String,
    pin: TriagePin?,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = rescuerName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (pin != null) {
                Text(
                    text = "${pin.triageLevel.symbol} ${pin.triageLevel.displayLabel}",
                    fontSize = 12.sp,
                    color = levelColor(pin.triageLevel),
                )
            } else {
                Text(
                    text = "Unassigned",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onToggleLock) {
            Icon(
                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Outlined.LockOpen,
                contentDescription = if (isLocked) "Unlock assignment" else "Lock assignment",
                tint = if (isLocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
fun MyAssignmentCard(
    pin: TriagePin,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Your Assignment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "${pin.triageLevel.symbol} ${pin.triageLevel.displayLabel}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = levelColor(pin.triageLevel),
            )
            Text(
                text = "Victims: ${pin.victimCount}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "%.5f, %.5f".format(pin.lat, pin.lon),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
