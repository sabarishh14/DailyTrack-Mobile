package com.example.dailytrack_mobile.presentation.screens.forms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailytrack_mobile.data.repository.ActivitiesRepository
import com.example.dailytrack_mobile.data.repository.InvestmentsRepository
import com.example.dailytrack_mobile.data.repository.MoneyRepository
import com.example.dailytrack_mobile.data.repository.SabdekhoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormsVM @Inject constructor(
    private val moneyRepository: MoneyRepository,
    private val activitiesRepository: ActivitiesRepository,
    private val investmentsRepository: InvestmentsRepository,
    private val sabdekhoRepository: SabdekhoRepository
) : ViewModel() {

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
            moneyRepository.addTransaction(
                type = type,
                category = category,
                amount = amount,
                note = note,
                accountName = accountName,
                date = date,
                excludeAnalytics = excludeAnalytics
            )
            onSuccess()
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
            activitiesRepository.addPhysicalActivity(
                date = date,
                gym = gym,
                badminton = badminton,
                tableTennis = tableTennis,
                cricket = cricket,
                others = others,
                description = description
            )
            onSuccess()
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
