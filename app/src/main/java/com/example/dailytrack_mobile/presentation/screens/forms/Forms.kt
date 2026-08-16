package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dailytrack_mobile.presentation.util.Dimens

@Composable
fun SyncBrokerScreen(
    onNavigateBack: () -> Unit = {}
) {
    val dims = Dimens.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(dims.avatarSizeLarge + 16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 5.dp
            )
            Text(
                text = "Syncing with Broker...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Fetching portfolio holdings & transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
            OutlinedButton(onClick = onNavigateBack) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
