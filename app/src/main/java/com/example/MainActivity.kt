package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ReminderViewModel
import com.example.util.FormatUtils
import com.example.util.FormatUtils.toPersianDigits

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get database and setup repository
        val database = ReminderDatabase.getDatabase(this)
        val repository = ReminderRepository(database.installmentDao, database.chequeDao)

        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Android 13+
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasNotificationPermission = isGranted
                    if (!isGranted) {
                        Toast.makeText(
                            this,
                            "بدون دسترسی نوتیفیکیشن، یادآوری‌ها نشان داده نخواهند شد.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val viewModel: ReminderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ReminderViewModel.Factory(application, repository)
                )

                // Render App in RTL
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        ReminderAppScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderAppScreen(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val installments by viewModel.installments.collectAsStateWithLifecycle()
    val cheques by viewModel.cheques.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Installments, 1 = Cheques
    var showAddInstallmentDialog by remember { mutableStateOf(false) }
    var showAddChequeDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()

    // Calculations for the dashboard overview cards
    val totalPendingInstallmentsAmount = installments
        .filter { !it.isCompleted }
        .sumOf { it.amount }

    val totalPendingChequesAmount = cheques
        .filter { !it.isCleared }
        .sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Premium Header (Professional Polish Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "مدیریت چک و اقساط",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "جمعه، ۹ خرداد ۱۴۰۵",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Interactive Profile avatar icon with active toast feedback
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        Toast.makeText(context, "پروفایل فعال است", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👤", fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        // Dashboard Viewport Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Next Reminder Hero Card
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
                val itemTitle: String
                val itemType: String
                val itemAmount: Long
                val itemDueDate: Long

                if (closestItem is Installment) {
                    itemTitle = closestItem.title
                    itemType = "قسط: ${closestItem.category}"
                    itemAmount = closestItem.amount
                    itemDueDate = closestItem.dueDate
                } else {
                    val ch = closestItem as Cheque
                    itemTitle = "چک شماره ${ch.chequeNumber} (${ch.bankName})"
                    itemType = if (ch.isMyCheque) "چک صادره" else "چک وارده"
                    itemAmount = ch.amount
                    itemDueDate = ch.dueDate
                }

                val countdown = FormatUtils.getDaysCountdown(now, itemDueDate)
                val remainingDaysStr = countdown.first

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
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
                                    .background(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "نزدیک‌ترین موعد",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Glowing notifications alarm bell
                            IconButton(
                                onClick = { viewModel.checkForUpcomingPayments() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(text = "🔔", fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = remainingDaysStr,
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        Text(
                            text = "$itemTitle • $itemType",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "مبلغ قابل پرداخت",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = FormatUtils.formatAmount(itemAmount),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ریال",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // High fidelity notification testing
                            Button(
                                onClick = { viewModel.triggerTestNotification() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = "تست یادآور", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Empty card state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🎉", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "همه حساب‌ها تسویه هستند!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "هیچ قسط یا چک فعالی برای نمایش وجود ندارد.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Quick Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Installment Stats
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "مجموع اقساط",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${FormatUtils.formatInteger(installments.count { !it.isCompleted })} مورد",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Cheque Stats
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "کل چک‌های ماه",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${FormatUtils.formatInteger(cheques.count { !it.isCleared })} فقره",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Compact Tab Pills Selector (Professional Polish Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (activeTab == 0) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { activeTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "برنامه اقساط (${FormatUtils.formatInteger(installments.count { !it.isCompleted })})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (activeTab == 1) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { activeTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "دفترچه چک‌ها (${FormatUtils.formatInteger(cheques.count { !it.isCleared })})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // List Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (activeTab == 0) {
                // Installments Screen
                val activeInstallments = installments.filter { !it.isCompleted }
                val completedInstallments = installments.filter { it.isCompleted }

                if (installments.isEmpty()) {
                    EmptyStatePlaceholder(
                        title = "جدول اقساط شما خالی است",
                        description = "برای ثبت قسط ماهانه روی دکمه + در پایین کلیک کنید."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (activeInstallments.isNotEmpty()) {
                            item {
                                Text(
                                    text = "اقساط معلق",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            items(activeInstallments) { inst ->
                                InstallmentCard(
                                    installment = inst,
                                    now = now,
                                    onPayClick = { viewModel.payCurrentMonth(inst) },
                                    onDeleteClick = { viewModel.deleteInstallment(inst) }
                                )
                            }
                        }

                        if (completedInstallments.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "تسویه شده",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            items(completedInstallments) { inst ->
                                CompletedInstallmentCard(
                                    installment = inst,
                                    onDeleteClick = { viewModel.deleteInstallment(inst) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Cheques Screen
                val pendingCheques = cheques.filter { !it.isCleared }
                val clearedCheques = cheques.filter { it.isCleared }

                if (cheques.isEmpty()) {
                    EmptyStatePlaceholder(
                        title = "دفترچه چک شما خالی است",
                        description = "تمام چک‌های صادره یا وارده خود را با سررسیدها ثبت کنید."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (pendingCheques.isNotEmpty()) {
                            item {
                                Text(
                                    text = "چک‌های سررسید نشده",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            items(pendingCheques) { ch ->
                                ChequeCard(
                                    cheque = ch,
                                    now = now,
                                    onToggleCleared = { viewModel.toggleChequeCleared(ch) },
                                    onDeleteClick = { viewModel.deleteCheque(ch) }
                                )
                            }
                        }

                        if (clearedCheques.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "چک‌های پاس شده",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            items(clearedCheques) { ch ->
                                ClearedChequeCard(
                                    cheque = ch,
                                    onToggleCleared = { viewModel.toggleChequeCleared(ch) },
                                    onDeleteClick = { viewModel.deleteCheque(ch) }
                                )
                            }
                        }
                    }
                }
            }

            // Universal FAB - centered dynamically or at bottom corner
            FloatingActionButton(
                onClick = {
                    if (activeTab == 0) showAddInstallmentDialog = true
                    else showAddChequeDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 4.dp)
                    .testTag("fab_add_item")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "اضافه کردن")
            }
        }
    }

    // Modal Dialog for adding an Installment
    if (showAddInstallmentDialog) {
        AddInstallmentDialog(
            onDismiss = { showAddInstallmentDialog = false },
            onConfirm = { title, amount, dueTimestamp, total, category, notes ->
                viewModel.addInstallment(title, amount, dueTimestamp, total, category, notes)
                showAddInstallmentDialog = false
            }
        )
    }

    // Modal Dialog for adding a Cheque
    if (showAddChequeDialog) {
        AddChequeDialog(
            onDismiss = { showAddChequeDialog = false },
            onConfirm = { num, bank, amount, dueTimestamp, payee, isMyCheque, notes ->
                viewModel.addCheque(num, bank, amount, dueTimestamp, payee, isMyCheque, notes)
                showAddChequeDialog = false
            }
        )
    }
}

// ---------------------- Sub-composables ----------------------

@Composable
fun EmptyStatePlaceholder(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Empty list placeholder",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InstallmentCard(
    installment: Installment,
    now: Long,
    onPayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val countdown = FormatUtils.getDaysCountdown(now, installment.dueDate)
    val remainingDaysStr = countdown.first
    val diffDays = countdown.second
    val isNearDue = countdown.third

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("installment_card_${installment.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title, Delete, and Category Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = installment.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = installment.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف قسط",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Metrics: Amount and Shamsi Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مبلغ این قسط",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${FormatUtils.formatAmount(installment.amount)} ریال",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "تاریخ سررسید",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = FormatUtils.getJalaliDateString(installment.dueDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Indicators
            val progress = installment.paidInstallments.toFloat() / installment.totalInstallments.toFloat()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وضعیت پرداخت: ${FormatUtils.formatInteger(installment.paidInstallments)} از ${FormatUtils.formatInteger(installment.totalInstallments)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${FormatUtils.formatInteger((progress * 100).toInt())}٪",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row: Countdown Badge & "Pay" Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown Badge
                val badgeColor = when {
                    diffDays < 0 -> Color(0xFFD32F2F)       // Red Overdue
                    diffDays <= 2 -> Color(0xFFFF9800)      // Orange urgent
                    else -> Color(0xFF2E7D32)               // Quiet Emerald
                }

                Box(
                    modifier = Modifier
                        .background(color = badgeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = remainingDaysStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                // Submit Payment Action Button
                Button(
                    onClick = onPayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "ثبت قسط",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "پرداخت این ماه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (installment.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "یادداشت: ${installment.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun CompletedInstallmentCard(
    installment: Installment,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardStrokeHelper.greyStroke()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(color = Color(0xFF2E7D32).copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "تکمیل شده",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = installment.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تسویه تمام ${FormatUtils.formatInteger(installment.totalInstallments)} قسط به مبلغ ${FormatUtils.formatAmount(installment.amount)} ریال",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف قسط",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun ChequeCard(
    cheque: Cheque,
    now: Long,
    onToggleCleared: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val countdown = FormatUtils.getDaysCountdown(now, cheque.dueDate)
    val remainingDaysStr = countdown.first
    val diffDays = countdown.second

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cheque_card_${cheque.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Bank + Number, Type Identifier, & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (cheque.isMyCheque) Color(0xFFD32F2F).copy(alpha = 0.1f)
                                        else Color(0xFF2E7D32).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (cheque.isMyCheque) "چک صادره (امضا)" else "چک وارده (دریافتی)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cheque.isMyCheque) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${cheque.bankName} - ش‌چ (${cheque.chequeNumber.toPersianDigits()})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف چک",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Metrics: Amount & Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مبلغ چک",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${FormatUtils.formatAmount(cheque.amount)} ریال",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "تاریخ سررسید",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = FormatUtils.getJalaliDateString(cheque.dueDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payee info
            Text(
                text = "در وجه / بابت: ${cheque.payeeName}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Row: Countdown Badge & "Clear" Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown Badge
                val badgeColor = when {
                    diffDays < 0 -> Color(0xFFD32F2F)       // Overdue
                    diffDays <= 2 -> Color(0xFFFF9800)      // Urgent
                    else -> Color(0xFF2E7D32)               // Normal
                }

                Box(
                    modifier = Modifier
                        .background(color = badgeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = remainingDaysStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                // Check cleared marker Button
                Button(
                    onClick = onToggleCleared,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "پاس شد",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "پاس کردن چک", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (cheque.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "یادداشت: ${cheque.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun ClearedChequeCard(
    cheque: Cheque,
    onToggleCleared: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardStrokeHelper.greyStroke()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(color = Color(0xFF2E7D32).copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "پاس شده",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${cheque.bankName} - (${cheque.chequeNumber.toPersianDigits()})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "رقم: ${FormatUtils.formatAmount(cheque.amount)} ریال • گیرنده: ${cheque.payeeName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleCleared) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "بازنشانی",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف چک",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// High-fidelity Shamsi/Jalali interactive Date Picker widget
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
        // Date Header with Next/Previous Month Controls (chevron chevals)
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

// Dialog for registering installments
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstallmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Long, dueTimestamp: Long, totalInstallments: Int, category: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) } 
    var totalInstallmentsStr by remember { mutableStateOf("12") }
    var selectedCategory by remember { mutableStateOf("وام") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("وام", "خرید اقساطی", "خودرو", "شخصی", "اجاره", "سایر")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ثبت‌نام قسط تازه",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان قسط (مثال: قسط وام مسکن)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_title_inst")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("قسط ناخالص ماهیانه (به ریال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_amount_inst")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalInstallmentsStr,
                        onValueChange = { totalInstallmentsStr = it },
                        label = { Text("تعداد کل اقساط") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category dropdown representation
                Column {
                    Text(
                        text = "دسته‌بندی پرداخت",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedCategory == cat) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategory == cat) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedCategory == cat) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategory == cat) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Date picker
                Column {
                    Text(
                        text = "سررسید اولین قسط:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    JalaliDatePicker(
                        initialTimestamp = selectedTimestamp,
                        onDateSelected = { selectedTimestamp = it }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت یا کد پیگیری (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toLongOrNull() ?: 0L
                    val total = totalInstallmentsStr.toIntOrNull() ?: 12
                    if (title.isNotEmpty() && amt > 0L) {
                        onConfirm(title, amt, selectedTimestamp, total, selectedCategory, notes)
                    }
                }
            ) {
                Text("ثبت یادآور")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

// Dialog for registering bank cheques
@Composable
fun AddChequeDialog(
    onDismiss: () -> Unit,
    onConfirm: (chequeNumber: String, bankName: String, amount: Long, dueTimestamp: Long, payeeName: String, isMyCheque: Boolean, notes: String) -> Unit
) {
    var chequeNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var payeeName by remember { mutableStateOf("") }
    var isMyCheque by remember { mutableStateOf(true) } // default Issued (صادره)
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) } 
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ثبت چک بانکی جدید",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector check type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMyCheque) Color(0xFFD32F2F).copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { isMyCheque = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "چک صادر کردم (صادره)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMyCheque) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isMyCheque) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { isMyCheque = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "چک دریافت کردم (وارده)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isMyCheque) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("نام بانک (مثال: بانک ملی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_bank_ch")
                )

                OutlinedTextField(
                    value = chequeNumber,
                    onValueChange = { chequeNumber = it },
                    label = { Text("شماره صیادی یا شناسه چک") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_number_ch")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ چک (به ریال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_amount_ch")
                )

                OutlinedTextField(
                    value = payeeName,
                    onValueChange = { payeeName = it },
                    label = { Text(if (isMyCheque) "در وجه یا بابت" else "صادرکننده چک") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date picker
                Column {
                    Text(
                        text = "سررسید چک:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    JalaliDatePicker(
                        initialTimestamp = selectedTimestamp,
                        onDateSelected = { selectedTimestamp = it }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات بیشتر (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toLongOrNull() ?: 0L
                    if (chequeNumber.isNotEmpty() && bankName.isNotEmpty() && amt > 0L) {
                        onConfirm(chequeNumber, bankName, amt, selectedTimestamp, payeeName, isMyCheque, notes)
                    }
                }
            ) {
                Text("ثبت یادآور چک")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

// Isolated Stroke Helper to prevent redundant inline allocations
object CardStrokeHelper {
    @Composable
    fun greyStroke(): BorderStroke {
        return BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}
