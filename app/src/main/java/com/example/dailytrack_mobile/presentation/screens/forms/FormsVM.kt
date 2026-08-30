package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.repository.ActivitiesRepository
import com.example.dailytrack_mobile.data.repository.InvestmentsRepository
import com.example.dailytrack_mobile.data.repository.MoneyRepository
import com.example.dailytrack_mobile.data.repository.SabdekhoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun saveAsset(
        name: String,
        assetClass: String,
        purchasePrice: Double,
        currentValue: Double,
        note: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            investmentsRepository.addAsset(
                name = name,
                assetClass = assetClass,
                purchasePrice = purchasePrice,
                currentValue = currentValue,
                note = note
            )
            onSuccess()
        }
    }

    fun saveMediaShow(
        title: String,
        type: String,
        status: String,
        platform: String?,
        rating: Int,
        review: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            sabdekhoRepository.addMediaShow(
                title = title,
                type = type,
                status = status,
                platform = platform,
                rating = rating,
                review = review
            )
            onSuccess()
        }
    }
}
