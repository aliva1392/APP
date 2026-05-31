package com.aliva.reminder

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.aliva.reminder.data.ReminderDatabase
import com.aliva.reminder.data.ReminderRepository
import com.aliva.reminder.ui.navigation.AppNavigation
import com.aliva.reminder.ui.screens.PinScreen
import com.aliva.reminder.ui.theme.MyApplicationTheme
import com.aliva.reminder.ui.viewmodel.ReminderViewModel

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

                // PIN passcode system check on program launch
                var pinEnteredSuccessfully by remember { mutableStateOf(false) }
                val requirePin = viewModel.isPinEnabled && viewModel.securityPin.isNotEmpty()

                if (requirePin && !pinEnteredSuccessfully) {
                    PinScreen(
                        correctPin = viewModel.securityPin,
                        onPinCorrect = { pinEnteredSuccessfully = true },
                        modifier = Modifier.safeDrawingPadding()
                    )
                } else {
                    AppNavigation(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
