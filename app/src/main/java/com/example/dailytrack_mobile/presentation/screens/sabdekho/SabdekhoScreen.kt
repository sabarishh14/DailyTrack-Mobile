package com.example.dailytrack_mobile.presentation.screens.sabdekho

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dailytrack_mobile.data.remote.dto.MediaShowDto
import com.example.dailytrack_mobile.presentation.util.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabdekhoScreen(viewModel: SabdekhoVM = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var gridSpan by remember { mutableIntStateOf(2) }
    val dims = Dimens.current
    
    val filteredItems = remember(state.searchQuery, state.shows) {
        if (state.searchQuery.isBlank()) state.shows
        else state.shows.filter { it.name?.contains(state.searchQuery, ignoreCase = true) == true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dims.screenHorizontalPadding)
    ) {
        Spacer(modifier = Modifier.height(dims.screenTopPadding))
        
        // Search and Grid Toggle Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onAction(SabdekhoAction.SearchQueryChanged(it)) },
                modifier = Modifier
                    .weight(1f)
                    .height(dims.searchBarHeight),
                placeholder = { 
                    Text(
                        "Search titles...", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(dims.iconSizeMedium)
                    ) 
                },
                shape = RoundedCornerShape(dims.buttonCornerRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.width(dims.itemSpacingLarge))
            
            // Grid option toggle
            IconButton(
                onClick = {
                    gridSpan = when (gridSpan) {
                        2 -> 3
                        3 -> 4
                        else -> 2
                    }
                },
                modifier = Modifier
                    .size(dims.searchBarHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(dims.buttonCornerRadius))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val gridIcon = when (gridSpan) {
                        2 -> Icons.Default.GridView
                        3 -> Icons.Default.ViewModule
                        else -> Icons.Default.ViewComfy
                    }
                    Icon(
                        imageVector = gridIcon,
                        contentDescription = "Toggle Grid",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(dims.iconSizeMedium)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(dims.sectionSpacing))
        
        Text(
            text = "${filteredItems.size} TITLES TRACKED",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(dims.itemSpacingLarge))
        
        // Scrollable row for filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            FilterChipView("Watching", isSelected = state.activeFilter == "WATCHING") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("WATCHING"))
            }
            FilterChipView("To Watch", isSelected = state.activeFilter == "TO WATCH") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("TO WATCH"))
            }
            FilterChipView("Watched", isSelected = state.activeFilter == "WATCHED") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("WATCHED"))
            }
            FilterChipView("Dropped", isSelected = state.activeFilter == "DROPPED") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("DROPPED"))
            }
        }
        
        Spacer(modifier = Modifier.height(dims.itemSpacingLarge))
        
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Grid of items
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSpan),
                verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
                horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
                contentPadding = PaddingValues(bottom = dims.screenBottomPadding + 48.dp) // padding for bottom nav
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    MediaCard(item = item, gridSpan = gridSpan)
                }
            }
        }
    }
}

@Composable
fun FilterChipView(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val dims = Dimens.current
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = dims.itemSpacingLarge, vertical = dims.itemSpacingSmall + 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun MediaCard(item: MediaShowDto, gridSpan: Int) {
    val dims = Dimens.current
    // Dynamic height based on span to keep aspect ratio approximately 2:3 and scale with screen width
    val height = when (gridSpan) {
        2 -> dims.mediaCardHeight2Col
        3 -> dims.mediaCardHeight3Col
        else -> dims.mediaCardHeight4Col
    }
    
    val imageUrl = if (item.posterPath != null) "https://image.tmdb.org/t/p/w500${item.posterPath}" else ""
    val typeLabel = if (item.type == "movie") "Movie" else "Series"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name ?: "",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Gradient overlay for bottom text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        startY = 0f
                    )
                )
        )
        
        // Top Left: Type (Movie/Series)
        if (gridSpan <= 3 && item.type != null) {
            Box(
                modifier = Modifier
                    .padding(dims.itemSpacingMedium)
                    .align(Alignment.TopStart)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Top Right: Status
        val isWatching = item.status?.equals("WATCHING", ignoreCase = true) == true
        val statusBg = if (isWatching) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        val statusText = if (isWatching) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        
        Box(
            modifier = Modifier
                .padding(dims.itemSpacingMedium)
                .align(Alignment.TopEnd)
                .background(statusBg, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.status ?: "UNKNOWN",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (gridSpan == 4) 8.sp else 9.sp
                ),
                color = statusText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        
        // Bottom Left: Title
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(dims.itemSpacingLarge - 2.dp)
        ) {
            Text(
                text = item.name ?: "Unknown",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = when (gridSpan) {
                    2 -> MaterialTheme.typography.bodyMedium
                    3 -> MaterialTheme.typography.labelLarge
                    else -> MaterialTheme.typography.labelSmall
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
