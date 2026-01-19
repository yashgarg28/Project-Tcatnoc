package com.example.tempcontacts

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
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
                        showDeleteNotification(context, contact)

                        // Save to DeletionStore to trigger in-app popup
                        val deletionStore = DeletionStore.getInstance(context)
                        deletionStore.saveDeletedContact(contact.name, contact.phone)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showDeleteNotification(context: Context, contact: Contact) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val restoreIntent = Intent(context, RestoreContactReceiver::class.java).apply {
            putExtra("contact_id", contact.id)
            putExtra("contact_name", contact.name)
            putExtra("contact_phone", contact.phone)
        }
        val restorePendingIntent = PendingIntent.getBroadcast(context, contact.id, restoreIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, "contact_deletion_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Contact Deleted")
            .setContentText("${contact.name} has been automatically deleted.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Restore", restorePendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(contact.id, notification)
    }
}
