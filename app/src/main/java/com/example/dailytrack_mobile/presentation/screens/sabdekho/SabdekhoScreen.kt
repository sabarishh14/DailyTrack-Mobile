package com.example.dailytrack_mobile.presentation.screens.sabdekho

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto
import com.example.dailytrack_mobile.data.remote.dto.MediaShowDto
import com.example.dailytrack_mobile.presentation.util.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabdekhoScreen(
    viewModel: SabdekhoVM = hiltViewModel(),
    onNavigateToAddMovie: (MediaSearchResultDto?) -> Unit = {}
) {
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
                        "Search titles or TMDB...",
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
                    if (state.isSearchingOnline) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (state.searchQuery.isNotEmpty()) {
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
                text = if (filteredItems.isNotEmpty() || state.searchQuery.isBlank()) {
                    "${filteredItems.size} TITLES TRACKED"
                } else {
                    "ONLINE DISCOVERY"
                },
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
            // Local search has no matches
            if (state.searchQuery.isNotBlank()) {
                if (state.isSearchingOnline && state.onlineResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Text(
                                text = "Searching TMDB / IMDb for \"${state.searchQuery}\"...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (state.onlineResults.isNotEmpty()) {
                    // Display online TMDB/IMDb search results
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
                        contentPadding = PaddingValues(bottom = dims.screenBottomPadding + 48.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(dims.buttonCornerRadius),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Not in your library",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Found on TMDB/IMDb — Tap any title to add to your library",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        items(state.onlineResults, key = { "${it.id}_${it.mediaType}" }) { result ->
                            OnlineMediaResultCard(
                                item = result,
                                onAddClick = { onNavigateToAddMovie(result) }
                            )
                        }
                    }
                } else {
                    // No results found locally or online
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No matches for \"${state.searchQuery}\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Couldn't find this title in your library or on TMDB/IMDb.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { onNavigateToAddMovie(null) },
                                shape = RoundedCornerShape(dims.buttonCornerRadius)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Custom Title")
                            }
                        }
                    }
                }
            } else {
                // Search query is empty and library has 0 items for current filter
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
                            text = "No titles in this section",
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
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { onNavigateToAddMovie(null) },
                            shape = RoundedCornerShape(dims.buttonCornerRadius)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Title")
                        }
                    }
                }
            }
        } else {
            // Grid of media posters from local library
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
fun OnlineMediaResultCard(
    item: MediaSearchResultDto,
    onAddClick: () -> Unit
) {
    val dims = Dimens.current
    val posterUrl = if (item.posterPath != null) "https://image.tmdb.org/t/p/w342${item.posterPath}" else ""
    val isMovie = item.isMovie
    val typeLabel = if (isMovie) "Movie" else "TV Series"
    val typeIcon = if (isMovie) Icons.Default.Movie else Icons.Default.Tv

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddClick() },
        shape = RoundedCornerShape(dims.buttonCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.cardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail
            Box(
                modifier = Modifier
                    .size(width = 65.dp, height = 95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(dims.itemSpacingLarge))

            // Details Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                // Title
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Badges Row: Type + Year + Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type Badge
                    Surface(
                        color = if (isMovie) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMovie) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.year.isNotEmpty()) {
                        Text(
                            text = item.year,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (item.voteAverage != null && item.voteAverage > 0) {
                        Text(
                            text = "⭐ ${String.format(java.util.Locale.US, "%.1f", item.voteAverage)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!item.overview.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add Button
                FilledTonalButton(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(dims.buttonCornerRadius),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMovie) "Add Movie" else "Add Series",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
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
