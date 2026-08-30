package com.example.dailytrack_mobile.presentation.screens.sabdekho

import com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto
import com.example.dailytrack_mobile.data.remote.dto.MediaShowDto

data class SabdekhoState(
    val isLoading: Boolean = false,
    val shows: List<MediaShowDto> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val activeFilter: String = "WATCHING",
    val mediaTypeFilter: String = "all",
    val totalCount: Int = 0,
    val isSearchingOnline: Boolean = false,
    val onlineResults: List<MediaSearchResultDto> = emptyList()
)