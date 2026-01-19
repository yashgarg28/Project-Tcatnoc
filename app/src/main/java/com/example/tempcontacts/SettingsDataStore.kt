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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
    }

    val themeFlow: Flow<String> = appContext.dataStore.data.map {
        it[THEME_KEY] ?: "System"
    }

    suspend fun saveTheme(theme: String) {
        appContext.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    val hasSeenOnboardingFlow: Flow<Boolean> = appContext.dataStore.data.map {
        it[HAS_SEEN_ONBOARDING_KEY] ?: false
    }

    suspend fun saveOnboardingSeen() {
        appContext.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = true
        }
    }
}