package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.data.remote.dto.BulkEditTransactionItemDto
import com.example.dailytrack_mobile.presentation.screens.money.CategoryEmojis
import com.example.dailytrack_mobile.presentation.screens.money.DEFAULT_CANONICAL_ACCOUNTS
import com.example.dailytrack_mobile.presentation.screens.money.Transaction
import com.example.dailytrack_mobile.presentation.screens.money.TransactionType
import com.example.dailytrack_mobile.presentation.screens.money.sortAccountsCanonical
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.text.SimpleDateFormat
import java.util.*

private val defaultCategories = listOf(
    "Food", "Transport", "Shopping", "Entertainment", "Bills",
    "Health", "Education", "Cinema", "Daily Need", "Salary",
    "Freelance", "Investment", "Gift", "Other"
)

private val defaultAccounts = DEFAULT_CANONICAL_ACCOUNTS

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
    recentDescriptions: List<String> = emptyList(),
    isUpdating: Boolean,
    onSave: (List<BulkEditTransactionItemDto>) -> Unit,
    onDismiss: () -> Unit
) {
    val dims = Dimens.current
    val focusManager = LocalFocusManager.current
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
        val list = if (availableAccounts.isNotEmpty()) availableAccounts else defaultAccounts
        sortAccountsCanonical(list)
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

    // Category Picker Dialog states (Batch & Individual)
    var showBatchCategoryPicker by remember { mutableStateOf(false) }
    var categoryPickerTargetItem by remember { mutableStateOf<EditableTxItem?>(null) }

    // Account Picker Dialog states (Batch & Individual)
    var showBatchAccountPicker by remember { mutableStateOf(false) }
    var accountPickerTargetItem by remember { mutableStateOf<EditableTxItem?>(null) }

    // Description / Note Suggestions state
    var batchDescription by remember { mutableStateOf("") }
    var isBatchDescriptionFocused by remember { mutableStateOf(false) }
    var activeDescriptionTargetItem by remember { mutableStateOf<EditableTxItem?>(null) }

    val isAnyDescriptionFocused = isBatchDescriptionFocused || activeDescriptionTargetItem != null

    // Intercept back gesture while Description is focused to dismiss keyboard cleanly
    BackHandler(enabled = isAnyDescriptionFocused) {
        focusManager.clearFocus()
        isBatchDescriptionFocused = false
        activeDescriptionTargetItem = null
    }

    val currentDescriptionText = when {
        isBatchDescriptionFocused -> batchDescription
        activeDescriptionTargetItem != null -> activeDescriptionTargetItem?.description ?: ""
        else -> ""
    }

    val defaultFallbackSuggestions = remember {
        listOf(
            "Food & Dining", "Grocery", "Uber / Auto", "Swiggy / Zomato",
            "Amazon / Shopping", "Mobile Recharge", "Electricity Bill", "Fuel / Petrol",
            "Medical / Health", "Entertainment", "Rent", "Salary"
        )
    }

    val descriptionSuggestions = remember(currentDescriptionText, recentDescriptions, items.map { it.description }) {
        val existing = (recentDescriptions + items.map { it.description }.filter { it.isNotBlank() } + defaultFallbackSuggestions).distinct()
        val query = currentDescriptionText.trim()
        if (query.isBlank()) {
            existing.take(40)
        } else {
            val (startsWith, contains) = existing
                .filter { !it.equals(query, ignoreCase = true) }
                .partition { it.startsWith(query, ignoreCase = true) }
            (startsWith + contains.filter { it.contains(query, ignoreCase = true) }).take(40)
        }
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

    // ── Batch & Individual Category Picker Dialog ─────────────────────
    if (showBatchCategoryPicker) {
        CategoryPickerDialog(
            allCategories = allCategories,
            recentCategories = recentCategories,
            onCategorySelected = { cat ->
                items.forEach { it.category = cat }
                batchFeedbackMessage = "Applied \"$cat\" to all ${items.size} items"
                showBatchCategoryPicker = false
            },
            onDismiss = { showBatchCategoryPicker = false }
        )
    }

    categoryPickerTargetItem?.let { target ->
        CategoryPickerDialog(
            allCategories = allCategories,
            recentCategories = recentCategories,
            onCategorySelected = { cat ->
                target.category = cat
                categoryPickerTargetItem = null
            },
            onDismiss = { categoryPickerTargetItem = null }
        )
    }

    // ── Batch & Individual Account Picker Dialog ──────────────────────
    if (showBatchAccountPicker) {
        AccountPickerDialog(
            accountsList = accountsList,
            currentAccount = items.firstOrNull()?.account ?: "",
            onAccountSelected = { acc ->
                items.forEach { it.account = acc }
                batchFeedbackMessage = "Applied \"$acc\" to all ${items.size} items"
                showBatchAccountPicker = false
            },
            onDismiss = { showBatchAccountPicker = false }
        )
    }

    accountPickerTargetItem?.let { target ->
        AccountPickerDialog(
            accountsList = accountsList,
            currentAccount = target.account,
            onAccountSelected = { acc ->
                target.account = acc
                accountPickerTargetItem = null
            },
            onDismiss = { accountPickerTargetItem = null }
        )
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.79f)
                .imePadding()
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

                                        // + More categories chip with search popup
                                        FilterChip(
                                            selected = false,
                                            onClick = { showBatchCategoryPicker = true },
                                            label = {
                                                Text(
                                                    text = "+ More (${allCategories.size})",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Search more categories",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                labelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = false,
                                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                                        )
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

                                        // + More accounts chip with search/picker popup
                                        FilterChip(
                                            selected = false,
                                            onClick = { showBatchAccountPicker = true },
                                            label = {
                                                Text(
                                                    text = "+ More (${accountsList.size})",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Search more accounts",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                labelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = false,
                                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                                        )
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

                        // 5. Batch Description / Note
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
                                            text = "Description / Note",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap to apply note to all items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = batchDescription,
                                            onValueChange = { batchDescription = it },
                                            placeholder = { Text("Note for all items...", style = MaterialTheme.typography.bodyMedium) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .onFocusChanged {
                                                    isBatchDescriptionFocused = it.isFocused
                                                    if (it.isFocused) activeDescriptionTargetItem = null
                                                }
                                        )

                                        Button(
                                            onClick = {
                                                items.forEach { it.description = batchDescription }
                                                batchFeedbackMessage = "Applied note to all ${items.size} items"
                                                focusManager.clearFocus()
                                            },
                                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text("Apply", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
                                onRequestDatePicker = { datePickerTargetItem = item },
                                onRequestCategoryPicker = { categoryPickerTargetItem = item },
                                onRequestAccountPicker = { accountPickerTargetItem = item },
                                onDescriptionFocused = {
                                    activeDescriptionTargetItem = item
                                    isBatchDescriptionFocused = false
                                },
                                onDescriptionBlurred = {
                                    if (activeDescriptionTargetItem == item) {
                                        activeDescriptionTargetItem = null
                                    }
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Sticky Bottom Footer or Docked Suggestion Accessory Bar ──────
            if (isAnyDescriptionFocused) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "SUGGESTIONS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• 1-tap to fill",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            TextButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    isBatchDescriptionFocused = false
                                    activeDescriptionTargetItem = null
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            descriptionSuggestions.forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.clickable {
                                        if (isBatchDescriptionFocused) {
                                            batchDescription = suggestion
                                            items.forEach { it.description = suggestion }
                                            batchFeedbackMessage = "Applied note to all ${items.size} items"
                                        } else if (activeDescriptionTargetItem != null) {
                                            activeDescriptionTargetItem?.description = suggestion
                                        }
                                        focusManager.clearFocus()
                                        isBatchDescriptionFocused = false
                                        activeDescriptionTargetItem = null
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
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
}
}

// ─────────────────────────────────────────────────────────────────────────────
// Clean Individual Transaction Card (Tactile Surfaces with Dialog Pickers)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CleanIndividualTxCard(
    index: Int,
    item: EditableTxItem,
    accountsList: List<String>,
    allCategories: List<String>,
    recentCategories: List<String>,
    displayDateFormat: SimpleDateFormat,
    apiDateFormat: SimpleDateFormat,
    onRequestDatePicker: () -> Unit,
    onRequestCategoryPicker: () -> Unit,
    onRequestAccountPicker: () -> Unit,
    onDescriptionFocused: () -> Unit,
    onDescriptionBlurred: () -> Unit
) {
    val dims = Dimens.current

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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    modifier = Modifier.width(115.dp)
                )
            }

            // Quick Attribute Selectors Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Picker Button
                Surface(
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(dims.buttonCornerRadius - 4.dp))
                        .clickable { onRequestCategoryPicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(CategoryEmojis.forCategory(item.category), fontSize = 13.sp)
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select category",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Account Picker Button
                Surface(
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(dims.buttonCornerRadius - 4.dp))
                        .clickable { onRequestAccountPicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.account,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select account",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Date Button
                Surface(
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(dims.buttonCornerRadius - 4.dp))
                        .clickable { onRequestDatePicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select date",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onDescriptionFocused()
                            } else {
                                onDescriptionBlurred()
                            }
                        }
                )

                Surface(
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 4.dp),
                    color = if (item.excludeAnalytics) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
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

// ─────────────────────────────────────────────────────────────────────────────
// Category Picker Dialog (Searchable with Add Custom Category)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerDialog(
    allCategories: List<String>,
    recentCategories: List<String> = emptyList(),
    currentCategory: String = "",
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val query = searchQuery.trim()

    val combinedCategories = remember(allCategories, recentCategories) {
        (recentCategories + allCategories).distinct()
    }

    val filteredCategories = remember(query, combinedCategories) {
        if (query.isBlank()) {
            combinedCategories
        } else {
            combinedCategories.filter { it.contains(query, ignoreCase = true) }
        }
    }

    val isExactMatch = combinedCategories.any { it.equals(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search or type category...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Custom category creation button if no exact match
                    if (query.isNotBlank() && !isExactMatch) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCategorySelected(query)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Add \"$query\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCategories.forEach { category ->
                            val isSelected = currentCategory.equals(category, ignoreCase = true)
                            val emoji = CategoryEmojis.forCategory(category)
                            val chipBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            val chipTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            val chipBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        onCategorySelected(category)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = emoji, fontSize = 16.sp)
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = chipTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Account Picker Dialog (Searchable with Account List & Icons)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountPickerDialog(
    accountsList: List<String>,
    currentAccount: String = "",
    onAccountSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val query = searchQuery.trim()

    val sortedAccounts = remember(accountsList) {
        sortAccountsCanonical(if (accountsList.isNotEmpty()) accountsList else defaultAccounts)
    }

    val filteredAccounts = remember(query, sortedAccounts) {
        if (query.isBlank()) sortedAccounts
        else sortedAccounts.filter { it.contains(query, ignoreCase = true) }
    }

    val isExactMatch = sortedAccounts.any { it.equals(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search or type account...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Custom account creation button if no exact match
                    if (query.isNotBlank() && !isExactMatch) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAccountSelected(query)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Add \"$query\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredAccounts.forEach { account ->
                            val isSelected = currentAccount.equals(account, ignoreCase = true)
                            val chipBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            val chipTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            val chipBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        onAccountSelected(account)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = account,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = chipTextColor
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
