package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val accountType: String, // "CASH" or "BANK"
    val category: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    // Timestamp of the last local edit. Used for last-write-wins conflict
    // resolution during cloud sync (0 = legacy record created before this field).
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
)
