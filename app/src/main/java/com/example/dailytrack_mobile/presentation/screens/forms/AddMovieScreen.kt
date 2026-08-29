package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailytrack_mobile.presentation.util.Dimens

import androidx.hilt.navigation.compose.hiltViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Add Movie / Show Form
// ─────────────────────────────────────────────────────────────────────────────

private enum class ContentType(val label: String) {
    MOVIE("Movie"),
    SERIES("Series"),
    ANIME("Anime"),
    DOCUMENTARY("Documentary")
}

private enum class WatchStatus(val label: String, val emoji: String, val dbStatus: String) {
    WATCHED("Watched", "✅", "WATCHED"),
    IN_PROGRESS("In Progress", "▶️", "WATCHING"),
    PLAN_TO_WATCH("Plan to Watch", "📋", "TO WATCH"),
    DROPPED("Dropped", "❌", "DROPPED")
}

private enum class WatchPlatform(val label: String, val emoji: String) {
    NETFLIX("Netflix", "🔴"),
    PRIME("Prime Video", "🔵"),
    HOTSTAR("Hotstar", "💚"),
    YOUTUBE("YouTube", "🎬"),
    THEATRE("Theatre", "🎥"),
    OTHER("Other", "📺")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMovieScreen(
    formsVM: FormsVM = hiltViewModel(),
    onDirtyStateChanged: (Boolean) -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var selectedContentType by remember { mutableStateOf(ContentType.MOVIE) }
    var selectedStatus by remember { mutableStateOf<WatchStatus?>(null) }
    var selectedPlatform by remember { mutableStateOf<WatchPlatform?>(null) }
    var rating by remember { mutableIntStateOf(0) }
    var review by remember { mutableStateOf("") }
    val dims = Dimens.current

    val isDirty = remember(title, selectedContentType, selectedStatus, selectedPlatform, rating, review) {
        title.isNotBlank() ||
                selectedStatus != null ||
                selectedPlatform != null ||
                rating > 0 ||
                review.isNotBlank() ||
                selectedContentType != ContentType.MOVIE
    }

    LaunchedEffect(isDirty) {
        onDirtyStateChanged(isDirty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.screenHorizontalPadding, vertical = dims.itemSpacingLarge),
        verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing)
    ) {
        // ── Content Type ─────────────────────────────────────────────
        SectionLabel("Type")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ContentType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selectedContentType == type,
                    onClick = { selectedContentType = type },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ContentType.entries.size
                    )
                ) {
                    Text(type.label, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ── Title ────────────────────────────────────────────────────
        SectionLabel("Title")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("e.g. Interstellar", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        // ── Watch Status ─────────────────────────────────────────────
        SectionLabel("Status")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            WatchStatus.entries.forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { selectedStatus = status },
                    label = { Text("${status.emoji} ${status.label}", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                )
            }
        }

        // ── Platform ─────────────────────────────────────────────────
        SectionLabel("Where did you watch?")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium),
            verticalArrangement = Arrangement.spacedBy(dims.itemSpacingMedium)
        ) {
            WatchPlatform.entries.forEach { platform ->
                FilterChip(
                    selected = selectedPlatform == platform,
                    onClick = { selectedPlatform = platform },
                    label = { Text("${platform.emoji} ${platform.label}", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(dims.buttonCornerRadius - 2.dp)
                )
            }
        }

        // ── Rating (stars) ───────────────────────────────────────────
        SectionLabel("Rating")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { star ->
                IconButton(onClick = { rating = if (rating == star) 0 else star }) {
                    Icon(
                        imageVector = if (star <= rating) Icons.Outlined.Star
                        else Icons.Outlined.StarOutline,
                        contentDescription = "$star star",
                        tint = if (star <= rating) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(dims.iconSizeLarge)
                    )
                }
            }
        }

        // ── Review Notes ─────────────────────────────────────────────
        SectionLabel("Quick Review (optional)")
        OutlinedTextField(
            value = review,
            onValueChange = { review = it },
            placeholder = { Text("What did you think?", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null, modifier = Modifier.size(dims.iconSizeMedium))
            },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Save Button ──────────────────────────────────────────────
        Button(
            onClick = {
                formsVM.saveMediaShow(
                    title = title,
                    type = selectedContentType.label,
                    status = selectedStatus?.dbStatus ?: "WATCHING",
                    platform = selectedPlatform?.label,
                    rating = rating,
                    review = review.takeIf { it.isNotBlank() },
                    onSuccess = onSaveSuccess
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.searchBarHeight),
            shape = RoundedCornerShape(dims.buttonCornerRadius),
            enabled = title.isNotBlank() && selectedStatus != null
        ) {
            Text(
                "Save ${selectedContentType.label}",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(dims.itemSpacingMedium))
    }
}
