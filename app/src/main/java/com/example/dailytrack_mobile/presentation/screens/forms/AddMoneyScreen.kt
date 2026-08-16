package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Add Money (Transaction) Form
// ─────────────────────────────────────────────────────────────────────────────

private enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income")
}

private enum class MoneyCategory(val label: String, val emoji: String) {
    FOOD("Food", "🍔"),
    TRANSPORT("Transport", "🚌"),
    SHOPPING("Shopping", "🛒"),
    ENTERTAINMENT("Fun", "🎮"),
    BILLS("Bills", "📄"),
    HEALTH("Health", "💊"),
    EDUCATION("Education", "📚"),
    SALARY("Salary", "💰"),
    FREELANCE("Freelance", "💻"),
    INVESTMENT("Investment", "📈"),
    GIFT("Gift", "🎁"),
    OTHER("Other", "📦")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMoneyScreen() {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf<MoneyCategory?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val dims = Dimens.current

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
        SectionLabel("Note (optional)")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("What was this for?", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(Icons.Outlined.Notes, contentDescription = null, modifier = Modifier.size(dims.iconSizeMedium))
            },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Save Button ──────────────────────────────────────────────
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.searchBarHeight),
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            enabled = amount.isNotBlank() && selectedCategory != null
        ) {
            Text(
                "Save Transaction",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helper used across all form screens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
