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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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

private fun formatFull(amount: Double): String {
    val prefix = if (amount < 0) "-" else ""
    return "$prefix₹%,.2f".format(Math.abs(amount))
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

    var showExpandedChart by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showExpandedChart) {
        ExpandedChartOverlay(
            state = state,
            onDismiss = { showExpandedChart = false },
            onTimeRangeSelected = { viewModel.onAction(InvestAction.SelectTimeRange(it)) },
            onTabSelected = { viewModel.onAction(InvestAction.SelectTab(it)) }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = dims.screenBottomPadding)
    ) {
        // ── Portfolio header ────────────────────────────────────────────
        item {
            PortfolioHeader(
                state = state,
                onExpandClicked = { showExpandedChart = true }
            )
        }

        // ── Advanced chart ─────────────────────────────────────────────
        item {
            AdvancedChart(
                points = state.chartPoints,
                isValueMode = true,
                isGain = state.isFilteredGain,
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
private fun PortfolioHeader(
    state: InvestState,
    onExpandClicked: () -> Unit
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dims.screenHorizontalPadding + 4.dp,
                end = dims.screenHorizontalPadding + 4.dp,
                top = dims.screenTopPadding,
                bottom = dims.itemSpacingMedium
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
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
                text = formatFull(state.totalCurrent),
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
        
        androidx.compose.material3.IconButton(onClick = onExpandClicked) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Expand Chart",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Advanced Chart
// ─────────────────────────────────────────────────────────────────────────────
private fun formatMonth(dateStr: String): String {
    try {
        val date = java.time.LocalDate.parse(dateStr)
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val year = date.year.toString().takeLast(2)
        return "$month '$year"
    } catch (e: Exception) {
        return ""
    }
}

@Composable
private fun AdvancedChart(
    points: List<ChartPoint>,
    isValueMode: Boolean,
    isGain: Boolean,
    showXAxis: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Not enough data", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    val scrollState = rememberScrollState()
    
    val primaryColor = if (isGain) InvestColors.GainGreen else InvestColors.LossRed
    val investedColor = MaterialTheme.colorScheme.primary

    val yVals = if (isValueMode) {
        points.flatMap { listOf(it.current, it.invested) }
    } else {
        points.map { it.pnlPercent }
    }

    val minVal = yVals.minOrNull() ?: 0f
    val maxVal = yVals.maxOrNull() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(0.01f)
    
    val paddingTop = 24f
    val paddingBottom = if (showXAxis) 60f else 24f
    
    BoxWithConstraints(modifier = modifier) {
        val baseWidth = maxWidth
        val canvasWidth = baseWidth * scale
        
        Row(modifier = Modifier.fillMaxSize()) {
            // Chart part
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                        }
                    }
                    .horizontalScroll(scrollState)
            ) {
                val textPaint = remember {
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(canvasWidth)
                ) {
                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                    
                    fun yOf(value: Float): Float {
                        return size.height - paddingBottom - ((value - minVal) / range) * (size.height - paddingTop - paddingBottom)
                    }

                    // Dotted grid
                    val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = paddingTop + i * ((size.height - paddingTop - paddingBottom) / gridLines)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f,
                            pathEffect = pathEffect
                        )
                    }
                    
                    if (showXAxis && points.isNotEmpty()) {
                        val labelCount = 6
                        val step = (points.size / labelCount).coerceAtLeast(1)
                        for (i in 0 until points.size step step) {
                            val x = (i * stepX).coerceIn(40f, size.width - 40f)
                            val y = size.height - 10f
                            val monthLabel = formatMonth(points[i].date)
                            drawContext.canvas.nativeCanvas.drawText(monthLabel, x, y, textPaint)
                        }
                    }

                    if (isValueMode) {
                        // Draw Invested Line
                        val invPath = Path().apply {
                            moveTo(0f, yOf(points[0].invested))
                            for (i in 1 until points.size) {
                                lineTo(i * stepX, yOf(points[i].invested))
                            }
                        }
                        drawPath(
                            path = invPath,
                            color = investedColor.copy(alpha = 0.8f),
                            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        
                        // Draw Current Line
                        val currPath = Path().apply {
                            moveTo(0f, yOf(points[0].current))
                            for (i in 1 until points.size) {
                                lineTo(i * stepX, yOf(points[i].current))
                            }
                        }
                        
                        val fillPath = Path().apply {
                            addPath(currPath)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.3f), primaryColor.copy(alpha = 0.05f), Color.Transparent)
                            ),
                            style = Fill
                        )

                        drawPath(
                            path = currPath,
                            color = primaryColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        
                        // Draw end dots
                        drawCircle(color = investedColor.copy(alpha = 0.8f), radius = 6f, center = Offset((points.size - 1) * stepX, yOf(points.last().invested)))
                        drawCircle(color = primaryColor, radius = 6f, center = Offset((points.size - 1) * stepX, yOf(points.last().current)))
                    } else {
                        // Return Mode
                        val returnPath = Path().apply {
                            moveTo(0f, yOf(points[0].pnlPercent))
                            for (i in 1 until points.size) {
                                lineTo(i * stepX, yOf(points[i].pnlPercent))
                            }
                        }
                        
                        val fillPath = Path().apply {
                            addPath(returnPath)
                            lineTo(size.width, yOf(0f))
                            lineTo(0f, yOf(0f))
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.3f), primaryColor.copy(alpha = 0.05f), Color.Transparent)
                            ),
                            style = Fill
                        )

                        drawPath(
                            path = returnPath,
                            color = primaryColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        
                        drawCircle(color = primaryColor, radius = 6f, center = Offset((points.size - 1) * stepX, yOf(points.last().pnlPercent)))

                        // Zero line
                        if (minVal < 0 && maxVal > 0) {
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(0f, yOf(0f)),
                                end = Offset(size.width, yOf(0f)),
                                strokeWidth = 2f,
                                pathEffect = pathEffect
                            )
                        }
                    }
                }
            }
            
            // Y-Axis
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .padding(start = 4.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatCompactForAxis(maxVal.toDouble(), !isValueMode), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCompactForAxis(((maxVal + minVal) / 2).toDouble(), !isValueMode), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCompactForAxis(minVal.toDouble(), !isValueMode), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatCompactForAxis(amount: Double, isPercent: Boolean = false): String {
    if (isPercent) return "%.1f%%".format(amount)
    val abs = Math.abs(amount)
    val prefix = if (amount < 0) "-" else ""
    return when {
        abs >= 1_00_00_000 -> "$prefix%.1fCr".format(abs / 1_00_00_000)
        abs >= 1_00_000    -> "$prefix%.1fL".format(abs / 1_00_000)
        abs >= 1_000       -> "$prefix%.1fK".format(abs / 1_000)
        else               -> "$prefix%.0f".format(abs)
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

// ─────────────────────────────────────────────────────────────────────────────
// Time Range Toggle
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TimeRangeToggle(
    selectedRange: ChartTimeRange,
    onRangeSelected: (ChartTimeRange) -> Unit
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ChartTimeRange.values().forEach { range ->
            val isSelected = range == selectedRange
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .clickable { onRangeSelected(range) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = contentColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small Filter Pills (for Overlay)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SmallFilterPills(
    selectedTab: InvestTab,
    onTabSelected: (InvestTab) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InvestTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Expanded Chart Overlay
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExpandedChartOverlay(
    state: InvestState,
    onDismiss: () -> Unit,
    onTimeRangeSelected: (ChartTimeRange) -> Unit,
    onTabSelected: (InvestTab) -> Unit
) {
    var isValueMode by remember { mutableStateOf(true) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Portfolio Chart",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Value vs Return Toggle
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isValueMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { isValueMode = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Value", style = MaterialTheme.typography.labelSmall, color = if (isValueMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isValueMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { isValueMode = false }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Return", style = MaterialTheme.typography.labelSmall, color = if (!isValueMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    androidx.compose.material3.IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AdvancedChart(
                    points = state.chartPoints,
                    isValueMode = isValueMode,
                    isGain = state.isFilteredGain,
                    showXAxis = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SmallFilterPills(
                    selectedTab = state.selectedTab,
                    onTabSelected = onTabSelected
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimeRangeToggle(
                        selectedRange = state.selectedTimeRange,
                        onRangeSelected = onTimeRangeSelected
                    )
                }
            }
        }
    }
}
