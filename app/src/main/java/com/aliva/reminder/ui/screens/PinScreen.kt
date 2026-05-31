package com.aliva.reminder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliva.reminder.util.FormatUtils

@Composable
fun PinScreen(
    correctPin: String,
    onPinCorrect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredDigits by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "امنیتی",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "برنامه قفل است",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Text(
            text = "جهت ثبت یا مشاهده اطلاعات مالی، لطفا پین‌کد ورود خود را وارد کنید",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Multi circles corresponding to inputs
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val hasDigit = index < enteredDigits.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasDigit) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Standalone Grid Keyboard Layout
        val buttonsList = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("پاک کردن", "0", "")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            buttonsList.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    row.forEach { char ->
                        if (char.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        if (char == "پاک کردن") {
                                            if (enteredDigits.isNotEmpty()) {
                                                enteredDigits = enteredDigits.dropLast(1)
                                            }
                                        } else {
                                            if (enteredDigits.length < 4) {
                                                enteredDigits += char
                                                if (enteredDigits.length == 4) {
                                                    if (enteredDigits == correctPin) {
                                                        onPinCorrect()
                                                    } else {
                                                        Toast.makeText(context, "پین‌کد اشتباه است!", Toast.LENGTH_SHORT).show()
                                                        enteredDigits = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (char == "پاک کردن") char else FormatUtils.formatInteger(char.toIntOrNull() ?: 0),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (char == "پاک کردن") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
