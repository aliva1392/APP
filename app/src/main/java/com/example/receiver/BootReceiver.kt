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
