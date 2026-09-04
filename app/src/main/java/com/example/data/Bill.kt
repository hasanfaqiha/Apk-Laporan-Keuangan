package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val dueDateMillis: Long,
    val isPaid: Boolean = false,
    val category: String, // e.g. "Listrik", "Air", "Internet", "Sewa", "Lain-lain"
    val note: String = "",
    // Timestamp of the last local edit. Used for last-write-wins conflict
    // resolution during cloud sync (0 = legacy record created before this field).
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
)
