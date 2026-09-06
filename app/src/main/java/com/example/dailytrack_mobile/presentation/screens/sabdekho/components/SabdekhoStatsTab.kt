package com.example.dailytrack_mobile.presentation.screens.sabdekho.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dailytrack_mobile.data.remote.dto.*
import com.example.dailytrack_mobile.presentation.screens.sabdekho.SabdekhoAction
import com.example.dailytrack_mobile.presentation.screens.sabdekho.SabdekhoState
import com.example.dailytrack_mobile.presentation.util.Dimens
import java.util.Locale

@Composable
fun SabdekhoStatsTab(
    state: SabdekhoState,
    onAction: (SabdekhoAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    val stats = state.stats

    if (state.isStatsLoading && stats == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (stats == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "No stats available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { onAction(SabdekhoAction.LoadStats("all")) }) {
                    Text("Load Stats")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dims.itemSpacingLarge),
        contentPadding = PaddingValues(bottom = dims.screenBottomPadding + 56.dp)
    ) {
        // ── Year Selector Row ──────────────────────────────────────────
        item {
            val availableYears = remember(stats.available_years) {
                listOf("all") + stats.available_years.map { it.toString() }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availableYears.forEach { yr ->
                    val label = if (yr == "all") "All Time" else yr
                    val isSelected = state.selectedStatsYear.equals(yr, ignoreCase = true)
                    FilterChipView(label = label, isSelected = isSelected) {
                        onAction(SabdekhoAction.SelectStatsYear(yr))
                    }
                }
            }
        }

        // ── KPI Summary Cards (2x2 Grid) ───────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
                ) {
                    KpiStatCard(
                        title = "Films Logged",
                        value = "${stats.films_logged}",
                        icon = Icons.Default.Movie,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Likes",
                        value = "${stats.total_likes}",
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
                ) {
                    KpiStatCard(
                        title = "Hours Watched",
                        value = String.format(Locale.US, "%.1f", stats.total_hours),
                        icon = Icons.Default.Schedule,
                        iconTint = Color(0xFF00B0FF),
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Theatre Visits",
                        value = "${stats.theatre_stats?.total_visits ?: 0}",
                        icon = Icons.Default.ConfirmationNumber,
                        iconTint = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Rating Distribution Bar Chart ──────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dims.cardCornerRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(dims.cardInnerPadding)) {
                    Text(
                        text = "RATING DISTRIBUTION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(dims.itemSpacingLarge))

                    val ratingKeys = listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0")
                    val maxVal = remember(stats.rating_distribution) {
                        ratingKeys.maxOfOrNull { stats.rating_distribution[it] ?: 0 }?.coerceAtLeast(1) ?: 1
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        ratingKeys.forEach { key ->
                            val count = stats.rating_distribution[key] ?: 0
                            val fraction = (count.toFloat() / maxVal).coerceIn(0.06f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (count > 0) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenStarColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (count > 0) GoldenStarColor else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Monthly Breakdown Bar Chart ────────────────────────────────
        if (stats.by_month.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dims.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(dims.cardInnerPadding)) {
                        Text(
                            text = "MONTHLY ACTIVITY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(dims.itemSpacingLarge))

                        val monthLabels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                        val maxMonth = remember(stats.by_month) {
                            stats.by_month.maxOrNull()?.coerceAtLeast(1) ?: 1
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            stats.by_month.take(12).forEachIndexed { index, count ->
                                val label = monthLabels.getOrElse(index) { "" }
                                val fraction = (count.toFloat() / maxMonth).coerceIn(0.06f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (count > 0) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .fillMaxHeight(fraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                if (count > 0) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Highest Rated Carousel ────────────────────────────────────
        if (stats.highest_rated.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "HIGHEST RATED FILMS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = dims.itemSpacingMedium)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
                    ) {
                        items(stats.highest_rated) { movie ->
                            val posterUrl = if (movie.poster_path != null) "https://image.tmdb.org/t/p/w342${movie.poster_path}" else ""
                            Card(
                                modifier = Modifier
                                    .width(115.dp)
                                    .clickable {
                                        onAction(
                                            SabdekhoAction.OpenMediaDetails(
                                                MediaShowDto(
                                                    id = movie.movie_id,
                                                    tmdbId = movie.tmdb_id,
                                                    name = movie.name,
                                                    posterPath = movie.poster_path,
                                                    type = "movie",
                                                    status = "WATCHED"
                                                )
                                            )
                                        )
                                    },
                                shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(165.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (posterUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(posterUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = movie.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Rating badge overlay
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = GoldenStarColor,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%.1f", movie.rating),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = movie.name ?: "Unknown",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!movie.release_year.isNullOrBlank()) {
                                            Text(
                                                text = movie.release_year,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Theatre Experience Section ────────────────────────────────
        stats.theatre_stats?.let { theatre ->
            if (theatre.movies.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dims.cardCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(dims.cardInnerPadding)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "THEATRE VISITS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${theatre.total_visits} Visits",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(dims.itemSpacingMedium))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(theatre.movies) { m ->
                                    val pUrl = if (m.poster_path != null) "https://image.tmdb.org/t/p/w200${m.poster_path}" else ""
                                    Box(
                                        modifier = Modifier
                                            .size(width = 60.dp, height = 90.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (pUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(pUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = m.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── The Extremes Section ──────────────────────────────────────
        stats.extremes?.let { extremes ->
            item {
                Column {
                    Text(
                        text = "THE EXTREMES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = dims.itemSpacingMedium)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
                        ) {
                            extremes.longest?.let {
                                ExtremeCard(
                                    label = "Longest Runtime",
                                    title = it.name ?: "Unknown",
                                    subtitle = "${it.runtime ?: 0} mins",
                                    posterPath = it.poster_path,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            extremes.shortest?.let {
                                ExtremeCard(
                                    label = "Shortest Runtime",
                                    title = it.name ?: "Unknown",
                                    subtitle = "${it.runtime ?: 0} mins",
                                    posterPath = it.poster_path,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
                        ) {
                            extremes.oldest?.let {
                                ExtremeCard(
                                    label = "Oldest Release",
                                    title = it.name ?: "Unknown",
                                    subtitle = it.release_year ?: "",
                                    posterPath = it.poster_path,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            extremes.newest?.let {
                                ExtremeCard(
                                    label = "Newest Release",
                                    title = it.name ?: "Unknown",
                                    subtitle = it.release_year ?: "",
                                    posterPath = it.poster_path,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dims.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.cardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(dims.buttonCornerRadius - 2.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ExtremeCard(
    label: String,
    title: String,
    subtitle: String,
    posterPath: String?,
    modifier: Modifier = Modifier
) {
    val dims = Dimens.current
    val pUrl = if (posterPath != null) "https://image.tmdb.org/t/p/w200$posterPath" else ""

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dims.buttonCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 58.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (pUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(pUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
