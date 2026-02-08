package com.example.progettopm.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.progettopm.R

class DailyNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("DailyNotificationWorker", "Esecuzione Worker: notifica in background")

        showNotification(
            "Buongiorno!",
            "È il momento perfetto per dare un’occhiata alla tua app 👗✨"
        )

        // Ripianifica la prossima notifica giornaliera
        NotificationHelper.scheduleDailyNotification(applicationContext)

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "daily_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Notifiche giornaliere",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.notifications)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
