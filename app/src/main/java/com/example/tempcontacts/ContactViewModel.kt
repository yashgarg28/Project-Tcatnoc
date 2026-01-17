package com.example.tempcontacts

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val contactDao = ContactDatabase.getDatabase(application).contactDao()
    private val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val allContacts: StateFlow<List<Contact>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedContacts: StateFlow<Map<Char, List<Contact>>> = contactDao.getAllContacts()
        .map { contacts ->
            contacts.groupBy { it.name.first().uppercaseChar() }.toSortedMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun insert(contact: Contact) {
        viewModelScope.launch {
            val contactId = contactDao.insert(contact)
            contact.deletionTimestamp?.let {
                scheduleDeletion(contact.copy(id = contactId.toInt()))
            }
        }
    }

    fun update(contact: Contact) {
        viewModelScope.launch {
            contactDao.update(contact)
            cancelDeletion(contact)
            contact.deletionTimestamp?.let {
                scheduleDeletion(contact)
            }
        }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch {
            contactDao.delete(contact)
            cancelDeletion(contact)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            allContacts.value.forEach { cancelDeletion(it) }
            contactDao.deleteAll()
        }
    }

    fun restoreContact(name: String, phone: String) {
        viewModelScope.launch {
            contactDao.insert(Contact(name = name, phone = phone, deletionTimestamp = null))
        }
    }

    private fun scheduleDeletion(contact: Contact) {
        val intent = Intent(getApplication(), DeleteContactReceiver::class.java).apply {
            putExtra("contact_id", contact.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            contact.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        contact.deletionTimestamp?.let {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                it,
                pendingIntent
            )
        }
    }

    private fun cancelDeletion(contact: Contact) {
        val intent = Intent(getApplication(), DeleteContactReceiver::class.java).apply {
            putExtra(
                "contact_id",
                contact.id
            )
        }
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            contact.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
