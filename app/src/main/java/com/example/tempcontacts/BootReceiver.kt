package com.example.tempcontacts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val pendingResult = goAsync()
            val contactDao = ContactDatabase.getDatabase(context).contactDao()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val contacts = contactDao.getAllContacts().first()
                    contacts.forEach { contact ->
                        contact.deletionTimestamp?.let {
                            val deleteIntent = Intent(context, DeleteContactReceiver::class.java).apply {
                                putExtra("contact_id", contact.id)
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                contact.id,
                                deleteIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                it,
                                pendingIntent
                            )
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
