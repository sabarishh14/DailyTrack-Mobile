package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import com.example.dailytrack_mobile.presentation.components.MonthYearPickerDialog
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.dailytrack_mobile.presentation.screens.money.*
import com.example.dailytrack_mobile.presentation.theme.AppTheme
import com.example.dailytrack_mobile.presentation.theme.DtOgChartColors
import com.example.dailytrack_mobile.presentation.theme.LocalAppTheme
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun formatExactCurrency(amount: Double): String {
    val isNegative = amount < 0
    val absAmount = Math.abs(amount)
    val integerPart = absAmount.toLong()
    val remainder = ((absAmount - integerPart) * 100).roundToLong()
    val decimalStr = if (remainder > 0) String.format(java.util.Locale.US, ".%02d", remainder) else ""

    val str = integerPart.toString()
    val formattedInt = if (str.length <= 3) {
        str
    } else {
        val last3 = str.substring(str.length - 3)
        val rest = str.substring(0, str.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count == 2 && i > 0) {
                sb.append(',')
                count = 0
            }
        }
        sb.reverse().toString() + "," + last3
    }

    val sign = if (isNegative) "-" else ""
    return "${sign}₹$formattedInt$decimalStr"
}

private fun formatShortened(amount: Double, withPrefix: Boolean = false): String {
    val abs = Math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    val prefix = if (withPrefix) "₹" else ""
    val (num, suffix) = when {
        abs >= 1_00_00_000 -> (abs / 1_00_00_000) to "Cr"
        abs >= 1_00_000 -> (abs / 1_00_000) to "L"
        abs >= 1_000 -> (abs / 1_000) to "k"
        else -> abs to ""
    }
    val formattedNum = if (suffix.isEmpty()) {
        String.format(java.util.Locale.US, "%.0f", num)
    } else if (num >= 100) {
        String.format(java.util.Locale.US, "%.0f%s", num, suffix)
    } else if (num >= 10 || suffix == "k") {
        val s = String.format(java.util.Locale.US, "%.1f%s", num, suffix)
        s.replace(".0", "")
    } else {
        val s = String.format(java.util.Locale.US, "%.2f%s", num, suffix)
        s.replace(".00", "").replace(Regex("""(\.\d)0$"""), "$1")
    }
    return "$sign$prefix$formattedNum"
}

private fun formatCompact(amount: Double): String {
    val abs = Math.abs(amount)
    return when {
        abs >= 1_00_000 -> String.format(java.util.Locale.US, "%.1fL", amount / 1_00_000)
        abs >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", amount / 1_000)
        else -> String.format(java.util.Locale.US, "%.0f", amount)
    }
}

private fun cleanDescriptionTitle(raw: String): String {
    var text = raw.trim()
    // Strip leading payment method tags like UPI/, UPI-, POS-, POS , IMPS-, NEFT-, etc.
    text = text.replace(Regex("^(UPI[-/]|POS[- ]|IMPS[-/]|NEFT[-/]|ACH[-/]|BILLDESK[- ]|PAYTM[-* ]|RAZORPAY[-* ]|BBPS[-/])", RegexOption.IGNORE_CASE), "")
    // Strip "Paid to " or "Transfer to " prefixes
    text = text.replace(Regex("^(Paid to |Transfer to |Payment to |To )", RegexOption.IGNORE_CASE), "")
    // If there's an internal slash separation (like 12345/Merchant/Bank), pick the most descriptive word
    if (text.contains("/")) {
        val parts = text.split("/").map { it.trim() }.filter { it.length > 2 && !it.all { ch -> ch.isDigit() } }
        if (parts.isNotEmpty()) {
            text = parts.firstOrNull { !it.equals("UPI", ignoreCase = true) && !it.equals("OK", ignoreCase = true) } ?: parts.first()
        }
    }
    // Remove trailing reference numbers/IDs like /1234567 or - 1234567
    text = text.replace(Regex("[-/]\\s*\\d{6,}.*$"), "")
    text = text.replace(Regex("\\s+"), " ").trim()
    return text.ifBlank { "Other" }
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
    val isInitialLoading = state.isLoading && state.transactions.isEmpty()

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
            val periodTitle = filterState.financialYear ?: when (filterState.activeDatePreset) {
                QuickFilterPreset.THIS_MONTH -> "THIS MONTH"
                QuickFilterPreset.LAST_MONTH -> "LAST MONTH"
                else -> filterState.formattedDateRange() ?: "ALL TIME"
            }
            if (isInitialLoading && categories.isEmpty()) {
                CashFlowBreakdownCard(
                    categories = emptyList(),
                    transactions = emptyList(),
                    periodLabel = periodTitle,
                    hasActiveFilters = filterState.hasActiveFilters,
                    isLoading = true,
                    onViewTransactions = {}
                )
            } else if (categories.isNotEmpty()) {
                CashFlowBreakdownCard(
                    categories = categories,
                    transactions = state.filteredAnalysisTransactions,
                    periodLabel = periodTitle,
                    hasActiveFilters = filterState.hasActiveFilters,
                    isLoading = false,
                    onViewTransactions = { category ->
                        if (category != null) {
                            onAction(MoneyAction.ViewCategoryTransactions(category))
                        } else {
                            onAction(MoneyAction.SelectTab(1))
                        }
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
            val periodSubtitle = filterState.financialYear ?: when (filterState.activeDatePreset) {
                QuickFilterPreset.THIS_MONTH -> "This month"
                QuickFilterPreset.LAST_MONTH -> "Last month"
                else -> filterState.formattedDateRange() ?: "All time"
            }
            IncomeExpenseRow(
                income = state.filteredTotalIncome,
                expenses = state.filteredTotalExpenses,
                periodLabel = periodSubtitle,
                isLoading = isInitialLoading
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

    val scrollState = rememberScrollState()
    var showMonthYearPicker by remember { mutableStateOf(false) }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            selectedMonth = filterState.selectedMonth,
            selectedYear = filterState.selectedYear ?: LocalDate.now().year,
            onSelected = { month, year ->
                onAction(MoneyAction.SelectMonthYearFilter(month, year))
                showMonthYearPicker = false
            },
            onDismiss = { showMonthYearPicker = false }
        )
    }

    LaunchedEffect(filterState.hasActiveFilters) {
        if (!filterState.hasActiveFilters) {
            scrollState.animateScrollTo(0)
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
            // Main Filter Button with Badge (Always visible at start!)
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

            // Clear Button (Visible when active filters exist)
            if (filterState.hasActiveFilters) {
                Surface(
                    onClick = { onAction(MoneyAction.ResetAnalysisFilters) },
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear all filters",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Vertical divider separating main filter button from quick pills
            VerticalDivider(
                modifier = Modifier.height(20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // ─── 1. ACTIVE / SELECTED FILTERS FIRST ──────────────────────────

            // A. Active Categories (Included / Excluded)
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

            // B. Active Accounts (Included / Excluded)
            val sortedAccountFilters = remember(filterState.accountFilters) {
                sortAccountsCanonical(filterState.accountFilters.keys.toList()).mapNotNull { acc ->
                    filterState.accountFilters[acc]?.let { status -> acc to status }
                }
            }
            sortedAccountFilters.forEach { (acc, status) ->
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

            // C. Active Date Filter (Month/Year, Preset, Financial Year, or Custom Range)
            val isMonthYearActive = filterState.selectedYear != null
            if (isMonthYearActive) {
                val monthYearActiveText = when {
                    filterState.selectedMonth != null ->
                        "${filterState.selectedMonth.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${filterState.selectedYear}"
                    else ->
                        "Year ${filterState.selectedYear}"
                }
                ActiveFilterRemovableChip(
                    text = monthYearActiveText,
                    onRemove = { onAction(MoneyAction.ClearDateRangeFilter) }
                )
            } else if (filterState.activeDatePreset == QuickFilterPreset.THIS_MONTH) {
                QuickPresetChip(
                    text = "This Month",
                    isSelected = true,
                    onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.THIS_MONTH)) }
                )
            } else if (filterState.activeDatePreset == QuickFilterPreset.LAST_MONTH) {
                QuickPresetChip(
                    text = "Last Month",
                    isSelected = true,
                    onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.LAST_MONTH)) }
                )
            } else if (!filterState.financialYear.isNullOrBlank() && filterState.financialYear != "All Time") {
                ActiveFilterRemovableChip(
                    text = filterState.financialYear,
                    onRemove = { onAction(MoneyAction.ClearFinancialYearFilter) }
                )
            } else if (filterState.customDateRange != null) {
                filterState.formattedDateRange()?.let { rangeText ->
                    ActiveFilterRemovableChip(
                        text = rangeText,
                        onRemove = { onAction(MoneyAction.ClearDateRangeFilter) }
                    )
                }
            }

            // D. Active Type Filter
            val isExpensesOnly = filterState.selectedTypes == setOf(TransactionType.DEBIT)
            val isIncomeOnly = filterState.selectedTypes == setOf(TransactionType.CREDIT)
            if (isExpensesOnly) {
                QuickPresetChip(
                    text = "Expenses Only",
                    isSelected = true,
                    onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.EXPENSES_ONLY)) }
                )
            } else if (isIncomeOnly) {
                QuickPresetChip(
                    text = "Income Only",
                    isSelected = true,
                    onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.INCOME_ONLY)) }
                )
            } else if (filterState.selectedTypes.isNotEmpty() && filterState.selectedTypes.size != TransactionType.values().size) {
                filterState.selectedTypes.forEach { type ->
                    ActiveFilterRemovableChip(
                        text = type.displayName,
                        onRemove = { onAction(MoneyAction.RemoveTypeFilter(type)) }
                    )
                }
            }

            // Subtle divider between active filters and available quick presets
            if (filterState.hasActiveFilters) {
                VerticalDivider(
                    modifier = Modifier.height(16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }

            // ─── 2. AVAILABLE / UNSELECTED PRESETS ───────────────────────────
            if (filterState.activeDatePreset != QuickFilterPreset.THIS_MONTH) {
                QuickPresetChip(
                    text = "This Month",
                    isSelected = false,
                    onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.THIS_MONTH)) }
                )
            }

            if (!isExpensesOnly) {
                QuickPresetChip(
                    text = "Expenses Only",
                    isSelected = false,
                    onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.EXPENSES_ONLY)) }
                )
            }

            if (filterState.activeDatePreset != QuickFilterPreset.LAST_MONTH) {
                QuickPresetChip(
                    text = "Last Month",
                    isSelected = false,
                    onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.LAST_MONTH)) }
                )
            }

            if (!isMonthYearActive) {
                QuickPresetChip(
                    text = "Month / Year",
                    isSelected = false,
                    onClick = { showMonthYearPicker = true }
                )
            }

            if (!isIncomeOnly) {
                QuickPresetChip(
                    text = "Income Only",
                    isSelected = false,
                    onClick = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.INCOME_ONLY)) }
                )
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
    transactions: List<Transaction>,
    periodLabel: String,
    hasActiveFilters: Boolean = false,
    isLoading: Boolean = false,
    onViewTransactions: (String?) -> Unit
) {
    val dims = Dimens.current
    val total = categories.sumOf { it.amount }

    val currentTheme = LocalAppTheme.current
    val isDtOg = currentTheme == AppTheme.DT_OG
    val primaryColor = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()

    var activeDrilldownCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var drilldownBackStack by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    val isOthersDrilldown = activeDrilldownCategory == "__OTHERS__"

    val handleBack: () -> Unit = {
        if (drilldownBackStack.isNotEmpty()) {
            activeDrilldownCategory = drilldownBackStack.last()
            drilldownBackStack = drilldownBackStack.dropLast(1)
        } else {
            activeDrilldownCategory = null
        }
    }

    // Intercept back button when drill-down is active
    BackHandler(enabled = activeDrilldownCategory != null) {
        handleBack()
    }

    // Filter transactions for the selected drill-down category
    val categoryTransactions = remember(transactions, activeDrilldownCategory) {
        if (activeDrilldownCategory == null || activeDrilldownCategory == "__OTHERS__") emptyList()
        else transactions.filter { it.category.equals(activeDrilldownCategory, ignoreCase = true) }
    }

    // Auto-reset if category no longer exists in filtered transactions (ignore for __OTHERS__)
    LaunchedEffect(categoryTransactions, activeDrilldownCategory, isLoading) {
        if (activeDrilldownCategory != null && !isOthersDrilldown && categoryTransactions.isEmpty() && !isLoading) {
            handleBack()
        }
    }

    // Calculate drill-down breakdown (Top 4 descriptions + "Others")
    val categoryTotal = remember(categoryTransactions) {
        categoryTransactions.sumOf { Math.abs(it.amount) }
    }

    val othersColor = if (isDtOg) Color(0xFF888888) else Color(0xFF7A889B)

    val drilldownItemsPerPage = 6
    val maxDrilldownPages = 5
    val maxDrilldownItems = drilldownItemsPerPage * maxDrilldownPages

    val processedDrilldownCategories = remember(categoryTransactions, categoryTotal, primaryColor, isDtOg, othersColor) {
        if (categoryTransactions.isEmpty()) emptyList()
        else {
            val grouped = categoryTransactions.groupBy { tx ->
                val raw = tx.note?.takeIf { it.isNotBlank() }
                    ?: tx.description?.takeIf { it.isNotBlank() }
                    ?: tx.title.takeIf { it.isNotBlank() && !it.equals(tx.category, ignoreCase = true) }
                    ?: "General"
                cleanDescriptionTitle(raw)
            }
            val aggregated = grouped.map { (desc, txs) ->
                desc to txs.sumOf { Math.abs(it.amount) }
            }.sortedByDescending { it.second }

            val items = if (aggregated.size <= maxDrilldownItems) {
                aggregated
            } else {
                val topItems = aggregated.take(maxDrilldownItems - 1)
                val othersSum = aggregated.drop(maxDrilldownItems - 1).sumOf { it.second }
                topItems + listOf("Others" to othersSum)
            }

            if (isDtOg) {
                items.mapIndexed { idx, (name, amt) ->
                    val color = if (name == "Others") othersColor else DtOgChartColors.PieColors[idx % DtOgChartColors.PieColors.size]
                    SpendingCategory(name, amt, color)
                }
            } else {
                val palette = generateThemeChartPalette(primaryColor, items.size)
                items.mapIndexed { idx, (name, amt) ->
                    val color = if (name == "Others") othersColor else palette[idx]
                    SpendingCategory(name, amt, color)
                }
            }
        }
    }

    val drilldownPages = remember(processedDrilldownCategories, drilldownItemsPerPage) {
        processedDrilldownCategories.chunked(drilldownItemsPerPage)
    }
    val drilldownPagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { drilldownPages.size.coerceAtLeast(1) }
    )

    LaunchedEffect(activeDrilldownCategory) {
        if (activeDrilldownCategory != null && activeDrilldownCategory != "__OTHERS__" && drilldownPages.isNotEmpty() && drilldownPagerState.currentPage != 0) {
            drilldownPagerState.scrollToPage(0)
        }
    }

    // Top descriptions for the donut chart: Top 7 individual descriptions + 1 "Others" (with sleek slate color)
    val drilldownDonutCategories = remember(processedDrilldownCategories, categoryTotal, othersColor) {
        if (processedDrilldownCategories.size <= 7) {
            processedDrilldownCategories
        } else {
            val top7 = processedDrilldownCategories.take(7)
            val remainingAmt = (categoryTotal - top7.sumOf { it.amount }).coerceAtLeast(0.0)
            top7 + listOf(SpendingCategory("Others", remainingAmt, othersColor))
        }
    }

    // Assign colors to all categories. For > 10 categories, the top 9 get full-spread theme palette.
    val processedCategories = remember(categories, primaryColor, isDtOg) {
        if (isDtOg) {
            categories.mapIndexed { index, cat ->
                cat.copy(color = DtOgChartColors.PieColors[index % DtOgChartColors.PieColors.size])
            }
        } else {
            val palette = if (categories.size <= 10) {
                generateThemeChartPalette(primaryColor, categories.size)
            } else {
                val top9 = generateThemeChartPalette(primaryColor, 9)
                val rest = generateThemeChartPalette(primaryColor.copy(alpha = 0.7f), categories.size - 9)
                top9 + rest
            }
            categories.mapIndexed { index, cat ->
                cat.copy(color = palette[index])
            }
        }
    }

    // ─── OTHER CATEGORIES BREAKDOWN (For the "Others" Drilldown) ─────────────
    val otherCategoriesRaw = remember(categories) {
        if (categories.size <= 10) emptyList() else categories.drop(9)
    }
    val othersTotal = remember(otherCategoriesRaw) { otherCategoriesRaw.sumOf { it.amount } }

    val otherCategories = remember(otherCategoriesRaw, primaryColor, isDtOg) {
        if (otherCategoriesRaw.isEmpty()) emptyList()
        else {
            if (isDtOg) {
                otherCategoriesRaw.mapIndexed { idx, cat ->
                    cat.copy(color = DtOgChartColors.PieColors[(idx + 9) % DtOgChartColors.PieColors.size])
                }
            } else {
                val palette = generateThemeChartPalette(primaryColor.copy(alpha = 0.85f), otherCategoriesRaw.size)
                otherCategoriesRaw.mapIndexed { idx, cat ->
                    cat.copy(color = palette[idx])
                }
            }
        }
    }

    val othersDonutCategories = remember(otherCategories, othersTotal, othersColor) {
        if (otherCategories.size <= 7) {
            otherCategories
        } else {
            val top7 = otherCategories.take(7)
            val remainingAmt = (othersTotal - top7.sumOf { it.amount }).coerceAtLeast(0.0)
            top7 + listOf(SpendingCategory("Others", remainingAmt, othersColor))
        }
    }

    val otherCategoriesPages = remember(otherCategories) {
        otherCategories.chunked(6)
    }
    val otherCategoriesPagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { otherCategoriesPages.size.coerceAtLeast(1) }
    )

    LaunchedEffect(activeDrilldownCategory) {
        if (isOthersDrilldown && otherCategoriesPages.isNotEmpty() && otherCategoriesPagerState.currentPage != 0) {
            otherCategoriesPagerState.scrollToPage(0)
        }
    }

    // Top categories for the main donut chart: Exactly 10 portions max (Top 9 individual categories + 1 "Others" slice with sleek slate color)
    val mainDonutCategories = remember(processedCategories, total, othersTotal, othersColor) {
        if (processedCategories.size <= 10) {
            processedCategories
        } else {
            val top9 = processedCategories.take(9)
            val othersSlice = SpendingCategory("Others", othersTotal, othersColor)
            top9 + listOf(othersSlice)
        }
    }

    // Main level pills: Exactly 10 pills (Top 9 individual categories + 1 "Others" pill). NO pager needed!
    val mainPills = remember(categories, processedCategories, total, othersTotal, otherCategoriesRaw, othersColor) {
        if (categories.size <= 10) {
            processedCategories
        } else {
            val top9 = processedCategories.take(9)
            val othersPill = SpendingCategory("Others (${otherCategoriesRaw.size})", othersTotal, othersColor)
            top9 + listOf(othersPill)
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
                .padding(
                    start = dims.cardInnerPadding,
                    end = dims.cardInnerPadding,
                    top = dims.cardInnerPadding,
                    bottom = (dims.cardInnerPadding - 6.dp).coerceAtLeast(8.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            if (activeDrilldownCategory != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { handleBack() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Back",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = if (isOthersDrilldown) "📦 OTHER CATEGORIES" else "${CategoryEmojis.forCategory(activeDrilldownCategory!!)} ${activeDrilldownCategory!!.uppercase()}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val share = if (total > 0) {
                        if (isOthersDrilldown) (othersTotal / total) * 100.0 else (categoryTotal / total) * 100.0
                    } else 0.0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "%.1f%%".format(share),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "SPENDING ANALYSER — ${periodLabel.uppercase()}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(dims.itemSpacingMedium))

            // Donut chart with center text (3D for DT_OG, 2D for standard themes) or Inside-Card Loading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading && categories.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dims.donutChartSize),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
                        Text(
                            text = "Analyzing spending...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (isOthersDrilldown) {
                    DonutChart(
                        categories = othersDonutCategories,
                        total = othersTotal,
                        isDtOgStyle = isDtOg,
                        centerTitle = "OTHER CATS",
                        modifier = Modifier.size(dims.donutChartSize)
                    )
                } else if (activeDrilldownCategory != null) {
                    DonutChart(
                        categories = drilldownDonutCategories,
                        total = categoryTotal,
                        isDtOgStyle = isDtOg,
                        centerTitle = "${CategoryEmojis.forCategory(activeDrilldownCategory!!)} ${activeDrilldownCategory!!.uppercase()}",
                        modifier = Modifier.size(dims.donutChartSize)
                    )
                } else {
                    DonutChart(
                        categories = mainDonutCategories,
                        total = total,
                        isDtOgStyle = isDtOg,
                        centerTitle = "TOTAL",
                        modifier = Modifier.size(dims.donutChartSize)
                    )
                }
            }

            if (!isLoading) {
                Spacer(modifier = Modifier.height(dims.itemSpacingSmall))

                if (isOthersDrilldown) {
                    // Drilldown Mode: OTHER CATEGORIES (Paginated 6 per page)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALL OTHER CATEGORIES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (otherCategoriesPages.size > 1) {
                            Text(
                                text = "Page ${otherCategoriesPagerState.currentPage + 1} of ${otherCategoriesPages.size} ›",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (otherCategoriesPages.size > 1) {
                        HorizontalPager(
                            state = otherCategoriesPagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { pageIndex ->
                            val pageCategories = otherCategoriesPages.getOrElse(pageIndex) { emptyList() }
                            LegendGrid(
                                categories = pageCategories,
                                totalAmount = othersTotal,
                                isDtOg = isDtOg,
                                onCategoryClick = { clickedCat ->
                                    drilldownBackStack = drilldownBackStack + listOf("__OTHERS__")
                                    activeDrilldownCategory = clickedCat
                                }
                            )
                        }

                        // Page Indicator Dots
                        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(otherCategoriesPages.size) { index ->
                                val isSelected = otherCategoriesPagerState.currentPage == index
                                val width by animateDpAsState(
                                    targetValue = if (isSelected) 18.dp else 6.dp,
                                    label = "others_dot_width"
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(6.dp)
                                        .width(width)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                        )
                                        .clickable {
                                            scope.launch {
                                                otherCategoriesPagerState.animateScrollToPage(index)
                                            }
                                        }
                                )
                            }
                        }
                    } else if (otherCategoriesPages.isNotEmpty()) {
                        LegendGrid(
                            categories = otherCategoriesPages[0],
                            totalAmount = othersTotal,
                            isDtOg = isDtOg,
                            onCategoryClick = { clickedCat ->
                                drilldownBackStack = drilldownBackStack + listOf("__OTHERS__")
                                activeDrilldownCategory = clickedCat
                            }
                        )
                    }
                } else if (activeDrilldownCategory != null) {
                    // Drilldown Mode: TOP DESCRIPTIONS (Paginated & Swipeable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOP DESCRIPTIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (drilldownPages.size > 1) {
                            Text(
                                text = "Page ${drilldownPagerState.currentPage + 1} of ${drilldownPages.size} ›",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (drilldownPages.size > 1) {
                        HorizontalPager(
                            state = drilldownPagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { pageIndex ->
                            val pageCategories = drilldownPages.getOrElse(pageIndex) { emptyList() }
                            LegendGrid(
                                categories = pageCategories,
                                totalAmount = categoryTotal,
                                isDtOg = isDtOg,
                                onCategoryClick = { /* Leaf description pills */ }
                            )
                        }

                        // Page Indicator Dots
                        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(drilldownPages.size) { index ->
                                val isSelected = drilldownPagerState.currentPage == index
                                val width by animateDpAsState(
                                    targetValue = if (isSelected) 18.dp else 6.dp,
                                    label = "drilldown_dot_width"
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(6.dp)
                                        .width(width)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                        )
                                        .clickable {
                                            scope.launch {
                                                drilldownPagerState.animateScrollToPage(index)
                                            }
                                        }
                                )
                            }
                        }
                    } else if (drilldownPages.isNotEmpty()) {
                        LegendGrid(
                            categories = drilldownPages[0],
                            totalAmount = categoryTotal,
                            isDtOg = isDtOg,
                            onCategoryClick = { /* Leaf description pills */ }
                        )
                    }
                } else {
                    // Normal Mode: CATEGORIES (Max 10 pills, exactly matching Donut Chart 1:1, NO Pager!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATEGORIES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap to explore ›",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LegendGrid(
                        categories = mainPills,
                        totalAmount = total,
                        isDtOg = isDtOg,
                        onCategoryClick = { clickedCat ->
                            if (clickedCat.startsWith("Others")) {
                                drilldownBackStack = emptyList()
                                activeDrilldownCategory = "__OTHERS__"
                            } else {
                                drilldownBackStack = emptyList()
                                activeDrilldownCategory = clickedCat
                            }
                        }
                    )
                }

                // In-Card Action Button in the exact same spot for both Normal & Drilldown states
                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = { onViewTransactions(if (isOthersDrilldown) null else activeDrilldownCategory) },
                    shape = RoundedCornerShape(dims.buttonCornerRadius),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isOthersDrilldown) {
                            "View All Other Transactions →"
                        } else if (activeDrilldownCategory != null) {
                            "View All $activeDrilldownCategory Transactions →"
                        } else if (hasActiveFilters) {
                            "View Filtered Transactions →"
                        } else {
                            "View All Transactions →"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
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
    centerTitle: String = "TOTAL",
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 8.dp.toPx() }
    val sliceTotal = remember(categories) { categories.sumOf { it.amount } }

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
                val gapDegrees = if (categories.size > 1) 5f else 0f // 5 degree visible gap between segments
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f
                categories.forEach { category ->
                    val rawSweep = if (sliceTotal > 0) (((category.amount / sliceTotal) * 360f).toFloat()) else 0f
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
                val gapDegrees = if (categories.size > 1) 3.5f else 0f // Slightly tighter gaps
                val softCornerPx = cornerRadiusPx * 0.75f  // Softer corners
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f
                categories.forEach { category ->
                    val rawSweep = if (sliceTotal > 0) (((category.amount / sliceTotal) * 360f).toFloat()) else 0f
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
                text = centerTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    totalAmount: Double = 0.0,
    isDtOg: Boolean = false,
    onCategoryClick: (String) -> Unit
) {
    // Chunk into rows of 2
    val rows = categories.chunked(2)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { category ->
                    val percentage = if (totalAmount > 0) (category.amount / totalAmount) * 100.0 else 0.0
                    val percentageStr = "%.1f%%".format(percentage)
                    LegendItem(
                        category = category,
                        percentageText = percentageStr,
                        isDtOg = isDtOg,
                        onClick = { onCategoryClick(category.name) },
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
    percentageText: String? = null,
    isDtOg: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorShape = if (isDtOg) RoundedCornerShape(2.dp) else CircleShape

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Color dot / square
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(indicatorShape)
                        .background(category.color)
                )
                // Label
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            // Amount & Percentage Stack
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "₹${formatCompact(category.amount)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (!percentageText.isNullOrBlank()) {
                    Text(
                        text = percentageText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Income / Expenses Summary Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IncomeExpenseRow(
    income: Double,
    expenses: Double,
    periodLabel: String = "This month",
    isLoading: Boolean = false
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
    ) {
        SummaryCard(
            label = "INCOME",
            amount = income,
            accentColor = ChartColors.IncomeGreen,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            periodLabel = periodLabel,
            isLoading = isLoading,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "EXPENSES",
            amount = expenses,
            accentColor = ChartColors.ExpenseRed,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            periodLabel = periodLabel,
            isLoading = isLoading,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    accentColor: Color,
    icon: ImageVector,
    periodLabel: String,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    var isExactPrimary by rememberSaveable { mutableStateOf(true) }

    val exactText = formatExactCurrency(amount)
    val shortenedText = formatShortened(amount, withPrefix = false)
    val shortenedWithCurrency = formatShortened(amount, withPrefix = true)

    // Dynamic auto-scaling font size for exact amount to comfortably fit within half-screen card width
    val dynamicFontSize = remember(exactText.length) {
        when {
            exactText.length <= 6 -> 22.sp
            exactText.length <= 8 -> 19.sp
            exactText.length <= 10 -> 17.sp
            exactText.length <= 12 -> 15.sp
            else -> 13.sp
        }
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(dims.cardCornerRadius))
            .clickable(enabled = !isLoading) { isExactPrimary = !isExactPrimary },
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Icon + Type label on left, Shortened Pill Badge on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = accentColor
                    )
                }

                // Shortened / Exact badge (swaps on tap or shows placeholder when loading)
                Surface(
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    contentColor = accentColor
                ) {
                    Text(
                        text = if (isLoading) "—" else if (isExactPrimary) shortenedText else exactText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Primary amount display or inside-card loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                }
            } else {
                AnimatedContent(
                    targetState = isExactPrimary,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 3 }) togetherWith (fadeOut() + slideOutVertically { -it / 3 })
                    },
                    label = "AmountDisplay"
                ) { showExact ->
                    if (showExact) {
                        Text(
                            text = exactText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = dynamicFontSize,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = shortenedWithCurrency,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Subtitle: Active filter period + interactive toggle hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (!isLoading) {
                    Text(
                        text = if (isExactPrimary) "tap for k/L" else "tap for exact",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}


