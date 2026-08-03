package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.domain.model.AccountType
import com.example.domain.model.Bill
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.RecurringInterval
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import java.util.Date

/**
 * Room Entity for Transaction - Local persistence
 */
@Entity(tableName = "transactions")
@TypeConverters(Converters::class)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val type: String, // TransactionType.name
    val accountType: String, // AccountType.name
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val date: Long,
    val note: String,
    val isRecurring: Boolean,
    val recurringInterval: String?,
    val isDeleted: Boolean,
    val lastSyncedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Room Entity for Category - Local persistence
 */
@Entity(tableName = "categories")
@TypeConverters(Converters::class)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: String, // CategoryType.name
    val isSystem: Boolean,
    val budgetLimit: Double?,
    val spentAmount: Double,
    val createdAt: Long
)

/**
 * Room Entity for Bill - Local persistence
 */
@Entity(tableName = "bills")
@TypeConverters(Converters::class)
data class BillEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val isPaid: Boolean,
    val paidDate: Long?,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val note: String,
    val isRecurring: Boolean,
    val recurringInterval: String?,
    val reminderDaysBefore: Int,
    val isDeleted: Boolean,
    val lastSyncedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Type converters for Room database
 */
class Converters {
    @androidx.room.TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    
    @androidx.room.TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    
    @androidx.room.TypeConverter
    fun fromAccountType(value: AccountType): String = value.name
    
    @androidx.room.TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)
    
    @androidx.room.TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name
    
    @androidx.room.TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)
    
    @androidx.room.TypeConverter
    fun fromRecurringInterval(value: RecurringInterval): String? = value?.name
    
    @androidx.room.TypeConverter
    fun toRecurringInterval(value: String?): RecurringInterval? = value?.let { 
        try { RecurringInterval.valueOf(it) } catch (e: Exception) { null }
    }
}

// ==================== ENTITY <-> DOMAIN MAPPERS ====================

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        title = title,
        amount = amount,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        accountType = runCatching { AccountType.valueOf(accountType) }.getOrDefault(AccountType.CASH),
        category = Category(
            id = categoryId,
            name = categoryName,
            icon = categoryIcon,
            color = categoryColor,
            type = runCatching { CategoryType.valueOf(type) }.getOrDefault(CategoryType.EXPENSE)
        ),
        date = Date(date),
        note = note,
        isRecurring = isRecurring,
        recurringInterval = recurringInterval?.let {
            runCatching { RecurringInterval.valueOf(it) }.getOrNull()
        },
        isDeleted = isDeleted,
        lastSyncedAt = lastSyncedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        title = title,
        amount = amount,
        type = type.name,
        accountType = accountType.name,
        categoryId = category.id,
        categoryName = category.name,
        categoryIcon = category.icon,
        categoryColor = category.color,
        date = date.time,
        note = note,
        isRecurring = isRecurring,
        recurringInterval = recurringInterval?.name,
        isDeleted = isDeleted,
        lastSyncedAt = lastSyncedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        color = color,
        type = runCatching { CategoryType.valueOf(type) }.getOrDefault(CategoryType.EXPENSE),
        isSystem = isSystem,
        budgetLimit = budgetLimit,
        spentAmount = spentAmount,
        createdAt = createdAt
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color,
        type = type.name,
        isSystem = isSystem,
        budgetLimit = budgetLimit,
        spentAmount = spentAmount,
        createdAt = createdAt
    )
}

fun BillEntity.toDomain(): Bill {
    return Bill(
        id = id,
        title = title,
        amount = amount,
        dueDate = Date(dueDate),
        isPaid = isPaid,
        paidDate = paidDate?.let { Date(it) },
        category = Category(
            id = categoryId,
            name = categoryName,
            icon = categoryIcon,
            color = categoryColor
        ),
        note = note,
        isRecurring = isRecurring,
        recurringInterval = recurringInterval?.let {
            runCatching { RecurringInterval.valueOf(it) }.getOrNull()
        },
        reminderDaysBefore = reminderDaysBefore,
        isDeleted = isDeleted,
        lastSyncedAt = lastSyncedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Bill.toEntity(): BillEntity {
    return BillEntity(
        id = id,
        title = title,
        amount = amount,
        dueDate = dueDate.time,
        isPaid = isPaid,
        paidDate = paidDate?.time,
        categoryId = category.id,
        categoryName = category.name,
        categoryIcon = category.icon,
        categoryColor = category.color,
        note = note,
        isRecurring = isRecurring,
        recurringInterval = recurringInterval?.name,
        reminderDaysBefore = reminderDaysBefore,
        isDeleted = isDeleted,
        lastSyncedAt = lastSyncedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
