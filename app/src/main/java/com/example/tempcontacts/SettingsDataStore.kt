package com.example.tempcontacts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")

    val themeFlow = context.dataStore.data.map {
        it[themeKey] ?: "System"
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit {
            it[themeKey] = theme
        }
    }
}
