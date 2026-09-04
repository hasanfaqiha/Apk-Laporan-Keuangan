package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "EXPENSE" or "INCOME"
    // Timestamp of the last local edit. Used for last-write-wins conflict
    // resolution during cloud sync (0 = legacy record created before this field).
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0L
)
