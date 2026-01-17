package com.example.tempcontacts

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RestoreContactReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val contactId = intent.getIntExtra("contact_id", -1)
        val contactName = intent.getStringExtra("contact_name")
        val contactPhone = intent.getStringExtra("contact_phone")

        if (contactId != -1 && contactName != null && contactPhone != null) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val contactDao = ContactDatabase.getDatabase(context).contactDao()
                    contactDao.insert(Contact(id = contactId, name = contactName, phone = contactPhone, deletionTimestamp = null))

                    // Dismiss the original deletion notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(contactId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}