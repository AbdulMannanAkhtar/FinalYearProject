package com.example.mymotor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ExpiryAlarmReciever: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val message = intent?.getStringExtra("message") ?: return

        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            notificationManager.createNotificationChannel(NotificationChannel("expiry", "Expiry reminders",
                NotificationManager.IMPORTANCE_HIGH))
        }

        val notification = NotificationCompat.Builder(context, "expiry").setContentTitle("Upcoming Reminder")
            .setContentText(message).setAutoCancel(true).build()

        notificationManager.notify(message.hashCode(), notification)
    }


}