package com.example.dailytrack_mobile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Reusable Month & Year picker dialog with year navigation, 3x4 month grid,
 * and a quick "Current Month" shortcut button.
 */
@Composable
fun MonthYearPickerDialog(
    selectedMonth: Month = LocalDate.now().month,
    selectedYear: Int = LocalDate.now().year,
    onDismiss: () -> Unit,
    onSelected: (Month, Int) -> Unit
) {
    var displayYear by remember { mutableIntStateOf(selectedYear) }
    val dims = Dimens.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val dialogFraction = when {
        screenWidth < 360  -> 0.94f
        screenWidth <= 410 -> 0.90f
        else               -> 0.84f
    }
    val currentMonth = remember { LocalDate.now().month }
    val currentYear = remember { LocalDate.now().year }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape     = RoundedCornerShape(dims.cardCornerRadius + 4.dp),
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier  = Modifier
                .fillMaxWidth(dialogFraction)
                .wrapContentHeight()
        ) {
            Column(
                modifier            = Modifier.padding(dims.cardInnerPadding),
                verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
            ) {
                // Dialog header with title & Current Month shortcut
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "Select Month & Year",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Quick "This Month" shortcut text button
                    TextButton(
                        onClick = { onSelected(currentMonth, currentYear) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "This Month",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "This Month",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Year navigation row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { displayYear-- },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous year",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text  = displayYear.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { displayYear++ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next year",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )

                // Month grid (3 columns × 4 rows)
                val months  = Month.entries
                val chunked = months.chunked(3)
                chunked.forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
                    ) {
                        row.forEach { month ->
                            val isSelected = month == selectedMonth && displayYear == selectedYear
                            val isThisMonth = month == currentMonth && displayYear == currentYear
                            val bgColor = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else if (isThisMonth)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else
                                Color.Transparent
                            val textColor = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else if (isThisMonth)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgColor)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null
                                    ) { onSelected(month, displayYear) }
                                    .padding(vertical = dims.itemSpacingLarge),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected || isThisMonth) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
