package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.screens.money.CategoryEmojis
import com.example.dailytrack_mobile.presentation.screens.money.Transaction
import com.example.dailytrack_mobile.presentation.screens.money.TransactionType
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.text.SimpleDateFormat
import java.util.*

private enum class EditTxType(val label: String, val dbValue: String, val color: Color) {
    EXPENSE("Expense", "Debit", Color(0xFFFF5252)),
    INCOME("Income", "Credit", Color(0xFF2ECC71)),
    SAVINGS("Savings", "Savings", Color(0xFF29B6F6)),
    INVESTMENT("Invest", "Investment", Color(0xFFAB47BC))
}

private val defaultCategories = listOf(
    "Food", "Transport", "Shopping", "Entertainment", "Bills",
    "Health", "Education", "Cinema", "Daily Need", "Salary",
    "Freelance", "Investment", "Gift", "Other"
)

private val defaultAccounts = listOf(
    "KOTAK", "IDBI", "FEDERAL", "CUB", "INDIAN", "ICICI", "HDFC", "SBI", "Axis", "Cash", "CC-PINNACLE 6360"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    availableAccounts: List<String>,
    availableCategories: List<String>,
    mostUsedCategories: List<String> = emptyList(),
    isUpdating: Boolean,
    onSave: (
        id: Long,
        type: String,
        category: String,
        amount: Double,
        note: String?,
        accountName: String,
        date: String,
        excludeAnalytics: Boolean
    ) -> Unit,
    onDelete: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    val dims = Dimens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val apiDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    // Initialize state from transaction
    val initialType = remember(transaction) {
        when {
            transaction.type == TransactionType.CREDIT || transaction.rawType.equals("Credit", ignoreCase = true) -> EditTxType.INCOME
            transaction.type == TransactionType.SAVINGS || transaction.isSavings -> EditTxType.SAVINGS
            transaction.type == TransactionType.INVESTMENT || transaction.isInvestment -> EditTxType.INVESTMENT
            else -> EditTxType.EXPENSE
        }
    }

    val initialDateMillis = remember(transaction) {
        if (transaction.rawDate.isNotBlank()) {
            try {
                apiDateFormat.parse(transaction.rawDate)?.time ?: transaction.timestampMillis
            } catch (_: Exception) {
                transaction.timestampMillis
            }
        } else {
            transaction.timestampMillis
        }
    }

    var selectedType by remember { mutableStateOf(initialType) }
    var selectedDate by remember { mutableStateOf(initialDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedInitialAmount = remember(transaction.amount) {
        if (transaction.amount % 1.0 == 0.0) "%.0f".format(transaction.amount)
        else "%.2f".format(transaction.amount)
    }
    var amount by remember { mutableStateOf(formattedInitialAmount) }
    var selectedAccount by remember { mutableStateOf(transaction.bank) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    var categoryInput by remember { mutableStateOf(transaction.category) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var excludeAnalytics by remember { mutableStateOf(transaction.isExcluded) }

    val accountsList = remember(availableAccounts) {
        val list = if (availableAccounts.isNotEmpty()) availableAccounts else defaultAccounts
        list.sortedBy { account ->
            val index = defaultAccounts.indexOfFirst { it.equals(account, ignoreCase = true) }
            if (index == -1) Int.MAX_VALUE else index
        }
    }

    val allCategories = remember(availableCategories) {
        if (availableCategories.isNotEmpty()) availableCategories else defaultCategories
    }

    // Recent categories prioritizing user's actual most used categories
    val recentCategories = remember(mostUsedCategories, allCategories, selectedType) {
        val source = if (mostUsedCategories.isNotEmpty()) mostUsedCategories else allCategories
        val filtered = if (selectedType == EditTxType.EXPENSE) {
            source.filter { !it.equals("Salary", ignoreCase = true) && !it.equals("Freelance", ignoreCase = true) }
        } else {
            source.filter {
                it.equals("Salary", ignoreCase = true) || it.equals("Freelance", ignoreCase = true) ||
                it.equals("Investment", ignoreCase = true) || it.equals("Gift", ignoreCase = true) ||
                it.equals("Other", ignoreCase = true)
            }.ifEmpty { listOf("Salary", "Freelance", "Investment", "Gift", "Other") }
        }
        (listOf(categoryInput) + filtered).distinct().take(8)
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            // ── Header ───────────────────────────────────────────────────────
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
                        color = selectedType.color.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = CategoryEmojis.forCategory(categoryInput),
                                fontSize = 20.sp
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Edit Transaction",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ID: #${transaction.id} • ${transaction.bank}",
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Scrollable Form Body ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Hero Amount Card (Centered without weird left-box bias)
                val amountFocusRequester = remember { FocusRequester() }
                Card(
                    shape = RoundedCornerShape(dims.cardCornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            amountFocusRequester.requestFocus()
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AMOUNT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        // Truly centered currency row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = selectedType.color
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BasicTextField(
                                value = amount,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() || it == '.' }) amount = newValue
                                },
                                modifier = Modifier
                                    .width(IntrinsicSize.Min)
                                    .defaultMinSize(minWidth = if (amount.isEmpty()) 64.dp else 16.dp)
                                    .focusRequester(amountFocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                cursorBrush = SolidColor(selectedType.color),
                                textStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                ),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (amount.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                // 2. Transaction Type Segmented Pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditTxType.entries.forEach { type ->
                            val isSelected = selectedType == type
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) type.color.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                label = "pill_bg"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) type.color
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                label = "pill_border"
                            )

                            Surface(
                                shape = RoundedCornerShape(dims.buttonCornerRadius),
                                color = bgColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(dims.buttonCornerRadius))
                                    .clickable { selectedType = type }
                            ) {
                                Text(
                                    text = type.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) type.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Date Selector with Shortcuts
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { selectedDate = System.currentTimeMillis() },
                            label = { Text("Today", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                selectedDate = cal.timeInMillis
                            },
                            label = { Text("Yesterday", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                        )

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(dims.iconSizeSmall)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayDateFormat.format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 4. Account Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = accountDropdownExpanded,
                        onExpandedChange = { accountDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Select Account", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(dims.buttonCornerRadius)
                        )
                        ExposedDropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            accountsList.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedAccount = account
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. Category Selector with Recent Categories Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Recent categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Horizontal scrolling row of Recent Categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentCategories.forEach { category ->
                            val isSelected = categoryInput.trim().equals(category.trim(), ignoreCase = true)
                            val emoji = CategoryEmojis.forCategory(category)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryInput = if (isSelected) "" else category
                                    categoryDropdownExpanded = false
                                },
                                label = { Text("$emoji $category", style = MaterialTheme.typography.bodyMedium) },
                                shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Search / Custom Category Input with Autocomplete
                    val query = categoryInput.trim()
                    val matchingCategories = remember(query, allCategories) {
                        if (query.isBlank()) emptyList()
                        else allCategories.filter { it.contains(query, ignoreCase = true) }
                    }
                    val isExactMatch = allCategories.any { it.equals(query, ignoreCase = true) }

                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded && (matchingCategories.isNotEmpty() || (query.isNotBlank() && !isExactMatch)),
                        onExpandedChange = { categoryDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = categoryInput,
                            onValueChange = {
                                categoryInput = it
                                categoryDropdownExpanded = it.isNotBlank()
                            },
                            placeholder = { Text("Search or type custom category", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                if (categoryInput.isNotBlank()) {
                                    Text(
                                        text = CategoryEmojis.forCategory(categoryInput),
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                if (categoryInput.isNotEmpty()) {
                                    IconButton(onClick = {
                                        categoryInput = ""
                                        categoryDropdownExpanded = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Category",
                                            modifier = Modifier.size(dims.iconSizeSmall)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            shape = RoundedCornerShape(dims.buttonCornerRadius),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded && (matchingCategories.isNotEmpty() || (query.isNotBlank() && !isExactMatch)),
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            matchingCategories.forEach { category ->
                                val emoji = CategoryEmojis.forCategory(category)
                                DropdownMenuItem(
                                    leadingIcon = { Text(emoji, fontSize = 16.sp) },
                                    text = { Text(category, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        categoryInput = category
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }

                            if (query.isNotBlank() && !isExactMatch) {
                                if (matchingCategories.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(dims.iconSizeSmall)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "Create \"$query\"",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    },
                                    onClick = {
                                        categoryInput = query
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 6. Note / Description
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Description (optional)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("What was this for?", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.Notes,
                                contentDescription = null,
                                modifier = Modifier.size(dims.iconSizeMedium)
                            )
                        },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(dims.buttonCornerRadius),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 7. Exclude from Spending Analyser
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
                                text = "Hide this transaction from the pie chart and stats",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = excludeAnalytics,
                            onCheckedChange = { excludeAnalytics = it }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Sticky Footer Action Buttons ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
                horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete Button
                OutlinedButton(
                    onClick = { onDelete(transaction) },
                    enabled = !isUpdating,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(dims.buttonCornerRadius),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(dims.iconSizeSmall)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Save Changes Button
                val isValidAmount = (amount.toDoubleOrNull() ?: 0.0) > 0
                val isFormValid = isValidAmount && categoryInput.isNotBlank() && selectedAccount.isNotBlank()

                Button(
                    onClick = {
                        val parsedAmt = amount.toDoubleOrNull() ?: 0.0
                        val dateStr = apiDateFormat.format(Date(selectedDate))
                        onSave(
                            transaction.id,
                            selectedType.dbValue,
                            categoryInput.trim().ifEmpty { "Other" },
                            parsedAmt,
                            note.takeIf { it.isNotBlank() },
                            selectedAccount,
                            dateStr,
                            excludeAnalytics
                        )
                    },
                    enabled = !isUpdating && isFormValid,
                    shape = RoundedCornerShape(dims.buttonCornerRadius),
                    modifier = Modifier
                        .weight(1f)
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
                        Text("Save Changes", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
