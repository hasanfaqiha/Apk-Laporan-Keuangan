package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recurring transaction rule. When [nextRunMillis] is reached, the app
 * generates a real [Transaction] and advances [nextRunMillis] by one
 * [frequency] step (DAILY / WEEKLY / MONTHLY / YEARLY).
 */
@Entity(tableName = "recurring_rules")
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME" / "EXPENSE" / "WITHDRAWAL" / "DEPOSIT"
    val accountType: String, // "CASH" / "BANK" / "CREDIT_CARD"
    val category: String,
    val frequency: String, // "DAILY" / "WEEKLY" / "MONTHLY" / "YEARLY"
    val startDateMillis: Long,
    @ColumnInfo(defaultValue = "0")
    val nextRunMillis: Long = 0L,
    @ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true,
    val note: String = ""
)
