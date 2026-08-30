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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.ui.text.style.TextAlign
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

        // ── Search and Grid Toggle Row ──────────────────────────────
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
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onAction(SabdekhoAction.SearchQueryChanged("")) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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

            // Grid Option Toggle Button
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

        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))

        // ── Media Type & Status Filter Chips ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type filters
            FilterChipView("All", isSelected = state.mediaTypeFilter == "all") {
                viewModel.onAction(SabdekhoAction.ChangeMediaType("all"))
            }
            FilterChipView("🎬 Movies", isSelected = state.mediaTypeFilter == "movie") {
                viewModel.onAction(SabdekhoAction.ChangeMediaType("movie"))
            }
            FilterChipView("📺 Series", isSelected = state.mediaTypeFilter == "tv") {
                viewModel.onAction(SabdekhoAction.ChangeMediaType("tv"))
            }

            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Status filters
            FilterChipView("▶️ Watching", isSelected = state.activeFilter == "WATCHING") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("WATCHING"))
            }
            FilterChipView("📋 To Watch", isSelected = state.activeFilter == "TO WATCH") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("TO WATCH"))
            }
            FilterChipView("✅ Watched", isSelected = state.activeFilter == "WATCHED") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("WATCHED"))
            }
            FilterChipView("❌ Dropped", isSelected = state.activeFilter == "DROPPED") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("DROPPED"))
            }
            FilterChipView("All Status", isSelected = state.activeFilter == "all") {
                viewModel.onAction(SabdekhoAction.ChangeFilter("all"))
            }
        }

        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))

        // ── Section Header / Counter ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredItems.size} TITLES TRACKED",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )

            IconButton(
                onClick = { viewModel.onAction(SabdekhoAction.Refresh) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(dims.itemSpacingSmall))

        // ── Content Area ────────────────────────────────────────────
        if (state.isLoading && state.shows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null && state.shows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = state.error ?: "Error loading library",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.onAction(SabdekhoAction.Refresh) },
                        shape = RoundedCornerShape(dims.buttonCornerRadius)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (state.searchQuery.isNotBlank()) "No matches for \"${state.searchQuery}\"" else "No titles in this section",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Use the '+' button to add movies or shows to your library.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Grid of media posters
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSpan),
                verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
                horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
                contentPadding = PaddingValues(bottom = dims.screenBottomPadding + 48.dp)
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
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun MediaCard(item: MediaShowDto, gridSpan: Int) {
    val dims = Dimens.current
    val height = when (gridSpan) {
        2 -> dims.mediaCardHeight2Col
        3 -> dims.mediaCardHeight3Col
        else -> dims.mediaCardHeight4Col
    }

    val imageUrl = if (item.posterPath != null) "https://image.tmdb.org/t/p/w500${item.posterPath}" else ""
    val isMovie = item.type?.equals("movie", ignoreCase = true) == true
    val typeLabel = if (isMovie) "Movie" else "Series"

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
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Gradient overlay for bottom text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
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
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Top Right: Status
        val isWatching = item.status?.equals("WATCHING", ignoreCase = true) == true
        val statusBg = if (isWatching) MaterialTheme.colorScheme.primaryContainer else Color.Black.copy(alpha = 0.65f)
        val statusText = if (isWatching) MaterialTheme.colorScheme.onPrimaryContainer else Color.White

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
