package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for finance operations.
 * Defines the contract for data operations in the domain layer.
 */
interface FinanceRepository {
    
    // Transaction operations
    fun getTransactions(): Flow<List<Transaction>>
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Result<String>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(id: String): Result<Unit>
    suspend fun deleteAllTransactions(): Result<Unit>
    
    // Category operations
    fun getCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun insertCategory(category: Category): Result<String>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(id: String): Result<Unit>
    suspend fun insertDefaultCategories(): Result<Unit>
    
    // Bill operations
    fun getBills(): Flow<List<Bill>>
    fun getUpcomingBills(daysAhead: Int = 7): Flow<List<Bill>>
    fun getOverdueBills(): Flow<List<Bill>>
    suspend fun getBillById(id: String): Bill?
    suspend fun insertBill(bill: Bill): Result<String>
    suspend fun updateBill(bill: Bill): Result<Unit>
    suspend fun markBillAsPaid(id: String): Result<Unit>
    suspend fun deleteBill(id: String): Result<Unit>
    suspend fun generateRecurringBills(): Result<Unit>
    
    // Summary and Analytics
    fun getFinanceSummary(timePeriod: TimePeriod): Flow<FinanceSummary>
    fun getCategoryExpenses(timePeriod: TimePeriod): Flow<Map<String, CategoryExpense>>
    fun getDailyExpenses(days: Int = 30): Flow<List<DailyExpense>>
    fun getMonthlyComparison(months: Int = 6): Flow<List<MonthlyComparison>>
    
    // Sync operations
    suspend fun syncWithData(): Result<Unit>
    suspend fun getLastSyncTime(): Long?
    suspend fun clearLocalData(): Result<Unit>
}

/**
 * Daily expense data for charts
 */
data class DailyExpense(
    val date: Long,
    val amount: Double,
    val transactionCount: Int
)

/**
 * Monthly comparison data for analytics
 */
data class MonthlyComparison(
    val month: String,
    val income: Double,
    val expense: Double,
    val savings: Double
)
