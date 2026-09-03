package com.example.dailytrack_mobile.presentation.screens.home

import java.time.Month

sealed class HomeAction {
    data class Refresh(val forceRefresh: Boolean = false) : HomeAction()
    data class DateSelected(val month: Month, val year: Int) : HomeAction()
}
