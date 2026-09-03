package com.example.dailytrack_mobile.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTrackTimePickerDialog(
    initialTime: LocalTime,
    is24Hour: Boolean = false,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = is24Hour
    )

    var showDialMode by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(enabled = false) {}
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                ) {
                    val selectedTime = remember(timePickerState.hour, timePickerState.minute) {
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                    }

                    // Header Row: Formatted time + "Now" button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
                        val formattedTime = remember(selectedTime, is24Hour) {
                            selectedTime.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
                        }

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = {
                                val now = LocalTime.now()
                                timePickerState.hour = now.hour
                                timePickerState.minute = now.minute
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Jump to Now",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Now",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clock Dial or Number Input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showDialMode) {
                            TimePicker(state = timePickerState)
                        } else {
                            TimeInput(state = timePickerState)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Row: Mode Toggle Switch (Dial / Input) + Actions (Cancel / OK)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showDialMode,
                            onCheckedChange = { showDialMode = it },
                            thumbContent = {
                                Icon(
                                    imageVector = if (showDialMode) Icons.Default.Schedule else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = onDismiss) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                                    onTimeSelected(time)
                                }
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}
