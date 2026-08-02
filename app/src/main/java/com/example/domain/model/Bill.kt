package com.example.domain.model

import java.util.UUID
import java.util.Date

/**
 * Domain model for bills and recurring payments.
 */
data class Bill(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val dueDate: Date,
    val isPaid: Boolean = false,
    val paidDate: Date? = null,
    val category: Category,
    val note: String = "",
    val isRecurring: Boolean = false,
    val recurringInterval: RecurringInterval? = null,
    val reminderDaysBefore: Int = 3,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null
)

enum class BillStatus {
    PENDING,
    UPCOMING,
    OVERDUE,
    PAID
}

/**
 * Extension to get bill status based on due date and paid status
 */
fun Bill.getStatus(): BillStatus {
    if (isPaid && paidDate != null) return BillStatus.PAID
    
    val now = System.currentTimeMillis()
    val dueTime = dueDate.time
    val daysUntilDue = (dueTime - now) / (1000 * 60 * 60 * 24)
    
    return when {
        daysUntilDue < 0 -> BillStatus.OVERDUE
        daysUntilDue <= reminderDaysBefore -> BillStatus.UPCOMING
        else -> BillStatus.PENDING
    }
}

/**
 * Helper extension to convert Bill to a map for Firebase/Room storage
 */
fun Bill.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "title" to title,
        "amount" to amount,
        "dueDate" to dueDate.time,
        "isPaid" to isPaid,
        "paidDate" to paidDate?.time,
        "categoryId" to category.id,
        "categoryName" to category.name,
        "note" to note,
        "isRecurring" to isRecurring,
        "recurringInterval" to recurringInterval?.name,
        "reminderDaysBefore" to reminderDaysBefore,
        "isDeleted" to isDeleted,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "lastSyncedAt" to lastSyncedAt
    )
}

/**
 * Helper extension to create a Bill from a map (Firebase/Room)
 */
fun Map<String, Any?>.toBill(categoryMap: Map<String, Category> = emptyMap()): Bill {
    val categoryId = this["categoryId"] as? String ?: ""
    val categoryName = this["categoryName"] as? String ?: "Lainnya"
    
    val category = categoryMap[categoryId] ?: Category(
        id = categoryId,
        name = categoryName,
        icon = "receipt",
        color = "#9E9E9E"
    )

    return Bill(
        id = (this["id"] as? String) ?: UUID.randomUUID().toString(),
        title = (this["title"] as? String) ?: "",
        amount = (this["amount"] as? Double) ?: 0.0,
        dueDate = Date((this["dueDate"] as? Long) ?: System.currentTimeMillis()),
        isPaid = (this["isPaid"] as? Boolean) ?: false,
        paidDate = (this["paidDate"] as? Long)?.let { Date(it) },
        category = category,
        note = (this["note"] as? String) ?: "",
        isRecurring = (this["isRecurring"] as? Boolean) ?: false,
        recurringInterval = (this["recurringInterval"] as? String)?.let { 
            try { RecurringInterval.valueOf(it) } catch (e: Exception) { null }
        },
        reminderDaysBefore = (this["reminderDaysBefore"] as? Int) ?: 3,
        isDeleted = (this["isDeleted"] as? Boolean) ?: false,
        createdAt = (this["createdAt"] as? Long) ?: System.currentTimeMillis(),
        updatedAt = (this["updatedAt"] as? Long) ?: System.currentTimeMillis(),
        lastSyncedAt = this["lastSyncedAt"] as? Long
    )
}
