package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.domain.model.AccountType
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.RecurringInterval
import com.example.domain.model.TransactionType

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
