package com.example.tempcontacts

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "settings")

val LAST_COUNTRY_CODE = stringPreferencesKey("last_country_code")
