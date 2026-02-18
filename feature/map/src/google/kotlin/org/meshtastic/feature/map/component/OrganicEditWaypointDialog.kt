/*
 * Copyright (c) 2025-2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.feature.map.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.cancel
import org.meshtastic.core.strings.date
import org.meshtastic.core.strings.delete
import org.meshtastic.core.strings.description
import org.meshtastic.core.strings.expires
import org.meshtastic.core.strings.locked
import org.meshtastic.core.strings.name
import org.meshtastic.core.strings.send
import org.meshtastic.core.strings.time
import org.meshtastic.core.strings.waypoint_edit
import org.meshtastic.core.strings.waypoint_new
import org.meshtastic.core.ui.emoji.EmojiPickerDialog
import org.meshtastic.core.ui.theme.LeafShape
import org.meshtastic.proto.MeshProtos.Waypoint
import org.meshtastic.proto.copy
import java.util.Calendar
import java.util.TimeZone

private data class PlaceType(val label: String, val emoji: Int)

private val PLACE_TYPES = listOf(
    PlaceType("Hospital",    127973), // 🏥
    PlaceType("First Aid",     9971), // ⛑
    PlaceType("Shelter",       9978), // ⛺
    PlaceType("Food",        127838), // 🍞
    PlaceType("Water",       128167), // 💧
    PlaceType("Aid Center",  127384), // 🆘
    PlaceType("Police",      128148), // 🚔
    PlaceType("Fire Dept",   128146), // 🚒
    PlaceType("Danger",        9888), // ⚠
    PlaceType("Blocked",     128679), // 🚧
)

/**
 * Organic styled waypoint editing dialog with natural shapes and colors.
 * Features LeafShape container, organic text fields, and earthy color palette.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
@Composable
fun OrganicEditWaypointDialog(
    waypoint: Waypoint,
    onSendClicked: (Waypoint) -> Unit,
    onDeleteClicked: (Waypoint) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var waypointInput by remember { mutableStateOf(waypoint) }
    val title = if (waypoint.id == 0) Res.string.waypoint_new else Res.string.waypoint_edit
    val defaultEmoji = 0x1F4CD // 📍 Round Pushpin
    val currentEmojiCodepoint = if (waypointInput.icon == 0) defaultEmoji else waypointInput.icon
    var showEmojiPickerView by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    var selectedDateString by remember { mutableStateOf("") }
    var selectedTimeString by remember { mutableStateOf("") }
    var isExpiryEnabled by remember {
        mutableStateOf(waypointInput.expire != 0 && waypointInput.expire != Int.MAX_VALUE)
    }

    val dateFormat = remember { android.text.format.DateFormat.getDateFormat(context) }
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    dateFormat.timeZone = TimeZone.getDefault()
    timeFormat.timeZone = TimeZone.getDefault()

    LaunchedEffect(waypointInput.expire, isExpiryEnabled) {
        if (isExpiryEnabled) {
            if (waypointInput.expire != 0 && waypointInput.expire != Int.MAX_VALUE) {
                calendar.timeInMillis = waypointInput.expire * 1000L
                selectedDateString = dateFormat.format(calendar.time)
                selectedTimeString = timeFormat.format(calendar.time)
            } else {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.HOUR_OF_DAY, 8)
                waypointInput = waypointInput.copy { expire = (calendar.timeInMillis / 1000).toInt() }
            }
        } else {
            selectedDateString = ""
            selectedTimeString = ""
        }
    }

    // Organic text field colors
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    )

    if (!showEmojiPickerView) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            shape = LeafShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                Column(modifier = modifier.fillMaxWidth()) {
                    // Name field with emoji picker
                    OutlinedTextField(
                        value = waypointInput.name,
                        onValueChange = { waypointInput = waypointInput.copy { name = it.take(29) } },
                        label = { Text(stringResource(Res.string.name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showEmojiPickerView = true }) {
                                Text(
                                    text = String(Character.toChars(currentEmojiCodepoint)),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(6.dp),
                                    fontSize = 20.sp,
                                )
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick place type selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PLACE_TYPES.forEach { placeType ->
                            val selected = waypointInput.name == placeType.label &&
                                    waypointInput.icon == placeType.emoji
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    waypointInput = waypointInput.copy {
                                        name = placeType.label
                                        icon = placeType.emoji
                                    }
                                },
                                label = {
                                    Text(
                                        text = "${String(Character.toChars(placeType.emoji))} ${placeType.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description field
                    OutlinedTextField(
                        value = waypointInput.description,
                        onValueChange = { waypointInput = waypointInput.copy { description = it.take(99) } },
                        label = { Text(stringResource(Res.string.description)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { }),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Locked toggle with organic styling
                    OrganicToggleRow(
                        icon = Icons.Default.Lock,
                        label = stringResource(Res.string.locked),
                        checked = waypointInput.lockedTo != 0,
                        onCheckedChange = { waypointInput = waypointInput.copy { lockedTo = if (it) 1 else 0 } }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Expiry toggle
                    OrganicToggleRow(
                        icon = Icons.Default.CalendarMonth,
                        label = stringResource(Res.string.expires),
                        checked = isExpiryEnabled,
                        onCheckedChange = { checked ->
                            isExpiryEnabled = checked
                            if (checked) {
                                if (waypointInput.expire == 0 || waypointInput.expire == Int.MAX_VALUE) {
                                    val cal = Calendar.getInstance()
                                    cal.timeInMillis = System.currentTimeMillis()
                                    cal.add(Calendar.HOUR_OF_DAY, 8)
                                    waypointInput = waypointInput.copy { expire = (cal.timeInMillis / 1000).toInt() }
                                }
                            } else {
                                waypointInput = waypointInput.copy { expire = Int.MAX_VALUE }
                            }
                        }
                    )

                    // Date/Time pickers when expiry is enabled
                    if (isExpiryEnabled) {
                        val currentCalendar = Calendar.getInstance().apply {
                            if (waypointInput.expire != 0 && waypointInput.expire != Int.MAX_VALUE) {
                                timeInMillis = waypointInput.expire * 1000L
                            } else {
                                timeInMillis = System.currentTimeMillis()
                                add(Calendar.HOUR_OF_DAY, 8)
                            }
                        }
                        val year = currentCalendar.get(Calendar.YEAR)
                        val month = currentCalendar.get(Calendar.MONTH)
                        val day = currentCalendar.get(Calendar.DAY_OF_MONTH)
                        val hour = currentCalendar.get(Calendar.HOUR_OF_DAY)
                        val minute = currentCalendar.get(Calendar.MINUTE)

                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                                calendar.clear()
                                calendar.set(selectedYear, selectedMonth, selectedDay, hour, minute)
                                waypointInput = waypointInput.copy { expire = (calendar.timeInMillis / 1000).toInt() }
                            },
                            year, month, day,
                        )

                        val timePickerDialog = TimePickerDialog(
                            context,
                            { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                                val tempCal = Calendar.getInstance()
                                tempCal.timeInMillis = waypointInput.expire * 1000L
                                tempCal.set(Calendar.HOUR_OF_DAY, selectedHour)
                                tempCal.set(Calendar.MINUTE, selectedMinute)
                                waypointInput = waypointInput.copy { expire = (tempCal.timeInMillis / 1000).toInt() }
                            },
                            hour, minute,
                            android.text.format.DateFormat.is24HourFormat(context),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Organic date/time buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OrganicDateTimeButton(
                                label = stringResource(Res.string.date),
                                value = selectedDateString,
                                onClick = { datePickerDialog.show() }
                            )
                            OrganicDateTimeButton(
                                label = stringResource(Res.string.time),
                                value = selectedTimeString,
                                onClick = { timePickerDialog.show() }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (waypoint.id != 0) {
                        TextButton(
                            onClick = { onDeleteClicked(waypointInput) },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                stringResource(Res.string.delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Button(
                        onClick = { onSendClicked(waypointInput) },
                        enabled = waypointInput.name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(Res.string.send))
                    }
                }
            },
            dismissButton = null,
            modifier = modifier,
        )
    } else {
        EmojiPickerDialog(onDismiss = { showEmojiPickerView = false }) { selectedEmoji ->
            showEmojiPickerView = false
            waypointInput = waypointInput.copy { icon = selectedEmoji.codePointAt(0) }
        }
    }
}

/**
 * Organic styled toggle row with icon and switch.
 */
@Composable
private fun OrganicToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        )
    }
}

/**
 * Organic styled date/time picker button.
 */
@Composable
private fun OrganicDateTimeButton(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(label)
        }
        if (value.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

