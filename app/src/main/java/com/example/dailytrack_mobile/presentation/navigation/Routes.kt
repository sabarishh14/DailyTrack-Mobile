package com.example.dailytrack_mobile.presentation.navigation

sealed class Routes(val route: String) {
    object Auth : Routes("auth")
    object Home : Routes("home")
}
