package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Installment
import com.example.data.Cheque

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

        fun scheduleNotificationAlarm(context: Context, title: String, message: String, triggerAtMillis: Long) {
            if (triggerAtMillis < System.currentTimeMillis()) return
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("title", title)
                    putExtra("message", message)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    triggerAtMillis.toInt(), // unique request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
                if (canScheduleExact) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("NotificationReceiver", "SecurityException scheduling exact alarm", e)
                try {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        putExtra("title", title)
                        putExtra("message", message)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        triggerAtMillis.toInt(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } catch (ex: Exception) {
                    Log.e("NotificationReceiver", "Failed secondary alarm schedule fallback", ex)
                }
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Failed to schedule alarm", e)
            }
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

        fun scheduleInstallmentAlarms(context: Context, inst: Installment) {
            cancelInstallmentAlarms(context, inst) // clear existing first
            if (inst.isCompleted) return

            val dayMillis = 24L * 60 * 60 * 1000
            val baseCode = 2000000 + inst.id * 10

            scheduleNotificationAlarmDetailed(context, baseCode + 0, "سررسید قسط: ${inst.title}", "۷ روز تا سررسید قسط ${inst.title} باقی مانده.", inst.dueDate - 7 * dayMillis)
            scheduleNotificationAlarmDetailed(context, baseCode + 1, "سررسید قسط: ${inst.title}", "۳ روز تا سررسید قسط ${inst.title} باقی مانده.", inst.dueDate - 3 * dayMillis)
            scheduleNotificationAlarmDetailed(context, baseCode + 2, "سررسید قسط: ${inst.title}", "فردا سررسید قسط ${inst.title} است!", inst.dueDate - 1 * dayMillis)
            scheduleNotificationAlarmDetailed(context, baseCode + 3, "سررسید قسط: ${inst.title}", "امروز موعد پرداخت قسط ${inst.title} فرا رسیده است.", inst.dueDate)
        }

        fun cancelInstallmentAlarms(context: Context, inst: Installment) {
            val baseCode = 2000000 + inst.id * 10
            for (i in 0..3) {
                cancelNotificationAlarm(context, baseCode + i)
            }
        }

        fun scheduleChequeAlarms(context: Context, ch: Cheque) {
            cancelChequeAlarms(context, ch) // clear existing first
            if (ch.isCleared) return

            val dayMillis = 24L * 60 * 60 * 1000
            val baseCode = 5000000 + ch.id * 10
            val typeDesc = if (ch.isMyCheque) "پرداختی شما" else "دریافتی شما"

            scheduleNotificationAlarmDetailed(context, baseCode + 0, "سررسید چک شماره ${ch.chequeNumber}", "۷ روز تا سررسید چک مربوط به $typeDesc", ch.dueDate - 7 * dayMillis)
            scheduleNotificationAlarmDetailed(context, baseCode + 1, "سررسید چک شماره ${ch.chequeNumber}", "۳ روز تا سررسید چک مربوط به $typeDesc", ch.dueDate - 3 * dayMillis)
            scheduleNotificationAlarmDetailed(context, baseCode + 2, "سررسید چک شماره ${ch.chequeNumber}", "فردا موعد پاس کردن چک سررسید است.", ch.dueDate - 1 * dayMillis)
            scheduleNotificationAlarmDetailed(context, baseCode + 3, "سررسید چک شماره ${ch.chequeNumber}", "امروز موعد سررسید نهایی چک فرا رسیده است.", ch.dueDate)
        }

        fun cancelChequeAlarms(context: Context, ch: Cheque) {
            val baseCode = 5000000 + ch.id * 10
            for (i in 0..3) {
                cancelNotificationAlarm(context, baseCode + i)
            }
        }

        fun cancelNotificationAlarm(context: Context, requestCode: Int) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Failed to cancel alarm with code $requestCode", e)
            }
        }

        fun scheduleNotificationAlarmDetailed(context: Context, requestCode: Int, title: String, message: String, triggerAtMillis: Long) {
            if (triggerAtMillis < System.currentTimeMillis()) return
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("title", title)
                    putExtra("message", message)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
                if (canScheduleExact) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("NotificationReceiver", "SecurityException scheduling exact alarm", e)
                try {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                        putExtra("title", title)
                        putExtra("message", message)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } catch (ex: Exception) {
                    Log.e("NotificationReceiver", "Failed secondary alarm schedule fallback", ex)
                }
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Failed to schedule alarm", e)
            }
        }
    }
}
