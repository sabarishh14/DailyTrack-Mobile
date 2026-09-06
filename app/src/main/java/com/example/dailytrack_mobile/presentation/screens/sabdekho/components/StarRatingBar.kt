package com.example.dailytrack_mobile.presentation.screens.sabdekho.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GoldenStarColor = Color(0xFFFFB800)

@Composable
fun StarRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 32.dp,
    isInteractive: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..5) {
            val starIndex = i
            val fillType = when {
                rating >= starIndex -> StarFill.FULL
                rating >= starIndex - 0.5f -> StarFill.HALF
                else -> StarFill.EMPTY
            }

            Box(
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (isInteractive) {
                            Modifier.pointerInput(rating, starIndex) {
                                detectTapGestures { offset ->
                                    val isLeftHalf = offset.x < size.width / 2
                                    val newRating = if (isLeftHalf) starIndex - 0.5f else starIndex.toFloat()
                                    // If tapping same value, toggle off to 0
                                    if (rating == newRating) {
                                        onRatingChanged(0f)
                                    } else {
                                        onRatingChanged(newRating)
                                    }
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (fillType) {
                    StarFill.FULL -> {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Star $starIndex",
                            tint = GoldenStarColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    StarFill.HALF -> {
                        // Background outline + filled half
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.StarHalf,
                                contentDescription = "Star $starIndex half",
                                tint = GoldenStarColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    StarFill.EMPTY -> {
                        Icon(
                            imageVector = Icons.Outlined.StarOutline,
                            contentDescription = "Star $starIndex empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (isInteractive && rating > 0f) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format(java.util.Locale.US, "%.1f", rating),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GoldenStarColor
            )
        }
    }
}

@Composable
fun StarDisplay(
    rating: Float?,
    modifier: Modifier = Modifier,
    starSize: Dp = 14.dp,
    showNumeric: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    if (rating == null || rating <= 0f) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..5) {
            val starIndex = i
            when {
                rating >= starIndex -> {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = GoldenStarColor,
                        modifier = Modifier.size(starSize)
                    )
                }
                rating >= starIndex - 0.5f -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.StarHalf,
                        contentDescription = null,
                        tint = GoldenStarColor,
                        modifier = Modifier.size(starSize)
                    )
                }
                else -> {
                    // Don't render empty stars in compact display to keep it tight and clean
                }
            }
        }

        if (showNumeric) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = String.format(java.util.Locale.US, "%.1f", rating),
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = GoldenStarColor
            )
        }
    }
}

private enum class StarFill {
    FULL,
    HALF,
    EMPTY
}
