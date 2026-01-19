package com.example.tempcontacts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrnBookCallerIdService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // Immediately respond to the system that we are not blocking the call.
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)

        // Asynchronously check for a contact match.
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        // 1. Number Cleaning: Extract the raw number.
        val phoneNumber = callDetails.handle.schemeSpecificPart ?: return

        // 2. Database Lookup: Use a coroutine for background processing.
        CoroutineScope(Dispatchers.IO).launch {
            val contactDao = ContactDatabase.getDatabase(applicationContext).contactDao()
            val allContacts = contactDao.getAllContactsList()

            // Use the robust PhoneNumberUtils.compare for matching.
            val matchingContact = allContacts.find { contact ->
                PhoneNumberUtils.compare(applicationContext, contact.phone, phoneNumber)
            }

            // 3. Dynamic Notification: Show only if a match is found.
            if (matchingContact != null) {
                Log.d("BrnBookCallerIdService", "Match found: ${matchingContact.name}. Showing notification.")
                showBurnerNotification(matchingContact)
            } else {
                Log.d("BrnBookCallerIdService", "No match found for $phoneNumber.")
            }
        }
    }

    private fun showBurnerNotification(contact: Contact) {
        val channelId = "brnbook_caller_id_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BrnBook Caller ID",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays an overlay for incoming calls from temporary contacts."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // An intent to open the app when the notification is tapped.
        val fullScreenIntent = Intent(this, MainActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            // Dynamic Notification Title
            .setContentTitle("🔥 Burner: ${contact.name}")
            // Dynamic Notification Text
            .setContentText("Recognized temporary contact")
            // 4. Priority: Set to ensure it shows as a heads-up notification.
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            // This is the key to making it slide down over the call screen.
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        NotificationManagerCompat.from(this).notify(contact.id, notification)
    }
}
