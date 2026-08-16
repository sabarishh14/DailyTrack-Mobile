package com.example.dailytrack_mobile.presentation.screens.invest

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun formatCompact(amount: Double): String {
    val abs = Math.abs(amount)
    return when {
        abs >= 1_00_00_000 -> "₹%.2fCr".format(abs / 1_00_00_000)
        abs >= 1_00_000    -> "₹%.2fL".format(abs / 1_00_000)
        abs >= 1_000       -> "₹%.1fK".format(abs / 1_000)
        else               -> "₹%.0f".format(abs)
    }
}

private fun formatPnl(amount: Double): String {
    val prefix = if (amount >= 0) "+" else "-"
    return "$prefix${formatCompact(amount)}"
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InvestmentsScreen(
    viewModel: InvestVM = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val dims = Dimens.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = dims.screenBottomPadding)
    ) {
        // ── Portfolio header ────────────────────────────────────────────
        item {
            PortfolioHeader(state = state)
        }

        // ── Sparkline chart ─────────────────────────────────────────────
        item {
            SparklineChart(
                points = state.chartPoints,
                isGain = state.isOverallGain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.sparklineHeight)
                    .padding(horizontal = dims.screenHorizontalPadding)
            )
        }

        // ── Summary row (Invested / Current / P&L) ─────────────────────
        item {
            SummaryRow(state = state)
        }

        // ── Filter pill tabs ────────────────────────────────────────────
        item {
            FilterPills(
                selectedTab = state.selectedTab,
                onTabSelected = { viewModel.onAction(InvestAction.SelectTab(it)) }
            )
        }

        // ── Content based on selected tab ───────────────────────────────
        when (state.selectedTab) {
            InvestTab.OVERVIEW -> {
                // Show category summary cards
                items(state.categorySummaries) { summary ->
                    CategorySummaryCard(summary = summary)
                }
            }
            else -> {
                // Show individual holdings
                val filtered = state.filteredHoldings
                val catInvested = filtered.sumOf { it.invested }
                val catCurrent = filtered.sumOf { it.current }
                val catPnl = catCurrent - catInvested
                val catPnlPercent = if (catInvested == 0.0) 0.0 else (catPnl / catInvested) * 100.0

                item {
                    TabSummaryHeader(
                        label = state.selectedTab.label,
                        invested = catInvested,
                        current = catCurrent,
                        pnl = catPnl,
                        pnlPercent = catPnlPercent
                    )
                }

                items(filtered) { holding ->
                    HoldingItem(holding = holding)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Portfolio Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PortfolioHeader(state: InvestState) {
    val dims = Dimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dims.screenHorizontalPadding + 4.dp,
                end = dims.screenHorizontalPadding + 4.dp,
                top = dims.screenTopPadding,
                bottom = dims.itemSpacingMedium
            )
    ) {
        Text(
            text = "PORTFOLIO VALUE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatCompact(state.totalCurrent),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (state.isOverallGain) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = if (state.isOverallGain) InvestColors.GainGreen else InvestColors.LossRed,
                modifier = Modifier.size(dims.iconSizeSmall)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${formatPnl(state.totalPnl)} (${String.format("%.1f", state.totalPnlPercent)}%)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (state.isOverallGain) InvestColors.GainGreen else InvestColors.LossRed
            )
            Spacer(modifier = Modifier.width(dims.itemSpacingMedium))
            Text(
                text = "Overall",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sparkline Chart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SparklineChart(
    points: List<Float>,
    isGain: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isGain) InvestColors.GainGreen else InvestColors.LossRed

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val minVal = points.min()
        val maxVal = points.max()
        val range = (maxVal - minVal).coerceAtLeast(0.01f)

        val stepX = size.width / (points.size - 1)
        val paddingY = 12f

        fun yOf(value: Float): Float {
            return size.height - paddingY - ((value - minVal) / range) * (size.height - 2 * paddingY)
        }

        // Build line path
        val linePath = Path().apply {
            moveTo(0f, yOf(points[0]))
            for (i in 1 until points.size) {
                lineTo(i * stepX, yOf(points[i]))
            }
        }

        // Draw gradient fill under the line
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.3f),
                    lineColor.copy(alpha = 0.05f),
                    Color.Transparent
                )
            ),
            style = Fill
        )

        // Draw line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 3f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw end dot
        val lastX = (points.size - 1) * stepX
        val lastY = yOf(points.last())
        drawCircle(
            color = lineColor,
            radius = 5f,
            center = Offset(lastX, lastY)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SummaryRow(state: InvestState) {
    val dims = Dimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
    ) {
        SummaryMiniCard(
            label = "INVESTED",
            value = formatCompact(state.totalInvested),
            modifier = Modifier.weight(1f)
        )
        SummaryMiniCard(
            label = "CURRENT",
            value = formatCompact(state.totalCurrent),
            modifier = Modifier.weight(1f)
        )
        SummaryMiniCard(
            label = "P&L",
            value = formatPnl(state.totalPnl),
            valueColor = if (state.isOverallGain) InvestColors.GainGreen else InvestColors.LossRed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryMiniCard(
    label: String,
    value: String,
    valueColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dims.buttonCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dims.miniCardPaddingHorizontal,
                vertical = dims.miniCardPaddingVertical
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dims.itemSpacingSmall))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter Pills (horizontally scrollable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FilterPills(
    selectedTab: InvestTab,
    onTabSelected: (InvestTab) -> Unit
) {
    val dims = Dimens.current
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
    ) {
        InvestTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dims.cardCornerRadius))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) }
                    .padding(horizontal = dims.itemSpacingLarge, vertical = dims.itemSpacingMedium)
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Summary Card (for Overview tab)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategorySummaryCard(summary: InvestState.CategorySummary) {
    val dims = Dimens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingSmall),
        shape = RoundedCornerShape(dims.cardCornerRadius - 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.cardInnerPadding - 4.dp)
        ) {
            // Header row: category name + P&L badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Color dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(summary.category.color)
                    )
                    Spacer(modifier = Modifier.width(dims.itemSpacingMedium))
                    Text(
                        text = summary.category.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // P&L percentage badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (summary.isGain) InvestColors.GainGreen.copy(alpha = 0.15f)
                            else InvestColors.LossRed.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = dims.itemSpacingMedium, vertical = 4.dp)
                ) {
                    Text(
                        text = "${if (summary.isGain) "+" else ""}${String.format("%.1f", summary.pnlPercent)}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (summary.isGain) InvestColors.GainGreen else InvestColors.LossRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(dims.itemSpacingLarge))

            // Values row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ValueColumn(label = "INVESTED", value = formatCompact(summary.invested))
                ValueColumn(label = "CURRENT", value = formatCompact(summary.current))
                ValueColumn(
                    label = "P&L",
                    value = formatPnl(summary.pnl),
                    valueColor = if (summary.isGain) InvestColors.GainGreen else InvestColors.LossRed
                )
            }

            Spacer(modifier = Modifier.height(dims.itemSpacingLarge))

            // Accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(summary.category.color)
            )
        }
    }
}

@Composable
private fun ValueColumn(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    val dims = Dimens.current
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab Summary Header (for filtered tabs)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TabSummaryHeader(
    label: String,
    invested: Double,
    current: Double,
    pnl: Double,
    pnlPercent: Double
) {
    val dims = Dimens.current
    val isGain = pnl >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingMedium),
        shape = RoundedCornerShape(dims.cardCornerRadius - 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.cardInnerPadding - 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$label Summary",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isGain) InvestColors.GainGreen.copy(alpha = 0.15f)
                            else InvestColors.LossRed.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = dims.itemSpacingMedium, vertical = 4.dp)
                ) {
                    Text(
                        text = "${if (isGain) "+" else ""}${String.format("%.1f", pnlPercent)}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isGain) InvestColors.GainGreen else InvestColors.LossRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(dims.itemSpacingLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ValueColumn(label = "INVESTED", value = formatCompact(invested))
                ValueColumn(label = "CURRENT", value = formatCompact(current))
                ValueColumn(
                    label = "P&L",
                    value = formatPnl(pnl),
                    valueColor = if (isGain) InvestColors.GainGreen else InvestColors.LossRed
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual Holding Item
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HoldingItem(holding: InvestmentHolding) {
    val dims = Dimens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenHorizontalPadding, vertical = 4.dp),
        shape = RoundedCornerShape(dims.buttonCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.itemSpacingLarge, vertical = dims.itemSpacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(holding.category.color)
            )

            Spacer(modifier = Modifier.width(dims.itemSpacingLarge))

            // Name + invested
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holding.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Invested: ${formatCompact(holding.invested)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Current value + P&L
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCompact(holding.current),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatPnl(holding.pnl)} (${String.format("%.1f", holding.pnlPercent)}%)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (holding.isGain) InvestColors.GainGreen else InvestColors.LossRed
                )
            }
        }
    }
}
