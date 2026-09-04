package com.example.dailytrack_mobile.presentation.screens.money.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dailytrack_mobile.presentation.screens.money.*
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
    val filterState = state.analysisFilterState

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search bar + Filter button + active filter chips (non-scrollable header)
        Column(
            modifier = Modifier.padding(
                start = dims.screenHorizontalPadding,
                end = dims.screenHorizontalPadding,
                top = dims.itemSpacingMedium,
                bottom = dims.itemSpacingSmall
            ),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            // Search Bar & Filter Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onAction(MoneyAction.UpdateSearchQuery(it)) },
                    modifier = Modifier.weight(1f)
                )

                FilterButtonWithBadge(
                    activeCount = filterState.activeFilterCount,
                    onClick = { onAction(MoneyAction.SetFilterSheetVisible(true)) }
                )
            }

            // Active Filter Chips or Category Selector
            if (filterState.hasActiveFilters) {
                ActiveFiltersChipRow(
                    filterState = filterState,
                    onAction = onAction
                )
            } else {
                CategoryFilterRow(
                    categories = state.categoryFilters,
                    selected = state.selectedCategory,
                    onSelect = { onAction(MoneyAction.SelectCategory(it)) }
                )
            }

            // Result summary status row when filtered or searching
            if (filterState.hasActiveFilters || state.searchQuery.isNotBlank() || state.selectedCategory != "All") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.filteredTransactions.size == 1) "1 transaction found"
                               else "${state.filteredTransactions.size} transactions found",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Clear Filters",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                onAction(MoneyAction.ResetAnalysisFilters)
                                if (state.selectedCategory != "All") {
                                    onAction(MoneyAction.SelectCategory("All"))
                                }
                                if (state.searchQuery.isNotBlank()) {
                                    onAction(MoneyAction.UpdateSearchQuery(""))
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
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
                // Filtered or Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
                        modifier = Modifier.padding(horizontal = dims.screenHorizontalPadding)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (filterState.hasActiveFilters || state.searchQuery.isNotBlank() || state.selectedCategory != "All")
                                "No Matching Transactions"
                            else "No transactions found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (filterState.hasActiveFilters || state.searchQuery.isNotBlank() || state.selectedCategory != "All")
                                "No transactions match your current search or active filters."
                            else "No transactions are available in your account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (filterState.hasActiveFilters || state.searchQuery.isNotBlank() || state.selectedCategory != "All") {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    onAction(MoneyAction.ResetAnalysisFilters)
                                    onAction(MoneyAction.UpdateSearchQuery(""))
                                    onAction(MoneyAction.SelectCategory("All"))
                                },
                                shape = RoundedCornerShape(dims.buttonCornerRadius)
                            ) {
                                Text("Reset Filters & Search")
                            }
                        }
                    }
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
// Filter Button With Active Badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FilterButtonWithBadge(
    activeCount: Int,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(dims.cardCornerRadius - 4.dp),
        color = if (activeCount > 0) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.height(dims.searchBarHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filters",
                tint = if (activeCount > 0) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            if (activeCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "$activeCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Active Filters Chip Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActiveFiltersChipRow(
    filterState: AnalysisFilterState,
    onAction: (MoneyAction) -> Unit
) {
    val dims = Dimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear all button
        Surface(
            onClick = { onAction(MoneyAction.ResetAnalysisFilters) },
            shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            modifier = Modifier.height(30.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear all",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Active Quick Date Preset
        if (filterState.activeDatePreset == QuickFilterPreset.THIS_MONTH) {
            ActiveFilterChipItem(
                text = "This Month",
                onRemove = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.THIS_MONTH)) }
            )
        }
        if (filterState.activeDatePreset == QuickFilterPreset.LAST_MONTH) {
            ActiveFilterChipItem(
                text = "Last Month",
                onRemove = { onAction(MoneyAction.ToggleQuickPreset(QuickFilterPreset.LAST_MONTH)) }
            )
        }

        // Active Financial Year
        if (!filterState.financialYear.isNullOrBlank() && filterState.financialYear != "All Time") {
            ActiveFilterChipItem(
                text = filterState.financialYear,
                onRemove = { onAction(MoneyAction.ClearFinancialYearFilter) }
            )
        }

        // Active Custom Date Range
        if (filterState.activeDatePreset == null) {
            filterState.formattedDateRange()?.let { rangeText ->
                ActiveFilterChipItem(
                    text = rangeText,
                    onRemove = { onAction(MoneyAction.ClearDateRangeFilter) }
                )
            }
        }

        // Active Types
        if (filterState.selectedTypes.isNotEmpty()) {
            filterState.selectedTypes.forEach { type ->
                ActiveFilterChipItem(
                    text = if (type == TransactionType.DEBIT) "Debit" else "Credit",
                    onRemove = { onAction(MoneyAction.RemoveTypeFilter(type)) }
                )
            }
        }

        // Active Categories
        filterState.categoryFilters.forEach { (cat, status) ->
            when (status) {
                ItemFilterStatus.INCLUDED -> {
                    ActiveFilterChipItem(
                        text = "+ $cat",
                        isIncluded = true,
                        onRemove = { onAction(MoneyAction.RemoveCategoryFilter(cat)) }
                    )
                }
                ItemFilterStatus.EXCLUDED -> {
                    ActiveFilterChipItem(
                        text = "- $cat",
                        isExcluded = true,
                        onRemove = { onAction(MoneyAction.RemoveCategoryFilter(cat)) }
                    )
                }
                ItemFilterStatus.NEUTRAL -> Unit
            }
        }

        // Active Accounts
        filterState.accountFilters.forEach { (acc, status) ->
            when (status) {
                ItemFilterStatus.INCLUDED -> {
                    ActiveFilterChipItem(
                        text = "+ $acc",
                        isIncluded = true,
                        onRemove = { onAction(MoneyAction.RemoveAccountFilter(acc)) }
                    )
                }
                ItemFilterStatus.EXCLUDED -> {
                    ActiveFilterChipItem(
                        text = "- $acc",
                        isExcluded = true,
                        onRemove = { onAction(MoneyAction.RemoveAccountFilter(acc)) }
                    )
                }
                ItemFilterStatus.NEUTRAL -> Unit
            }
        }
    }
}

@Composable
private fun ActiveFilterChipItem(
    text: String,
    onRemove: () -> Unit,
    isIncluded: Boolean = false,
    isExcluded: Boolean = false
) {
    val dims = Dimens.current
    val containerColor = when {
        isExcluded -> MaterialTheme.colorScheme.errorContainer
        isIncluded -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when {
        isExcluded -> MaterialTheme.colorScheme.onErrorContainer
        isIncluded -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
        color = containerColor,
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = contentColor
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove filter",
                    modifier = Modifier.size(12.dp),
                    tint = contentColor
                )
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
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
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
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else null,
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
