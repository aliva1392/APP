package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RESTORE_ONGOING) {
            val sharedPreferences = context.getSharedPreferences("ongoing_notif_prefs", Context.MODE_PRIVATE)
            val title = sharedPreferences.getString("last_title", null)
            val message = sharedPreferences.getString("last_desc", null)
            if (title != null && message != null) {
                showOngoingNotification(context, title, message)
            }
            return
        }

        val title = intent.getStringExtra("title") ?: "یادآور قسط و چک"
        val message = intent.getStringExtra("message") ?: "شما یک سررسید مالی پیش رو دارید."
        showNotification(context, title, message)
    }

    companion object {
        const val ACTION_RESTORE_ONGOING = "com.example.action.RESTORE_ONGOING"

        private const val CHANNEL_ID = "payment_reminder_channel"
        private const val CHANNEL_NAME = "یادآور سررسید پرداخت"

        private const val ONGOING_CHANNEL_ID = "ongoing_payment_reminder_channel"
        private const val ONGOING_CHANNEL_NAME = "سررسید فعال"
        private const val ONGOING_NOTIF_ID = 1001

        fun showOngoingNotification(context: Context, title: String, message: String) {
            val sharedPreferences = context.getSharedPreferences("ongoing_notif_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putString("last_title", title)
                .putString("last_desc", message)
                .apply()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    ONGOING_CHANNEL_ID,
                    ONGOING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "نمایش دائم نزدیک‌ترین قسط یا چک"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                ONGOING_NOTIF_ID,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val deleteIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_RESTORE_ONGOING
            }
            val deletePendingIntent = PendingIntent.getBroadcast(
                context,
                1002,
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)

            notificationManager.notify(ONGOING_NOTIF_ID, builder.build())
        }

        fun cancelOngoingNotification(context: Context) {
            val sharedPreferences = context.getSharedPreferences("ongoing_notif_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(ONGOING_NOTIF_ID)
        }

        fun showNotification(context: Context, title: String, message: String) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اطلاع‌رسانی اقساط و چک‌های بانکی"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Dynamic large icon or clean style
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Safe standard system fallback
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
