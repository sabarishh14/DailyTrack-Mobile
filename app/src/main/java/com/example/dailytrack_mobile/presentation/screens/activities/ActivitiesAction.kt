package com.example.dailytrack_mobile.presentation.screens.activities

import java.time.Month

sealed class ActivitiesAction {
    data class OnMonthChanged(val month: Month, val year: Int) : ActivitiesAction()
    // TODO: Add actions like OnActivityClicked, OnRefresh, etc., when database is wired up
}
