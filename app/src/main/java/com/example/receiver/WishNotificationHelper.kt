package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

object WishNotificationHelper {
    private const val CHANNEL_ID = "girigo_channel"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Girigo Pact"
            val descriptionText = "Notifications for Girigo wishes and their creepy countdown timers."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showInstantNotification(context: Context, wishText: String) {
        initChannel(context)

        val title = "🔮 PACT SEALED 🔮"
        val content = "Your wish \"$wishText\" has been accepted. The 24-hour countdown has commenced."
        val notificationId = 1001

        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF8B0000.toInt())

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun scheduleNotification(context: Context, wishText: String, triggerTimeMs: Long, notificationType: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WishNotificationReceiver::class.java).apply {
            putExtra("WISH_TEXT", wishText)
            putExtra("NOTIFICATION_TYPE", notificationType)
        }

        val requestCode = if (notificationType == "FIVE_MINUTES") 2002 else 2003
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d("GirigoNotification", "Scheduled $notificationType at $triggerTimeMs")
        } catch (e: SecurityException) {
            // Fallback for Android 12+ if exact alarm permission is missing
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
            Log.w("GirigoNotification", "SecurityException scheduling exact alarm, used inexact fallback", e)
        }
    }

    fun cancelAllScheduled(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WishNotificationReceiver::class.java)

        val pendingIntent1 = PendingIntent.getBroadcast(
            context,
            2002,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent1 != null) {
            alarmManager.cancel(pendingIntent1)
        }

        val pendingIntent2 = PendingIntent.getBroadcast(
            context,
            2003,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent2 != null) {
            alarmManager.cancel(pendingIntent2)
        }
    }
}
