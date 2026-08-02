package com.example.data.repository_impl

import com.example.data.local.FinanceDao
import com.example.domain.model.Bill
import com.example.domain.model.BillStatus
import com.example.domain.model.Category
import com.example.domain.model.CategoryExpense
import com.example.domain.model.CategoryType
import com.example.domain.model.DailyExpense
import com.example.domain.model.FinanceSummary
import com.example.domain.model.MonthlyComparison
import com.example.domain.model.RecurringInterval
import com.example.domain.model.TimePeriod
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(private val financeDao: FinanceDao) : FinanceRepository {

    override fun getTransactions(): Flow<List<Transaction>> = 
        financeDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getBills(): Flow<List<Bill>> = 
        financeDao.getAllBills().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getCategories(): Flow<List<Category>> = 
        financeDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertTransaction(transaction: Transaction): Result<String> = runCatching {
        val id = financeDao.insertTransaction(transaction.toEntity())
        id.toString()
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> = runCatching {
        financeDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        financeDao.deleteTransactionById(id.toInt())
    }

    override suspend fun insertBill(bill: Bill): Result<String> = runCatching {
        val id = financeDao.insertBill(bill.toEntity())
        id.toString()
    }

    override suspend fun updateBill(bill: Bill): Result<Unit> = runCatching {
        financeDao.updateBill(bill.toEntity())
    }

    override suspend fun deleteBill(id: String): Result<Unit> = runCatching {
        financeDao.deleteBillById(id.toInt())
    }

    override suspend fun insertCategory(category: Category): Result<String> = runCatching {
        val id = financeDao.insertCategory(category.toEntity())
        id.toString()
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        financeDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = runCatching {
        financeDao.deleteCategory(id.toInt())
    }

    private fun com.example.data.local.TransactionEntity.toDomain(): Transaction {
        return Transaction(
            id = id.toString(),
            type = TransactionType.valueOf(type),
            amount = amount,
            categoryId = categoryId.toString(),
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            date = java.util.Date(dateMillis),
            note = note ?: "",
            accountName = accountName ?: "Cash",
            isRecurring = isRecurring,
            recurringInterval = recurringInterval?.let { RecurringInterval.valueOf(it) } ?: RecurringInterval.NONE,
            isDeleted = isDeleted,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Transaction.toEntity(): com.example.data.local.TransactionEntity {
        return com.example.data.local.TransactionEntity(
            id = id.toIntOrNull() ?: 0,
            type = type.name,
            amount = amount,
            categoryId = categoryId.toIntOrNull() ?: 0,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            dateMillis = date.time,
            note = note,
            accountName = accountName,
            isRecurring = isRecurring,
            recurringInterval = recurringInterval.name,
            isDeleted = isDeleted,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun com.example.data.local.BillEntity.toDomain(): Bill {
        return Bill(
            id = id.toString(),
            title = title,
            amount = amount,
            dueDate = java.util.Date(dueDateMillis),
            status = if (isPaid) BillStatus.PAID else BillStatus.UPCOMING,
            categoryId = categoryId.toString(),
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            note = note ?: "",
            isRecurring = isRecurring,
            recurringInterval = recurringInterval?.let { RecurringInterval.valueOf(it) } ?: RecurringInterval.NONE,
            lastPaidDate = lastPaidDate?.let { java.util.Date(it) },
            nextDueDate = nextDueDate?.let { java.util.Date(it) },
            reminderDaysBefore = reminderDaysBefore,
            isDeleted = isDeleted,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Bill.toEntity(): com.example.data.local.BillEntity {
        return com.example.data.local.BillEntity(
            id = id.toIntOrNull() ?: 0,
            title = title,
            amount = amount,
            dueDateMillis = dueDate.time,
            isPaid = status == BillStatus.PAID,
            categoryId = categoryId.toIntOrNull() ?: 0,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            note = note,
            isRecurring = isRecurring,
            recurringInterval = recurringInterval.name,
            lastPaidDate = lastPaidDate?.time,
            nextDueDate = nextDueDate?.time,
            reminderDaysBefore = reminderDaysBefore,
            isDeleted = isDeleted,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun com.example.data.local.CategoryEntity.toDomain(): Category {
        return Category(
            id = id.toString(),
            name = name,
            type = CategoryType.valueOf(type),
            icon = icon,
            color = color,
            budgetLimit = budgetLimit,
            isSystem = isSystem,
            isDeleted = isDeleted,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Category.toEntity(): com.example.data.local.CategoryEntity {
        return com.example.data.local.CategoryEntity(
            id = id.toIntOrNull() ?: 0,
            name = name,
            type = type.name,
            icon = icon,
            color = color,
            budgetLimit = budgetLimit,
            isSystem = isSystem,
            isDeleted = isDeleted,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return financeDao.getTransactionsByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> {
        return financeDao.getTransactionsByCategory(categoryId.toInt()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return financeDao.getTransactionsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        return financeDao.getTransactionById(id.toInt())?.toDomain()
    }

    override suspend fun deleteAllTransactions(): Result<Unit> = runCatching {
        financeDao.deleteAllTransactions()
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return financeDao.getCategoriesByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return financeDao.getCategoryById(id.toInt())?.toDomain()
    }

    override suspend fun insertDefaultCategories(): Result<Unit> = runCatching {
        // Insert default categories if needed
    }

    override fun getUpcomingBills(daysAhead: Int): Flow<List<Bill>> {
        return financeDao.getUpcomingBills(daysAhead).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOverdueBills(): Flow<List<Bill>> {
        return financeDao.getOverdueBills().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBillById(id: String): Bill? {
        return financeDao.getBillById(id.toInt())?.toDomain()
    }

    override suspend fun markBillAsPaid(id: String): Result<Unit> = runCatching {
        financeDao.markBillAsPaid(id.toInt())
    }

    override suspend fun generateRecurringBills(): Result<Unit> = runCatching {
        // Generate recurring bills logic
    }

    override fun getFinanceSummary(timePeriod: TimePeriod): Flow<FinanceSummary> {
        return financeDao.getFinanceSummary(timePeriod).map { summary ->
            summary.toDomain()
        }
    }

    override fun getCategoryExpenses(timePeriod: TimePeriod): Flow<Map<String, CategoryExpense>> {
        return financeDao.getCategoryExpenses(timePeriod).map { entities ->
            entities.associate { it.categoryId.toString() to it.toDomain() }
        }
    }

    override fun getDailyExpenses(days: Int): Flow<List<DailyExpense>> {
        return financeDao.getDailyExpenses(days).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMonthlyComparison(months: Int): Flow<List<MonthlyComparison>> {
        return financeDao.getMonthlyComparison(months).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncWithData(): Result<Unit> = runCatching {
        // Sync logic with Firebase
    }

    override suspend fun getLastSyncTime(): Long? {
        return financeDao.getLastSyncTime()
    }

    override suspend fun clearLocalData(): Result<Unit> = runCatching {
        financeDao.clearLocalData()
    }
}
