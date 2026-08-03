package com.example.domain.repository

import com.example.domain.model.Bill
import com.example.domain.model.Category
import com.example.domain.model.CategoryExpense
import com.example.domain.model.CategoryType
import com.example.domain.model.DailyExpense
import com.example.domain.model.FinanceSummary
import com.example.domain.model.MonthlyComparison
import com.example.domain.model.TimePeriod
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for finance operations.
 * Defines the contract for data operations in the domain layer.
 */
interface FinanceRepository {

    // Observable data streams
    val transactions: Flow<List<Transaction>>
    val bills: Flow<List<Bill>>
    val categories: Flow<List<Category>>

    // Transaction operations
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    suspend fun getTransactionsDirect(): List<Transaction>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Result<String>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(id: String): Result<Unit>
    suspend fun deleteAllTransactions(): Result<Unit>

    // Category operations
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun getCategoriesDirect(): List<Category>
    suspend fun getCategoryById(id: String): Category?
    suspend fun insertCategory(category: Category): Result<String>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(id: String): Result<Unit>
    suspend fun insertDefaultCategories(): Result<Unit>

    // Bill operations
    fun getUpcomingBills(daysAhead: Int = 7): Flow<List<Bill>>
    fun getOverdueBills(): Flow<List<Bill>>
    suspend fun getBillsDirect(): List<Bill>
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
