package com.example.util

import java.util.Calendar
import java.util.Date

object FormatUtils {

    /**
     * Converts a millisecond timestamp to a Shamsi/Jalali date string, e.g. "۱۴۰۵/۰۳/۰۹"
     */
    fun getJalaliDateString(timestamp: Long): String {
        val calendar = java.util.GregorianCalendar()
        calendar.timeInMillis = timestamp
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)

        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        val monthStr = if (jm < 10) "۰$jm" else "$jm"
        val dayStr = if (jd < 10) "۰$jd" else "$jd"

        return "$jy/$monthStr/$dayStr".toPersianDigits()
    }

    /**
     * Get dynamic Persian Date string with day of week (e.g., "جمعه، ۹ خرداد ۱۴۰۵")
     */
    fun getCurrentJalaliDateWithDayOfWeek(): String {
        val calendar = java.util.GregorianCalendar()
        val gDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayNames = arrayOf("", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه")
        val dayName = dayNames.getOrElse(gDayOfWeek) { "" }
        
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        val monthNames = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        val monthName = monthNames.getOrElse((jm - 1).coerceIn(0, 11)) { "خرداد" }
        
        return "$dayName، ${formatInteger(jd)} $monthName ${formatInteger(jy)}"
    }

    /**
     * Formats long numbers with commas and converts them to Persian digits, e.g. 1500000 -> ۱,۵۰۰,۰۰۰
     */
    fun formatAmount(amount: Long): String {
        val formatter = java.text.DecimalFormat("#,###")
        val formatted = formatter.format(amount)
        return formatted.toPersianDigits()
    }

    /**
     * Formats integer numbers to Persian digits, e.g. 12 -> ۱۲
     */
    fun formatInteger(value: Int): String {
        return value.toString().toPersianDigits()
    }

    /**
     * Returns a countdown description with custom styling metrics
     */
    fun getDaysCountdown(currentMillis: Long, targetMillis: Long): Triple<String, Int, Boolean> {
        val diff = targetMillis - currentMillis
        // Calculate difference in whole days
        val daysDiff = (diff / (1000 * 60 * 60 * 24)).toInt()

        return when {
            daysDiff == 0 -> Triple("امروز سررسید است", 0, true)
            daysDiff == 1 -> Triple("فردا سررسید است", 1, true)
            daysDiff > 1 -> Triple("$daysDiff روز مانده", daysDiff, true)
            daysDiff == -1 -> Triple("۱ روز گذشته (تأخیر)", -1, false)
            else -> Triple("${Math.abs(daysDiff)} روز گذشته (تأخیر)", daysDiff, false)
        }
    }

    /**
     * Extension to convert standard English digits in string to Persian digits
     */
    fun String.toPersianDigits(): String {
        var result = this
        val english = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val farsi = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
        for (i in 0..9) {
            result = result.replace(english[i], farsi[i])
        }
        return result
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 1 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 2 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        var jd = 0
        for (i in 0..11) {
            val days = if (i < 6) 31 else if (i < 11) 30 else 29
            val actualDays = if (i == 11 && isJalaliLeap(jy)) 30 else days
            if (jDayNo < actualDays) {
                jm = i + 1
                jd = jDayNo + 1
                break
            }
            jDayNo -= actualDays
        }
        return Triple(jy, jm, jd)
    }

    fun isJalaliLeap(jy: Int): Boolean {
        val r = (jy - 979) % 33
        return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }

    /**
     * Returns the maximum days in a given Jalali month of a given Jalali year.
     */
    fun getJalaliMaxDays(jy: Int, jm: Int): Int {
        if (jm in 1..6) return 31
        if (jm in 7..11) return 30
        if (jm == 12) {
            return if (isJalaliLeap(jy)) 30 else 29
        }
        return 30
    }

    /**
     * Extracts Shamsi year, month, and day as a Triple(jy, jm, jd) from a timestamp.
     */
    fun getJalaliDateParts(timestamp: Long): Triple<Int, Int, Int> {
        val calendar = java.util.GregorianCalendar()
        calendar.timeInMillis = timestamp
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gy, gm, gd)
    }

    /**
     * Converts a Jalali date back to Gregorian components Triple(gy, gm, gd).
     */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jyRaw = jy - 979
        val jmRaw = jm - 1
        val jdRaw = jd - 1

        var jDayNo = jyRaw * 365 + (jyRaw / 33) * 8 + (jyRaw % 33 + 3) / 4
        for (i in 0 until jmRaw) {
            jDayNo += if (i < 6) 31 else 30
        }
        jDayNo += jdRaw

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = 1
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = 0
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = 0
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (leap == 1 && (gy % 4 == 0 && gy % 100 != 0 || gy % 400 == 0)) {
            gDaysInMonth[2] = 29
        }

        var gm = 1
        var gd = 1
        for (m in 1..12) {
            val days = gDaysInMonth[m]
            if (gDayNo < days) {
                gm = m
                gd = gDayNo + 1
                break
            }
            gDayNo -= days
        }
        return Triple(gy, gm, gd)
    }

    /**
     * Converts Jalali Year, Month, Day to a millisecond UTC timestamp.
     */
    fun jalaliToTimestamp(jy: Int, jm: Int, jd: Int): Long {
        val (gy, gm, gd) = jalaliToGregorian(jy, jm, jd)
        val calendar = java.util.GregorianCalendar()
        calendar.set(Calendar.YEAR, gy)
        calendar.set(Calendar.MONTH, gm - 1)
        calendar.set(Calendar.DAY_OF_MONTH, gd)
        calendar.set(Calendar.HOUR_OF_DAY, 12) // set midday to avoid timezone edge issues
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
