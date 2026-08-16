package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    "Cinema"        to Color(0xFFE040FB),
    "Daily Need"    to Color(0xFF8D6E63),
    "Education"     to Color(0xFF42A5F5),
    "Investment"    to Color(0xFF66BB6A),
    "Salary"        to Color(0xFF26A69A),
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

        // Content: Loading, Error, Empty, or Transaction list
        when {
            state.isLoading && state.transactions.isEmpty() -> {
                // Initial loading state
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
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Loading transactions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            state.errorMessage != null && state.transactions.isEmpty() -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
                        modifier = Modifier.padding(horizontal = dims.screenHorizontalPadding)
                    ) {
                        Text(
                            text = "😕",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "Couldn't load transactions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = { onAction(MoneyAction.Refresh) }) {
                            Text("Retry", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            state.filteredTransactions.isEmpty() && !state.isLoading -> {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                // Transaction list with pagination
                val listState = rememberLazyListState()

                // Trigger load more when reaching near bottom
                val shouldLoadMore = remember {
                    derivedStateOf {
                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItems = listState.layoutInfo.totalItemsCount
                        lastVisibleItem >= totalItems - 5 // Load 5 items before end
                    }
                }

                LaunchedEffect(shouldLoadMore.value) {
                    if (shouldLoadMore.value && state.hasMore && !state.isLoadingMore) {
                        onAction(MoneyAction.LoadMore)
                    }
                }

                LazyColumn(
                    state = listState,
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
                        key = { it.id }
                    ) { transaction ->
                        TransactionItemCard(transaction = transaction)
                    }

                    // Loading more indicator
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = dims.itemSpacingLarge),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
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

            // Title + Description + Date/Bank
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Show description (category) as subtitle when it differs from title
                if (!transaction.description.isNullOrBlank()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
