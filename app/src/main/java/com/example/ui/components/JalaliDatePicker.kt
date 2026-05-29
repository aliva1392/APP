package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.FormatUtils

@Composable
fun JalaliDatePicker(
    initialTimestamp: Long,
    onDateSelected: (Long) -> Unit
) {
    var selectedParts by remember { mutableStateOf(FormatUtils.getJalaliDateParts(initialTimestamp)) }
    val (jy, jm, jd) = selectedParts

    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val maxDays = FormatUtils.getJalaliMaxDays(jy, jm)
    LaunchedEffect(jy, jm) {
        if (jd > maxDays) {
            selectedParts = Triple(jy, jm, maxDays)
            onDateSelected(FormatUtils.jalaliToTimestamp(jy, jm, maxDays))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Date Header with Next/Previous Month Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val nextMonth = jm + 1
                    if (nextMonth > 12) {
                        selectedParts = Triple(jy + 1, 1, jd)
                    } else {
                        selectedParts = Triple(jy, nextMonth, jd)
                    }
                    onDateSelected(FormatUtils.jalaliToTimestamp(selectedParts.first, selectedParts.second, selectedParts.third))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "ماه بعد",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            val currentMonthName = monthNames[jm - 1]
            Text(
                text = "$currentMonthName ${FormatUtils.formatInteger(jy)}",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            IconButton(
                onClick = {
                    val prevMonth = jm - 1
                    if (prevMonth < 1) {
                        selectedParts = Triple(jy - 1, 12, jd)
                    } else {
                        selectedParts = Triple(jy, prevMonth, jd)
                    }
                    onDateSelected(FormatUtils.jalaliToTimestamp(selectedParts.first, selectedParts.second, selectedParts.third))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "ماه قبل",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of Month selection Grid
        val chunkedDays = (1..maxDays).toList().chunked(7)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            chunkedDays.forEach { rowDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowDays.forEach { dayNumber ->
                        val isSelected = (dayNumber == jd)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                  )
                                .clickable {
                                    selectedParts = Triple(jy, jm, dayNumber)
                                    onDateSelected(FormatUtils.jalaliToTimestamp(jy, jm, dayNumber))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = FormatUtils.formatInteger(dayNumber),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (rowDays.size < 7) {
                        repeat(7 - rowDays.size) {
                            Spacer(modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live stats block for chosen date
        val selectedStr = FormatUtils.getJalaliDateString(FormatUtils.jalaliToTimestamp(jy, jm, jd))
        val daysRem = FormatUtils.getDaysCountdown(System.currentTimeMillis(), FormatUtils.jalaliToTimestamp(jy, jm, jd)).first
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سررسید: $selectedStr",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = daysRem,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
