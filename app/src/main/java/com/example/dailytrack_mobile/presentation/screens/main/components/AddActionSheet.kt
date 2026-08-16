package com.example.dailytrack_mobile.presentation.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dailytrack_mobile.presentation.util.Dimens

data class ActionItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionSheet(
    onActionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val actions = listOf(
        ActionItem("Money", Icons.Default.CurrencyRupee, "add_money"),
        ActionItem("Activity", Icons.Default.FitnessCenter, "add_activity"),
        ActionItem("Movie", Icons.Default.Movie, "add_movie"),
        ActionItem("Asset", Icons.Default.AccountBalance, "add_asset"),
        ActionItem("Investment", Icons.Default.TrendingUp, "add_investment"),
        ActionItem("Sync", Icons.Default.Sync, "sync_broker")
    )
    val dims = Dimens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "What would you like to add?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = dims.sectionSpacing)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
                horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
                modifier = Modifier.padding(bottom = dims.screenBottomPadding)
            ) {
                items(actions) { action ->
                    ActionCard(
                        item = action,
                        onClick = {
                            onActionSelected(action.route)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    item: ActionItem,
    onClick: () -> Unit
) {
    val dims = Dimens.current
    Surface(
        shape = RoundedCornerShape(dims.cardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dims.cardCornerRadius))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(dims.cardInnerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(dims.avatarSizeLarge - 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(dims.iconSizeLarge)
                )
            }
            Spacer(modifier = Modifier.height(dims.itemSpacingLarge))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
