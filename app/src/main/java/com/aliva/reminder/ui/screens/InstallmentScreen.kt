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
import com.aliva.reminder.data.Installment
import com.aliva.reminder.ui.components.CompletedInstallmentCard
import com.aliva.reminder.ui.components.InstallmentCard

@Composable
fun InstallmentScreen(
    installments: List<Installment>,
    now: Long,
    onPayClick: (Installment) -> Unit,
    onDeleteClick: (Installment) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("همه") }
    var selectedSortOption by remember { mutableStateOf(0) } // 0 = Due Date Asc, 1 = Due Date Desc, 2 = Amount Desc, 3 = Amount Asc
    var showSortMenu by remember { mutableStateOf(false) }

    val categories = listOf("همه", "وام", "خرید اقساطی", "خودرو", "شخصی", "اجاره", "سایر")
    val sortOptions = listOf(
        "سررسید (زود به دیر)",
        "سررسید (دیر به زود)",
        "مبلغ (بیشترین به کمترین)",
        "مبلغ (کمترین به بیشترین)"
    )

    // Apply filtering
    val filteredInstallments = installments.filter { inst ->
        val matchesSearch = inst.title.contains(searchQuery, ignoreCase = true) || 
                            inst.notes.contains(searchQuery, ignoreCase = true) ||
                            inst.category.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "همه" || inst.category == selectedCategoryFilter
        matchesSearch && matchesCategory
    }

    // Apply sorting
    val sortedInstallments = when (selectedSortOption) {
        0 -> filteredInstallments.sortedBy { it.dueDate }
        1 -> filteredInstallments.sortedByDescending { it.dueDate }
        2 -> filteredInstallments.sortedByDescending { it.amount }
        3 -> filteredInstallments.sortedBy { it.amount }
        else -> filteredInstallments
    }

    val activeInstallments = sortedInstallments.filter { !it.isCompleted }
    val completedInstallments = sortedInstallments.filter { it.isCompleted }

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
            placeholder = { Text("جستجو در بین اقساط سررسید...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "جستجو") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true
        )

        // Sorting & Filter Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "دسته‌بندی‌های اقساط",
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

        // Horizontal Category Row Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val visibleCategories = categories.take(4) // show top 4
            visibleCategories.forEach { cat ->
                val isSelected = (selectedCategoryFilter == cat)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedCategoryFilter = cat }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Actual items lists
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (activeInstallments.isEmpty() && completedInstallments.isEmpty()) {
                item {
                    Text(
                        text = "هیچ موردی یافت نشد",
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Unpaid/Active Installments header
            if (activeInstallments.isNotEmpty()) {
                item {
                    Text(
                        text = "اقساط فعال جاری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(activeInstallments, key = { it.id }) { inst ->
                    InstallmentCard(
                        installment = inst,
                        now = now,
                        onPayClick = { onPayClick(inst) },
                        onDeleteClick = { onDeleteClick(inst) }
                    )
                }
            }

            // Settled/Completed Installments header
            if (completedInstallments.isNotEmpty()) {
                item {
                    Text(
                        text = "اقساط تسویه شده",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(completedInstallments, key = { it.id }) { inst ->
                    CompletedInstallmentCard(
                        installment = inst,
                        onDeleteClick = { onDeleteClick(inst) }
                    )
                }
            }
        }
    }
}
