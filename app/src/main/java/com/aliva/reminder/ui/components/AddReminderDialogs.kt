package com.aliva.reminder.ui.components

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

    var showErrors by remember { mutableStateOf(false) }
    val isTitleValid = title.isNotBlank()
    val amountLong = amountStr.toLongOrNull()
    val isAmountValid = amountLong != null && amountLong > 0L
    val totalInt = totalInstallmentsStr.toIntOrNull()
    val isTotalValid = totalInt != null && totalInt > 0

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
                    isError = showErrors && !isTitleValid,
                    supportingText = {
                        if (showErrors && !isTitleValid) {
                            Text("وارد کردن عنوان الزامی است.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_title_inst")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ قسط ماهیانه (به ریال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showErrors && !isAmountValid,
                    supportingText = {
                        if (showErrors && !isAmountValid) {
                            Text("مبلغ معتبر (بیشتر از صفر ریال) وارد کنید.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                        }
                    },
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
                        isError = showErrors && !isTotalValid,
                        supportingText = {
                            if (showErrors && !isTotalValid) {
                                Text("تعداد باید بیشتر از صفر باشد.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                            }
                        },
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
                    if (isTitleValid && isAmountValid && isTotalValid) {
                        onConfirm(title, amountLong!!, selectedTimestamp, totalInt!!, selectedCategory, notes)
                    } else {
                        showErrors = true
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

    var showErrors by remember { mutableStateOf(false) }
    val isBankNameValid = bankName.isNotBlank()
    val isChequeNumberValid = chequeNumber.isNotBlank()
    val amountLong = amountStr.toLongOrNull()
    val isAmountValid = amountLong != null && amountLong > 0L
    val isPayeeValid = payeeName.isNotBlank()

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
                    isError = showErrors && !isBankNameValid,
                    supportingText = {
                        if (showErrors && !isBankNameValid) {
                            Text("وارد کردن نام بانک الزامی است.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_bank_ch")
                )

                OutlinedTextField(
                    value = chequeNumber,
                    onValueChange = { chequeNumber = it },
                    label = { Text("شماره صیادی یا شناسه چک") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showErrors && !isChequeNumberValid,
                    supportingText = {
                        if (showErrors && !isChequeNumberValid) {
                            Text("وارد کردن شماره صیادی یا شناسه معتبر چک صیادی الزامی است.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_number_ch")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("مبلغ چک (به ریال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showErrors && !isAmountValid,
                    supportingText = {
                        if (showErrors && !isAmountValid) {
                            Text("مبلغ معتبر (بیشتر از صفر ریال) وارد کنید.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_amount_ch")
                )

                OutlinedTextField(
                    value = payeeName,
                    onValueChange = { payeeName = it },
                    label = { Text(if (isMyCheque) "در وجه یا بابت" else "صادرکننده چک") },
                    singleLine = true,
                    isError = showErrors && !isPayeeValid,
                    supportingText = {
                        if (showErrors && !isPayeeValid) {
                            Text("وارد کردن صادرکننده/دریافت‌کننده الزامی است.", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                        }
                    },
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
                    if (isBankNameValid && isChequeNumberValid && isAmountValid && isPayeeValid) {
                        onConfirm(chequeNumber, bankName, amountLong!!, selectedTimestamp, payeeName, isMyCheque, notes)
                    } else {
                        showErrors = true
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
