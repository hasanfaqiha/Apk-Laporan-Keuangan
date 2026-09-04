package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A monthly spending limit applied to one EXPENSE category. The limit is
 * compared against the sum of expense transactions of that category within
 * the current calendar month.
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val monthlyLimit: Double
)
