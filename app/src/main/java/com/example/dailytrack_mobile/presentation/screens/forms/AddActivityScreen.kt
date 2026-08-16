package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Add Activity Form
// ─────────────────────────────────────────────────────────────────────────────

private enum class FormActivityType(val label: String, val emoji: String) {
    GYM("Gym", "🏋️"),
    BADMINTON("Badminton", "🏸"),
    RUNNING("Running", "🏃"),
    TABLE_TENNIS("Table Tennis", "🏓"),
    CYCLING("Cycling", "🚴"),
    SWIMMING("Swimming", "🏊"),
    YOGA("Yoga", "🧘"),
    OTHERS("Others", "⚡")
}

private enum class Intensity(val label: String) {
    LIGHT("Light"),
    MODERATE("Moderate"),
    INTENSE("Intense")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddActivityScreen(
    onDirtyStateChanged: (Boolean) -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var selectedActivities by remember { mutableStateOf(setOf<FormActivityType>()) }
    var selectedIntensity by remember { mutableStateOf<Intensity?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val dims = Dimens.current

    val isDirty = remember(selectedActivities, selectedIntensity, hours, minutes, note, selectedDate) {
        selectedActivities.isNotEmpty() ||
                selectedIntensity != null ||
                hours.isNotBlank() ||
                minutes.isNotBlank() ||
                note.isNotBlank() ||
                selectedDate != LocalDate.now()
    }

    LaunchedEffect(isDirty) {
        onDirtyStateChanged(isDirty)
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    // ── Date Picker Dialog ───────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
        verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing)
    ) {
        // ── Activity Type (multi-select) ─────────────────────────────
        SectionLabel("What did you do?")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            FormActivityType.entries.forEach { activity ->
                val isSelected = activity in selectedActivities
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedActivities = if (isSelected) {
                            selectedActivities - activity
                        } else {
                            selectedActivities + activity
                        }
                    },
                    label = { Text("${activity.emoji} ${activity.label}", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                )
            }
        }

        // ── Intensity ────────────────────────────────────────────────
        SectionLabel("Intensity")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Intensity.entries.forEachIndexed { index, intensity ->
                SegmentedButton(
                    selected = selectedIntensity == intensity,
                    onClick = { selectedIntensity = intensity },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = Intensity.entries.size
                    )
                ) {
                    Text(intensity.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ── Date & Duration (side by side) ───────────────────────────
        SectionLabel("Date & Duration")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
        ) {
            // Date field (read-only, opens picker on click)
            OutlinedTextField(
                value = selectedDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = "Pick date",
                            modifier = Modifier.size(dims.iconSizeMedium)
                        )
                    }
                },
                shape = RoundedCornerShape(dims.buttonCornerRadius),
                modifier = Modifier.weight(1f)
            )

            // Hours
            OutlinedTextField(
                value = hours,
                onValueChange = { newValue ->
                    if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                        val num = newValue.toIntOrNull() ?: 0
                        if (num <= 23) hours = newValue
                    }
                },
                placeholder = { Text("0") },
                suffix = { Text("h") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text("Hrs") },
                shape = RoundedCornerShape(dims.buttonCornerRadius),
                modifier = Modifier.weight(0.5f)
            )

            // Minutes
            OutlinedTextField(
                value = minutes,
                onValueChange = { newValue ->
                    if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                        val num = newValue.toIntOrNull() ?: 0
                        if (num <= 59) minutes = newValue
                    }
                },
                placeholder = { Text("0") },
                suffix = { Text("m") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text("Min") },
                shape = RoundedCornerShape(dims.buttonCornerRadius),
                modifier = Modifier.weight(0.5f)
            )
        }

        // ── Note ─────────────────────────────────────────────────────
        SectionLabel("Note (optional)")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("How did it go?", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null, modifier = Modifier.size(dims.iconSizeMedium))
            },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Save Button ──────────────────────────────────────────────
        Button(
            onClick = { onSaveSuccess() },
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.searchBarHeight),
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            enabled = selectedActivities.isNotEmpty()
        ) {
            Text(
                "Save Activity",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
    }
}
