package com.example.dailytrack_mobile.presentation.screens.sabdekho

sealed class SabdekhoAction {
    data class LoadShows(val status: String = "WATCHING") : SabdekhoAction()
    data class SearchQueryChanged(val query: String) : SabdekhoAction()
    data class ChangeFilter(val filter: String) : SabdekhoAction()
}
