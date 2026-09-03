package com.example.dailytrack_mobile.presentation.screens.sabdekho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto
import com.example.dailytrack_mobile.data.repository.SabdekhoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SabdekhoVM @Inject constructor(
    private val repository: SabdekhoRepository,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    private val _state = MutableStateFlow(SabdekhoState())
    val state: StateFlow<SabdekhoState> = _state.asStateFlow()

    private var onlineSearchJob: Job? = null

    init {
        viewModelScope.launch {
            demoModeManager.isDemoModeEnabledFlow.collect {
                loadShows(status = _state.value.activeFilter, type = _state.value.mediaTypeFilter)
            }
        }
        viewModelScope.launch {
            repository.dataUpdateFlow.collect {
                loadShows(status = _state.value.activeFilter, type = _state.value.mediaTypeFilter)
            }
        }
    }

    fun onAction(action: SabdekhoAction) {
        when (action) {
            is SabdekhoAction.LoadShows -> {
                loadShows(action.status, action.type)
            }
            is SabdekhoAction.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = action.query) }
                performOnlineSearch(action.query)
            }
            is SabdekhoAction.ChangeFilter -> {
                _state.update { it.copy(activeFilter = action.filter) }
                loadShows(action.filter, _state.value.mediaTypeFilter)
            }
            is SabdekhoAction.ChangeMediaType -> {
                _state.update { it.copy(mediaTypeFilter = action.mediaType) }
                loadShows(_state.value.activeFilter, action.mediaType)
            }
            is SabdekhoAction.Refresh -> {
                loadShows(_state.value.activeFilter, _state.value.mediaTypeFilter, forceRefresh = true)
            }
        }
    }

    private fun performOnlineSearch(query: String) {
        onlineSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _state.update { it.copy(isSearchingOnline = false, onlineResults = emptyList()) }
            return
        }

        onlineSearchJob = viewModelScope.launch {
            delay(350) // Debounce
            _state.update { it.copy(isSearchingOnline = true) }
            val result = repository.searchMedia(trimmed)
            result.onSuccess { list ->
                _state.update { it.copy(isSearchingOnline = false, onlineResults = list) }
            }.onFailure {
                _state.update { it.copy(isSearchingOnline = false, onlineResults = emptyList()) }
            }
        }
    }

    private fun loadShows(status: String, type: String = "all", forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                repository.clearCache()
                _state.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _state.update { it.copy(isLoading = true, error = null) }
            }
            repository.getMediaLibrary(limit = 100, offset = 0, type = type, status = status, forceRefresh = forceRefresh)
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            shows = response.shows ?: emptyList(),
                            totalCount = response.totalCount ?: (response.shows?.size ?: 0)
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.message ?: "Failed to load shows"
                        )
                    }
                }
        }
    }
}
