package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installments",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["isCompleted"]),
        Index(value = ["amount"])
    ]
)
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Long,          // Monthly installment amount
    val dueDate: Long,         // Next due date system miles
    val totalInstallments: Int,// Total installments (e.g. 12)
    val paidInstallments: Int, // Paid installments count
    val category: String,      // Category (e.g. وام, خرید, قسط)
    val notes: String = "",
    val isCompleted: Boolean = false,
    val imageUri: String? = null
) {
    val remainingInstallments: Int
        get() = (totalInstallments - paidInstallments).coerceAtLeast(0)
}

@Entity(
    tableName = "cheques",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["isCleared"]),
        Index(value = ["chequeNumber"]),
        Index(value = ["payeeName"]),
        Index(value = ["amount"])
    ]
)
data class Cheque(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chequeNumber: String,  // شماره چک
    val bankName: String,      // نام بانک
    val amount: Long,          // مبلغ
    val dueDate: Long,         // تاریخ سررسید
    val payeeName: String,     // در وجه
    val isMyCheque: Boolean,   // Is issued by me (صادره) or received by me (وارده)
    val isCleared: Boolean = false, // پاس شده
    val notes: String = "",
    val isBounced: Boolean = false, // برگشتی
    val imageUri: String? = null
)
