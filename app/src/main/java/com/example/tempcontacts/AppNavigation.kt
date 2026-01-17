package com.example.tempcontacts

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation(viewModel: ContactViewModel, settingsDataStore: SettingsDataStore) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "contact_list") {
        composable("contact_list") {
            ContactListScreen(
                viewModel = viewModel, 
                onContactClick = { contactId ->
                    if (contactId == 0) {
                        navController.navigate("edit_contact/0")
                    } else {
                        navController.navigate("contact_detail/$contactId")
                    }
                },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable(
            route = "contact_detail/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.IntType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getInt("contactId")
            contactId?.let {
                ContactDetailScreen(
                    viewModel = viewModel,
                    contactId = it,
                    onEditClick = { navController.navigate("edit_contact/$it") },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = "edit_contact/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.IntType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getInt("contactId")
            contactId?.let {
                EditContactScreen(
                    viewModel = viewModel,
                    contactId = it,
                    onContactUpdated = { navController.popBackStack() },
                    onBackClick = { navController.popBackStack() }
                )
            }
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
