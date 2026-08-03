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
import com.example.domain.model.createFinanceSummary
import com.example.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date

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
        financeDao.softDeleteTransaction(id)
    }

    override suspend fun deleteAllTransactions(): Result<Unit> = runCatching {
        financeDao.deleteAllTransactions()
    }

    override suspend fun getTransactionById(id: String): Transaction? =
        financeDao.getTransactionById(id)?.toDomain()

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        financeDao.getTransactionsByType(type.name).map { entities -> entities.map { it.toDomain() } }

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
        financeDao.softDeleteBill(id)
    }

    override suspend fun getBillById(id: String): Bill? =
        financeDao.getBillById(id)?.toDomain()

    override fun getUpcomingBills(daysAhead: Int): Flow<List<Bill>> {
        val now = System.currentTimeMillis()
        val futureDate = now + daysAhead * 24L * 60 * 60 * 1000
        return financeDao.getUpcomingBills(now, futureDate).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getOverdueBills(): Flow<List<Bill>> =
        financeDao.getOverdueBills(System.currentTimeMillis()).map { entities -> entities.map { it.toDomain() } }

    override suspend fun markBillAsPaid(id: String): Result<Unit> = runCatching {
        val bill = financeDao.getBillById(id) ?: throw IllegalArgumentException("Bill not found")
        val updatedBill = bill.copy(
            isPaid = true,
            paidDate = System.currentTimeMillis()
        )
        financeDao.updateBill(updatedBill)
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
        financeDao.deleteCategoryById(id)
    }

    override suspend fun getCategoryById(id: String): Category? =
        financeDao.getCategoryById(id)?.toDomain()

    override suspend fun insertDefaultCategories(): Result<Unit> = runCatching {
        // Default categories will be inserted via use case
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
        financeDao.getCategoriesByType(type.name).map { entities -> entities.map { it.toDomain() } }

    override fun getFinanceSummary(timePeriod: TimePeriod): Flow<FinanceSummary> {
        return combine(transactions, bills) { txList, billList ->
            createFinanceSummary(txList, billList, timePeriod)
        }
    }

    override fun getCategoryExpenses(timePeriod: TimePeriod): Flow<Map<String, CategoryExpense>> {
        return transactions.map { txList ->
            val filtered = filterByPeriod(txList, timePeriod)
            val expenses = filtered.filter { it.type == TransactionType.EXPENSE }
            val totalExpense = expenses.sumOf { it.amount }

            expenses.groupBy { it.category.id }
                .mapValues { (catId, txs) ->
                    val firstTx = txs.firstOrNull()
                    val category = firstTx?.category
                    val categoryAmount = txs.sumOf { it.amount }
                    CategoryExpense(
                        categoryId = catId,
                        categoryName = category?.name ?: "Unknown",
                        categoryIcon = category?.icon,
                        categoryColor = category?.color,
                        amount = categoryAmount,
                        percentage = if (totalExpense > 0) (categoryAmount / totalExpense) * 100 else 0.0,
                        budgetLimit = category?.budgetLimit,
                        isOverBudget = category?.budgetLimit?.let { categoryAmount > it } ?: false
                    )
                }
        }
    }

    override fun getDailyExpenses(days: Int): Flow<List<DailyExpense>> {
        return transactions.map { txList ->
            txList
                .filter { it.type == TransactionType.EXPENSE && !it.isDeleted }
                .groupBy { getDayKey(it.date) }
                .map { (dayEpoch, txs) ->
                    DailyExpense(
                        date = dayEpoch,
                        amount = txs.sumOf { it.amount },
                        transactionCount = txs.size
                    )
                }
                .sortedByDescending { it.date }
                .take(days)
        }
    }

    override fun getMonthlyComparison(months: Int): Flow<List<MonthlyComparison>> {
        return transactions.map { txList ->
            txList
                .filter { !it.isDeleted }
                .groupBy { getMonthKey(it.date) }
                .map { (monthEpoch, txs) ->
                    val cal = Calendar.getInstance().apply { timeInMillis = monthEpoch }
                    val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                    val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    MonthlyComparison(
                        month = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}",
                        monthNumber = cal.get(Calendar.MONTH) + 1,
                        year = cal.get(Calendar.YEAR),
                        income = income,
                        expense = expense,
                        netSavings = income - expense
                    )
                }
                .sortedByDescending { it.monthNumber }
                .take(months)
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
        val now = Date()
        return when (period) {
            TimePeriod.TODAY -> transactions.filter { isSameDay(it.date, now) }
            TimePeriod.THIS_WEEK -> transactions.filter { isSameWeek(it.date, now) }
            TimePeriod.THIS_MONTH -> transactions.filter { isSameMonth(it.date, now) }
            TimePeriod.THIS_QUARTER -> transactions.filter { isSameQuarter(it.date, now) }
            TimePeriod.THIS_YEAR -> transactions.filter { isSameYear(it.date, now) }
            TimePeriod.CUSTOM -> transactions
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    private fun isSameWeek(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    private fun isSameMonth(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    private fun isSameYear(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    private fun isSameQuarter(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            (cal1.get(Calendar.MONTH) / 3) == (cal2.get(Calendar.MONTH) / 3)
    }

    private fun getDayKey(date: Date): Long {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getMonthKey(date: Date): Long {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
