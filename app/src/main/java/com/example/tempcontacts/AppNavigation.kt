package com.example.tempcontacts

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation(
    viewModel: ContactViewModel,
    settingsDataStore: SettingsDataStore,
    startDestination: String
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModelFactory(settingsDataStore))
            OnboardingScreen(onOnboardingComplete = { 
                onboardingViewModel.saveOnboardingSeen()
                navController.navigate("contactList") { popUpTo("onboarding") { inclusive = true } }
            })
        }
        composable("contactList") {
            ContactListScreen(
                viewModel = viewModel, 
                onContactClick = { contactId -> navController.navigate("contactDetail/$contactId") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable(
            "contactDetail/{contactId}",
            arguments = listOf(navArgument("contactId") { defaultValue = 0 })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getInt("contactId") ?: 0
            ContactDetailScreen(
                viewModel = viewModel,
                contactId = contactId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { navController.navigate("editContact/$contactId") }
            )
        }
        composable(
            "editContact/{contactId}",
            arguments = listOf(navArgument("contactId") { defaultValue = 0 })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getInt("contactId") ?: 0
            EditContactScreen(
                viewModel = viewModel,
                contactId = contactId,
                onContactUpdated = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                settingsDataStore = settingsDataStore,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}