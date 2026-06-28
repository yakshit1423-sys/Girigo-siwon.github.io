package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class WishNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wishText = intent.getStringExtra("WISH_TEXT") ?: "Your wish"
        val notificationType = intent.getStringExtra("NOTIFICATION_TYPE") ?: "GENERIC"

        val title: String
        val content: String
        val notificationId: Int

        when (notificationType) {
            "FIVE_MINUTES" -> {
                title = "💀 THE FINAL COUNTDOWN 💀"
                content = "Only 5 minutes remain for your pact: \"$wishText\". Watch the timer..."
                notificationId = 1002
            }
            "COMPLETED" -> {
                title = "🔴 TIME HAS EXPIRED 🔴"
                content = "Your wish \"$wishText\" has been fully granted. Girigo has collected the price."
                notificationId = 1003
            }
            else -> {
                title = "🔮 PACT SEALED 🔮"
                content = "Your wish has been granted. The 24-hour countdown has commenced."
                notificationId = 1001
            }
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "girigo_channel")
            .setSmallIcon(android.R.drawable.stat_sys_warning) // fallback
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF8B0000.toInt()) // Deep Dark Red Accent Color

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
