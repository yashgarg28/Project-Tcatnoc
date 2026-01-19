package com.example.tempcontacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class OnboardingViewModel(private val settingsDataStore: SettingsDataStore) : ViewModel() {

    fun saveOnboardingSeen() {
        viewModelScope.launch {
            settingsDataStore.saveOnboardingSeen()
        }
    }
}
