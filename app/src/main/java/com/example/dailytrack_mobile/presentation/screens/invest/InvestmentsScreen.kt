package com.example.dailytrack_mobile.presentation.screens.invest

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dailytrack_mobile.presentation.components.DailyTrackPullToRefreshBox
import com.example.dailytrack_mobile.presentation.util.Dimens
import kotlin.math.roundToInt

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

private fun formatExactCurrency(amount: Double): String {
    val isNegative = amount < 0
    val absAmount = Math.abs(amount)
    val integerPart = absAmount.toLong()
    val remainder = ((absAmount - integerPart) * 100).roundToInt()
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
    val prefix = if (isNegative) "-₹" else "₹"
    return "$prefix$formattedInt$decimalStr"
}

private fun formatPnl(amount: Double): String {
    val prefix = if (amount >= 0) "+" else "-"
    return "$prefix${formatCompact(amount)}"
}

private fun formatPnlExact(amount: Double): String {
    val prefix = if (amount >= 0) "+" else ""
    return "$prefix${formatExactCurrency(amount)}"
}

private fun formatPointDate(dateStr: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateStr)
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        "${date.dayOfMonth} $month ${date.year}"
    } catch (e: Exception) {
        dateStr
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InvestmentsScreen(
    viewModel: InvestVM = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val dims = Dimens.current

    var isValueMode by remember { mutableStateOf(true) }
    var selectedChartPoint by remember { mutableStateOf<ChartPoint?>(null) }

    LaunchedEffect(state.chartPoints) {
        selectedChartPoint = null
    }

    DailyTrackPullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.onAction(InvestAction.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.isLoading && state.holdings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "Loading portfolio...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
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
                        isValueMode = isValueMode,
                        onValueModeChanged = { isValueMode = it },
                        selectedPoint = selectedChartPoint,
                        onClearSelection = { selectedChartPoint = null }
                    )
                }

                // ── Advanced chart ─────────────────────────────────────────────
                item {
                    AdvancedChart(
                        points = state.chartPoints,
                        isValueMode = isValueMode,
                        isGain = state.isPeriodGain,
                        showXAxis = true,
                        selectedPoint = selectedChartPoint,
                        onPointSelected = { selectedChartPoint = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(205.dp)
                            .padding(horizontal = dims.screenHorizontalPadding)
                    )
                }

                // ── Chart Legend (Current vs Invested or Return vs Baseline) ───
                item {
                    ChartLegend(
                        isValueMode = isValueMode,
                        isGain = state.isPeriodGain
                    )
                }

                // ── Time range toggle (1M | 3M | 6M | 1Y | YTD | ALL) ───────────
                item {
                    Spacer(modifier = Modifier.height(dims.itemSpacingSmall))
                    TimeRangeToggle(
                        selectedRange = state.selectedTimeRange,
                        onRangeSelected = { viewModel.onAction(InvestAction.SelectTimeRange(it)) },
                        modifier = Modifier.padding(horizontal = dims.screenHorizontalPadding)
                    )
                }

                // ── Summary row (Invested / Current / P&L) ─────────────────────
                item {
                    Spacer(modifier = Modifier.height(dims.itemSpacingSmall))
                    SummaryRow(
                        state = state,
                        selectedPoint = selectedChartPoint
                    )
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
                        // Show asset allocation breakdown card
                        if (state.categorySummaries.isNotEmpty()) {
                            item {
                                AssetAllocationCard(
                                    summaries = state.categorySummaries,
                                    totalCurrent = state.totalCurrent
                                )
                            }
                        }
                        // Show category summary cards
                        items(state.categorySummaries) { summary ->
                            CategorySummaryCard(
                                summary = summary,
                                onClick = {
                                    val targetTab = when (summary.category) {
                                        InvestCategory.STOCKS -> InvestTab.STOCKS
                                        InvestCategory.MUTUAL_FUNDS -> InvestTab.MUTUAL_FUNDS
                                        InvestCategory.RETIREMENT -> InvestTab.RETIREMENT
                                        InvestCategory.FD -> InvestTab.FD
                                        InvestCategory.GOLD -> InvestTab.GOLD
                                        InvestCategory.REAL_ESTATE -> InvestTab.REAL_ESTATE
                                    }
                                    viewModel.onAction(InvestAction.SelectTab(targetTab))
                                }
                            )
                        }
                    }
                    else -> {
                        // Show individual holdings directly (TabSummaryHeader is removed as cards below graph show it)
                        val filtered = state.filteredHoldings
                        if (filtered.isEmpty()) {
                            item {
                                EmptyHoldingsState(tab = state.selectedTab)
                            }
                        } else {
                            items(filtered) { holding ->
                                HoldingItem(holding = holding)
                            }
                        }
                    }
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
    isValueMode: Boolean,
    onValueModeChanged: (Boolean) -> Unit,
    selectedPoint: ChartPoint? = null,
    onClearSelection: () -> Unit = {}
) {
    val dims = Dimens.current
    val isSelected = selectedPoint != null
    val displayCurrent = selectedPoint?.current?.toDouble() ?: state.periodCurrent
    val displayInvested = selectedPoint?.invested?.toDouble() ?: state.periodInvested
    val displayPnl = selectedPoint?.let { (it.current - it.invested).toDouble() } ?: state.periodPnl
    val displayPnlPercent = selectedPoint?.pnlPercent?.toDouble() ?: state.periodPnlPercent
    val isGain = displayPnl >= 0.0

    val headerTitle = when {
        isSelected -> "VALUE ON ${formatPointDate(selectedPoint!!.date).uppercase()}"
        state.selectedTab == InvestTab.OVERVIEW -> "PORTFOLIO VALUE"
        else -> "${state.selectedTab.label.uppercase()} VALUE"
    }

    val periodSubtext = when {
        isSelected -> "Inv: ${formatCompact(displayInvested)}"
        else -> state.periodLabel
    }

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
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isSelected) 1.2.sp else 1.8.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.clickable { onClearSelection() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = state.selectedTimeRange.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val heroValue = if (isValueMode) {
                formatFull(displayCurrent)
            } else {
                "${if (isGain) "+" else ""}${String.format(java.util.Locale.US, "%.2f", displayPnlPercent)}%"
            }

            Text(
                text = heroValue,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = if (isValueMode) MaterialTheme.colorScheme.onBackground else if (isGain) InvestColors.GainGreen else InvestColors.LossRed
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGain) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isGain) InvestColors.GainGreen else InvestColors.LossRed,
                    modifier = Modifier.size(dims.iconSizeSmall)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val sublineMetrics = if (isValueMode) {
                    "${formatPnl(displayPnl)} (${String.format(java.util.Locale.US, "%.1f", displayPnlPercent)}%)"
                } else {
                    "Val: ${formatCompact(displayCurrent)} • Net: ${formatPnl(displayPnl)}"
                }
                Text(
                    text = sublineMetrics,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isValueMode) (if (isGain) InvestColors.GainGreen else InvestColors.LossRed) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(dims.itemSpacingMedium))
                Text(
                    text = periodSubtext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ValueReturnToggle(
            isValueMode = isValueMode,
            onModeChanged = onValueModeChanged
        )
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
    selectedPoint: ChartPoint? = null,
    onPointSelected: ((ChartPoint?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Not enough data", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val haptic = LocalHapticFeedback.current
    var internalSelectedPoint by remember { mutableStateOf<ChartPoint?>(null) }
    val activePoint = selectedPoint ?: internalSelectedPoint

    fun updatePoint(point: ChartPoint?) {
        if (point != null && point.date != activePoint?.date) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        internalSelectedPoint = point
        onPointSelected?.invoke(point)
    }

    val activeIndex = remember(activePoint, points) {
        if (activePoint != null) {
            val idx = points.indexOfFirst { it.date == activePoint.date }
            if (idx >= 0) idx else null
        } else null
    }
    
    val primaryColor = if (isGain) InvestColors.GainGreen else InvestColors.LossRed
    val investedColor = MaterialTheme.colorScheme.primary
    val guideLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    val yVals = if (isValueMode) {
        points.flatMap { listOf(it.current, it.invested) }
    } else {
        points.map { it.pnlPercent }
    }

    val minVal = yVals.minOrNull() ?: 0f
    val maxVal = yVals.maxOrNull() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(0.01f)
    
    val paddingTop = 28f
    val paddingBottom = if (showXAxis) 60f else 24f
    
    BoxWithConstraints(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Chart part
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
                    .pointerInput(points) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (points.isNotEmpty()) {
                                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                    val index = (offset.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                                    val tappedPoint = points[index]
                                    if (activePoint?.date == tappedPoint.date) {
                                        updatePoint(null)
                                    } else {
                                        updatePoint(tappedPoint)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                if (points.isNotEmpty()) {
                                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                    val index = (offset.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                                    updatePoint(points[index])
                                }
                            },
                            onHorizontalDrag = { change, _ ->
                                if (points.isNotEmpty()) {
                                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                    val index = (change.position.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                                    updatePoint(points[index])
                                    change.consume()
                                }
                            }
                        )
                    }
            ) {
                val textPaint = remember {
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                }

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                    
                    fun yOf(value: Float): Float {
                        return size.height - paddingBottom - ((value - minVal) / range) * (size.height - paddingTop - paddingBottom)
                    }

                    // Dotted horizontal grid lines
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
                        
                        // Subtle point dots when point count is manageable
                        if (points.size in 3..25) {
                            for (i in 0 until points.size) {
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.5f),
                                    radius = 3.5f,
                                    center = Offset(i * stepX, yOf(points[i].current))
                                )
                            }
                        }

                        // Draw end dots
                        drawCircle(color = investedColor.copy(alpha = 0.8f), radius = 6f, center = Offset((points.size - 1) * stepX, yOf(points.last().invested)))
                        drawCircle(color = primaryColor, radius = 6f, center = Offset((points.size - 1) * stepX, yOf(points.last().current)))

                        // Selected point indicators
                        if (activeIndex != null && activeIndex in points.indices) {
                            val selPt = points[activeIndex]
                            val selX = activeIndex * stepX
                            val guidePath = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            
                            // Vertical guide line
                            drawLine(
                                color = guideLineColor,
                                start = Offset(selX, paddingTop),
                                end = Offset(selX, size.height - paddingBottom),
                                strokeWidth = 2f,
                                pathEffect = guidePath
                            )

                            // Invested marker
                            val invY = yOf(selPt.invested)
                            drawCircle(color = investedColor.copy(alpha = 0.25f), radius = 10f, center = Offset(selX, invY))
                            drawCircle(color = investedColor, radius = 5.5f, center = Offset(selX, invY))
                            drawCircle(color = Color.White, radius = 2.5f, center = Offset(selX, invY))

                            // Current marker
                            val currY = yOf(selPt.current)
                            drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 12f, center = Offset(selX, currY))
                            drawCircle(color = primaryColor, radius = 6.5f, center = Offset(selX, currY))
                            drawCircle(color = Color.White, radius = 3f, center = Offset(selX, currY))
                        }
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
                        
                        if (points.size in 3..25) {
                            for (i in 0 until points.size) {
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.5f),
                                    radius = 3.5f,
                                    center = Offset(i * stepX, yOf(points[i].pnlPercent))
                                )
                            }
                        }

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

                        // Selected point indicator for Return mode
                        if (activeIndex != null && activeIndex in points.indices) {
                            val selPt = points[activeIndex]
                            val selX = activeIndex * stepX
                            val guidePath = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                            drawLine(
                                color = guideLineColor,
                                start = Offset(selX, paddingTop),
                                end = Offset(selX, size.height - paddingBottom),
                                strokeWidth = 2f,
                                pathEffect = guidePath
                            )

                            val pnlY = yOf(selPt.pnlPercent)
                            drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 12f, center = Offset(selX, pnlY))
                            drawCircle(color = primaryColor, radius = 6.5f, center = Offset(selX, pnlY))
                            drawCircle(color = Color.White, radius = 3f, center = Offset(selX, pnlY))
                        }
                    }
                }

                // Interactive floating badge at top center of the chart
                androidx.compose.animation.AnimatedVisibility(
                    visible = activePoint != null,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                ) {
                    if (activePoint != null) {
                        val ptGain = if (isValueMode) activePoint.current >= activePoint.invested else activePoint.pnlPercent >= 0f
                        val ptGainColor = if (ptGain) InvestColors.GainGreen else InvestColors.LossRed

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatPointDate(activePoint.date),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                )

                                if (isValueMode) {
                                    Text(
                                        text = formatCompact(activePoint.current.toDouble()),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ptGainColor
                                    )
                                    Text(
                                        text = "(${if (ptGain) "+" else ""}${String.format(java.util.Locale.US, "%.1f", activePoint.pnlPercent)}%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = ptGainColor
                                    )
                                } else {
                                    Text(
                                        text = "${if (ptGain) "+" else ""}${String.format(java.util.Locale.US, "%.1f", activePoint.pnlPercent)}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ptGainColor
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .clickable { updatePoint(null) }
                                )
                            }
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
private fun SummaryRow(
    state: InvestState,
    selectedPoint: ChartPoint? = null
) {
    val dims = Dimens.current
    val isSelected = selectedPoint != null
    val displayCurrent = selectedPoint?.current?.toDouble() ?: state.periodCurrent
    val displayInvested = selectedPoint?.invested?.toDouble() ?: state.periodInvested
    val displayPnl = selectedPoint?.let { (it.current - it.invested).toDouble() } ?: state.periodPnl
    val displayPnlPercent = selectedPoint?.pnlPercent?.toDouble() ?: state.periodPnlPercent
    val isGain = displayPnl >= 0.0

    var showAbsoluteAmounts by rememberSaveable { mutableStateOf(false) }

    val pnlLabel = when {
        isSelected -> "POINT P&L"
        state.selectedTimeRange == ChartTimeRange.ALL -> "TOTAL P&L"
        else -> "${state.selectedTimeRange.label} RETURN"
    }

    val investedValue = if (showAbsoluteAmounts) formatExactCurrency(displayInvested) else formatCompact(displayInvested)
    val currentValue = if (showAbsoluteAmounts) formatExactCurrency(displayCurrent) else formatCompact(displayCurrent)
    val pnlValue = if (showAbsoluteAmounts) formatPnlExact(displayPnl) else formatPnl(displayPnl)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
    ) {
        SummaryMiniCard(
            label = "INVESTED",
            value = investedValue,
            subValue = "Cost basis",
            isExact = showAbsoluteAmounts,
            onClick = { showAbsoluteAmounts = !showAbsoluteAmounts },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        SummaryMiniCard(
            label = "CURRENT",
            value = currentValue,
            subValue = "Market value",
            isExact = showAbsoluteAmounts,
            onClick = { showAbsoluteAmounts = !showAbsoluteAmounts },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        SummaryMiniCard(
            label = pnlLabel,
            value = pnlValue,
            subValue = "${if (isGain) "+" else ""}${String.format(java.util.Locale.US, "%.1f", displayPnlPercent)}%",
            valueColor = if (isGain) InvestColors.GainGreen else InvestColors.LossRed,
            isExact = showAbsoluteAmounts,
            onClick = { showAbsoluteAmounts = !showAbsoluteAmounts },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun SummaryMiniCard(
    label: String,
    value: String,
    subValue: String? = null,
    valueColor: Color? = null,
    isExact: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    val valueFontSize = remember(value.length, isExact) {
        if (isExact) {
            when {
                value.length <= 8 -> 14.sp
                value.length <= 11 -> 12.sp
                value.length <= 14 -> 10.5.sp
                else -> 9.5.sp
            }
        } else {
            15.sp
        }
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(dims.buttonCornerRadius))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(dims.buttonCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = dims.miniCardPaddingHorizontal,
                    vertical = dims.miniCardPaddingVertical
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(dims.itemSpacingSmall))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = valueFontSize
                    ),
                    color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (subValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    ),
                    color = valueColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter Pills (horizontally scrollable)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InvestTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                label = {
                    Text(
                        text = tab.label,
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
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Summary Card (for Overview tab)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategorySummaryCard(
    summary: InvestState.CategorySummary,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    Card(
        onClick = onClick,
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
            // Header row: category name + P&L badge + chevron
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View holdings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
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
// Empty Holdings State (for filtered tabs with 0 items)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyHoldingsState(
    tab: InvestTab,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    Card(
        modifier = modifier
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
                .padding(dims.cardInnerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PieChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
            Text(
                text = "No ${tab.label} holdings yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Holdings categorized under ${tab.label} will be displayed here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
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
    onRangeSelected: (ChartTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.buttonCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartTimeRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "timeRangeBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "timeRangeText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
                        .background(bgColor)
                        .clickable { onRangeSelected(range) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Value vs Return Toggle (in Portfolio Header)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ValueReturnToggle(
    isValueMode: Boolean,
    onModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dims.buttonCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val valueBg by animateColorAsState(
                targetValue = if (isValueMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                animationSpec = tween(durationMillis = 180),
                label = "valueBg"
            )
            val valueColor by animateColorAsState(
                targetValue = if (isValueMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 180),
                label = "valueColor"
            )
            val returnBg by animateColorAsState(
                targetValue = if (!isValueMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                animationSpec = tween(durationMillis = 180),
                label = "returnBg"
            )
            val returnColor by animateColorAsState(
                targetValue = if (!isValueMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 180),
                label = "returnColor"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
                    .background(valueBg)
                    .clickable { onModeChanged(true) }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Value",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isValueMode) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = valueColor
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
                    .background(returnBg)
                    .clickable { onModeChanged(false) }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Return",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (!isValueMode) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    color = returnColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chart Legend (Current vs Invested or Return vs Baseline)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChartLegend(
    isValueMode: Boolean,
    isGain: Boolean,
    modifier: Modifier = Modifier
) {
    val gainLossColor = if (isGain) InvestColors.GainGreen else InvestColors.LossRed
    val investedColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.current.screenHorizontalPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isValueMode) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(gainLossColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Current Value",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(investedColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Invested",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(gainLossColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Return %",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(1.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "0% Baseline",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Asset Allocation Breakdown Card (for Overview tab)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AssetAllocationCard(
    summaries: List<InvestState.CategorySummary>,
    totalCurrent: Double,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    if (totalCurrent <= 0.0 || summaries.isEmpty()) return

    Card(
        modifier = modifier
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ASSET ALLOCATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${summaries.size} Categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-segment horizontal bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                summaries.forEach { cat ->
                    val ratio = (cat.current / totalCurrent).toFloat().coerceIn(0f, 1f)
                    if (ratio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(ratio)
                                .fillMaxHeight()
                                .background(cat.category.color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini legend pills row using FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                summaries.forEach { cat ->
                    val percent = if (totalCurrent > 0) (cat.current / totalCurrent) * 100.0 else 0.0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(cat.category.color)
                        )
                        Text(
                            text = "${cat.category.label}: ${String.format(java.util.Locale.US, "%.0f", percent)}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
