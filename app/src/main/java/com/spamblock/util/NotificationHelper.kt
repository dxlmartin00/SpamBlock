package com.spamblock.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.spamblock.MainActivity
import com.spamblock.receiver.NotificationActionReceiver

object NotificationHelper {

    const val CHANNEL_ID = "spamblock_notifications"
    private const val CHANNEL_NAME = "Blocked Calls"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Silent alerts for blocked incoming calls"
            setShowBadge(false)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun isDialableNumber(number: String): Boolean {
        if (number.isBlank()) return false
        val digits = number.replace(Regex("[^0-9]"), "")
        return digits.length >= 3 &&
                !number.equals("private", ignoreCase = true) &&
                !number.equals("unknown", ignoreCase = true) &&
                !number.equals("restricted", ignoreCase = true) &&
                !number.equals("anonymous", ignoreCase = true)
    }

    fun notifyBlockedCall(
        context: Context,
        displayLabel: String,
        rawNumber: String,
        reason: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        // Content intent: Opens MainActivity when tapping notification body
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayTitle = "Call Blocked"
        val displayText = if (displayLabel.isNotBlank()) {
            "Blocked $displayLabel ($reason)"
        } else {
            "Blocked private/hidden call"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.spamblock.R.drawable.ic_launcher_foreground)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Action buttons (Whitelist & Call Back) only for dialable numbers
        if (isDialableNumber(rawNumber)) {
            // Action 1: Whitelist (handled silently in background via BroadcastReceiver)
            val whitelistIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_WHITELIST
                putExtra(NotificationActionReceiver.EXTRA_PHONE_NUMBER, rawNumber)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val whitelistPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                whitelistIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 2: Call Back (opens system phone dialer with number pre-filled)
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${Uri.encode(rawNumber)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val dialPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 2,
                dialIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                0,
                "Whitelist",
                whitelistPendingIntent
            )
            builder.addAction(
                0,
                "Call Back",
                dialPendingIntent
            )
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    fun notifyBlockedCall(context: Context, phoneNumber: String, reason: String) {
        notifyBlockedCall(context, phoneNumber, phoneNumber, reason)
    }
}
