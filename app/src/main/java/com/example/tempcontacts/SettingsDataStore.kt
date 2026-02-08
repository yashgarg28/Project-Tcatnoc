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

// ✅ Single DataStore instance for the whole app

class SettingsDataStore(context: Context) {

    // ✅ Use applicationContext to avoid memory leaks
    private val appContext = context.applicationContext

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val HAS_SEEN_ONBOARDING_KEY =
            booleanPreferencesKey("has_seen_onboarding")
        private val HAS_COMPLETED_FIRST_SETUP_KEY =
            booleanPreferencesKey("has_completed_first_setup")

        private val LAST_COUNTRY_CODE_KEY =
            stringPreferencesKey("last_country_code")
    }

    // 🌗 Theme
    val themeFlow: Flow<String> =
        appContext.dataStore.data.map { prefs ->
            prefs[THEME_KEY] ?: "System"
        }

    suspend fun saveTheme(theme: String) {
        appContext.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme
        }
    }

    // 🚀 Onboarding
    val hasSeenOnboardingFlow: Flow<Boolean> =
        appContext.dataStore.data.map { prefs ->
            prefs[HAS_SEEN_ONBOARDING_KEY] ?: false
        }

    suspend fun saveOnboardingSeen() {
        appContext.dataStore.edit { prefs ->
            prefs[HAS_SEEN_ONBOARDING_KEY] = true
        }
    }

    // 🛂 First setup (Caller ID / Overlay)
    val hasCompletedFirstSetupFlow: Flow<Boolean> =
        appContext.dataStore.data.map { prefs ->
            prefs[HAS_COMPLETED_FIRST_SETUP_KEY] ?: false
        }

    suspend fun saveFirstSetupCompleted() {
        appContext.dataStore.edit { prefs ->
            prefs[HAS_COMPLETED_FIRST_SETUP_KEY] = true
        }
    }

    // 🌍 Last selected country
    val lastCountryCodeFlow: Flow<String?> =
        appContext.dataStore.data.map { prefs ->
            prefs[LAST_COUNTRY_CODE_KEY]
        }

    suspend fun saveLastCountryCode(code: String) {
        appContext.dataStore.edit { prefs ->
            prefs[LAST_COUNTRY_CODE_KEY] = code
        }
    }
}