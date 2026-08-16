package com.example.dailytrack_mobile.presentation.screens.home

import java.time.Month

sealed class HomeAction {
    object Refresh : HomeAction()
    data class DateSelected(val month: Month, val year: Int) : HomeAction()
}
