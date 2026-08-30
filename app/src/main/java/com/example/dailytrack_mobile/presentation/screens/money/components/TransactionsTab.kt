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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dailytrack_mobile.presentation.screens.money.MoneyAction
import com.example.dailytrack_mobile.presentation.screens.money.MoneyState
import com.example.dailytrack_mobile.presentation.util.Dimens

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
            modifier = Modifier.padding(
                start = dims.screenHorizontalPadding,
                end = dims.screenHorizontalPadding,
                top = dims.itemSpacingMedium,
                bottom = dims.itemSpacingSmall
            ),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
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

                // Track single swiped item state
                var swipedTransactionId by remember { mutableStateOf<Long?>(null) }

                // Auto-close swiped item when scrolling
                LaunchedEffect(listState.isScrollInProgress) {
                    if (listState.isScrollInProgress && swipedTransactionId != null) {
                        swipedTransactionId = null
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
                        SwipeableTransactionItem(
                            transaction = transaction,
                            isSwiped = swipedTransactionId == transaction.id,
                            onSwipeStateChanged = { isSwiped ->
                                swipedTransactionId = if (isSwiped) transaction.id else if (swipedTransactionId == transaction.id) null else swipedTransactionId
                            },
                            onClick = { onAction(MoneyAction.ShowTransactionDetail(transaction)) },
                            onEdit = { onAction(MoneyAction.ShowEditDialog(transaction)) },
                            onDelete = { onAction(MoneyAction.ShowDeleteConfirmation(transaction)) }
                        )
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
