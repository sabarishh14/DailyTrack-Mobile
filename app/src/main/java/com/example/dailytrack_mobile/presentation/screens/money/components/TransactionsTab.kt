package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.screens.money.ChartColors
import com.example.dailytrack_mobile.presentation.screens.money.MoneyAction
import com.example.dailytrack_mobile.presentation.screens.money.MoneyState
import com.example.dailytrack_mobile.presentation.screens.money.Transaction
import com.example.dailytrack_mobile.presentation.screens.money.TransactionType
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun formatAmount(transaction: Transaction): String {
    val prefix = if (transaction.type == TransactionType.CREDIT) "+" else "-"
    val abs = Math.abs(transaction.amount)
    return when {
        abs >= 1_00_000 -> "${prefix}₹%.0fL".format(abs / 1_00_000)
        abs >= 1_000 && abs % 1_000 == 0.0 -> "${prefix}₹%,.0f".format(abs)
        else -> "${prefix}₹%,.0f".format(abs)
    }
}

private val categoryEmojiColors: Map<String, Color> = mapOf(
    "Food"          to Color(0xFFF5A623),
    "Bills"         to Color(0xFF4A90D9),
    "Shopping"      to Color(0xFF9B59B6),
    "Transport"     to Color(0xFF1ABC9C),
    "Health"        to Color(0xFFE91E63),
    "Entertainment" to Color(0xFFFF7043),
    "Income"        to Color(0xFF2ECC71),
)

// ─────────────────────────────────────────────────────────────────────────────
// Transactions Tab
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TransactionsTab(
    state: MoneyState,
    onAction: (MoneyAction) -> Unit
) {
    val dims = Dimens.current
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search bar + category filters (non-scrollable header)
        Column(
            modifier = Modifier.padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { onAction(MoneyAction.UpdateSearchQuery(it)) }
            )
            CategoryFilterRow(
                categories = state.categoryFilters,
                selected = state.selectedCategory,
                onSelect = { onAction(MoneyAction.SelectCategory(it)) }
            )
        }

        // Transaction list (scrollable)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dims.screenHorizontalPadding,
                end = dims.screenHorizontalPadding,
                bottom = dims.screenBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            items(
                items = state.filteredTransactions,
                key = { "${it.title}_${it.date}" }
            ) { transaction ->
                TransactionItemCard(transaction = transaction)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val dims = Dimens.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.searchBarHeight),
        placeholder = {
            Text(
                text = "Search transactions...",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = RoundedCornerShape(dims.cardCornerRadius - 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Filter Chips
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
    ) {
        categories.forEach { category ->
            val isSelected = category == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dims.cardCornerRadius))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(dims.cardCornerRadius)
                    )
                    .clickable { onSelect(category) }
                    .padding(horizontal = dims.itemSpacingLarge, vertical = dims.itemSpacingMedium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium.copy(
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
// Transaction Item Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TransactionItemCard(transaction: Transaction) {
    val dims = Dimens.current
    val emojiBgColor = categoryEmojiColors[transaction.category]
        ?: MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.cardCornerRadius - 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
        ) {
            // Emoji icon in a coloured circle
            Box(
                modifier = Modifier
                    .size(dims.iconSizeXLarge)
                    .clip(CircleShape)
                    .background(emojiBgColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.emoji,
                    fontSize = dims.fontSizeTitleLarge
                )
            }

            // Title + Date/Bank
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)) {
                    Text(
                        text = transaction.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = transaction.bank,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount
            Text(
                text = formatAmount(transaction),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (transaction.type == TransactionType.CREDIT) ChartColors.IncomeGreen
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
