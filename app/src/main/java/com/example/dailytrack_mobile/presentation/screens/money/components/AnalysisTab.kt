package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.screens.money.*
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun formatCompact(amount: Double): String {
    val abs = Math.abs(amount)
    return when {
        abs >= 1_00_000 -> String.format(java.util.Locale.US, "%.1fL", amount / 1_00_000)
        abs >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", amount / 1_000)
        else -> String.format(java.util.Locale.US, "%.0f", amount)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Analysis Tab
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnalysisTab(
    state: MoneyState,
    onAction: (MoneyAction) -> Unit
) {
    val dims = Dimens.current
    val categories = state.spendingAnalyzerData
    val filterState = state.analysisFilterState

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = dims.screenHorizontalPadding,
            end = dims.screenHorizontalPadding,
            top = dims.screenTopPadding,
            bottom = dims.screenBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing)
    ) {
        // Active Filter Bar / Trigger Header
        item {
            AnalysisFilterRow(
                filterState = filterState,
                onOpenFilterSheet = { onAction(MoneyAction.SetFilterSheetVisible(true)) },
                onAction = onAction
            )
        }

        // Cash Flow Breakdown Donut Card
        item {
            if (categories.isNotEmpty()) {
                CashFlowBreakdownCard(
                    categories = categories,
                    periodLabel = filterState.financialYear ?: "ALL TIME"
                )
            } else {
                EmptyFilterResultsCard(
                    onResetFilters = { onAction(MoneyAction.ResetAnalysisFilters) }
                )
            }
        }

        // Income & Expense Summary Row
        item {
            IncomeExpenseRow(
                income = state.filteredTotalIncome,
                expenses = state.filteredTotalExpenses
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter Row with Quick Presets and Active Removable Chips
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalysisFilterRow(
    filterState: AnalysisFilterState,
    onOpenFilterSheet: () -> Unit,
    onAction: (MoneyAction) -> Unit
) {
    val dims = Dimens.current

    val density = androidx.compose.ui.platform.LocalDensity.current
    val clearButtonWidthPx = remember { with(density) { 88.dp.toPx().toInt() } }
    val scrollState = rememberScrollState(initial = if (filterState.hasActiveFilters) clearButtonWidthPx else 0)

    LaunchedEffect(filterState.hasActiveFilters) {
        if (filterState.hasActiveFilters && scrollState.value < clearButtonWidthPx) {
            scrollState.scrollTo(clearButtonWidthPx)
        } else if (!filterState.hasActiveFilters) {
            scrollState.scrollTo(0)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hidden "Pull to Reveal" Clear Button
            if (filterState.hasActiveFilters) {
                DottedClearButton(onClick = { onAction(MoneyAction.ResetAnalysisFilters) })
            }

            // Main Filter Button with Badge
            FilterChip(
                selected = filterState.hasActiveFilters,
                onClick = onOpenFilterSheet,
                label = {
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = if (filterState.hasActiveFilters) {
                    {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "${filterState.activeFilterCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
            )

            // Vertical divider separating main filter button from quick pills
            VerticalDivider(
                modifier = Modifier.height(20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 1. "Last 30 Days" Quick Preset Pill
            val isLast30Days = filterState.activeDatePreset == QuickFilterPreset.LAST_30_DAYS
            QuickPresetChip(
                text = "Last 30 Days",
                isSelected = isLast30Days,
                onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.LAST_30_DAYS)) }
            )

            // 2. "This Month" Quick Preset Pill
            val isThisMonth = filterState.activeDatePreset == QuickFilterPreset.THIS_MONTH
            QuickPresetChip(
                text = "This Month",
                isSelected = isThisMonth,
                onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.THIS_MONTH)) }
            )

            // 3. "Expenses Only" Quick Preset Pill
            val isExpensesOnly = filterState.selectedTypes == setOf(TransactionType.DEBIT)
            QuickPresetChip(
                text = "Expenses Only",
                isSelected = isExpensesOnly,
                onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.EXPENSES_ONLY)) }
            )

            // Active Financial Year Chip (if selected via bottom sheet)
            if (!filterState.financialYear.isNullOrBlank() && filterState.financialYear != "All Time") {
                ActiveFilterRemovableChip(
                    text = filterState.financialYear,
                    onRemove = { onAction(MoneyAction.ClearFinancialYearFilter) }
                )
            }

            // Active Custom Date Range Chip (only if not a preset)
            if (filterState.activeDatePreset == null) {
                filterState.formattedDateRange()?.let { rangeText ->
                    ActiveFilterRemovableChip(
                        text = rangeText,
                        onRemove = { onAction(MoneyAction.ClearDateRangeFilter) }
                    )
                }
            }

            // Active Types (if not standard single debit preset)
            if (filterState.selectedTypes != setOf(TransactionType.DEBIT)) {
                filterState.selectedTypes.forEach { type ->
                    ActiveFilterRemovableChip(
                        text = if (type == TransactionType.DEBIT) "Debit" else "Credit",
                        onRemove = { onAction(MoneyAction.RemoveTypeFilter(type)) }
                    )
                }
            }

            // Active Category Filters (Included / Excluded)
            filterState.categoryFilters.forEach { (cat, status) ->
                when (status) {
                    ItemFilterStatus.INCLUDED -> {
                        ActiveFilterRemovableChip(
                            text = "+ $cat",
                            isIncluded = true,
                            onRemove = { onAction(MoneyAction.RemoveCategoryFilter(cat)) }
                        )
                    }
                    ItemFilterStatus.EXCLUDED -> {
                        ActiveFilterRemovableChip(
                            text = "- $cat",
                            isExcluded = true,
                            onRemove = { onAction(MoneyAction.RemoveCategoryFilter(cat)) }
                        )
                    }
                    ItemFilterStatus.NEUTRAL -> Unit
                }
            }

            // Active Account Filters (Included / Excluded)
            filterState.accountFilters.forEach { (acc, status) ->
                when (status) {
                    ItemFilterStatus.INCLUDED -> {
                        ActiveFilterRemovableChip(
                            text = "+ $acc",
                            isIncluded = true,
                            onRemove = { onAction(MoneyAction.RemoveAccountFilter(acc)) }
                        )
                    }
                    ItemFilterStatus.EXCLUDED -> {
                        ActiveFilterRemovableChip(
                            text = "- $acc",
                            isExcluded = true,
                            onRemove = { onAction(MoneyAction.RemoveAccountFilter(acc)) }
                        )
                    }
                    ItemFilterStatus.NEUTRAL -> Unit
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dotted Clear Button Component
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DottedClearButton(onClick: () -> Unit) {
    val dims = Dimens.current
    val color = ChartColors.ExpenseRed
    
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
            .clickable { onClick() }
            .drawBehind {
                drawRoundRect(
                    color = color.copy(alpha = 0.8f),
                    size = size,
                    style = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    ),
                    cornerRadius = CornerRadius((dims.buttonCornerRadius - 2.dp).toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Clear",
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Preset Chip Component
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickPresetChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Active Filter Removable Chip Component
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveFilterRemovableChip(
    text: String,
    onRemove: () -> Unit,
    isIncluded: Boolean = false,
    isExcluded: Boolean = false
) {
    val dims = Dimens.current

    val containerColor = when {
        isExcluded -> MaterialTheme.colorScheme.errorContainer
        isIncluded -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val contentColor = when {
        isExcluded -> MaterialTheme.colorScheme.onErrorContainer
        isIncluded -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
        color = containerColor,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = contentColor
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove filter",
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cash Flow Breakdown Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CashFlowBreakdownCard(
    categories: List<SpendingCategory>,
    periodLabel: String
) {
    val dims = Dimens.current
    val total = categories.sumOf { it.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.cardInnerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section label
            Text(
                text = "SPENDING ANALYSER — ${periodLabel.uppercase()}",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dims.sectionSpacing))

            // Donut chart with center text (3D version)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart3D(
                    categories = categories,
                    total = total,
                    modifier = Modifier.size(dims.donutChartSize)
                )
            }

            Spacer(modifier = Modifier.height(dims.sectionSpacing))

            // Legend grid — 2 columns, 3 rows
            LegendGrid(categories = categories)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty Filter Results Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyFilterResultsCard(
    onResetFilters: () -> Unit
) {
    val dims = Dimens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.cardInnerPadding * 1.5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No Transactions Found",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "No transactions match your active filters. Try adjusting or clearing filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onResetFilters,
                shape = RoundedCornerShape(dims.buttonCornerRadius)
            ) {
                Text("Reset Filters")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3D Donut Chart (Canvas)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DonutChart3D(
    categories: List<SpendingCategory>,
    total: Double,
    modifier: Modifier = Modifier
) {
    val strokeWidth = 38f
    val gapDegrees = 3f  // gap between arcs

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val depthOffset = Offset(topLeft.x, topLeft.y + 16f) // Shift down for 3D effect
            val arcSize = Size(diameter, diameter)

            // Draw depth layer (darkened)
            var startAngle = -90f
            categories.forEach { category ->
                val sweep = if (total > 0) (((category.amount / total) * 360f).toFloat() - gapDegrees) else 0f
                if (sweep > 0f) {
                    val darkened = Color(
                        red = category.color.red * 0.45f,
                        green = category.color.green * 0.45f,
                        blue = category.color.blue * 0.45f,
                        alpha = 1f
                    )
                    drawArc(
                        color = darkened,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = depthOffset,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }
                startAngle += sweep + gapDegrees
            }

            // Draw top layer
            startAngle = -90f
            categories.forEach { category ->
                val sweep = if (total > 0) (((category.amount / total) * 360f).toFloat() - gapDegrees) else 0f
                if (sweep > 0f) {
                    drawArc(
                        color = category.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }
                startAngle += sweep + gapDegrees
            }
        }

        // Center text overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCompact(total),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Legend Grid (2 columns)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LegendGrid(categories: List<SpendingCategory>) {
    val dims = Dimens.current
    // Chunk into rows of 2
    val rows = categories.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { category ->
                    LegendItem(
                        category = category,
                        modifier = Modifier.weight(1f)
                    )
                }
                // If odd number of items, fill remaining space
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(category: SpendingCategory, modifier: Modifier = Modifier) {
    val dims = Dimens.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(category.color)
        )
        // Label
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Amount
        Text(
            text = formatCompact(category.amount),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Income / Expenses Summary Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IncomeExpenseRow(income: Double, expenses: Double) {
    val dims = Dimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
    ) {
        SummaryCard(
            label = "INCOME",
            amount = income,
            accentColor = ChartColors.IncomeGreen,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "EXPENSES",
            amount = expenses,
            accentColor = ChartColors.ExpenseRed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.cardInnerPadding - 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCompact(amount),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "This month",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
