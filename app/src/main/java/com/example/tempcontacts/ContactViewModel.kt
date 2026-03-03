package com.example.tempcontacts

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
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

    val allExistingTags: StateFlow<List<String>> = contactDao.getAllContacts()
        .map { contactList ->
            contactList.map { it.tag }
                .filter { it != "None" && it.isNotBlank() }
                .distinct()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupedContacts: StateFlow<Map<Char, List<Contact>>> = contactDao.getAllContacts()
        .map { contacts ->
            contacts.groupBy {
                it.name.firstOrNull()?.uppercaseChar() ?: '?'
            }.toSortedMap()
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

    // --- NEW LOGIC START ---

    private fun scheduleDeletion(contact: Contact) {
        val expiry = contact.deletionTimestamp ?: return
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val sevenDaysMillis = 7 * oneDayMillis

        // 1. Schedule Final Deletion
        setAlarm(contact, expiry, "ACTION_DELETE", 0)

        // 2. Schedule 24-Hour Warning (if time permits)
        if (expiry - currentTime > oneDayMillis) {
            setAlarm(contact, expiry - oneDayMillis, "ACTION_WARN_24H", 100000)
        }

        // 3. Schedule 7-Day Warning (if time permits)
        if (expiry - currentTime > sevenDaysMillis) {
            setAlarm(contact, expiry - sevenDaysMillis, "ACTION_WARN_7D", 200000)
        }
    }

    private fun setAlarm(contact: Contact, triggerAt: Long, action: String, offset: Int) {
        val intent = Intent(getApplication(), DeleteContactReceiver::class.java).apply {
            this.action = action
            putExtra("contact_id", contact.id)
            putExtra("notification_action", action)
        }
        // unique code = contact.id + offset ensures no collisions
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            contact.id + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    private fun cancelDeletion(contact: Contact) {
        // Cancel all three potential alarms
        val actions = listOf(0 to "ACTION_DELETE", 100000 to "ACTION_WARN_24H", 200000 to "ACTION_WARN_7D")

        actions.forEach { (offset, action) ->
            val intent = Intent(getApplication(), DeleteContactReceiver::class.java).apply {
                putExtra("contact_id", contact.id)
                putExtra("notification_action", action)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                contact.id + offset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}