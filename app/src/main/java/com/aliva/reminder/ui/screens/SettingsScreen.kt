package com.aliva.reminder.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    securityPin: String,
    isPinEnabled: Boolean,
    onSetPin: (String) -> Unit,
    onTogglePinEnabled: (Boolean) -> Unit,
    onExportData: () -> String,
    onImportData: (String) -> Boolean,
    onExportCsv: () -> String,
    onExportTextReport: () -> String,
    onTriggerTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var inputPinVal by remember { mutableStateOf(securityPin) }
    var importJsonInput by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "تنظیمات نرم‌افزار",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Security Set Screen
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "قفل گذاری")
                    Text(text = "امنیت و گذرواژهورودی", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "فعال‌سازی قفل ورود با پین‌کد کاربری", fontSize = 12.sp)
                    Switch(
                        checked = isPinEnabled,
                        onCheckedChange = { onTogglePinEnabled(it) }
                    )
                }

                if (isPinEnabled) {
                    OutlinedTextField(
                        value = inputPinVal,
                        onValueChange = { 
                            if (it.length <= 4) {
                                inputPinVal = it
                                onSetPin(it)
                            }
                        },
                        label = { Text("رمز عبور ورود به نرم افزار (۴ رقم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Backup & Restore Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "پشتیبان‌گیری")
                    Text(text = "مدیریت، پشتیبان‌گیری و خروجی اکسل و PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Text(
                    text = "می‌توانید اطلاعات کامل چک‌ها و اقساط ثبت‌شده را صادر کرده و کپی نمائید یا یک نسخه کپی را به برنامه بازگردانید. همچنین گزارش اکسل یا PDF قابل چاپ را استخراج کنید.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val json = onExportData()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Backup_Data", json)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "نسخه پشتیبان کپی شد!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("خروجی داده (JSON)", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("بازیابی داده (Import)", fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val csv = onExportCsv()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Backup_Data_CSV", csv)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "خروجی اکسل (CSV) در حافظه کپی شد! آماده چسباندن در شیت یا ذخیره است.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("خروجی Excel", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val report = onExportTextReport()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Backup_Data_Report", report)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "گزارش چاپی آماده در حافظه کپی شد! برای ارسال پیام یا ساخت فایل PDF استفاده کنید.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("گزارش چاپی / PDF", fontSize = 11.sp)
                    }
                }
            }
        }

        // Testing channel card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "نوتیف")
                    Text(text = "تست و عیب‌یابی سیستم هشدار", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = { onTriggerTestNotification() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ارسال نوتیفیکیشن تست فوری")
                }
            }
        }

        // Credits text
        Text(
            text = "دستیار مدیریت چک و اقساط مجرب\nنسخه ۱.۰.۰ • برای انتشار در گوگل‌پلی",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }

    // Modal Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("وارد کردن داده نسخه‌پشتیبان", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("لطفا متن JSON کپی شده نسخه پشتیبان خود را در بخش زیر وارد نمائید:", fontSize = 11.sp)
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("محتوای متنی JSON...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = onImportData(importJsonInput)
                        if (ok) {
                            showImportDialog = false
                            importJsonInput = ""
                            Toast.makeText(context, "بازیابی داده‌ها با موفقیت انجام شد!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "خطا در قالب متنی دیتای پشتیبان!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("تائید و اعمال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
