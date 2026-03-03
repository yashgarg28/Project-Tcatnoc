package com.example.tempcontacts

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeleteContactReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "contact_deletion_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        val contactId = intent.getIntExtra("contact_id", -1)
        val notificationAction = intent.action ?: "ACTION_DELETE"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (contactId == -1) return@launch

                val database = ContactDatabase.getDatabase(appContext)
                val contactDao = database.contactDao()

                // make sure your DAO function is suspend
                val contact = contactDao.getContactById(contactId)

                if (contact != null) {
                    when (notificationAction) {

                        "ACTION_DELETE" -> {
                            contactDao.delete(contact)
                            showDeleteNotification(appContext, contact)

                            DeletionStore
                                .getInstance(appContext)
                                .saveDeletedContact(contact.name, contact.phone)
                        }

                        "ACTION_WARN_24H" -> {
                            showWarningNotification(
                                appContext,
                                contact,
                                "24 hours"
                            )
                        }

                        "ACTION_WARN_7D" -> {
                            showWarningNotification(
                                appContext,
                                contact,
                                "7 days"
                            )
                        }
                    }
                }

            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showWarningNotification(
        context: Context,
        contact: Contact,
        timeLeft: String
    ) {

        val notificationManager =
            ContextCompat.getSystemService(
                context,
                NotificationManager::class.java
            ) ?: return

        // ---- REMOVE TIMER ----
        val removeIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = "REMOVE_TIMER"
            putExtra("contact_id", contact.id)
        }

        val removePendingIntent = PendingIntent.getBroadcast(
            context,
            contact.id + 100,
            removeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ---- EXTEND TIMER ----
        val extendIntent = Intent(context, TimerActionReceiver::class.java).apply {
            action = "EXTEND_TIMER"
            putExtra("contact_id", contact.id)
        }

        val extendPendingIntent = PendingIntent.getBroadcast(
            context,
            contact.id + 200,
            extendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Expiring Soon: ${contact.name}")
            .setContentText("This contact will be deleted in $timeLeft.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Keep Forever", removePendingIntent)
            .addAction(0, "Add 1 Week", extendPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(contact.id + 5000, notification)

    }

    private fun showDeleteNotification(
        context: Context,
        contact: Contact
    ) {

        val notificationManager =
            ContextCompat.getSystemService(
                context,
                NotificationManager::class.java
            ) ?: return

        val restoreIntent = Intent(context, RestoreContactReceiver::class.java).apply {
            putExtra("contact_id", contact.id)
            putExtra("contact_name", contact.name)
            putExtra("contact_phone", contact.phone)
        }

        val restorePendingIntent = PendingIntent.getBroadcast(
            context,
            contact.id,
            restoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Contact Deleted")
            .setContentText("${contact.name} has been automatically removed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Undo / Restore", restorePendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(contact.id, notification)
    }
}