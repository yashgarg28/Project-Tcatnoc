package com.example.tempcontacts

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val contactId = intent.getIntExtra("contact_id", -1)
        if (contactId == -1) return

        val actionType = intent.action ?: return
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contactDao = ContactDatabase
                    .getDatabase(appContext)
                    .contactDao()

                val contact = contactDao.getContactById(contactId)
                if (contact == null) return@launch

                when (actionType) {

                    "REMOVE_TIMER" -> {
                        val updatedContact = contact.copy(deletionTimestamp = null)
                        contactDao.update(updatedContact)
                    }

                    "EXTEND_TIMER" -> {
                        val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L

                        val newExpiry =
                            (contact.deletionTimestamp ?: System.currentTimeMillis()) +
                                    sevenDaysMillis

                        val updatedContact = contact.copy(deletionTimestamp = newExpiry)
                        contactDao.update(updatedContact)
                    }
                }

                // 👇 Cancel warning notification
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                notificationManager.cancel(contactId + 5000)

            } finally {
                pendingResult.finish()
            }
        }
    }
}