package com.aliva.reminder.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliva.reminder.data.Cheque
import com.aliva.reminder.data.Installment
import com.aliva.reminder.util.FormatUtils
import com.aliva.reminder.util.FormatUtils.toPersianDigits

@Composable
fun DashboardScreen(
    installments: List<Installment>,
    cheques: List<Cheque>,
    onNavigateToInstallments: () -> Unit,
    onNavigateToCheques: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val scrollState = rememberScrollState()

    var jalaliDateText by remember { mutableStateOf(FormatUtils.getCurrentJalaliDateWithDayOfWeek()) }
    LaunchedEffect(Unit) {
        while (true) {
            jalaliDateText = FormatUtils.getCurrentJalaliDateWithDayOfWeek()
            kotlinx.coroutines.delay(60000) // refresh every 60 seconds
        }
    }

    // Financial calculations
    val pendingInstallments = installments.filter { !it.isCompleted }
    val totalPendingInstAmount = pendingInstallments.sumOf { it.amount }
    
    val pendingCheques = cheques.filter { !it.isCleared }
    val totalPendingChequesAmount = pendingCheques.sumOf { it.amount }

    // Grouping for the canvas pie chart
    val categoryDuesMap = mutableMapOf<String, Long>()
    pendingInstallments.forEach {
        categoryDuesMap[it.category] = (categoryDuesMap[it.category] ?: 0L) + it.amount
    }
    pendingCheques.forEach {
        val label = if (it.isMyCheque) "چک صادره" else "چک وارده"
        categoryDuesMap[label] = (categoryDuesMap[label] ?: 0L) + it.amount
    }

    val totalDues = categoryDuesMap.values.sum()

    // Modern color scheme mapping for financial categories
    val categoryColors = listOf(
        Color(0xFF3F51B5), // Indigo
        Color(0xFF4CAF50), // Green
        Color(0xFF00BCD4), // Teal
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0)  // Purple
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp) // allow scroll past FAB
    ) {
        // Dynamic Jalali calendar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "پیشخوان مالی",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = jalaliDateText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        Toast.makeText(context, "سیستم آماده کار است", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📊", fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        // Hero nearest due payment card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val nextInstallment = pendingInstallments.minByOrNull { it.dueDate }
            val nextCheque = pendingCheques.minByOrNull { it.dueDate }

            val closestItem: Any? = when {
                nextInstallment != null && nextCheque != null -> {
                    if (nextInstallment.dueDate < nextCheque.dueDate) nextInstallment else nextCheque
                }
                nextInstallment != null -> nextInstallment
                nextCheque != null -> nextCheque
                else -> null
            }

            if (closestItem != null) {
                val itemTitle: String
                val itemType: String
                val itemAmount: Long
                val itemDueDate: Long
                val isMyCheque: Boolean

                if (closestItem is Installment) {
                    itemTitle = closestItem.title
                    itemType = "قسط: ${closestItem.category}"
                    itemAmount = closestItem.amount
                    itemDueDate = closestItem.dueDate
                    isMyCheque = false
                } else {
                    val ch = closestItem as Cheque
                    itemTitle = "چک شماره ${ch.chequeNumber} (${ch.bankName})"
                    itemType = if (ch.isMyCheque) "چک صادره" else "چک وارده"
                    itemAmount = ch.amount
                    itemDueDate = ch.dueDate
                    isMyCheque = ch.isMyCheque
                }

                val countdown = FormatUtils.getDaysCountdown(now, itemDueDate)
                val remainingDaysStr = countdown.first

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = itemType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "زمان رو به اتمام",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = itemTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "مبلغ سررسید",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${FormatUtils.formatAmount(itemAmount)} ریال",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = remainingDaysStr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Yellow
                                )
                                Text(
                                    text = FormatUtils.getJalaliDateString(itemDueDate),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🎉", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هیچ تعهد مالی یا سررسید نزدیکی ثبت نشده است!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Quick stats cards showing totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToInstallments() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "مجموع اقساط جاری", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${FormatUtils.formatAmount(totalPendingInstAmount)} ریال",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToCheques() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "مجموع چک‌های ابلاغی", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${FormatUtils.formatAmount(totalPendingChequesAmount)} ریال",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Near due items section
            Text(
                text = "🔔 سررسیدهای امروز و هفته جاری",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
            )

            val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
            val nearDueItems = mutableListOf<Pair<String, Any>>()
            pendingInstallments.filter { (it.dueDate - now) <= sevenDaysMillis }.forEach {
                nearDueItems.add(Pair("قسط", it))
            }
            pendingCheques.filter { (it.dueDate - now) <= sevenDaysMillis }.forEach {
                nearDueItems.add(Pair("چک", it))
            }

            val sortedNearDues = nearDueItems.sortedBy { pair ->
                if (pair.second is Installment) (pair.second as Installment).dueDate else (pair.second as Cheque).dueDate
            }

            if (sortedNearDues.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "هیچ سررسیدی برای امروز یا هفته جاری وجود ندارد. خیالتان آسوده!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedNearDues.forEach { (type, item) ->
                        val itemTitle: String
                        val itemAmount: Long
                        val itemDueDate: Long
                        val itemStatusColor: Color
                        val typeLabel: String
                        
                        if (item is Installment) {
                            itemTitle = "قسط: ${item.title}"
                            itemAmount = item.amount
                            itemDueDate = item.dueDate
                            itemStatusColor = Color(0xFF3F51B5)
                            typeLabel = item.category
                        } else {
                            val ch = item as Cheque
                            itemTitle = "چک: ${ch.bankName} (ش‌چ ${ch.chequeNumber})"
                            itemAmount = ch.amount
                            itemDueDate = ch.dueDate
                            itemStatusColor = if (ch.isMyCheque) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                            typeLabel = if (ch.isMyCheque) "پرداختی صادره" else "دریافتی وارده"
                        }
                        
                        val diffMillis = itemDueDate - now
                        val daysRemaining = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                        val daysRemainingStr = when {
                            diffMillis < 0 -> "⚠️ گذشته از سررسید"
                            daysRemaining == 0 -> "🔴 امروز"
                            daysRemaining == 1 -> "🟠 فردا"
                            else -> "🟡 $daysRemaining روز باقی‌مانده"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(itemStatusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = typeLabel,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = itemStatusColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = itemTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "مبلغ: ${FormatUtils.formatAmount(itemAmount)} ریال",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = daysRemainingStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (diffMillis < 0) Color(0xFFD32F2F) else if (daysRemaining == 0) Color(0xFFD32F2F) else if (daysRemaining == 1) Color(0xFFFF9800) else Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = FormatUtils.getJalaliDateString(itemDueDate),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High Fidelity Canvas Chart section
            if (totalDues > 0L) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "توزیع سهم بدهی جاری براساس دسته‌بندی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Draw high fidelity donut pie chart
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val animSweep = animateFloatAsState(
                                    targetValue = 360f,
                                    animationSpec = tween(1200)
                                )

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var startAngle = 0f
                                    categoryDuesMap.values.forEachIndexed { index, amount ->
                                        val percentage = amount.toFloat() / totalDues.toFloat()
                                        val sweepAngle = percentage * animSweep.value
                                        drawArc(
                                            color = categoryColors.getOrElse(index) { Color.LightGray },
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        startAngle += sweepAngle
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "مجموع بدهی",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormatUtils.formatAmount(totalDues),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Legend List representation
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                categoryDuesMap.entries.forEachIndexed { index, entry ->
                                    val color = categoryColors.getOrElse(index) { Color.LightGray }
                                    val percentage = (entry.value.toFloat() / totalDues.toFloat() * 100).toInt()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(color, shape = CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = entry.key,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "${FormatUtils.formatInteger(percentage)}٪",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "اطلاعات")
                        Text(
                            text = "برای نمایش نمودارهای آماری، چند یادآور اضافه کنید.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
