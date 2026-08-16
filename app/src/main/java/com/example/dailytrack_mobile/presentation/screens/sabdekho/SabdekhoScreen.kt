package com.example.dailytrack_mobile.presentation.screens.sabdekho

import androidx.compose.foundation.background
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dailytrack_mobile.presentation.util.Dimens

enum class MediaStatus(val label: String) {
    WATCHING("WATCHING"),
    WATCHED("WATCHED"),
    QUEUE("QUEUE"),
    DROPPED("DROPPED")
}

data class MediaItem(
    val id: Int,
    val title: String,
    val type: String,
    val status: MediaStatus,
    val rating: Double,
    val imageUrl: String
)

val mockMediaItems = listOf(
    MediaItem(1, "Dune: Part Two", "Movie", MediaStatus.WATCHING, 9.1, "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2JGqqUT1O.jpg"),
    MediaItem(2, "Shogun", "Series", MediaStatus.WATCHING, 9.3, "https://image.tmdb.org/t/p/w500/7O4iVfOMQmdCSxhOg1WNzG1SyKy.jpg"),
    MediaItem(3, "The Bear", "Series", MediaStatus.WATCHING, 8.7, "https://image.tmdb.org/t/p/w500/rFqETiaY6xGf2LbbFfE1P6qRjUa.jpg"),
    MediaItem(4, "Oppenheimer", "Movie", MediaStatus.WATCHED, 8.9, "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg"),
    MediaItem(5, "Fallout", "Series", MediaStatus.WATCHED, 8.6, "https://image.tmdb.org/t/p/w500/A8wQAh-JmXpA2B0s1eYjQdZqG1.jpg"),
    MediaItem(6, "Civil War", "Movie", MediaStatus.QUEUE, 7.5, "https://image.tmdb.org/t/p/w500/sh7Rg8Er3tFcN9BpKIPOMvALgZd.jpg"),
    MediaItem(7, "Severance", "Series", MediaStatus.QUEUE, 8.7, "https://image.tmdb.org/t/p/w500/zEqyD0SBt6HL7W9JQoWwtd5Do1O.jpg"),
    MediaItem(8, "The Boys", "Series", MediaStatus.DROPPED, 8.4, "https://image.tmdb.org/t/p/w500/2zmTngn1tYC1rmbUTB1Vlu5hNdo.jpg"),
    MediaItem(9, "3 Body Problem", "Series", MediaStatus.DROPPED, 7.6, "https://image.tmdb.org/t/p/w500/q3UfQfW00d-p-9zQzX28o5C31.jpg")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabdekhoScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var gridSpan by remember { mutableIntStateOf(2) }
    val dims = Dimens.current
    
    val filteredItems = remember(searchQuery) {
        if (searchQuery.isBlank()) mockMediaItems
        else mockMediaItems.filter { it.title.contains(searchQuery, ignoreCase = true) }
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
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
            
            // Grid option toggle (manual toggle preserved)
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
            FilterChipView("Watching", 3, isSelected = true)
            FilterChipView("Watched", 2, isSelected = false)
            FilterChipView("Queue", 2, isSelected = false)
            FilterChipView("Dropped", 2, isSelected = false)
        }
        
        Spacer(modifier = Modifier.height(dims.itemSpacingLarge))
        
        // Grid of items
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSpan),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
            contentPadding = PaddingValues(bottom = dims.screenBottomPadding + 48.dp) // padding for bottom nav
        ) {
            items(filteredItems) { item ->
                MediaCard(item = item, gridSpan = gridSpan)
            }
        }
    }
}

@Composable
fun FilterChipView(label: String, count: Int, isSelected: Boolean) {
    val dims = Dimens.current
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val badgeBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(50))
            .padding(horizontal = dims.itemSpacingLarge, vertical = dims.itemSpacingSmall + 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
        Spacer(modifier = Modifier.width(dims.itemSpacingSmall))
        Box(
            modifier = Modifier
                .size(dims.avatarSizeSmall - 8.dp)
                .background(badgeBg, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun MediaCard(item: MediaItem, gridSpan: Int) {
    val dims = Dimens.current
    // Dynamic height based on span to keep aspect ratio approximately 2:3 and scale with screen width
    val height = when (gridSpan) {
        2 -> dims.mediaCardHeight2Col
        3 -> dims.mediaCardHeight3Col
        else -> dims.mediaCardHeight4Col
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
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
        if (gridSpan <= 3) {
            Box(
                modifier = Modifier
                    .padding(dims.itemSpacingMedium)
                    .align(Alignment.TopStart)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Top Right: Status
        val statusBg = if (item.status == MediaStatus.WATCHING) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        val statusText = if (item.status == MediaStatus.WATCHING) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        
        Box(
            modifier = Modifier
                .padding(dims.itemSpacingMedium)
                .align(Alignment.TopEnd)
                .background(statusBg, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.status.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (gridSpan == 4) 8.sp else 9.sp
                ),
                color = statusText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        
        // Bottom Left: Title & Rating
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(dims.itemSpacingLarge - 2.dp)
        ) {
            Text(
                text = item.title,
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
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "★",
                    color = Color(0xFFFFC107),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.rating.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
