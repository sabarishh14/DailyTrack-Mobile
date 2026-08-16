package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.screens.money.FilterMode
import com.example.dailytrack_mobile.presentation.screens.money.ItemFilterStatus
import com.example.dailytrack_mobile.presentation.util.Dimens

/**
 * Reusable filter section implementing the "Mode Toggle" pattern for Include vs. Exclude logic.
 *
 * - SingleChoiceSegmentedButton with "Include Mode" and "Exclude Mode".
 * - FlowRow of FilterChips below.
 * - Tapping a Neutral chip in Include Mode marks it as Included (Check icon + Primary color).
 * - Tapping a Neutral chip in Exclude Mode marks it as Excluded (Cross icon + Error/Red color).
 * - Tapping an already-selected chip (regardless of current mode) deselects it back to Neutral.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSection(
    title: String,
    items: List<String>,
    selectedStatusMap: Map<String, ItemFilterStatus>,
    onItemStatusChange: (item: String, newStatus: ItemFilterStatus) -> Unit,
    modifier: Modifier = Modifier,
    initialMode: FilterMode = FilterMode.INCLUDE
) {
    var currentMode by remember { mutableStateOf(initialMode) }
    val dims = Dimens.current

    val includedCount = selectedStatusMap.count { it.value == ItemFilterStatus.INCLUDED }
    val excludedCount = selectedStatusMap.count { it.value == ItemFilterStatus.EXCLUDED }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
    ) {
        // Section Header with Title & Active Count Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Status Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (includedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = "+$includedCount included",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (excludedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = "-$excludedCount excluded",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Mode Toggle: SingleChoiceSegmentedButtonRow
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Segment 1: Include Mode
            SegmentedButton(
                selected = currentMode == FilterMode.INCLUDE,
                onClick = { currentMode = FilterMode.INCLUDE },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    SegmentedButtonDefaults.Icon(active = currentMode == FilterMode.INCLUDE) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Include Mode",
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "Include Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            // Segment 2: Exclude Mode
            SegmentedButton(
                selected = currentMode == FilterMode.EXCLUDE,
                onClick = { currentMode = FilterMode.EXCLUDE },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    SegmentedButtonDefaults.Icon(active = currentMode == FilterMode.EXCLUDE) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exclude Mode",
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.errorContainer,
                    activeContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "Exclude Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // FlowRow of FilterChips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val status = selectedStatusMap[item] ?: ItemFilterStatus.NEUTRAL
                val isSelected = status != ItemFilterStatus.NEUTRAL

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val nextStatus = when (status) {
                            ItemFilterStatus.INCLUDED,
                            ItemFilterStatus.EXCLUDED -> ItemFilterStatus.NEUTRAL
                            ItemFilterStatus.NEUTRAL -> {
                                if (currentMode == FilterMode.INCLUDE) ItemFilterStatus.INCLUDED
                                else ItemFilterStatus.EXCLUDED
                            }
                        }
                        onItemStatusChange(item, nextStatus)
                    },
                    label = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                    },
                    leadingIcon = when (status) {
                        ItemFilterStatus.INCLUDED -> {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Included",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        ItemFilterStatus.EXCLUDED -> {
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Excluded",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        ItemFilterStatus.NEUTRAL -> null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = if (status == ItemFilterStatus.INCLUDED) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        selectedLabelColor = if (status == ItemFilterStatus.INCLUDED) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        selectedLeadingIconColor = if (status == ItemFilterStatus.INCLUDED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        selectedBorderColor = if (status == ItemFilterStatus.INCLUDED) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                )
            }
        }
    }
}
