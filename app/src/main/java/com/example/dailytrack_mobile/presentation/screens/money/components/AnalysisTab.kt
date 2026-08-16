package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.screens.money.ChartColors
import com.example.dailytrack_mobile.presentation.screens.money.MoneyState
import com.example.dailytrack_mobile.presentation.screens.money.SpendingCategory
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun formatCompact(amount: Double): String {
    val abs = Math.abs(amount)
    return when {
        abs >= 1_00_000 -> "₹%.1fL".format(abs / 1_00_000)
        abs >= 1_000    -> "₹%.1fK".format(abs / 1_000)
        else            -> "₹%.0f".format(abs)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Analysis Tab
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnalysisTab(state: MoneyState) {
    val dims = Dimens.current
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
        item { SpendingBreakdownCard(categories = state.spendingCategories) }
        item { IncomeExpenseRow(income = state.totalIncome, expenses = state.totalExpenses) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Spending Breakdown Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SpendingBreakdownCard(categories: List<SpendingCategory>) {
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
                text = "SPENDING BREAKDOWN — JUL 2025",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dims.sectionSpacing))

            // Donut chart with center text
            DonutChart(
                categories = categories,
                total = total,
                modifier = Modifier.size(dims.donutChartSize)
            )

            Spacer(modifier = Modifier.height(dims.sectionSpacing))

            // Legend grid — 2 columns, 3 rows
            LegendGrid(categories = categories)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Donut Chart (Canvas)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DonutChart(
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
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f  // start from top

            categories.forEach { category ->
                val sweep = ((category.amount / total) * 360f).toFloat() - gapDegrees
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
