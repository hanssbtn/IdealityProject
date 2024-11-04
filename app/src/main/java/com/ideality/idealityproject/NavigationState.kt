package com.ideality.idealityproject

import androidx.navigation.NavController

class NavigationState(val navController: NavController) {
    fun navigate(dest: String) {
        navController.navigate(dest) {
            launchSingleTop = true
        }
    }

    fun popUp() {
        navController.popBackStack()
    }

    fun navigateAndPopUp(to: String, from: String) {
        navController.navigate(to) {
            launchSingleTop = true
            popUpTo(from) { inclusive = true }
        }
    }
}