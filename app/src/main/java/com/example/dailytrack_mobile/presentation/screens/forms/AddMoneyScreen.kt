package com.example.dailytrack_mobile.presentation.screens.forms

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Add Money (Transaction) Form — aligned with DB schema & Live Backend
// ─────────────────────────────────────────────────────────────────────────────

/** Maps to DB `type` column: "Debit" / "Credit" */
private enum class TransactionType(val label: String, val dbValue: String) {
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
fun AddMoneyScreen(
    formsVM: FormsVM = hiltViewModel(),
    onDirtyStateChanged: (Boolean) -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    val formState by formsVM.addMoneyState.collectAsState()

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var categoryInput by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var excludeAnalytics by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    val dims = Dimens.current

    val accountsList = remember(formState.accounts) {
        if (formState.accounts.isNotEmpty()) formState.accounts else defaultAccounts
    }

    // Auto-select first account if not yet selected
    LaunchedEffect(accountsList) {
        if (selectedAccount == null && accountsList.isNotEmpty()) {
            selectedAccount = accountsList.first()
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val apiDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val isDirty = remember(selectedType, categoryInput, amount, note, selectedAccount, excludeAnalytics) {
        amount.isNotBlank() || categoryInput.isNotBlank() || note.isNotBlank() ||
            selectedType != TransactionType.EXPENSE || excludeAnalytics
    }

    LaunchedEffect(isDirty) {
        onDirtyStateChanged(isDirty)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
        verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing)
    ) {
        // ── Error Banner if any ──────────────────────────────────────
        formState.errorMessage?.let { errorMsg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(dims.cardCornerRadius),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Transaction Type ─────────────────────────────────────────
        SectionLabel("Transaction Type")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TransactionType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selectedType == type,
                    onClick = { 
                        selectedType = type
                        categoryInput = ""
                        categoryDropdownExpanded = false
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TransactionType.entries.size
                    )
                ) {
                    Text(type.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ── Date Picker ──────────────────────────────────────────────
        SectionLabel("Date")
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
                text = dateFormat.format(Date(selectedDate)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Amount ───────────────────────────────────────────────────
        SectionLabel("Amount")
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

        // ── Account Selector ─────────────────────────────────────────
        SectionLabel("Account")
        ExposedDropdownMenuBox(
            expanded = accountDropdownExpanded,
            onExpandedChange = { accountDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedAccount ?: "",
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

        // ── Category ─────────────────────────────────────────────────
        SectionLabel("Category")
        val allCategories = remember(formState.categories) {
            if (formState.categories.isNotEmpty()) formState.categories else defaultCategories
        }

        val visibleCategories = remember(selectedType, allCategories) {
            if (selectedType == TransactionType.EXPENSE) {
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
            if (selectedType == TransactionType.EXPENSE) {
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

        // Quick Category Chips (Top 5-6)
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
                        if (isSelected) {
                            categoryInput = ""
                        } else {
                            categoryInput = category
                            categoryDropdownExpanded = false
                        }
                    },
                    label = { Text("$emoji $category", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                )
            }
        }

        // Search / Custom Category Input with Autocomplete Dropdown
        val query = categoryInput.trim()
        val matchingCategories = remember(query, visibleCategories) {
            if (query.isBlank()) {
                emptyList()
            } else {
                visibleCategories.filter { it.contains(query, ignoreCase = true) }
            }
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

        // ── Note ─────────────────────────────────────────────────────
        SectionLabel("Description (optional)")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("What was this for?", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null, modifier = Modifier.size(dims.iconSizeMedium))
            },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Exclude from Analytics Toggle ────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Exclude from Analytics",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Won't appear in spending charts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = excludeAnalytics,
                onCheckedChange = { excludeAnalytics = it }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Save Button ──────────────────────────────────────────────
        Button(
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val dateStr = apiDateFormat.format(Date(selectedDate))
                formsVM.saveTransaction(
                    type = selectedType.dbValue,
                    category = categoryInput.trim().ifEmpty { "Other" },
                    amount = amt,
                    note = note.takeIf { it.isNotBlank() },
                    accountName = selectedAccount ?: accountsList.firstOrNull() ?: "KOTAK",
                    date = dateStr,
                    excludeAnalytics = excludeAnalytics,
                    onSuccess = onSaveSuccess
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.searchBarHeight),
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            enabled = !formState.isSaving && amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && categoryInput.isNotBlank() && selectedAccount != null
        ) {
            if (formState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saving Transaction...", style = MaterialTheme.typography.labelLarge)
            } else {
                Text("Save Transaction", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
    }
}


