package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Add Money (Transaction) Form — aligned with DB schema
// ─────────────────────────────────────────────────────────────────────────────

/** Maps to DB `type` column: "Debit" / "Credit" */
private enum class TransactionType(val label: String, val dbValue: String) {
    EXPENSE("Expense", "Debit"),
    INCOME("Income", "Credit")
}

/** Maps to DB `heading` column — categories from the API */
private enum class MoneyCategory(val label: String, val emoji: String) {
    FOOD("Food", "🍔"),
    TRANSPORT("Transport", "🚌"),
    SHOPPING("Shopping", "🛒"),
    ENTERTAINMENT("Entertainment", "🎮"),
    BILLS("Bills", "📄"),
    HEALTH("Health", "💊"),
    EDUCATION("Education", "📚"),
    CINEMA("Cinema", "🎬"),
    DAILY_NEED("Daily Need", "🛒"),
    SALARY("Salary", "💰"),
    FREELANCE("Freelance", "💻"),
    INVESTMENT("Investment", "📈"),
    GIFT("Gift", "🎁"),
    OTHER("Other", "📦")
}

/** Available bank accounts — maps to DB `account` FK */
private val availableAccounts = listOf(
    "HDFC", "ICICI", "SBI", "Axis", "Kotak", "Cash",
    "CC-PINNACLE 6360", "CC-SBI"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMoneyScreen(
    onDirtyStateChanged: (Boolean) -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf<MoneyCategory?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var excludeAnalytics by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    val dims = Dimens.current

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val apiDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val isDirty = remember(selectedType, selectedCategory, amount, note, selectedAccount, excludeAnalytics) {
        amount.isNotBlank() || selectedCategory != null || note.isNotBlank() ||
            selectedType != TransactionType.EXPENSE || selectedAccount != null || excludeAnalytics
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
        // ── Transaction Type ─────────────────────────────────────────
        SectionLabel("Transaction Type")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TransactionType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
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
                availableAccounts.forEach { account ->
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
        val visibleCategories = if (selectedType == TransactionType.EXPENSE) {
            MoneyCategory.entries.filter {
                it != MoneyCategory.SALARY && it != MoneyCategory.FREELANCE
            }
        } else {
            MoneyCategory.entries.filter {
                it == MoneyCategory.SALARY || it == MoneyCategory.FREELANCE ||
                        it == MoneyCategory.INVESTMENT || it == MoneyCategory.GIFT ||
                        it == MoneyCategory.OTHER
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            visibleCategories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text("${category.emoji} ${category.label}", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                )
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
            onClick = { onSaveSuccess() },
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.searchBarHeight),
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            enabled = amount.isNotBlank() && selectedCategory != null && selectedAccount != null
        ) {
            Text(
                "Save Transaction",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
    }
}
