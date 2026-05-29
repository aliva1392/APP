package com.example.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.receiver.NotificationReceiver
import com.example.util.FormatUtils.toPersianDigits
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class BackupData(
    val installments: List<Installment>,
    val cheques: List<Cheque>
)

class ReminderViewModel(
    application: Application,
    private val repository: ReminderRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_settings_rem", Context.MODE_PRIVATE)

    var securityPin: String
        get() = prefs.getString("security_pin", "") ?: ""
        set(value) = prefs.edit().putString("security_pin", value).apply()

    var isPinEnabled: Boolean
        get() = prefs.getBoolean("pin_enabled", false)
        set(value) = prefs.edit().putBoolean("pin_enabled", value).apply()

    val installments: StateFlow<List<Installment>> = repository.allInstallments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cheques: StateFlow<List<Cheque>> = repository.allCheques
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Automatically check and alarm-notify for any near due dates on app launch
        checkForUpcomingPayments()

        // Persistent mandatory notification observer
        viewModelScope.launch {
            combine(installments, cheques) { insts, chs ->
                Pair(insts, chs)
            }.collect { (insts, chs) ->
                updateOngoingReminderNotification(insts, chs)
            }
        }
    }

    // JSON export implementation using standard Moshi reflection adapters
    fun exportDataToJson(): String {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = BackupData(installments.value, cheques.value)
            adapter.toJson(backup)
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Failed to export JSON backup", e)
            ""
        }
    }

    // JSON restore database configuration helper
    fun importDataFromJson(jsonStr: String): Boolean {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = adapter.fromJson(jsonStr) ?: return false
            
            viewModelScope.launch {
                backup.installments.forEach {
                    repository.insertInstallment(it.copy(id = 0))
                }
                backup.cheques.forEach {
                    repository.insertCheque(it.copy(id = 0))
                }
            }
            true
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Failed to import JSON backup", e)
            false
        }
    }

    private fun updateOngoingReminderNotification(instList: List<Installment>, chList: List<Cheque>) {
        try {
            val now = System.currentTimeMillis()
            val nextInstallment = instList.filter { !it.isCompleted }.minByOrNull { it.dueDate }
            val nextCheque = chList.filter { !it.isCleared }.minByOrNull { it.dueDate }

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

                if (closestItem is Installment) {
                    title = "نزدیک‌ترین قسط: ${closestItem.title}"
                    dueDate = closestItem.dueDate
                    val countdown = com.example.util.FormatUtils.getDaysCountdown(now, dueDate)
                    desc = "${countdown.first} • مبلغ: ${formatAmountLong(closestItem.amount)} ریال"
                } else {
                    val ch = closestItem as Cheque
                    title = "نزدیک‌ترین چک: شماره ${ch.chequeNumber} (${ch.bankName})"
                    dueDate = ch.dueDate
                    val countdown = com.example.util.FormatUtils.getDaysCountdown(now, dueDate)
                    val typeDesc = if (ch.isMyCheque) "صادره" else "وارده"
                    desc = "${countdown.first} ($typeDesc) • مبلغ: ${formatAmountLong(ch.amount)} ریال"
                }

                NotificationReceiver.showOngoingNotification(getApplication(), title, desc)
            } else {
                NotificationReceiver.cancelOngoingNotification(getApplication())
            }
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Failed to update ongoing notification", e)
        }
    }

    /**
     * Scan database for all pending installments/cheques due within 2 days, and push notifications.
     */
    fun checkForUpcomingPayments() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val twoDaysInput = 2L * 24 * 60 * 60 * 1000

            // Flow items are collected synchronously in coroutines
            installments.value.forEach { inst ->
                if (!inst.isCompleted) {
                    val diff = inst.dueDate - now
                    if (diff in 0..twoDaysInput) {
                        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                        val desc = if (days == 0) "امروز" else if (days == 1) "فردا" else "$days روز دیگر"
                        triggerNotification(
                            "سررسید قسط نزدیک است!",
                            "قسط ${inst.title} به مبلغ ${formatAmountLong(inst.amount)} ریال $desc سررسید می‌شود."
                        )
                    }
                }
            }

            cheques.value.forEach { ch ->
                if (!ch.isCleared) {
                    val diff = ch.dueDate - now
                    if (diff in 0..twoDaysInput) {
                        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                        val desc = if (days == 0) "امروز" else if (days == 1) "فردا" else "$days روز دیگر"
                        val chequeType = if (ch.isMyCheque) "پرداختی شما" else "دریافتی شما"
                        triggerNotification(
                            "سررسید چک نزدیک است!",
                            "چک شماره ${ch.chequeNumber} (${ch.bankName}) به مبلغ ${formatAmountLong(ch.amount)} ریال مربوط به $chequeType $desc سررسید می‌شود."
                        )
                    }
                }
            }
        }
    }

    /**
     * Manually triggers a mock instant notification for testing.
     */
    fun triggerTestNotification() {
        triggerNotification(
            "تست موفقیت‌آمیز سیستم نوتیفیکیشن",
            "سیستم یادآور اقساط و چک فعال است و در موعد مقرر به شما هشدار خواهد داد."
        )
    }

    private fun triggerNotification(title: String, message: String) {
        try {
            NotificationReceiver.showNotification(getApplication(), title, message)
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Failed to trigger notification", e)
        }
    }

    // Installments actions
    fun addInstallment(
        title: String,
        amount: Long,
        dueDate: Long,
        totalInstallments: Int,
        category: String,
        notes: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val inst = Installment(
                title = title,
                amount = amount,
                dueDate = dueDate,
                totalInstallments = totalInstallments,
                paidInstallments = 0,
                category = category,
                notes = notes,
                isCompleted = false,
                imageUri = imageUri
            )
            repository.insertInstallment(inst)
            
            // Multi-level alarming scheduled notifications: 7 days, 3 days, 1 day, and on due date
            val dayMillis = 24L * 60 * 60 * 1000
            scheduleNotificationAlarm("سررسید قسط: ${inst.title}", "۷ روز تا سررسید قسط ${inst.title} باقی مانده.", inst.dueDate - 7 * dayMillis)
            scheduleNotificationAlarm("سررسید قسط: ${inst.title}", "۳ روز تا سررسید قسط ${inst.title} باقی مانده.", inst.dueDate - 3 * dayMillis)
            scheduleNotificationAlarm("سررسید قسط: ${inst.title}", "فردا سررسید قسط ${inst.title} است!", inst.dueDate - 1 * dayMillis)
            scheduleNotificationAlarm("سررسید قسط: ${inst.title}", "امروز موعد پرداخت قسط ${inst.title} فرا رسیده است.", inst.dueDate)
        }
    }

    fun payCurrentMonth(installment: Installment) {
        viewModelScope.launch {
            val newPaid = installment.paidInstallments + 1
            val completed = newPaid >= installment.totalInstallments
            
            // Increment due date by 30 days
            val cal = java.util.GregorianCalendar().apply {
                timeInMillis = installment.dueDate
                add(Calendar.DAY_OF_MONTH, 30)
            }
            
            val updated = installment.copy(
                paidInstallments = newPaid,
                isCompleted = completed,
                dueDate = if (completed) installment.dueDate else cal.timeInMillis
            )
            repository.insertInstallment(updated)
        }
    }

    fun deleteInstallment(installment: Installment) {
        viewModelScope.launch {
            repository.deleteInstallment(installment)
        }
    }

    // Cheques actions
    fun addCheque(
        chequeNumber: String,
        bankName: String,
        amount: Long,
        dueDate: Long,
        payeeName: String,
        isMyCheque: Boolean,
        notes: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val ch = Cheque(
                chequeNumber = chequeNumber,
                bankName = bankName,
                amount = amount,
                dueDate = dueDate,
                payeeName = payeeName,
                isMyCheque = isMyCheque,
                isCleared = false,
                notes = notes,
                isBounced = false,
                imageUri = imageUri
            )
            repository.insertCheque(ch)

            // Multi-level alarming scheduled notifications: 7 days, 3 days, 1 day, and on due date
            val dayMillis = 24L * 60 * 60 * 1000
            val typeDesc = if (isMyCheque) "پرداختی شما" else "وارده دریافتی شما"
            scheduleNotificationAlarm("سررسید چک شماره ${ch.chequeNumber}", "۷ روز تا سررسید چک مربوط به $typeDesc", ch.dueDate - 7 * dayMillis)
            scheduleNotificationAlarm("سررسید چک شماره ${ch.chequeNumber}", "۳ روز تا سررسید چک مربوط به $typeDesc", ch.dueDate - 3 * dayMillis)
            scheduleNotificationAlarm("سررسید چک شماره ${ch.chequeNumber}", "فردا موعد पास کردن چک سررسید است.", ch.dueDate - 1 * dayMillis)
            scheduleNotificationAlarm("سررسید چک شماره ${ch.chequeNumber}", "امروز موعد سررسید نهایی چک فرا رسیده است.", ch.dueDate)
        }
    }

    fun toggleChequeCleared(cheque: Cheque) {
        viewModelScope.launch {
            val updated = cheque.copy(isCleared = !cheque.isCleared)
            repository.insertCheque(updated)
        }
    }

    fun toggleChequeBounced(cheque: Cheque) {
        viewModelScope.launch {
            val updated = cheque.copy(isBounced = !cheque.isBounced)
            repository.insertCheque(updated)
        }
    }

    fun deleteCheque(cheque: Cheque) {
        viewModelScope.launch {
            repository.deleteCheque(cheque)
        }
    }

    // Excel-compatible CSV export using UTF-8 BOM so Excel displays Persian correctly!
    fun exportDataToCsv(): String {
        return try {
            val s = StringBuilder()
            // Add UTF-8 BOM so Persian/Arabic letters are rendered correctly by Excel
            s.append('\ufeff')
            // CSV Headers
            s.append("نوع تعهد,عنوان,مبلغ (ریال),تاریخ سررسید,وضعیت,اطلاعات تکمیلی,پیوست\n")
            
            // Installments
            installments.value.forEach { inst ->
                val statusStr = if (inst.isCompleted) "تسویه شده" else "جاری (${inst.paidInstallments} از ${inst.totalInstallments})"
                val dateStr = com.example.util.FormatUtils.getJalaliDateString(inst.dueDate)
                s.append("قسط,${inst.title},${inst.amount},$dateStr,$statusStr,${inst.notes.replace(",", "-")},${inst.imageUri ?: "بدون فایل"}\n")
            }
            
            // Cheques
            cheques.value.forEach { ch ->
                val typeDesc = if (ch.isMyCheque) "چک صادره" else "چک وارده"
                val statusStr = when {
                    ch.isCleared -> "پاس شده"
                    ch.isBounced -> "برگشتی"
                    else -> "در انتظار"
                }
                val dateStr = com.example.util.FormatUtils.getJalaliDateString(ch.dueDate)
                s.append("$typeDesc,شماره ${ch.chequeNumber} (${ch.bankName}),${ch.amount},$dateStr,$statusStr,گیرنده/صادرکننده: ${ch.payeeName.replace(",", "-")} • ${ch.notes.replace(",", "-")},${ch.imageUri ?: "بدون تصویر"}\n")
            }
            
            s.toString()
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Failed to export CSV", e)
            ""
        }
    }

    // Dynamic clean structured TEXT Report perfect for PDF / Easy Print
    fun exportDataToTextReport(): String {
        return try {
            val s = StringBuilder()
            s.append("=========================\n")
            s.append("    گزارش تراز سررسید مالی - هوشمند\n")
            s.append("    تاریخ گزارش: ${com.example.util.FormatUtils.getCurrentJalaliDateWithDayOfWeek()}\n")
            s.append("=========================\n\n")
            
            s.append("📌 بخش اول: اقساط ماهیانه\n")
            s.append("-------------------------\n")
            if (installments.value.isEmpty()) {
                s.append("هیچ قسطی ثبت نشده است.\n")
            } else {
                installments.value.forEachIndexed { i, inst ->
                    val statusStr = if (inst.isCompleted) "تسویه شده" else "جاری (قسط شماره ${inst.paidInstallments + 1} از ${inst.totalInstallments})"
                    val dateStr = com.example.util.FormatUtils.getJalaliDateString(inst.dueDate)
                    s.append("${i+1}. عنوان: ${inst.title}\n")
                    s.append("    دسته‌بندی: ${inst.category}\n")
                    s.append("    تاریخ سررسید بعدی: $dateStr\n")
                    s.append("    مبلغ ماهیانه: ${formatAmountLong(inst.amount)} ریال\n")
                    s.append("    وضعیت قسط: $statusStr\n")
                    if (inst.notes.isNotEmpty()) s.append("    یادداشت: ${inst.notes}\n")
                    s.append("\n")
                }
            }
            
            s.append("📌 بخش دوم: چک‌های بانکی صیادی\n")
            s.append("-------------------------\n")
            if (cheques.value.isEmpty()) {
                s.append("هیچ چکی ثبت نشده است.\n")
            } else {
                cheques.value.forEachIndexed { i, ch ->
                    val typeDesc = if (ch.isMyCheque) "چک صادر شده (طلبکاران)" else "چک دریافت شده (بدهکاران)"
                    val statusStr = when {
                        ch.isCleared -> "پاس شده"
                        ch.isBounced -> "برگشتی و عودت داده شده"
                        else -> "در انتظار وصول"
                    }
                    val dateStr = com.example.util.FormatUtils.getJalaliDateString(ch.dueDate)
                    s.append("${i+1}. شماره چک صیاد: ${ch.chequeNumber}\n")
                    s.append("    نام بانک عامل: ${ch.bankName}\n")
                    s.append("    مبلغ چک: ${formatAmountLong(ch.amount)} ریال\n")
                    s.append("    سررسید: $dateStr\n")
                    s.append("    جهت / بابت: ${ch.payeeName}\n")
                    s.append("    نوع چک: $typeDesc\n")
                    s.append("    وضعیت چک: $statusStr\n")
                    if (ch.notes.isNotEmpty()) s.append("    یادداشت کاربر: ${ch.notes}\n")
                    s.append("\n")
                }
            }
            
            s.append("-------------------------\n")
            val pendingInst = installments.value.filter { !it.isCompleted }.sumOf { it.amount }
            val pendingCh = cheques.value.filter { !it.isCleared }.sumOf { it.amount }
            s.append("📊 خلاصه بدهی‌های تعهداتی در جریان:\n")
            s.append("مجموع اقساط باقی‌مانده: ${formatAmountLong(pendingInst)} ریال\n")
            s.append("مجموع چک‌های وصول‌تعلیق: ${formatAmountLong(pendingCh)} ریال\n")
            s.append("کل تعهدات در جریان: ${formatAmountLong(pendingInst + pendingCh)} ریال\n")
            s.append("=========================\n")
            
            s.toString()
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Failed to generate text report", e)
            ""
        }
    }

    private fun scheduleNotificationAlarm(title: String, message: String, triggerAtMillis: Long) {
        if (triggerAtMillis < System.currentTimeMillis()) return
        
        try {
            val context = getApplication<Application>()
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
            Log.e("ReminderViewModel", "SecurityException scheduling exact alarm", e)
            try {
                val context = getApplication<Application>()
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
                Log.e("ReminderViewModel", "Failed secondary alarm schedule fallback", ex)
            }
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Failed to schedule alarm", e)
        }
    }

    private fun formatAmountLong(value: Long): String {
        val formatter = java.text.DecimalFormat("#,###")
        return formatter.format(value).toPersianDigits()
    }

    // Factory Class
    class Factory(
        private val application: Application,
        private val repository: ReminderRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReminderViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
