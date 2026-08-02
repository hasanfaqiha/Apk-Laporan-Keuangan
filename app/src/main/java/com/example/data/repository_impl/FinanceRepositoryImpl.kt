package com.example.data.repository_impl

import com.example.data.local.FinanceDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
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
import com.example.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(private val financeDao: FinanceDao) : FinanceRepository {

    override val transactions: Flow<List<Transaction>> = 
        financeDao.getAllTransactions().map { entities -> entities.map { it.toDomain() } }

    override val bills: Flow<List<Bill>> = 
        financeDao.getAllBills().map { entities -> entities.map { it.toDomain() } }

    override val categories: Flow<List<Category>> = 
        financeDao.getAllCategories().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTransactionsDirect(): List<Transaction> =
        financeDao.getAllTransactionsDirect().map { it.toDomain() }

    override suspend fun getBillsDirect(): List<Bill> =
        financeDao.getAllBillsDirect().map { it.toDomain() }

    override suspend fun getCategoriesDirect(): List<Category> =
        financeDao.getAllCategoriesDirect().map { it.toDomain() }

    override suspend fun insertTransaction(transaction: Transaction): Result<String> = runCatching {
        val entity = transaction.toEntity()
        val id = financeDao.insertTransaction(entity)
        id.toString()
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> = runCatching {
        financeDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        financeDao.deleteTransaction(id)
    }

    override suspend fun deleteAllTransactions(): Result<Unit> = runCatching {
        financeDao.deleteAllTransactions()
    }

    override suspend fun getTransactionById(id: String): Transaction? =
        financeDao.getTransactionById(id)?.toDomain()

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        financeDao.getTransactionsByType(type).map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> =
        financeDao.getTransactionsByCategory(categoryId).map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        financeDao.getTransactionsByDateRange(startDate, endDate).map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertBill(bill: Bill): Result<String> = runCatching {
        val entity = bill.toEntity()
        val id = financeDao.insertBill(entity)
        id.toString()
    }

    override suspend fun updateBill(bill: Bill): Result<Unit> = runCatching {
        financeDao.updateBill(bill.toEntity())
    }

    override suspend fun deleteBill(id: String): Result<Unit> = runCatching {
        financeDao.deleteBill(id)
    }

    override suspend fun getBillById(id: String): Bill? =
        financeDao.getBillById(id)?.toDomain()

    override fun getUpcomingBills(daysAhead: Int): Flow<List<Bill>> =
        financeDao.getUpcomingBills(daysAhead).map { entities -> entities.map { it.toDomain() } }

    override fun getOverdueBills(): Flow<List<Bill>> =
        financeDao.getOverdueBills().map { entities -> entities.map { it.toDomain() } }

    override suspend fun markBillAsPaid(id: String): Result<Unit> = runCatching {
        val bill = financeDao.getBillById(id) ?: throw IllegalArgumentException("Bill not found")
        val updatedBill = bill.copy(
            isPaid = true,
            paidDate = System.currentTimeMillis()
        )
        financeDao.updateBill(updatedBill.toEntity())
    }

    override suspend fun generateRecurringBills(): Result<Unit> = runCatching {
        // Implementation for generating recurring bills
    }

    override suspend fun insertCategory(category: Category): Result<String> = runCatching {
        val entity = category.toEntity()
        val id = financeDao.insertCategory(entity)
        id.toString()
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        financeDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = runCatching {
        financeDao.deleteCategory(id)
    }

    override suspend fun getCategoryById(id: String): Category? =
        financeDao.getCategoryById(id)?.toDomain()

    override suspend fun insertDefaultCategories(): Result<Unit> = runCatching {
        // Default categories will be inserted via use case
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
        financeDao.getCategoriesByType(type).map { entities -> entities.map { it.toDomain() } }

    override fun getFinanceSummary(timePeriod: TimePeriod): Flow<FinanceSummary> {
        return transactions.map { txList ->
            val filtered = filterByPeriod(txList, timePeriod)
            val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            FinanceSummary(
                totalIncome = income,
                totalExpense = expense,
                netBalance = income - expense,
                transactionCount = filtered.size,
                period = timePeriod
            )
        }
    }

    override fun getCategoryExpenses(timePeriod: TimePeriod): Flow<Map<String, CategoryExpense>> {
        return transactions.map { txList ->
            val filtered = filterByPeriod(txList, timePeriod)
            val expenses = filtered.filter { it.type == TransactionType.EXPENSE }
            expenses.groupBy { it.categoryId }
                .mapValues { (_, txs) ->
                    CategoryExpense(
                        categoryId = txs.first().categoryId,
                        categoryName = txs.first().categoryName,
                        totalAmount = txs.sumOf { it.amount },
                        percentage = 0.0,
                        transactionCount = txs.size
                    )
                }
        }
    }

    override fun getDailyExpenses(days: Int): Flow<List<DailyExpense>> {
        return transactions.map { txList ->
            val expenses = txList.filter { it.type == TransactionType.EXPENSE }
                .groupBy { getDayKey(it.date) }
                .map { (day, txs) ->
                    DailyExpense(
                        date = day,
                        totalAmount = txs.sumOf { it.amount },
                        transactionCount = txs.size
                    )
                }
                .sortedByDescending { it.date }
                .take(days)
            expenses
        }
    }

    override fun getMonthlyComparison(months: Int): Flow<List<MonthlyComparison>> {
        return transactions.map { txList ->
            val expenses = txList.filter { it.type == TransactionType.EXPENSE }
                .groupBy { getMonthKey(it.date) }
                .map { (month, txs) ->
                    MonthlyComparison(
                        month = month,
                        totalAmount = txs.sumOf { it.amount },
                        transactionCount = txs.size
                    )
                }
                .sortedByDescending { it.month }
                .take(months)
            expenses
        }
    }

    override suspend fun syncWithData(): Result<Unit> = runCatching {
        // Firebase sync implementation
    }

    override suspend fun getLastSyncTime(): Long? = null

    override suspend fun clearLocalData(): Result<Unit> = runCatching {
        financeDao.deleteAllTransactions()
        financeDao.deleteAllBills()
        financeDao.deleteAllCategories()
    }

    private fun filterByPeriod(transactions: List<Transaction>, period: TimePeriod): List<Transaction> {
        val now = System.currentTimeMillis()
        return when (period) {
            TimePeriod.TODAY -> transactions.filter { isSameDay(it.date, now) }
            TimePeriod.WEEK -> transactions.filter { isSameWeek(it.date, now) }
            TimePeriod.MONTH -> transactions.filter { isSameMonth(it.date, now) }
            TimePeriod.YEAR -> transactions.filter { isSameYear(it.date, now) }
            TimePeriod.ALL -> transactions
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR) &&
               cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
    }

    private fun isSameWeek(time1: Long, time2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(java.util.Calendar.WEEK_OF_YEAR) == cal2.get(java.util.Calendar.WEEK_OF_YEAR) &&
               cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
    }

    private fun isSameMonth(time1: Long, time2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH) &&
               cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
    }

    private fun isSameYear(time1: Long, time2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
    }

    private fun getDayKey(timestamp: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH)+1}-${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    private fun getMonthKey(timestamp: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH)+1}"
    }
}
