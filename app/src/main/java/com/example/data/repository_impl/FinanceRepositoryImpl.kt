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
        financeDao.insertTransaction(transaction.toEntity())
        transaction.id
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> = runCatching {
        financeDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        financeDao.hardDeleteTransaction(id)
    }

    override suspend fun insertBill(bill: Bill): Result<String> = runCatching {
        financeDao.insertBill(bill.toEntity())
        bill.id
    }

    override suspend fun updateBill(bill: Bill): Result<Unit> = runCatching {
        financeDao.updateBill(bill.toEntity())
    }

    override suspend fun deleteBill(id: String): Result<Unit> = runCatching {
        financeDao.hardDeleteBill(id)
    }

    override suspend fun insertCategory(category: Category): Result<String> = runCatching {
        financeDao.insertCategory(category.toEntity())
        category.id
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        financeDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = runCatching {
        financeDao.deleteCategoryById(id)
    }

    private fun com.example.data.local.TransactionEntity.toDomain(): Transaction {
        val category = Category(
            id = categoryId,
            name = categoryName,
            icon = categoryIcon,
            color = categoryColor
        )
        return Transaction(
            id = id,
            title = title,
            amount = amount,
            type = TransactionType.valueOf(type),
            accountType = com.example.domain.model.AccountType.valueOf(accountType),
            category = category,
            date = java.util.Date(date),
            note = note,
            isRecurring = isRecurring,
            recurringInterval = recurringInterval?.let { 
                try { RecurringInterval.valueOf(it) } catch (e: Exception) { null } 
            },
            isDeleted = isDeleted,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Transaction.toEntity(): com.example.data.local.TransactionEntity {
        return com.example.data.local.TransactionEntity(
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

    private fun com.example.data.local.BillEntity.toDomain(): Bill {
        val category = Category(
            id = categoryId,
            name = categoryName,
            icon = categoryIcon,
            color = categoryColor
        )
        return Bill(
            id = id,
            title = title,
            amount = amount,
            dueDate = java.util.Date(dueDate),
            isPaid = isPaid,
            paidDate = paidDate?.let { java.util.Date(it) },
            category = category,
            note = note,
            isRecurring = isRecurring,
            recurringInterval = recurringInterval?.let { 
                try { RecurringInterval.valueOf(it) } catch (e: Exception) { null } 
            },
            reminderDaysBefore = reminderDaysBefore,
            isDeleted = isDeleted,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Bill.toEntity(): com.example.data.local.BillEntity {
        return com.example.data.local.BillEntity(
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

    private fun com.example.data.local.CategoryEntity.toDomain(): Category {
        return Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            type = CategoryType.valueOf(type),
            isSystem = isSystem,
            budgetLimit = budgetLimit,
            spentAmount = spentAmount,
            isDeleted = false,
            lastSyncedAt = lastSyncedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Category.toEntity(): com.example.data.local.CategoryEntity {
        return com.example.data.local.CategoryEntity(
            id = id,
            name = name,
            icon = icon,
            color = color,
            type = type.name,
            isSystem = isSystem,
            budgetLimit = budgetLimit,
            spentAmount = 0.0,
            isDeleted = false,
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
        return financeDao.getTransactionsByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return financeDao.getTransactionsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        return financeDao.getTransactionById(id)?.toDomain()
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
        return financeDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun insertDefaultCategories(): Result<Unit> = runCatching {
        // Insert default categories if needed
    }

    override fun getUpcomingBills(daysAhead: Int): Flow<List<Bill>> {
        val now = System.currentTimeMillis()
        val futureDate = now + (daysAhead * 24 * 60 * 60 * 1000L)
        return financeDao.getUpcomingBills(now, futureDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOverdueBills(): Flow<List<Bill>> {
        val now = System.currentTimeMillis()
        return financeDao.getOverdueBills(now).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBillById(id: String): Bill? {
        return financeDao.getBillById(id)?.toDomain()
    }

    override suspend fun markBillAsPaid(id: String): Result<Unit> = runCatching {
        financeDao.markBillAsPaid(id)
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
            entities.associate { it.categoryId to it.toDomain() }
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
        return financeDao.getLastTransactionSyncTime()
    }

    override suspend fun clearLocalData(): Result<Unit> = runCatching {
        financeDao.deleteAllTransactions()
        financeDao.deleteAllTransactions() // Also clear bills and categories if needed
    }
}
