package com.example.dailytrack_mobile.presentation.screens.sabdekho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.local.datastore.DemoModeManager
import com.example.dailytrack_mobile.data.repository.SabdekhoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
        viewModelScope.launch {
            demoModeManager.isDemoModeEnabledFlow.collect {
                loadShows(_state.value.activeFilter)
            }
        }
        viewModelScope.launch {
            repository.dataUpdateFlow.collect {
                loadShows(_state.value.activeFilter)
            }
        }
    }

    fun onAction(action: SabdekhoAction) {
        when (action) {
            is SabdekhoAction.LoadShows -> loadShows(action.status)
            is SabdekhoAction.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = action.query) }
            }
            is SabdekhoAction.ChangeFilter -> {
                _state.update { it.copy(activeFilter = action.filter) }
                loadShows(action.filter)
            }
        }
    }

    private fun loadShows(status: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.getMediaLibrary(limit = 100, offset = 0, type = "all", status = status)
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            shows = response.shows ?: emptyList()
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load shows"
                        )
                    }
                }
        }
    }
}
