package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.ReminderDatabase
import com.example.data.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = ReminderDatabase.getDatabase(context)
            val repo = ReminderRepository(db.installmentDao, db.chequeDao)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val installments = repo.allInstallments.first()
                    val cheques = repo.allCheques.first()
                    
                    val now = System.currentTimeMillis()
                    
                    // Reschedule alarms for all uncompleted installments
                    val dayMillis = 24L * 60 * 60 * 1000
                    installments.filter { !it.isCompleted }.forEach { inst ->
                        NotificationReceiver.scheduleNotificationAlarm(context, "سررسید قسط: ${inst.title}", "۷ روز تا سررسید قسط ${inst.title} باقی مانده.", inst.dueDate - 7 * dayMillis)
                        NotificationReceiver.scheduleNotificationAlarm(context, "سررسید قسط: ${inst.title}", "۳ روز تا سررسید قسط ${inst.title} باقی مانده.", inst.dueDate - 3 * dayMillis)
                        NotificationReceiver.scheduleNotificationAlarm(context, "سررسید قسط: ${inst.title}", "فردا سررسید قسط ${inst.title} است!", inst.dueDate - 1 * dayMillis)
                        NotificationReceiver.scheduleNotificationAlarm(context, "سررسید قسط: ${inst.title}", "امروز موعد پرداخت قسط ${inst.title} فرا رسیده است.", inst.dueDate)
                    }

                    // Reschedule alarms for all uncleared cheques
                    cheques.filter { !it.isCleared }.forEach { ch ->
                        val typeDesc = if (ch.isMyCheque) "پرداختی شما" else "دریافتی شما"
                        NotificationReceiver.scheduleNotificationAlarm(context, "سررسید چک شماره ${ch.chequeNumber}", "۷ روز تا سررسید چک مربوط به $typeDesc", ch.dueDate - 7 * dayMillis)
                        NotificationReceiver.scheduleNotificationAlarm(context, "سررسید چک شماره ${ch.chequeNumber}", "۳ روز تا سررسید چک مربوط به $typeDesc", ch.dueDate - 3 * dayMillis)
                        NotificationReceiver.scheduleNotificationAlarm(context, "سررسید چک شماره ${ch.chequeNumber}", "فردا موعد پاس کردن چک سررسید است.", ch.dueDate - 1 * dayMillis)
                        NotificationReceiver.scheduleNotificationAlarm(context, "سررسید چک شماره ${ch.chequeNumber}", "امروز موعد سررسید نهایی چک فرا رسیده است.", ch.dueDate)
                    }

                    val nextInstallment = installments.filter { !it.isCompleted }.minByOrNull { it.dueDate }
                    val nextCheque = cheques.filter { !it.isCleared }.minByOrNull { it.dueDate }
                    
                    val closestItem: Any? = when {
                        nextInstallment != null && nextCheque != null -> {
                            if (nextInstallment.dueDate < nextCheque.dueDate) nextInstallment else nextCheque
                        }
                        nextInstallment != null -> nextInstallment
                        nextCheque != null -> nextCheque
                        else -> null
                    }
                    
                    if (closestItem != null) {
                        val title: String
                        val desc: String
                        val dueDate: Long
                        
                        if (closestItem is com.example.data.Installment) {
                            title = "نزدیک‌ترین قسط: ${closestItem.title}"
                            dueDate = closestItem.dueDate
                            val countdown = com.example.util.FormatUtils.getDaysCountdown(now, dueDate)
                            val formatter = java.text.DecimalFormat("#,###")
                            val formattedAmt = formatter.format(closestItem.amount)
                            desc = "${countdown.first} • مبلغ: $formattedAmt ریال"
                        } else {
                            val ch = closestItem as com.example.data.Cheque
                            title = "نزدیک‌ترین چک: شماره ${ch.chequeNumber} (${ch.bankName})"
                            dueDate = ch.dueDate
                            val countdown = com.example.util.FormatUtils.getDaysCountdown(now, dueDate)
                            val typeDesc = if (ch.isMyCheque) "صادره" else "وارده"
                            val formatter = java.text.DecimalFormat("#,###")
                            val formattedAmt = formatter.format(ch.amount)
                            desc = "${countdown.first} ($typeDesc) • مبلغ: $formattedAmt ریال"
                        }
                        NotificationReceiver.showOngoingNotification(context, title, desc)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
