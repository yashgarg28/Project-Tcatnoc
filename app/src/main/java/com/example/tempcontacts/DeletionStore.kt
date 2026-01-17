package com.example.tempcontacts

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeletionStore private constructor(context: Context) {

    private val _deletedContactFlow = MutableStateFlow<Pair<String, String>?>(null)
    val deletedContactFlow = _deletedContactFlow.asStateFlow()

    fun saveDeletedContact(name: String, phone: String) {
        _deletedContactFlow.value = Pair(name, phone)
    }

    fun clearDeletedContact(name: String, phone: String) {
        if (_deletedContactFlow.value?.first == name && _deletedContactFlow.value?.second == phone) {
            _deletedContactFlow.value = null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DeletionStore? = null

        fun getInstance(context: Context): DeletionStore {
            return INSTANCE ?: synchronized(this) {
                val instance = DeletionStore(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}