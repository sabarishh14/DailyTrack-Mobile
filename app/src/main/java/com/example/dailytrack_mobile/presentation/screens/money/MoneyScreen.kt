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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailytrack_mobile.presentation.screens.money.components.AnalysisTab
import com.example.dailytrack_mobile.presentation.screens.money.components.FilterBottomSheet
import com.example.dailytrack_mobile.presentation.screens.money.components.TransactionsTab
import com.example.dailytrack_mobile.presentation.util.Dimens

// ─────────────────────────────────────────────────────────────────────────────
// Main composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MoneyScreen(
    viewModel: MoneyVM = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val dims = Dimens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom pill tab bar
        PillTabBar(
            selectedIndex = state.selectedTab,
            tabs = listOf("Cash Flow", "Transactions"),
            onTabSelected = { viewModel.onAction(MoneyAction.SelectTab(it)) },
            modifier = Modifier.padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge)
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

    // Filter Bottom Sheet for Spending Analyzer
    if (state.isFilterSheetVisible) {
        FilterBottomSheet(
            filterState = state.analysisFilterState,
            allCategories = state.allAvailableCategories,
            allAccounts = state.allAvailableAccounts,
            onApply = { updatedFilters ->
                viewModel.onAction(MoneyAction.ApplyAnalysisFilters(updatedFilters))
            },
            onDismiss = {
                viewModel.onAction(MoneyAction.SetFilterSheetVisible(false))
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
                        .padding(vertical = dims.itemSpacingLarge),
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
