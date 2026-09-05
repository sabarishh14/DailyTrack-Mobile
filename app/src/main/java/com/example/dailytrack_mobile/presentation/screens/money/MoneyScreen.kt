package com.example.dailytrack_mobile.presentation.screens.money

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dailytrack_mobile.presentation.components.DailyTrackPullToRefreshBox
import com.example.dailytrack_mobile.presentation.screens.money.components.AnalysisTab
import com.example.dailytrack_mobile.presentation.screens.money.components.BulkDeleteConfirmationDialog
import com.example.dailytrack_mobile.presentation.screens.money.components.BulkEditTransactionsSheet
import com.example.dailytrack_mobile.presentation.screens.money.components.DeleteConfirmationDialog
import com.example.dailytrack_mobile.presentation.screens.money.components.EditTransactionDialog
import com.example.dailytrack_mobile.presentation.screens.money.components.FilterBottomSheet
import com.example.dailytrack_mobile.presentation.screens.money.components.TransactionDetailBottomSheet
import com.example.dailytrack_mobile.presentation.screens.money.components.TransactionsTab
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Main composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MoneyScreen(
    viewModel: MoneyVM = hiltViewModel(),
    initialTab: Int? = null,
    onTabConsumed: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val dims = Dimens.current
    val snackbarHostState = remember { SnackbarHostState() }

    androidx.activity.compose.BackHandler(enabled = state.isSelectionMode) {
        viewModel.onAction(MoneyAction.ClearTransactionSelection)
    }

    LaunchedEffect(state.isSelectionMode) {
        onSelectionModeChange(state.isSelectionMode)
    }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            viewModel.onAction(MoneyAction.SelectTab(initialTab))
            onTabConsumed()
        }
    }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(MoneyAction.ClearActionMessage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
    DailyTrackPullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.onAction(MoneyAction.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom pill tab bar
            PillTabBar(
                selectedIndex = state.selectedTab,
                tabs = listOf("Cash Flow", "Transactions"),
                onTabSelected = { viewModel.onAction(MoneyAction.SelectTab(it)) },
                modifier = Modifier.padding(
                    start = dims.screenHorizontalPadding,
                    end = dims.screenHorizontalPadding,
                    top = dims.itemSpacingMedium,
                    bottom = dims.itemSpacingSmall
                )
            )

            // Tab content with crossfade
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    fadeIn() + slideInHorizontally(
                        initialOffsetX = { if (targetState > initialState) it / 4 else -it / 4 }
                    ) togetherWith fadeOut() + slideOutHorizontally(
                        targetOffsetX = { if (targetState > initialState) -it / 4 else it / 4 }
                    )
                },
                label = "MoneyTabContent"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> AnalysisTab(
                        state = state,
                        onAction = viewModel::onAction
                    )
                    1 -> TransactionsTab(
                        state = state,
                        onAction = viewModel::onAction
                    )
                }
            }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Transaction Detail Bottom Sheet
    state.detailTransaction?.let { tx ->
        TransactionDetailBottomSheet(
            transaction = tx,
            onEdit = { transactionToEdit ->
                viewModel.onAction(MoneyAction.ShowEditDialog(transactionToEdit))
            },
            onDelete = { transactionToDelete ->
                viewModel.onAction(MoneyAction.ShowDeleteConfirmation(transactionToDelete))
            },
            onDismiss = {
                viewModel.onAction(MoneyAction.DismissDialogs)
            }
        )
    }

    // Edit Transaction Dialog
    state.editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            availableAccounts = state.allAvailableAccounts,
            availableCategories = state.allAvailableCategories,
            mostUsedCategories = state.mostUsedCategories,
            isUpdating = state.isUpdating,
            onSave = { id, type, category, amount, note, accountName, date, excludeAnalytics ->
                viewModel.onAction(
                    MoneyAction.UpdateTransaction(
                        id = id,
                        type = type,
                        category = category,
                        amount = amount,
                        note = note,
                        accountName = accountName,
                        date = date,
                        excludeAnalytics = excludeAnalytics
                    )
                )
            },
            onDelete = { transactionToDelete ->
                viewModel.onAction(MoneyAction.ShowDeleteConfirmation(transactionToDelete))
            },
            onDismiss = {
                viewModel.onAction(MoneyAction.DismissDialogs)
            }
        )
    }

    // Delete Confirmation Dialog
    state.deletingTransaction?.let { tx ->
        DeleteConfirmationDialog(
            transaction = tx,
            isDeleting = state.isDeleting,
            onConfirm = {
                viewModel.onAction(MoneyAction.DeleteTransaction(tx.id))
            },
            onDismiss = {
                viewModel.onAction(MoneyAction.DismissDialogs)
            }
        )
    }

    // Filter Bottom Sheet for Spending Analyzer
    if (state.isFilterSheetVisible) {
        FilterBottomSheet(
            filterState = state.analysisFilterState,
            allCategories = state.mostUsedCategories,
            allAccounts = state.allAvailableAccounts,
            onApply = { updatedFilters ->
                viewModel.onAction(MoneyAction.ApplyAnalysisFilters(updatedFilters))
            },
            onDismiss = {
                viewModel.onAction(MoneyAction.SetFilterSheetVisible(false))
            }
        )
    }

    // Bulk Edit Transactions Bottom Sheet
    if (state.showBulkEditSheet && state.selectedTransactions.isNotEmpty()) {
        val recentDescriptions = remember(state.transactions) {
            state.transactions
                .mapNotNull { it.note?.takeIf { n -> n.isNotBlank() } ?: it.title.takeIf { t -> t != it.category && t.isNotBlank() } }
                .distinct()
                .take(30)
        }
        BulkEditTransactionsSheet(
            transactions = state.selectedTransactions,
            availableAccounts = state.allAvailableAccounts,
            availableCategories = state.allAvailableCategories,
            mostUsedCategories = state.mostUsedCategories,
            recentDescriptions = recentDescriptions,
            isUpdating = state.isBulkUpdating,
            onSave = { updates ->
                viewModel.onAction(MoneyAction.ExecuteBulkEdit(updates))
            },
            onDismiss = {
                viewModel.onAction(MoneyAction.ShowBulkEditSheet(false))
            }
        )
    }

    // Bulk Delete Confirmation Dialog
    if (state.showBulkDeleteConfirm && state.selectedTransactionIds.isNotEmpty()) {
        BulkDeleteConfirmationDialog(
            selectedCount = state.selectedTransactionIds.size,
            isDeleting = state.isBulkDeleting,
            onConfirm = {
                viewModel.onAction(MoneyAction.ExecuteBulkDelete)
            },
            onDismiss = {
                viewModel.onAction(MoneyAction.ShowBulkDeleteConfirmation(false))
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pill Tab Bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PillTabBar(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.buttonCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .padding(vertical = dims.itemSpacingMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
