
package org.meshtastic.feature.map.component

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import org.meshtastic.core.ui.component.EditTextPreference
import org.meshtastic.core.ui.emoji.EmojiPickerDialog
import org.meshtastic.core.ui.theme.LeafShape
import org.meshtastic.proto.MeshProtos.Waypoint
import org.meshtastic.proto.copy
import java.util.Calendar

private data class PlaceType(val label: String, val emoji: Int)

private val PLACE_TYPES = listOf(
    PlaceType("Hospital",    127973), 
    PlaceType("First Aid",     9971), 
    PlaceType("Shelter",       9978), 
    PlaceType("Food",        127838), 
    PlaceType("Water",       128167), 
    PlaceType("Aid Center",  127384), 
    PlaceType("Police",      128148), 
    PlaceType("Fire Dept",   128146), 
    PlaceType("Danger",        9888), 
    PlaceType("Blocked",     128679), 
)

@Suppress("LongMethod")
@OptIn(ExperimentalLayoutApi::class)
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

    @Suppress("MagicNumber")
    val emoji = if (waypointInput.icon == 0) 128205 else waypointInput.icon
    var showEmojiPickerView by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val currentTime = System.currentTimeMillis()
    calendar.timeInMillis = currentTime
    @Suppress("MagicNumber")
    calendar.add(Calendar.HOUR_OF_DAY, 8)

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val dateFormat = android.text.format.DateFormat.getDateFormat(context)
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormat = android.text.format.DateFormat.getTimeFormat(context)

    var selectedDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var selectedTime by remember { mutableStateOf(timeFormat.format(calendar.time)) }
    var epochTime by remember { mutableStateOf<Long?>(null) }

    if (!showEmojiPickerView) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            shape = LeafShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            text = {
                Column(modifier = modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )

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

                    EditTextPreference(
                        title = stringResource(Res.string.name),
                        value = waypointInput.name,
                        maxSize = 29,
                        enabled = true,
                        isError = false,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {}),
                        onValueChanged = { waypointInput = waypointInput.copy { name = it } },
                        trailingIcon = {
                            IconButton(onClick = { showEmojiPickerView = true }) {
                                Text(
                                    text = String(Character.toChars(emoji)),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(6.dp),
                                    fontSize = 24.sp,
                                )
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    EditTextPreference(
                        title = stringResource(Res.string.description),
                        value = waypointInput.description,
                        maxSize = 99,
                        enabled = true,
                        isError = false,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {}),
                        onValueChanged = { waypointInput = waypointInput.copy { description = it } },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OrganicToggleRow(
                        icon = Icons.Default.Lock,
                        label = stringResource(Res.string.locked),
                        checked = waypointInput.lockedTo != 0,
                        onCheckedChange = { waypointInput = waypointInput.copy { lockedTo = if (it) 1 else 0 } }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                            calendar.set(selectedYear, selectedMonth, selectedDay)
                            epochTime = calendar.timeInMillis
                            selectedDate = dateFormat.format(calendar.time)
                        },
                        year, month, day,
                    )

                    val timePickerDialog = TimePickerDialog(
                        context,
                        { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                            calendar.set(Calendar.MINUTE, selectedMinute)
                            epochTime = calendar.timeInMillis
                            selectedTime = timeFormat.format(calendar.time)
                            @Suppress("MagicNumber")
                            waypointInput = waypointInput.copy { expire = (epochTime!! / 1000).toInt() }
                        },
                        hour, minute, is24Hour,
                    )

                    OrganicToggleRow(
                        icon = Icons.Default.CalendarMonth,
                        label = stringResource(Res.string.expires),
                        checked = waypointInput.expire != Int.MAX_VALUE && waypointInput.expire != 0,
                        onCheckedChange = { isChecked ->
                            waypointInput = waypointInput.copy {
                                expire = if (isChecked) {
                                    @Suppress("MagicNumber")
                                    calendar.timeInMillis / 1000
                                } else {
                                    Int.MAX_VALUE
                                }.toInt()
                            }
                            if (isChecked) {
                                selectedDate = dateFormat.format(calendar.time)
                                selectedTime = timeFormat.format(calendar.time)
                            } else {
                                selectedDate = ""
                                selectedTime = ""
                            }
                        }
                    )

                    if (waypointInput.expire != Int.MAX_VALUE && waypointInput.expire != 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OrganicDateTimeButton(
                                label = stringResource(Res.string.date),
                                value = selectedDate,
                                onClick = { datePickerDialog.show() }
                            )
                            OrganicDateTimeButton(
                                label = stringResource(Res.string.time),
                                value = selectedTime,
                                onClick = { timePickerDialog.show() }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                FlowRow(
                    modifier = modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        modifier = modifier.weight(1f),
                        onClick = onDismissRequest
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                    if (waypoint.id != 0) {
                        Button(
                            modifier = modifier.weight(1f),
                            onClick = { onDeleteClicked(waypointInput) },
                            enabled = waypointInput.name.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(Res.string.delete))
                        }
                    }
                    Button(
                        modifier = modifier.weight(1f),
                        onClick = { onSendClicked(waypointInput) },
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(Res.string.send))
                    }
                }
            },
        )
    } else {
        EmojiPickerDialog(onDismiss = { showEmojiPickerView = false }) {
            showEmojiPickerView = false
            waypointInput = waypointInput.copy { icon = it.codePointAt(0) }
        }
    }
}

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
    ) {
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
        Switch(
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End),
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            )
        )
    }
}

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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
