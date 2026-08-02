package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String,
    val accountType: String,
    val category: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)
