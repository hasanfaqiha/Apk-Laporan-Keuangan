package com.example.data

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
    val note: String = ""
)
