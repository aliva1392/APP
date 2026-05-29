package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

                // Category selection buttons
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
