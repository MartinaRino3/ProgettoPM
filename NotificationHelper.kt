package com.example.progettopm.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.progettopm.R
import java.util.concurrent.TimeUnit

object NotificationHelper {

    private const val DAILY_CHANNEL_ID = "daily_channel"
    private const val INSTANT_CHANNEL_ID = "instant_channel"
    private const val WORK_TAG = "daily_notification_work"

    fun showInstantNotification(context: Context, title: String, message: String) {
        createChannelIfNeeded(context, INSTANT_CHANNEL_ID, "Notifiche istantanee")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, INSTANT_CHANNEL_ID)
            .setSmallIcon(R.drawable.notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun scheduleDailyNotification(context: Context) {
        createChannelIfNeeded(context, DAILY_CHANNEL_ID, "Notifiche giornaliere")

        val delay = TimeUnit.HOURS.toMillis(24)

        val workRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d("NotificationHelper", "Pianificazione notifica aggiornata (REPLACE).")
    }

    fun scheduleTestNotification(context: Context) {
        createChannelIfNeeded(context, DAILY_CHANNEL_ID, "Notifiche giornaliere")

        val workRequest = OneTimeWorkRequestBuilder<DailyNotificationWorker>()
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_TAG + "_test",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d("NotificationHelper", "Notifica di test pianificata tra 15 secondi")
    }

    private fun createChannelIfNeeded(context: Context, channelId: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT)
                manager.createNotificationChannel(channel)
            }
        }
    }
}