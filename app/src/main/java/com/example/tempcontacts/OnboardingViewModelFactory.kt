package com.example.tempcontacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class OnboardingViewModelFactory(private val settingsDataStore: SettingsDataStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}