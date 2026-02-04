package com.example.tempcontacts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// This creates the single DataStore instance for the app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    // Use applicationContext to prevent memory leaks
    private val appContext = context.applicationContext

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")

        // Tracks if the user has handled the Caller ID/Overlay permission popup
        private val HAS_COMPLETED_FIRST_SETUP_KEY = booleanPreferencesKey("has_completed_first_setup")
    }

    // --- Theme Logic ---
    val themeFlow: Flow<String> = appContext.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System"
    }

    suspend fun saveTheme(theme: String) {
        appContext.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    // --- Onboarding Logic ---
    val hasSeenOnboardingFlow: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    suspend fun saveOnboardingSeen() {
        appContext.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = true
        }
    }

    // --- Caller ID Setup Logic (Consolidated) ---
    // This connects to the popup in EditContactScreen
    val hasCompletedFirstSetupFlow: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[HAS_COMPLETED_FIRST_SETUP_KEY] ?: false
    }

    suspend fun saveFirstSetupCompleted() {
        appContext.dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_FIRST_SETUP_KEY] = true
        }
    }
}