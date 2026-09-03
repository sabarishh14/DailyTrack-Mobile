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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.screens.money.*
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import com.example.dailytrack_mobile.presentation.theme.DtOgChartColors
import com.example.dailytrack_mobile.presentation.theme.LocalAppTheme
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
            top = dims.itemSpacingMedium,
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
                    periodLabel = filterState.financialYear ?: "ALL TIME",
                    onCategoryClick = { category ->
                        onAction(MoneyAction.ViewCategoryTransactions(category))
                    }
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

        // View Transactions Action Card
        item {
            ViewTransactionsActionCard(
                transactionCount = state.filteredTransactions.size,
                hasActiveFilters = filterState.hasActiveFilters,
                onClick = { onAction(MoneyAction.SelectTab(1)) }
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

            // 4. "Income Only" Quick Preset Pill
            val isIncomeOnly = filterState.selectedTypes == setOf(TransactionType.CREDIT)
            QuickPresetChip(
                text = "Income Only",
                isSelected = isIncomeOnly,
                onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.INCOME_ONLY)) }
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

            // Active Types (if not standard single debit or credit preset)
            if (filterState.selectedTypes != setOf(TransactionType.DEBIT) && filterState.selectedTypes != setOf(TransactionType.CREDIT)) {
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
    periodLabel: String,
    onCategoryClick: (String) -> Unit
) {
    val dims = Dimens.current
    val total = categories.sumOf { it.amount }

    val currentTheme = LocalAppTheme.current
    val isDtOg = currentTheme == AppTheme.DT_OG
    val primaryColor = MaterialTheme.colorScheme.primary

    val maxCategories = if (isDtOg) 10 else 6
    val processedCategories = remember(categories, primaryColor, isDtOg) {
        val baseList = if (categories.size > maxCategories) {
            val topN = categories.take(maxCategories - 1)
            val othersTotal = categories.drop(maxCategories - 1).sumOf { it.amount }
            topN + SpendingCategory("Others", othersTotal, primaryColor)
        } else {
            categories
        }
        
        if (isDtOg) {
            baseList.mapIndexed { index, cat ->
                cat.copy(color = DtOgChartColors.PieColors[index % DtOgChartColors.PieColors.size])
            }
        } else {
            val palette = generateThemeChartPalette(primaryColor, baseList.size)
            baseList.mapIndexed { index, cat ->
                cat.copy(color = palette[index])
            }
        }
    }

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

            // Donut chart with center text (3D for DT_OG, 2D for standard themes)
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
                DonutChart(
                    categories = processedCategories,
                    total = total,
                    isDtOgStyle = isDtOg,
                    modifier = Modifier.size(dims.donutChartSize)
                )
            }

            Spacer(modifier = Modifier.height(dims.sectionSpacing))

            // Legend grid — 2 columns, 3 rows
            LegendGrid(
                categories = processedCategories,
                isDtOg = isDtOg,
                onCategoryClick = onCategoryClick
            )
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
// Donut Chart (Canvas: Segmented 2D for DT_OG, standard 2D for other themes)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DonutChart(
    categories: List<SpendingCategory>,
    total: Double,
    isDtOgStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 8.dp.toPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isDtOgStyle) {
                // Flat 2D Donut Chart (DT_OG Theme): Thick annular segments with clean gaps & rounded corners
                val diameter = size.minDimension * 0.96f
                val rOuter = diameter / 2f
                val rInner = rOuter * 0.58f // ~42% thickness for chunky look matching reference
                val gapDegrees = 5f // 5 degree visible gap between segments
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f
                categories.forEach { category ->
                    val rawSweep = if (total > 0) (((category.amount / total) * 360f).toFloat()) else 0f
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(0f)
                    if (sweep > 0.5f) {
                        val sliceStartAngle = startAngle + (gapDegrees / 2f)
                        val slicePath = buildAnnularSectorPath(
                            center = center,
                            rInner = rInner,
                            rOuter = rOuter,
                            startAngleDeg = sliceStartAngle,
                            sweepAngleDeg = sweep,
                            cornerRadiusPx = cornerRadiusPx
                        )
                        drawPath(slicePath, category.color)
                    }
                    startAngle += rawSweep
                }
            } else {
                // Standard 2D Donut Chart (for other themes): Annular segments with softer proportions
                val diameter = size.minDimension * 0.92f
                val rOuter = diameter / 2f
                val rInner = rOuter * 0.64f  // Slightly thinner ring than DT_OG for elegance
                val gapDegrees = 3.5f        // Slightly tighter gaps
                val softCornerPx = cornerRadiusPx * 0.75f  // Softer corners
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f
                categories.forEach { category ->
                    val rawSweep = if (total > 0) (((category.amount / total) * 360f).toFloat()) else 0f
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(0f)
                    if (sweep > 0.5f) {
                        val sliceStartAngle = startAngle + (gapDegrees / 2f)
                        val slicePath = buildAnnularSectorPath(
                            center = center,
                            rInner = rInner,
                            rOuter = rOuter,
                            startAngleDeg = sliceStartAngle,
                            sweepAngleDeg = sweep,
                            cornerRadiusPx = softCornerPx
                        )
                        drawPath(slicePath, category.color)
                    }
                    startAngle += rawSweep
                }
            }
        }

        // Center text overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

/**
 * Builds an annular sector (donut segment) Path with rounded corners, matching d3.arc / Recharts.
 */
private fun buildAnnularSectorPath(
    center: Offset,
    rInner: Float,
    rOuter: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float,
    cornerRadiusPx: Float
): Path {
    val path = Path()
    if (sweepAngleDeg <= 0.5f || rOuter <= rInner) return path

    val degToRad = (Math.PI / 180.0).toFloat()
    val endAngleDeg = startAngleDeg + sweepAngleDeg

    val daOuter = (cornerRadiusPx / rOuter) * (180f / Math.PI.toFloat())
    val daInner = (cornerRadiusPx / rInner) * (180f / Math.PI.toFloat())
    val daOut = daOuter.coerceAtMost(sweepAngleDeg * 0.45f)
    val daIn = daInner.coerceAtMost(sweepAngleDeg * 0.45f)
    val rCorner = cornerRadiusPx.coerceAtMost((rOuter - rInner) * 0.45f)

    val cosStart = cos(startAngleDeg * degToRad)
    val sinStart = sin(startAngleDeg * degToRad)
    val cosEnd = cos(endAngleDeg * degToRad)
    val sinEnd = sin(endAngleDeg * degToRad)

    // Points on start radial line
    val pA = Offset(center.x + (rInner + rCorner) * cosStart, center.y + (rInner + rCorner) * sinStart)
    val pB = Offset(center.x + (rOuter - rCorner) * cosStart, center.y + (rOuter - rCorner) * sinStart)
    // Corner 1 vertex (Outer Start)
    val v1 = Offset(center.x + rOuter * cosStart, center.y + rOuter * sinStart)
    // Point on outer arc start
    val aOutStartRad = (startAngleDeg + daOut) * degToRad
    val pC = Offset(center.x + rOuter * cos(aOutStartRad), center.y + rOuter * sin(aOutStartRad))

    // Corner 2 vertex (Outer End)
    val v2 = Offset(center.x + rOuter * cosEnd, center.y + rOuter * sinEnd)
    // Point on end radial line (Outer)
    val pD = Offset(center.x + (rOuter - rCorner) * cosEnd, center.y + (rOuter - rCorner) * sinEnd)
    // Point on end radial line (Inner)
    val pE = Offset(center.x + (rInner + rCorner) * cosEnd, center.y + (rInner + rCorner) * sinEnd)

    // Corner 3 vertex (Inner End)
    val v3 = Offset(center.x + rInner * cosEnd, center.y + rInner * sinEnd)
    // Point on inner arc end
    val aInEndRad = (endAngleDeg - daIn) * degToRad
    val pF = Offset(center.x + rInner * cos(aInEndRad), center.y + rInner * sin(aInEndRad))

    // Corner 4 vertex (Inner Start)
    val v4 = Offset(center.x + rInner * cosStart, center.y + rInner * sinStart)

    // Build closed path
    path.moveTo(pA.x, pA.y)
    path.lineTo(pB.x, pB.y)
    path.quadraticTo(v1.x, v1.y, pC.x, pC.y)

    val outerSweep = sweepAngleDeg - 2 * daOut
    if (outerSweep > 0.1f) {
        path.arcTo(
            rect = Rect(center.x - rOuter, center.y - rOuter, center.x + rOuter, center.y + rOuter),
            startAngleDegrees = startAngleDeg + daOut,
            sweepAngleDegrees = outerSweep,
            forceMoveTo = false
        )
    }

    path.quadraticTo(v2.x, v2.y, pD.x, pD.y)
    path.lineTo(pE.x, pE.y)
    path.quadraticTo(v3.x, v3.y, pF.x, pF.y)

    val innerSweep = sweepAngleDeg - 2 * daIn
    if (innerSweep > 0.1f) {
        path.arcTo(
            rect = Rect(center.x - rInner, center.y - rInner, center.x + rInner, center.y + rInner),
            startAngleDegrees = endAngleDeg - daIn,
            sweepAngleDegrees = -innerSweep,
            forceMoveTo = false
        )
    }

    path.quadraticTo(v4.x, v4.y, pA.x, pA.y)
    path.close()

    return path
}

/**
 * Generates a harmonious chart palette from the theme's primary color.
 * Uses HSL hue rotation to produce visually distinct yet cohesive colors.
 * The first color is always the primary itself, with subsequent colors
 * spreading across a controlled hue arc while preserving saturation/lightness family.
 */
private fun generateThemeChartPalette(primary: Color, count: Int): List<Color> {
    if (count <= 0) return emptyList()

    // Convert primary to HSL
    val argb = (primary.alpha * 255).toInt() shl 24 or
            ((primary.red * 255).toInt() shl 16) or
            ((primary.green * 255).toInt() shl 8) or
            (primary.blue * 255).toInt()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    val baseHue = hsl[0]      // 0-360
    val baseSat = hsl[1]      // 0-1
    val baseLit = hsl[2]      // 0-1

    // Spread hues across a 180° arc centered on the primary hue, with
    // alternating saturation/lightness tweaks for extra distinction.
    val hueSpread = when {
        count <= 2 -> 60f
        count <= 4 -> 120f
        else -> 180f
    }
    val step = if (count > 1) hueSpread / (count - 1) else 0f
    val startHue = baseHue - hueSpread / 2f

    return List(count) { i ->
        val hue = ((startHue + step * i) % 360f + 360f) % 360f
        // Alternate saturation and lightness slightly for more distinction
        val satOffset = if (i % 2 == 0) 0f else -0.08f
        val litOffset = when (i % 3) {
            0 -> 0f
            1 -> 0.04f
            else -> -0.04f
        }
        val sat = (baseSat + satOffset).coerceIn(0.25f, 1f)
        val lit = (baseLit + litOffset).coerceIn(0.30f, 0.70f)
        val outHsl = floatArrayOf(hue, sat, lit)
        val resultArgb = ColorUtils.HSLToColor(outHsl)
        Color(
            red = (resultArgb shr 16 and 0xFF) / 255f,
            green = (resultArgb shr 8 and 0xFF) / 255f,
            blue = (resultArgb and 0xFF) / 255f,
            alpha = 1f
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Legend Grid (2 columns)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LegendGrid(
    categories: List<SpendingCategory>,
    isDtOg: Boolean = false,
    onCategoryClick: (String) -> Unit
) {
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
                        isDtOg = isDtOg,
                        onClick = {
                            if (category.name != "Others") {
                                onCategoryClick(category.name)
                            }
                        },
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
private fun LegendItem(
    category: SpendingCategory,
    isDtOg: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    val indicatorShape = if (isDtOg) RoundedCornerShape(3.dp) else CircleShape

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dims.buttonCornerRadius - 4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
    ) {
        // Color dot / square
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(indicatorShape)
                .background(category.color)
        )
        // Label
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false)
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

// ─────────────────────────────────────────────────────────────────────────────
// View Transactions Action Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ViewTransactionsActionCard(
    transactionCount: Int,
    hasActiveFilters: Boolean,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.cardInnerPadding, vertical = dims.itemSpacingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
                modifier = Modifier.weight(1f)
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "Transactions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (hasActiveFilters) "View Filtered Transactions" else "View All Transactions",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (transactionCount == 1) "1 transaction matches current filters"
                               else "$transactionCount transactions match current filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Arrow button / pill
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Transactions",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
