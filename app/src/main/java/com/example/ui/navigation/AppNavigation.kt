package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddChequeDialog
import com.example.ui.components.AddInstallmentDialog
import com.example.ui.screens.ChequeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InstallmentScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.ReminderViewModel

sealed class AppDestination(val route: String, val title: String) {
    object Dashboard : AppDestination("dashboard", "پیشخوان")
    object Installments : AppDestination("installments", "اقساط")
    object Cheques : AppDestination("cheques", "چک‌ها")
    object Settings : AppDestination("settings", "تنظیمات")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val installments by viewModel.installments.collectAsStateWithLifecycle()
    val cheques by viewModel.cheques.collectAsStateWithLifecycle()

    var activeDestination by remember { mutableStateOf<AppDestination>(AppDestination.Dashboard) }
    var showAddInstallmentDialog by remember { mutableStateOf(false) }
    var showAddChequeDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeDestination == AppDestination.Dashboard,
                        onClick = { activeDestination = AppDestination.Dashboard },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "پیشخوان") },
                        label = { Text("پیشخوان", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = activeDestination == AppDestination.Installments,
                        onClick = { activeDestination = AppDestination.Installments },
                        icon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = "اقساط") },
                        label = { Text("اقساط", fontSize = 11.sp) },
                        modifier = Modifier.testTag("tab_installments")
                    )
                    NavigationBarItem(
                        selected = activeDestination == AppDestination.Cheques,
                        onClick = { activeDestination = AppDestination.Cheques },
                        icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = "چک‌ها") },
                        label = { Text("چک‌ها", fontSize = 11.sp) },
                        modifier = Modifier.testTag("tab_cheques")
                    )
                    NavigationBarItem(
                        selected = activeDestination == AppDestination.Settings,
                        onClick = { activeDestination = AppDestination.Settings },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "تنظیمات") },
                        label = { Text("تنظیمات", fontSize = 11.sp) }
                    )
                }
            },
            floatingActionButton = {
                if (activeDestination == AppDestination.Installments || activeDestination == AppDestination.Cheques) {
                    FloatingActionButton(
                        onClick = {
                            if (activeDestination == AppDestination.Installments) {
                                showAddInstallmentDialog = true
                            } else {
                                showAddChequeDialog = true
                            }
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("fab_add_item")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "ثبت‌نام جدید")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeDestination) {
                    AppDestination.Dashboard -> {
                        DashboardScreen(
                            installments = installments,
                            cheques = cheques,
                            onNavigateToInstallments = { activeDestination = AppDestination.Installments },
                            onNavigateToCheques = { activeDestination = AppDestination.Cheques }
                        )
                    }
                    AppDestination.Installments -> {
                        InstallmentScreen(
                            installments = installments,
                            now = now,
                            onPayClick = { viewModel.payCurrentMonth(it) },
                            onDeleteClick = { viewModel.deleteInstallment(it) }
                        )
                    }
                    AppDestination.Cheques -> {
                        ChequeScreen(
                            cheques = cheques,
                            now = now,
                            onToggleCleared = { viewModel.toggleChequeCleared(it) },
                            onToggleBounced = { viewModel.toggleChequeBounced(it) },
                            onDeleteClick = { viewModel.deleteCheque(it) }
                        )
                    }
                    AppDestination.Settings -> {
                        SettingsScreen(
                            securityPin = viewModel.securityPin,
                            isPinEnabled = viewModel.isPinEnabled,
                            onSetPin = { viewModel.securityPin = it },
                            onTogglePinEnabled = { viewModel.isPinEnabled = it },
                            onExportData = { viewModel.exportDataToJson() },
                            onImportData = { viewModel.importDataFromJson(it) },
                            onExportCsv = { viewModel.exportDataToCsv() },
                            onExportTextReport = { viewModel.exportDataToTextReport() },
                            onTriggerTestNotification = { viewModel.triggerTestNotification() }
                        )
                    }
                }
            }
        }

        // Add installment dialog
        if (showAddInstallmentDialog) {
            AddInstallmentDialog(
                onDismiss = { showAddInstallmentDialog = false },
                onConfirm = { title, amt, due, total, cat, notes ->
                    viewModel.addInstallment(title, amt, due, total, cat, notes)
                    showAddInstallmentDialog = false
                }
            )
        }

        // Add cheque dialog
        if (showAddChequeDialog) {
            AddChequeDialog(
                onDismiss = { showAddChequeDialog = false },
                onConfirm = { num, bank, amt, due, payee, my, notes ->
                    viewModel.addCheque(num, bank, amt, due, payee, my, notes)
                    showAddChequeDialog = false
                }
            )
        }
    }
}
