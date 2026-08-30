package com.example.dailytrack_mobile.presentation.screens.sabdekho

sealed class SabdekhoAction {
    data class LoadShows(val status: String = "WATCHING", val type: String = "all") : SabdekhoAction()
    data class SearchQueryChanged(val query: String) : SabdekhoAction()
    data class ChangeFilter(val filter: String) : SabdekhoAction()
    data class ChangeMediaType(val mediaType: String) : SabdekhoAction()
    object Refresh : SabdekhoAction()
}
