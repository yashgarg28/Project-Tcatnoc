package com.example.tempcontacts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrnBookCallerIdService : CallScreeningService() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onScreenCall(callDetails: Call.Details) {

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)

        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val phoneNumber = callDetails.handle.schemeSpecificPart ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val contactDao =
                ContactDatabase.getDatabase(applicationContext).contactDao()
            val allContacts = contactDao.getAllContactsList()

            val matchingContact = allContacts.find { contact ->
                PhoneNumberUtils.compare(
                    applicationContext,
                    contact.phone,
                    phoneNumber
                )
            }

            if (matchingContact != null) {
                showBurnerNotification(matchingContact)

                if (Settings.canDrawOverlays(this@BrnBookCallerIdService)) {
                    launch(Dispatchers.Main) {
                        showOverlay(matchingContact)
                    }
                }
            }
        }
    }

    private fun showOverlay(contact: Contact) {

        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 450
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_caller_id, null)

        overlayView?.let { view ->

            view.findViewById<TextView>(R.id.contactName).text =
                "🔥 Burner: ${contact.name}"

            view.findViewById<ImageButton>(R.id.closeBtn).setOnClickListener {
                removeOverlay()
            }

            makeOverlayDraggable(view)

            windowManager?.addView(view, layoutParams)
        }
    }

    @Suppress("ClickableViewAccessibility")
    private fun makeOverlayDraggable(view: View) {

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.apply {
                        x = initialX + (event.rawX - initialTouchX).toInt()
                        y = initialY + (event.rawY - initialTouchY).toInt()
                    }
                    windowManager?.updateViewLayout(view, layoutParams)
                    true
                }

                else -> false
            }
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    private fun showBurnerNotification(contact: Contact) {

        val channelId = "brnbook_caller_id_channel"
        val notificationManager =
            getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BrnBook Caller ID",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("🔥 Burner: ${contact.name}")
            .setContentText("Incoming call from temporary contact")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(contact.id, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}