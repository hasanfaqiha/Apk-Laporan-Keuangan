package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FinanceRepository(private val financeDao: FinanceDao) {
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val allBills: Flow<List<Bill>> = financeDao.getAllBills()
    val allCategories: Flow<List<Category>> = financeDao.getAllCategories()

    suspend fun getAllTransactionsDirect(): List<Transaction> {
        return financeDao.getAllTransactionsDirect()
    }

    suspend fun getAllBillsDirect(): List<Bill> {
        return financeDao.getAllBillsDirect()
    }

    suspend fun getAllCategoriesDirect(): List<Category> {
        return financeDao.getAllCategoriesDirect()
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return financeDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        financeDao.updateTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        financeDao.deleteTransactionById(id)
    }

    suspend fun insertBill(bill: Bill): Long {
        return financeDao.insertBill(bill)
    }

    suspend fun updateBill(bill: Bill) {
        financeDao.updateBill(bill)
    }

    suspend fun deleteBillById(id: Int) {
        financeDao.deleteBillById(id)
    }

    suspend fun insertCategory(category: Category): Long {
        return financeDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        financeDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        financeDao.deleteCategory(category)
    }

    // Generate credit-card bills for past months with CC spending
    suspend fun generateCreditCardBills() {
        val transList = financeDao.getAllTransactionsDirect()
        val billList = financeDao.getAllBillsDirect()
        val newBills = computeCreditCardBills(transList, billList)
        newBills.forEach { financeDao.insertBill(it) }
    }
}

// Compute CC bills that still need to be created (shared by UI & background worker)
fun computeCreditCardBills(transList: List<Transaction>, billList: List<Bill>): List<Bill> {
    val ccExpenses = transList.filter { it.type == "EXPENSE" && it.accountType == "CREDIT_CARD" }
    if (ccExpenses.isEmpty()) return emptyList()

    val calendar = Calendar.getInstance()
    val grouped = ccExpenses.groupBy { t ->
        calendar.timeInMillis = t.dateMillis
        Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    val currentCalendar = Calendar.getInstance()
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH)

    val monthNames = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    val result = mutableListOf<Bill>()
    for ((yearMonth, transactionsInMonth) in grouped) {
        val (year, month) = yearMonth
        val isPastMonth = currentYear > year || (currentYear == year && currentMonth > month)
        if (!isPastMonth) continue

        val monthName = monthNames[month]
        val billTitle = "Tagihan Kartu Kredit - $monthName $year"
        if (billList.any { it.title == billTitle }) continue

        val totalAmount = transactionsInMonth.sumOf { it.amount }
        if (totalAmount <= 0) continue

        val dueCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        result.add(
            Bill(
                id = kotlin.random.Random.nextInt(1000000, 2_000_000_000),
                title = billTitle,
                amount = totalAmount,
                dueDateMillis = dueCal.timeInMillis,
                isPaid = false,
                category = "Sewa & Tagihan",
                note = "Akumulasi belanja Kartu Kredit selama bulan $monthName $year"
            )
        )
    }
    return result
}
