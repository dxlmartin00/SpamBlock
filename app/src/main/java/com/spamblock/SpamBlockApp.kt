package com.spamblock

import android.app.Application
import com.spamblock.util.NotificationHelper

class SpamBlockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
