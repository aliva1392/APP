package com.aliva.reminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliva.reminder.data.Cheque
import com.aliva.reminder.ui.components.ChequeCard
import com.aliva.reminder.ui.components.ClearedChequeCard

@Composable
fun ChequeScreen(
    cheques: List<Cheque>,
    now: Long,
    onToggleCleared: (Cheque) -> Unit,
    onToggleBounced: (Cheque) -> Unit,
    onDeleteClick: (Cheque) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDirectionFilter by remember { mutableStateOf("همه") } // "همه", "صادره", "وارده"
    var selectedSortOption by remember { mutableStateOf(0) } // 0 = Due Date Asc, 1 = Due Date Desc, 2 = Amount Desc, 3 = Amount Asc
    var showSortMenu by remember { mutableStateOf(false) }

    val sortOptions = listOf(
        "سررسید (زود به دیر)",
        "سررسید (دیر به زود)",
        "مبلغ (بیشترین به کمترین)",
        "مبلغ (کمترین به بیشترین)"
    )

    // Filtering logic
    val filteredCheques = cheques.filter { ch ->
        val matchesSearch = ch.bankName.contains(searchQuery, ignoreCase = true) ||
                            ch.chequeNumber.contains(searchQuery, ignoreCase = true) ||
                            ch.payeeName.contains(searchQuery, ignoreCase = true) ||
                            ch.notes.contains(searchQuery, ignoreCase = true)
        
        val matchesDirection = when (selectedDirectionFilter) {
            "صادره" -> ch.isMyCheque
            "وارده" -> !ch.isMyCheque
            else -> true
        }

        matchesSearch && matchesDirection
    }

    // Sorting logic
    val sortedCheques = when (selectedSortOption) {
        0 -> filteredCheques.sortedBy { it.dueDate }
        1 -> filteredCheques.sortedByDescending { it.dueDate }
        2 -> filteredCheques.sortedByDescending { it.amount }
        3 -> filteredCheques.sortedBy { it.amount }
        else -> filteredCheques
    }

    val pendingCheques = sortedCheques.filter { !it.isCleared }
    val clearedCheques = sortedCheques.filter { it.isCleared }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جستجو در مبالغ، شماره یا صادرکننده...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "جستجو") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true
        )

        // Sorting & Filter header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "دسته‌بندی تعهدات چک",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                Button(
                    onClick = { showSortMenu = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(text = "مرتب‌سازی", fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    sortOptions.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            text = { Text(option, fontSize = 11.sp) },
                            onClick = {
                                selectedSortOption = index
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Direction toggles row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val directions = listOf("همه", "صادره", "وارده")
            val directionLabels = mapOf("همه" to "همه چک‌ها", "صادره" to "صادره (ما)", "وارده" to "وارده (دریافتی)")
            
            directions.forEach { dir ->
                val isSelected = (selectedDirectionFilter == dir)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                        .clickable { selectedDirectionFilter = dir }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = directionLabels[dir] ?: dir,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Items representation
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (pendingCheques.isEmpty() && clearedCheques.isEmpty()) {
                item {
                    Text(
                        text = "هیچ موردی یافت نشد",
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pending cheques representation
            if (pendingCheques.isNotEmpty()) {
                item {
                    Text(
                        text = "چک‌های سررسید جاری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(pendingCheques, key = { it.id }) { ch ->
                    ChequeCard(
                        cheque = ch,
                        now = now,
                        onToggleCleared = { onToggleCleared(ch) },
                        onToggleBounced = { onToggleBounced(ch) },
                        onDeleteClick = { onDeleteClick(ch) }
                    )
                }
            }

            // Cleared cheques representation
            if (clearedCheques.isNotEmpty()) {
                item {
                    Text(
                        text = "چک‌های پاس‌شده نهایی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(clearedCheques, key = { it.id }) { ch ->
                    ClearedChequeCard(
                        cheque = ch,
                        onToggleCleared = { onToggleCleared(ch) },
                        onDeleteClick = { onDeleteClick(ch) }
                    )
                }
            }
        }
    }
}
