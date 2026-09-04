package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.data.remote.dto.BulkEditTransactionItemDto
import com.example.dailytrack_mobile.presentation.screens.money.CategoryEmojis
import com.example.dailytrack_mobile.presentation.screens.money.Transaction
import com.example.dailytrack_mobile.presentation.screens.money.TransactionType
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.text.SimpleDateFormat
import java.util.*

private val defaultCategories = listOf(
    "Food", "Transport", "Shopping", "Entertainment", "Bills",
    "Health", "Education", "Cinema", "Daily Need", "Salary",
    "Freelance", "Investment", "Gift", "Other"
)

private val defaultAccounts = listOf(
    "KOTAK", "IDBI", "FEDERAL", "CUB", "INDIAN", "ICICI", "HDFC", "SBI", "Axis", "Cash", "CC-PINNACLE 6360"
)

private class EditableTxItem(
    val id: Long,
    val initialTitle: String,
    initialType: String,
    initialCategory: String,
    initialAccount: String,
    initialDate: String,
    initialAmount: String,
    initialDescription: String,
    initialExcludeAnalytics: Boolean
) {
    var type by mutableStateOf(initialType)
    var category by mutableStateOf(initialCategory)
    var account by mutableStateOf(initialAccount)
    var date by mutableStateOf(initialDate)
    var amount by mutableStateOf(initialAmount)
    var description by mutableStateOf(initialDescription)
    var excludeAnalytics by mutableStateOf(initialExcludeAnalytics)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BulkEditTransactionsSheet(
    transactions: List<Transaction>,
    availableAccounts: List<String>,
    availableCategories: List<String>,
    mostUsedCategories: List<String> = emptyList(),
    isUpdating: Boolean,
    onSave: (List<BulkEditTransactionItemDto>) -> Unit,
    onDismiss: () -> Unit
) {
    val dims = Dimens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val apiDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    // Tab state: 0 = "Apply to All", 1 = "Per Transaction"
    var selectedTab by remember { mutableIntStateOf(0) }

    // Feedback badge for batch actions (e.g. "Applied 'Food' to all items")
    var batchFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Initialize list of editable items from selected transactions
    val items = remember(transactions) {
        transactions.map { tx ->
            val typeStr = when {
                tx.type == TransactionType.CREDIT || tx.rawType.equals("Credit", ignoreCase = true) -> "Credit"
                tx.type == TransactionType.SAVINGS || tx.isSavings -> "Savings"
                tx.type == TransactionType.INVESTMENT || tx.isInvestment -> "Investment"
                else -> "Debit"
            }
            val dateStr = if (tx.rawDate.isNotBlank()) tx.rawDate else apiDateFormat.format(Date(tx.timestampMillis))
            val amountStr = if (tx.amount % 1.0 == 0.0) "%.0f".format(tx.amount) else "%.2f".format(tx.amount)
            val desc = tx.note ?: tx.description ?: ""
            EditableTxItem(
                id = tx.id,
                initialTitle = tx.title,
                initialType = typeStr,
                initialCategory = tx.category,
                initialAccount = tx.bank,
                initialDate = dateStr,
                initialAmount = amountStr,
                initialDescription = desc,
                initialExcludeAnalytics = tx.isExcluded
            )
        }
    }

    val accountsList = remember(availableAccounts) {
        if (availableAccounts.isNotEmpty()) availableAccounts else defaultAccounts
    }
    val allCategories = remember(availableCategories) {
        if (availableCategories.isNotEmpty()) availableCategories else defaultCategories
    }
    val recentCategories = remember(mostUsedCategories, allCategories) {
        val source = if (mostUsedCategories.isNotEmpty()) mostUsedCategories else allCategories
        source.distinct().take(8)
    }

    val totalAmount = remember(items.map { it.amount }) {
        items.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }

    // Global Date Picker for Batch Apply
    var showBatchDatePicker by remember { mutableStateOf(false) }
    if (showBatchDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showBatchDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val dateStr = apiDateFormat.format(Date(ms))
                        items.forEach { it.date = dateStr }
                        batchFeedbackMessage = "Applied ${displayDateFormat.format(Date(ms))} to all items"
                    }
                    showBatchDatePicker = false
                }) {
                    Text("Apply to All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Single item date picker
    var datePickerTargetItem by remember { mutableStateOf<EditableTxItem?>(null) }
    datePickerTargetItem?.let { target ->
        val currentMs = remember(target.date) {
            try { apiDateFormat.parse(target.date)?.time ?: System.currentTimeMillis() }
            catch (_: Exception) { System.currentTimeMillis() }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentMs)
        DatePickerDialog(
            onDismissRequest = { datePickerTargetItem = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        target.date = apiDateFormat.format(Date(ms))
                    }
                    datePickerTargetItem = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTargetItem = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Top Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Bulk Edit",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${items.size} Selected",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Total: ₹${"%,.2f".format(totalAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isUpdating
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Clean Mode Segmented Bar ─────────────────────────────────────
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                "Apply to All",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                "Per Transaction (${items.size})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                )
            }

            // ── Body based on Tab ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    // TAB 0: APPLY TO ALL
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = dims.screenHorizontalPadding,
                            vertical = dims.itemSpacingLarge
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Feedback banner
                        batchFeedbackMessage?.let { msg ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(dims.buttonCornerRadius),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { batchFeedbackMessage = null },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 1. Batch Category
                        item {
                            Card(
                                shape = RoundedCornerShape(dims.cardCornerRadius),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Category",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap to apply to all ${items.size} items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        recentCategories.forEach { cat ->
                                            val emoji = CategoryEmojis.forCategory(cat)
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    items.forEach { it.category = cat }
                                                    batchFeedbackMessage = "Applied \"$cat\" to all ${items.size} items"
                                                },
                                                label = { Text("$emoji $cat", style = MaterialTheme.typography.bodyMedium) },
                                                shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Batch Account
                        item {
                            Card(
                                shape = RoundedCornerShape(dims.cardCornerRadius),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Account",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap to apply to all ${items.size} items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        accountsList.take(8).forEach { acc ->
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    items.forEach { it.account = acc }
                                                    batchFeedbackMessage = "Applied \"$acc\" to all ${items.size} items"
                                                },
                                                label = { Text(acc, style = MaterialTheme.typography.bodyMedium) },
                                                shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Batch Type
                        item {
                            Card(
                                shape = RoundedCornerShape(dims.cardCornerRadius),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Transaction Type",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap to apply to all ${items.size} items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Debit" to "Expense",
                                            "Credit" to "Income",
                                            "Savings" to "Savings",
                                            "Investment" to "Invest"
                                        ).forEach { (dbVal, label) ->
                                            Surface(
                                                shape = RoundedCornerShape(dims.buttonCornerRadius),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(dims.buttonCornerRadius))
                                                    .clickable {
                                                        items.forEach { it.type = dbVal }
                                                        batchFeedbackMessage = "Applied \"$label\" type to all items"
                                                    }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Batch Date
                        item {
                            Card(
                                shape = RoundedCornerShape(dims.cardCornerRadius),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Date",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap to apply to all ${items.size} items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                val todayStr = apiDateFormat.format(Date())
                                                items.forEach { it.date = todayStr }
                                                batchFeedbackMessage = "Applied Today to all items"
                                            },
                                            label = { Text("Today", style = MaterialTheme.typography.labelSmall) },
                                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                                        )

                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                                val yesterdayStr = apiDateFormat.format(cal.time)
                                                items.forEach { it.date = yesterdayStr }
                                                batchFeedbackMessage = "Applied Yesterday to all items"
                                            },
                                            label = { Text("Yesterday", style = MaterialTheme.typography.labelSmall) },
                                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                                        )

                                        OutlinedButton(
                                            onClick = { showBatchDatePicker = true },
                                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Choose Date...", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Batch Exclude Analytics
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(dims.cardCornerRadius),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Exclude from Spending Analyser",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Apply exclusion to all selected transactions",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TextButton(
                                            onClick = {
                                                items.forEach { it.excludeAnalytics = true }
                                                batchFeedbackMessage = "Excluded all items from analyser"
                                            }
                                        ) {
                                            Text("Exclude All", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(
                                            onClick = {
                                                items.forEach { it.excludeAnalytics = false }
                                                batchFeedbackMessage = "Included all items in analyser"
                                            }
                                        ) {
                                            Text("Include All", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        // Prompt to switch to individual customize tab
                        item {
                            FilledTonalButton(
                                onClick = { selectedTab = 1 },
                                shape = RoundedCornerShape(dims.buttonCornerRadius),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Customize Separate Transactions (${items.size}) →", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                } else {
                    // TAB 1: PER TRANSACTION (Clean, fast, individual customizations)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = dims.screenHorizontalPadding,
                            vertical = dims.itemSpacingLarge
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Customize individual fields for each transaction below:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        itemsIndexed(
                            items = items,
                            key = { _, itm -> itm.id }
                        ) { index, item ->
                            CleanIndividualTxCard(
                                index = index,
                                item = item,
                                accountsList = accountsList,
                                allCategories = allCategories,
                                recentCategories = recentCategories,
                                displayDateFormat = displayDateFormat,
                                apiDateFormat = apiDateFormat,
                                onRequestDatePicker = { datePickerTargetItem = item }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Sticky Bottom Footer ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
                horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(dims.buttonCornerRadius),
                    modifier = Modifier.weight(0.35f)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                }

                val allAmountsValid = remember(items.map { it.amount }) {
                    items.all {
                        val parsed = it.amount.toDoubleOrNull()
                        parsed != null && parsed > 0.0
                    }
                }

                Button(
                    onClick = {
                        val updates = items.map { item ->
                            BulkEditTransactionItemDto(
                                id = item.id,
                                account = item.account,
                                date = item.date,
                                type = item.type,
                                heading = item.category.trim().ifEmpty { "Other" },
                                description = item.description.takeIf { it.isNotBlank() } ?: "",
                                amount = item.amount.toDoubleOrNull() ?: 0.0,
                                excludeAnalytics = item.excludeAnalytics
                            )
                        }
                        onSave(updates)
                    },
                    enabled = !isUpdating && allAmountsValid,
                    shape = RoundedCornerShape(dims.buttonCornerRadius),
                    modifier = Modifier
                        .weight(0.65f)
                        .height(dims.searchBarHeight)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save All (${items.size})", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Clean Individual Transaction Card (No heavy accordion, clean and tactile)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CleanIndividualTxCard(
    index: Int,
    item: EditableTxItem,
    accountsList: List<String>,
    allCategories: List<String>,
    recentCategories: List<String>,
    displayDateFormat: SimpleDateFormat,
    apiDateFormat: SimpleDateFormat,
    onRequestDatePicker: () -> Unit
) {
    val dims = Dimens.current
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(item.date) {
        try {
            val d = apiDateFormat.parse(item.date)
            d?.let { displayDateFormat.format(it) } ?: item.date
        } catch (_: Exception) {
            item.date
        }
    }

    val typeColor = when (item.type.lowercase()) {
        "credit", "income" -> Color(0xFF2ECC71)
        "savings", "saving" -> Color(0xFF29B6F6)
        "investment", "investments" -> Color(0xFFAB47BC)
        else -> Color(0xFFFF5252)
    }

    Card(
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: #Index, Emoji, Title, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = CategoryEmojis.forCategory(item.category),
                        fontSize = 18.sp
                    )

                    Text(
                        text = item.initialTitle.ifBlank { item.category },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                // Amount text input directly visible
                OutlinedTextField(
                    value = item.amount,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' }) item.amount = newValue
                    },
                    prefix = { Text("₹", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = typeColor)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    modifier = Modifier.width(110.dp)
                )
            }

            // Quick Attribute Selectors Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Picker Button / Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .clickable { categoryDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${CategoryEmojis.forCategory(item.category)} ${item.category}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        (recentCategories + allCategories).distinct().forEach { cat ->
                            DropdownMenuItem(
                                leadingIcon = { Text(CategoryEmojis.forCategory(cat), fontSize = 16.sp) },
                                text = { Text(cat, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    item.category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Account Picker Button / Dropdown
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .clickable { accountDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.account,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accountsList.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    item.account = acc
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Button
                Surface(
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(0.9f)
                        .clickable { onRequestDatePicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }

            // Description / Note field + Exclude checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.description,
                    onValueChange = { item.description = it },
                    placeholder = { Text("Note (optional)", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    color = if (item.excludeAnalytics) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (item.excludeAnalytics) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(dims.buttonCornerRadius - 4.dp))
                        .clickable { item.excludeAnalytics = !item.excludeAnalytics }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.excludeAnalytics) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (item.excludeAnalytics) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (item.excludeAnalytics) "Excluded" else "Included",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.excludeAnalytics) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
