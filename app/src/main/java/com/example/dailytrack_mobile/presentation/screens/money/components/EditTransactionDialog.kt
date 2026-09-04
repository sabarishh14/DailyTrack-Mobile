package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dailytrack_mobile.presentation.screens.money.Transaction
import com.example.dailytrack_mobile.presentation.screens.money.TransactionType
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class EditTxType(val label: String, val dbValue: String) {
    EXPENSE("Expense", "Debit"),
    INCOME("Income", "Credit")
}

private val defaultCategories = listOf(
    "Food", "Transport", "Shopping", "Entertainment", "Bills",
    "Health", "Education", "Cinema", "Daily Need", "Salary",
    "Freelance", "Investment", "Gift", "Other"
)

private val defaultAccounts = listOf(
    "KOTAK", "IDBI", "FEDERAL", "CUB", "INDIAN", "ICICI", "HDFC", "SBI", "Axis", "Cash", "CC-PINNACLE 6360"
)

private fun getCategoryEmoji(category: String): String {
    return when (category.lowercase(Locale.ROOT).trim()) {
        "food" -> "🍔"
        "transport", "travel", "fuel", "petrol" -> "🚌"
        "shopping", "grocery", "groceries" -> "🛒"
        "entertainment" -> "🎮"
        "bills", "utilities", "electricity", "rent" -> "📄"
        "health", "medical" -> "💊"
        "education" -> "📚"
        "cinema", "movies" -> "🎬"
        "daily need", "daily need's", "daily needs" -> "🛒"
        "salary" -> "💰"
        "freelance" -> "💻"
        "investment", "investments", "mutual funds", "stocks" -> "📈"
        "gift", "gifts" -> "🎁"
        else -> "🏷️"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    availableAccounts: List<String>,
    availableCategories: List<String>,
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
    val apiDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    // Initialize state from transaction
    val initialType = remember(transaction) {
        if (transaction.type == TransactionType.CREDIT || transaction.rawType.equals("Credit", ignoreCase = true)) {
            EditTxType.INCOME
        } else {
            EditTxType.EXPENSE
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

    Dialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isUpdating,
            dismissOnClickOutside = !isUpdating
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = dims.itemSpacingMedium),
            shape = RoundedCornerShape(dims.cardCornerRadius + 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Header ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✏️",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Edit Transaction",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Scrollable Form Body ─────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing)
                ) {
                    // 1. Transaction Type
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        EditTxType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = EditTxType.entries.size
                                )
                            ) {
                                Text(type.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // 2. Date Picker
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dims.buttonCornerRadius),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(dims.iconSizeSmall)
                        )
                        Spacer(modifier = Modifier.width(dims.itemSpacingMedium))
                        Text(
                            text = displayDateFormat.format(Date(selectedDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3. Amount
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() || it == '.' }) amount = newValue
                        },
                        placeholder = { Text("0.00", style = MaterialTheme.typography.bodyMedium) },
                        prefix = { Text("₹ ", style = MaterialTheme.typography.bodyLarge) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(dims.buttonCornerRadius),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. Account Selector
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

                    // 5. Category
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val visibleCategories = remember(selectedType, allCategories) {
                        if (selectedType == EditTxType.EXPENSE) {
                            allCategories.filter {
                                !it.equals("Salary", ignoreCase = true) && !it.equals("Freelance", ignoreCase = true)
                            }
                        } else {
                            allCategories.filter {
                                it.equals("Salary", ignoreCase = true) || it.equals("Freelance", ignoreCase = true) ||
                                it.equals("Investment", ignoreCase = true) || it.equals("Gift", ignoreCase = true) ||
                                it.equals("Other", ignoreCase = true)
                            }.ifEmpty { listOf("Salary", "Freelance", "Investment", "Gift", "Other") }
                        }
                    }

                    val quickCategories = remember(selectedType, visibleCategories) {
                        if (selectedType == EditTxType.EXPENSE) {
                            val preferred = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health")
                            val matched = preferred.filter { p -> visibleCategories.any { it.equals(p, ignoreCase = true) } }
                            val rest = visibleCategories.filter { c -> preferred.none { it.equals(c, ignoreCase = true) } }
                            (matched + rest).distinct().take(6)
                        } else {
                            val preferred = listOf("Salary", "Freelance", "Investment", "Gift", "Other")
                            val matched = preferred.filter { p -> visibleCategories.any { it.equals(p, ignoreCase = true) } }
                            val rest = visibleCategories.filter { c -> preferred.none { it.equals(c, ignoreCase = true) } }
                            (matched + rest).distinct().take(5)
                        }
                    }

                    // Quick category chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
                        verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
                    ) {
                        quickCategories.forEach { category ->
                            val isSelected = categoryInput.trim().equals(category.trim(), ignoreCase = true)
                            val emoji = getCategoryEmoji(category)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryInput = if (isSelected) "" else category
                                    categoryDropdownExpanded = false
                                },
                                label = { Text("$emoji $category", style = MaterialTheme.typography.bodyMedium) },
                                shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                            )
                        }
                    }

                    // Search / Custom Category Input with Autocomplete
                    val query = categoryInput.trim()
                    val matchingCategories = remember(query, visibleCategories) {
                        if (query.isBlank()) emptyList()
                        else visibleCategories.filter { it.contains(query, ignoreCase = true) }
                    }
                    val isExactMatch = visibleCategories.any { it.equals(query, ignoreCase = true) }

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
                                        text = getCategoryEmoji(categoryInput),
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
                                val emoji = getCategoryEmoji(category)
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

                    // 6. Note / Description
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

                    // 7. Spending Analyser Exclude Toggle
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(dims.cardCornerRadius),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Footer Action Buttons ────────────────────────────
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
                            Text("Save Changes", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
