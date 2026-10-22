package com.monayem.crono

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.monayem.crono.data.ClassTestEntry
import com.monayem.crono.data.Subject

class CTNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Class Test Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming and pending Class Tests"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCTUpcomingNotification(subject: Subject, ct: ClassTestEntry) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SHOW_ALL_CTS", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upcoming Class Test")
            .setContentText("${subject.name} CT Today at ${ct.startTime}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ct.id, notification)
    }

    fun showCTConfirmationNotification(subject: Subject, ct: ClassTestEntry) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Class Test Finished?")
            .setContentText("Please confirm if the CT for ${subject.name} was taken.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        notificationManager.notify(ct.id + 10000, notification)
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    companion object {
        private const val CHANNEL_ID = "ct_notifications"
    }
}
