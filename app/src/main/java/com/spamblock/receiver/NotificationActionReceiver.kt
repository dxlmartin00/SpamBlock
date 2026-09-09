package com.spamblock.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.spamblock.data.PreferencesManager

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WHITELIST = "com.spamblock.action.WHITELIST_NUMBER"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WHITELIST) {
            val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

            val prefs = PreferencesManager.getInstance(context)
            prefs.addWhitelist(phoneNumber)

            if (notificationId != -1) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(notificationId)
            }

            Toast.makeText(context, "$phoneNumber added to Allowed list", Toast.LENGTH_SHORT).show()
        }
    }
}
