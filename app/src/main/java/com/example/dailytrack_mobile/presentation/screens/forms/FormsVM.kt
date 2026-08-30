package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.remote.dto.MediaSearchResultDto
import com.example.dailytrack_mobile.data.repository.ActivitiesRepository
import com.example.dailytrack_mobile.data.repository.InvestmentsRepository
import com.example.dailytrack_mobile.data.repository.MoneyRepository
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

data class AddMoneyFormState(
    val isLoadingData: Boolean = false,
    val isSaving: Boolean = false,
    val accounts: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val errorMessage: String? = null
)

data class AddActivityFormState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class AddAssetFormState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class AddMovieFormState(
    val isSearching: Boolean = false,
    val searchResults: List<MediaSearchResultDto> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FormsVM @Inject constructor(
    private val moneyRepository: MoneyRepository,
    private val activitiesRepository: ActivitiesRepository,
    private val investmentsRepository: InvestmentsRepository,
    private val sabdekhoRepository: SabdekhoRepository
) : ViewModel() {

    private val _addMoneyState = MutableStateFlow(AddMoneyFormState())
    val addMoneyState: StateFlow<AddMoneyFormState> = _addMoneyState.asStateFlow()

    private val _addActivityState = MutableStateFlow(AddActivityFormState())
    val addActivityState: StateFlow<AddActivityFormState> = _addActivityState.asStateFlow()

    private val _addAssetState = MutableStateFlow(AddAssetFormState())
    val addAssetState: StateFlow<AddAssetFormState> = _addAssetState.asStateFlow()

    private val _addMovieState = MutableStateFlow(AddMovieFormState())
    val addMovieState: StateFlow<AddMovieFormState> = _addMovieState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadMoneyFormData()
    }

    fun loadMoneyFormData() {
        viewModelScope.launch {
            _addMoneyState.update { it.copy(isLoadingData = true) }
            val accountsRes = moneyRepository.getAccounts()
            val categoriesRes = moneyRepository.getCategories()

            _addMoneyState.update { state ->
                state.copy(
                    isLoadingData = false,
                    accounts = accountsRes.getOrNull()?.map { it.account } ?: state.accounts,
                    categories = categoriesRes.getOrNull() ?: state.categories
                )
            }
        }
    }

    fun clearAddMoneyError() {
        _addMoneyState.update { it.copy(errorMessage = null) }
    }

    fun clearAddActivityError() {
        _addActivityState.update { it.copy(errorMessage = null) }
    }

    fun clearAddAssetError() {
        _addAssetState.update { it.copy(errorMessage = null) }
    }

    fun clearAddMovieError() {
        _addMovieState.update { it.copy(errorMessage = null) }
    }

    fun searchMedia(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _addMovieState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce
            _addMovieState.update { it.copy(isSearching = true) }
            val result = sabdekhoRepository.searchMedia(trimmed)
            result.onSuccess { list ->
                _addMovieState.update { it.copy(isSearching = false, searchResults = list) }
            }.onFailure { error ->
                _addMovieState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = emptyList()
                    )
                }
            }
        }
    }

    fun clearSearchResults() {
        searchJob?.cancel()
        _addMovieState.update { it.copy(isSearching = false, searchResults = emptyList()) }
    }

    fun saveTransaction(
        type: String,
        category: String,
        amount: Double,
        note: String?,
        accountName: String,
        date: String,
        excludeAnalytics: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _addMoneyState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = moneyRepository.addTransaction(
                type = type,
                category = category,
                amount = amount,
                note = note,
                accountName = accountName,
                date = date,
                excludeAnalytics = excludeAnalytics
            )

            result.onSuccess {
                _addMoneyState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure { error ->
                _addMoneyState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to add transaction"
                    )
                }
            }
        }
    }

    fun saveActivity(
        date: String,
        gym: Boolean,
        badminton: Boolean,
        tableTennis: Boolean,
        cricket: Boolean,
        others: Boolean,
        description: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _addActivityState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = activitiesRepository.addPhysicalActivity(
                date = date,
                gym = gym,
                badminton = badminton,
                tableTennis = tableTennis,
                cricket = cricket,
                others = others,
                description = description
            )

            result.onSuccess {
                _addActivityState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure { error ->
                _addActivityState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to log activity"
                    )
                }
            }
        }
    }

    fun saveInvestment(
        name: String,
        category: String,
        amount: Double,
        frequency: String,
        note: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            investmentsRepository.addInvestment(
                name = name,
                category = category,
                amount = amount,
                frequency = frequency,
                note = note
            )
            onSuccess()
        }
    }

    fun saveManualAsset(
        category: String,
        name: String,
        investedValue: Double,
        currentValue: Double,
        interestRate: Double? = null,
        startDate: String? = null,
        maturityDate: String? = null,
        isRecurring: Boolean = false,
        amountToAdd: Double? = null,
        intervalValue: Int? = null,
        intervalUnit: String? = null,
        nextRunDate: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _addAssetState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = investmentsRepository.addManualAsset(
                category = category,
                name = name,
                investedValue = investedValue,
                currentValue = currentValue,
                interestRate = interestRate,
                startDate = startDate,
                maturityDate = maturityDate,
                isRecurring = isRecurring,
                amountToAdd = amountToAdd,
                intervalValue = intervalValue,
                intervalUnit = intervalUnit,
                nextRunDate = nextRunDate
            )
            result.onSuccess {
                _addAssetState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure { error ->
                _addAssetState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to save asset"
                    )
                }
            }
        }
    }

    fun saveAsset(
        name: String,
        assetClass: String,
        purchasePrice: Double,
        currentValue: Double,
        note: String?,
        onSuccess: () -> Unit
    ) {
        saveManualAsset(
            category = assetClass,
            name = name,
            investedValue = purchasePrice,
            currentValue = currentValue,
            onSuccess = onSuccess
        )
    }

    fun saveMediaShow(
        tmdbId: Int?,
        title: String,
        type: String,
        status: String,
        posterPath: String?,
        releaseYear: Int?,
        platform: String?,
        rating: Float?,
        review: String?,
        date: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _addMovieState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = sabdekhoRepository.addMediaShow(
                tmdbId = tmdbId,
                title = title,
                type = type,
                posterPath = posterPath,
                status = status,
                releaseYear = releaseYear,
                platform = platform,
                rating = rating,
                review = review,
                date = date
            )

            result.onSuccess {
                _addMovieState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure { error ->
                _addMovieState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to add movie/show"
                    )
                }
            }
        }
    }
}
