package com.example.domain.model

import java.util.UUID
import java.util.Date

/**
 * Domain model for financial transactions.
 * Represents all types of financial movements in the system.
 */
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val accountType: AccountType,
    val category: Category,
    val date: Date,
    val note: String = "",
    val isRecurring: Boolean = false,
    val recurringInterval: RecurringInterval? = null,
    val lastSyncedAt: Long? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER_IN,
    TRANSFER_OUT
}

enum class AccountType {
    CASH,
    BANK_ACCOUNT,
    E_WALLET,
    CREDIT_CARD,
    INVESTMENT
}

enum class RecurringInterval {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * Helper extension to convert Transaction to a map for Firebase/Room storage
 */
fun Transaction.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "title" to title,
        "amount" to amount,
        "type" to type.name,
        "accountType" to accountType.name,
        "categoryId" to category.id,
        "categoryName" to category.name,
        "date" to date.time,
        "note" to note,
        "isRecurring" to isRecurring,
        "recurringInterval" to recurringInterval?.name,
        "isDeleted" to isDeleted,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "lastSyncedAt" to lastSyncedAt
    )
}

/**
 * Helper extension to create a Transaction from a map (Firebase/Room)
 */
fun Map<String, Any?>.toTransaction(categoryMap: Map<String, Category> = emptyMap()): Transaction {
    val categoryId = this["categoryId"] as? String ?: ""
    val categoryName = this["categoryName"] as? String ?: "Lainnya"
    
    val category = categoryMap[categoryId] ?: Category(
        id = categoryId,
        name = categoryName,
        icon = "attach_money",
        color = "#9E9E9E"
    )

    return Transaction(
        id = (this["id"] as? String) ?: UUID.randomUUID().toString(),
        title = (this["title"] as? String) ?: "",
        amount = (this["amount"] as? Double) ?: 0.0,
        type = TransactionType.valueOf((this["type"] as? String) ?: "EXPENSE"),
        accountType = AccountType.valueOf((this["accountType"] as? String) ?: "CASH"),
        category = category,
        date = Date((this["date"] as? Long) ?: System.currentTimeMillis()),
        note = (this["note"] as? String) ?: "",
        isRecurring = (this["isRecurring"] as? Boolean) ?: false,
        recurringInterval = (this["recurringInterval"] as? String)?.let { 
            try { RecurringInterval.valueOf(it) } catch (e: Exception) { null }
        },
        isDeleted = (this["isDeleted"] as? Boolean) ?: false,
        createdAt = (this["createdAt"] as? Long) ?: System.currentTimeMillis(),
        updatedAt = (this["updatedAt"] as? Long) ?: System.currentTimeMillis(),
        lastSyncedAt = this["lastSyncedAt"] as? Long
    )
}
