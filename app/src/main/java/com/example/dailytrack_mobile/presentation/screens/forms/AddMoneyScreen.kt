package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Add Money (Transaction) Form — Modern Redesign with Theme Integration
// ─────────────────────────────────────────────────────────────────────────────

/** Maps to DB `type` column: "Debit" / "Credit" */
private enum class TransactionType(val label: String, val dbValue: String) {
    EXPENSE("Expense", "Debit"),
    INCOME("Income", "Credit")
}

private val defaultExpenseCategories = listOf(
    "Food", "Transport", "Shopping", "Health", "Housing",
    "Entertainment", "Education", "Travel", "Utilities", "Other"
)

private val defaultIncomeCategories = listOf(
    "Salary", "Freelance", "Investment", "Gift", "Other"
)

private val defaultAccounts = listOf(
    "Cash", "KOTAK", "IDBI", "FEDERAL", "CUB", "INDIAN", "ICICI", "HDFC", "SBI", "Axis", "CC-PINNACLE 6360"
)

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
    var categorySearchDialogOpen by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var excludeAnalytics by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val amountFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
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

    val displayDateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val apiDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val isDirty = remember(selectedType, categoryInput, amount, note, selectedAccount, excludeAnalytics) {
        amount.isNotBlank() || categoryInput.isNotBlank() || note.isNotBlank() ||
            selectedType != TransactionType.EXPENSE || excludeAnalytics
    }

    LaunchedEffect(isDirty) {
        onDirtyStateChanged(isDirty)
    }

    // Category lists based on type and DB
    val allCategories = remember(formState.categories) {
        if (formState.categories.isNotEmpty()) formState.categories else (defaultExpenseCategories + defaultIncomeCategories).distinct()
    }

    val currentCategoryList = remember(selectedType, allCategories) {
        if (selectedType == TransactionType.EXPENSE) {
            val base = defaultExpenseCategories
            val custom = allCategories.filter { c ->
                !c.equals("Salary", ignoreCase = true) &&
                !c.equals("Freelance", ignoreCase = true) &&
                !base.any { it.equals(c, ignoreCase = true) }
            }
            (base + custom).distinct()
        } else {
            val base = defaultIncomeCategories
            val custom = allCategories.filter { c ->
                (c.equals("Salary", ignoreCase = true) || c.equals("Freelance", ignoreCase = true) ||
                 c.equals("Investment", ignoreCase = true) || c.equals("Gift", ignoreCase = true) ||
                 c.equals("Other", ignoreCase = true)) && !base.any { it.equals(c, ignoreCase = true) }
            }
            (base + custom).distinct()
        }
    }

    // Display 4 pills: top 4 default categories, plus selected category if outside the top 4
    val visibleCategoryPills = remember(selectedType, currentCategoryList, categoryInput) {
        val top4 = currentCategoryList.take(4)
        if (categoryInput.isNotBlank() && !top4.any { it.equals(categoryInput, ignoreCase = true) }) {
            top4 + categoryInput
        } else {
            top4
        }
    }

    // Reset or validate category when type switches
    LaunchedEffect(selectedType) {
        if (categoryInput.isNotBlank() && !currentCategoryList.any { it.equals(categoryInput, ignoreCase = true) }) {
            categoryInput = ""
        }
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
                    Text("OK", fontWeight = FontWeight.Bold)
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

    // Category Search & Custom Creation Dialog
    if (categorySearchDialogOpen) {
        val query = categorySearchQuery.trim()
        val filteredCategories = remember(query, currentCategoryList) {
            if (query.isBlank()) {
                currentCategoryList
            } else {
                currentCategoryList.filter { it.contains(query, ignoreCase = true) }
            }
        }
        val isExactMatch = currentCategoryList.any { it.equals(query, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = {
                categorySearchDialogOpen = false
                categorySearchQuery = ""
            },
            title = {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = categorySearchQuery,
                        onValueChange = { categorySearchQuery = it },
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
                            if (categorySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { categorySearchQuery = "" }) {
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
                        // Create custom category button if no exact match
                        if (query.isNotBlank() && !isExactMatch) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        categoryInput = query
                                        categorySearchDialogOpen = false
                                        categorySearchQuery = ""
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

                        // Flow of categories
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredCategories.forEach { category ->
                                val isSelected = categoryInput.equals(category, ignoreCase = true)
                                val chipBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                val chipTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                val chipBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                        .clickable {
                                            categoryInput = category
                                            categorySearchDialogOpen = false
                                            categorySearchQuery = ""
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
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
            },
            confirmButton = {
                TextButton(onClick = {
                    categorySearchDialogOpen = false
                    categorySearchQuery = ""
                }) {
                    Text("Close")
                }
            }
        )
    }

    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

    // Theme-harmonious colors for expense and income
    val expenseBgColor = MaterialTheme.colorScheme.errorContainer
    val expenseTextColor = MaterialTheme.colorScheme.onErrorContainer
    val expenseAccentColor = MaterialTheme.colorScheme.error

    val incomeBgColor = MaterialTheme.colorScheme.tertiaryContainer
    val incomeTextColor = MaterialTheme.colorScheme.onTertiaryContainer
    val incomeAccentColor = MaterialTheme.colorScheme.tertiary

    val activeAmountColor = if (selectedType == TransactionType.EXPENSE) expenseAccentColor else incomeAccentColor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.screenHorizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Error Banner if any ──────────────────────────────────────
        formState.errorMessage?.let { errorMsg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    IconButton(
                        onClick = { formsVM.clearAddMoneyError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ── Expense / Income Segmented Switcher ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Expense Option
                val isExpense = selectedType == TransactionType.EXPENSE
                val expenseBg by animateColorAsState(
                    targetValue = if (isExpense) expenseBgColor else Color.Transparent,
                    animationSpec = tween(200),
                    label = "expenseBg"
                )
                val expenseText by animateColorAsState(
                    targetValue = if (isExpense) expenseTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "expenseText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(expenseBg)
                        .clickable { selectedType = TransactionType.EXPENSE }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↑ Expense",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = expenseText
                    )
                }

                // Income Option
                val isIncome = selectedType == TransactionType.INCOME
                val incomeBg by animateColorAsState(
                    targetValue = if (isIncome) incomeBgColor else Color.Transparent,
                    animationSpec = tween(200),
                    label = "incomeBg"
                )
                val incomeText by animateColorAsState(
                    targetValue = if (isIncome) incomeTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "incomeText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(incomeBg)
                        .clickable { selectedType = TransactionType.INCOME }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↓ Income",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = incomeText
                    )
                }
            }
        }

        // ── Large Amount Input Box (Tapping opens soft numpad) ─────────
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder,
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
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "AMOUNT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAmountColor.copy(alpha = 0.85f),
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    BasicTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Allow numbers and at most one decimal point with 2 decimals
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amount = newValue
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAmountColor
                        ),
                        cursorBrush = SolidColor(activeAmountColor),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(amountFocusRequester),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (amount.isEmpty()) {
                                    Text(
                                        text = "0",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeAmountColor.copy(alpha = 0.35f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }

        // ── Date & Account Selection Cards (Side-by-Side) ─────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // DATE Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "DATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayDateFormat.format(Date(selectedDate)),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ACCOUNT Card
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "ACCOUNT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedAccount ?: "Select",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Account",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    accountsList.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = account,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selectedAccount == account) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            onClick = {
                                selectedAccount = account
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // ── Category Flow Chips (4 Pills + Compact "+" Pill) ───────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleCategoryPills.forEach { cat ->
                    val isSelected = categoryInput.equals(cat, ignoreCase = true)
                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else cardBg
                    val chipTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    val chipBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorderColor, RoundedCornerShape(20.dp))
                            .clickable {
                                categoryInput = if (isSelected) "" else cat
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp
                            ),
                            color = chipTextColor
                        )
                    }
                }

                // Compact "+" icon-only pill to search and add categories
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable {
                            categorySearchDialogOpen = true
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Search / Add Category",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Description Box ──────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "DESCRIPTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.TopStart) {
                            if (note.isEmpty()) {
                                Text(
                                    text = "Optional note...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        // ── Exclude from Analytics Card ──────────────────────────────
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Exclude from Analytics",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Won't count in reports or charts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Switch(
                    checked = excludeAnalytics,
                    onCheckedChange = { excludeAnalytics = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Save Transaction Button ──────────────────────────────────
        val isValidAmount = (amount.toDoubleOrNull() ?: 0.0) > 0
        val isFormValid = !formState.isSaving && isValidAmount && categoryInput.isNotBlank() && selectedAccount != null

        Button(
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val dateStr = apiDateFormat.format(Date(selectedDate))
                formsVM.saveTransaction(
                    type = selectedType.dbValue,
                    category = categoryInput.trim().ifEmpty { "Other" },
                    amount = amt,
                    note = note.trim().takeIf { it.isNotBlank() },
                    accountName = selectedAccount ?: accountsList.firstOrNull() ?: "Cash",
                    date = dateStr,
                    excludeAnalytics = excludeAnalytics,
                    onSuccess = onSaveSuccess
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ),
            enabled = isFormValid
        ) {
            if (formState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Saving Transaction...",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            } else {
                Text(
                    text = "Save Transaction",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
