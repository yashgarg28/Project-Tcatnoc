package com.example.tempcontacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeleteContactReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val contactId = intent.getIntExtra("contact_id", -1)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (contactId != -1) {
                    val contactDao = ContactDatabase.getDatabase(context).contactDao()
                    val contact = contactDao.getContactById(contactId)
                    if (contact != null) {
                        contactDao.delete(contact)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
