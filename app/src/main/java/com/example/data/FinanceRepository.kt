package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FinanceRepository(private val financeDao: FinanceDao) {
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val allBills: Flow<List<Bill>> = financeDao.getAllBills()
    val allCategories: Flow<List<Category>> = financeDao.getAllCategories()
    val allRecurringRules: Flow<List<RecurringRule>> = financeDao.getAllRecurringRules()
    val allBudgets: Flow<List<Budget>> = financeDao.getAllBudgets()

    suspend fun getAllTransactionsDirect(): List<Transaction> {
        return financeDao.getAllTransactionsDirect()
    }

    suspend fun getAllBillsDirect(): List<Bill> {
        return financeDao.getAllBillsDirect()
    }

    suspend fun getAllCategoriesDirect(): List<Category> {
        return financeDao.getAllCategoriesDirect()
    }

    suspend fun getTransactionById(id: Int): Transaction? = financeDao.getTransactionById(id)
    suspend fun getBillById(id: Int): Bill? = financeDao.getBillById(id)
    suspend fun getCategoryById(id: Int): Category? = financeDao.getCategoryById(id)

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

    // --- Recurring rules ---
    suspend fun getAllRecurringRulesDirect(): List<RecurringRule> = financeDao.getAllRecurringRulesDirect()

    suspend fun insertRecurringRule(rule: RecurringRule): Long = financeDao.insertRecurringRule(rule)

    suspend fun updateRecurringRule(rule: RecurringRule) = financeDao.updateRecurringRule(rule)

    suspend fun deleteRecurringRule(id: Int) = financeDao.deleteRecurringRule(id)

    // --- Budgets ---
    suspend fun getAllBudgetsDirect(): List<Budget> = financeDao.getAllBudgetsDirect()

    suspend fun insertBudget(budget: Budget): Long = financeDao.insertBudget(budget)

    suspend fun updateBudget(budget: Budget) = financeDao.updateBudget(budget)

    suspend fun deleteBudget(id: Int) = financeDao.deleteBudget(id)

    // --- Recurring transaction generation ---
    /**
     * Creates real [Transaction]s for every recurring rule whose next run is
     * due (up to [maxPerRule] catch-up occurrences per rule) and advances the
     * rules' next run. Idempotent: rules only fire again after their scheduled
     * date actually passes, so relaunching the app never duplicates entries.
     * Returns the transactions created.
     */
    suspend fun generateDueRecurringTransactions(
        now: Long = System.currentTimeMillis(),
        maxPerRule: Int = 36
    ): List<Transaction> {
        val rules = financeDao.getAllRecurringRulesDirect()
        val occurrences = computeRecurringOccurrences(rules, now, maxPerRule)
        if (occurrences.isEmpty()) return emptyList()

        val created = mutableListOf<Transaction>()
        val nextRuns = mutableMapOf<Int, Long>()
        for (occ in occurrences) {
            val tx = Transaction(
                id = kotlin.random.Random.nextInt(1000000, 2_000_000_000),
                title = occ.rule.title,
                amount = occ.rule.amount,
                type = occ.rule.type,
                accountType = occ.rule.accountType,
                category = occ.rule.category,
                dateMillis = occ.dateMillis,
                note = if (occ.rule.note.isBlank()) "Transaksi berulang otomatis" else occ.rule.note,
                updatedAt = now
            )
            financeDao.insertTransaction(tx)
            created.add(tx)
            nextRuns[occ.rule.id] = nextRecurringRunMillis(occ.rule, occ.dateMillis)
        }
        val rulesById = rules.associateBy { it.id }
        nextRuns.forEach { (ruleId, nextRun) ->
            val rule = rulesById[ruleId] ?: return@forEach
            financeDao.updateRecurringRule(rule.copy(nextRunMillis = nextRun))
        }
        return created
    }

    // Generate credit-card bills for past months with CC spending
    suspend fun generateCreditCardBills() {
        val transList = financeDao.getAllTransactionsDirect()
        val billList = financeDao.getAllBillsDirect()
        val newBills = computeCreditCardBills(transList, billList)
        newBills.forEach { financeDao.insertBill(it) }
    }
}

/** One scheduled occurrence of a [RecurringRule]. */
data class RecurringOccurrence(val rule: RecurringRule, val dateMillis: Long)

/**
 * Pure scheduling helper: lists the occurrences of each active rule that fall
 * on or before [now], ordered by date. Never generates more than [maxPerRule]
 * catch-up occurrences per rule so a long-disabled rule cannot flood the
 * ledger.
 */
fun computeRecurringOccurrences(
    rules: List<RecurringRule>,
    now: Long,
    maxPerRule: Int = 36
): List<RecurringOccurrence> {
    val result = mutableListOf<RecurringOccurrence>()
    for (rule in rules) {
        if (!rule.isActive || rule.amount <= 0) continue
        var run = if (rule.nextRunMillis > 0) rule.nextRunMillis else rule.startDateMillis
        var generated = 0
        while (run <= now && generated < maxPerRule) {
            result.add(RecurringOccurrence(rule, run))
            run = nextRecurringRunMillis(rule, run)
            generated++
        }
    }
    return result
}

/**
 * Computes the next scheduled run after [afterMillis] for [rule]. Monthly and
 * yearly runs anchor on the rule's start date (e.g. a rule started on the 31st
 * runs on the 28th/29th/30th of shorter months) instead of drifting.
 */
fun nextRecurringRunMillis(rule: RecurringRule, afterMillis: Long): Long {
    when (rule.frequency) {
        "WEEKLY" -> return Calendar.getInstance().apply {
            timeInMillis = afterMillis
            add(Calendar.DAY_OF_MONTH, 7)
        }.timeInMillis
        "MONTHLY" -> return advanceAnchored(rule, afterMillis, monthsToAdd = 1)
        "YEARLY" -> return advanceAnchored(rule, afterMillis, monthsToAdd = 12)
        else -> return Calendar.getInstance().apply {
            timeInMillis = afterMillis
            add(Calendar.DAY_OF_MONTH, 1) // DAILY & fallback
        }.timeInMillis
    }
}

private fun advanceAnchored(rule: RecurringRule, afterMillis: Long, monthsToAdd: Int): Long {
    val startCal = Calendar.getInstance().apply { timeInMillis = rule.startDateMillis }
    val anchorDay = startCal.get(Calendar.DAY_OF_MONTH)
    val anchorHour = startCal.get(Calendar.HOUR_OF_DAY)
    val anchorMinute = startCal.get(Calendar.MINUTE)

    val afterCal = Calendar.getInstance().apply { timeInMillis = afterMillis }
    val totalMonths = afterCal.get(Calendar.YEAR) * 12 + afterCal.get(Calendar.MONTH) + monthsToAdd
    val year = totalMonths / 12
    val month = totalMonths % 12

    val cal = Calendar.getInstance()
    cal.clear()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month)
    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, minOf(anchorDay, lastDay))
    cal.set(Calendar.HOUR_OF_DAY, anchorHour)
    cal.set(Calendar.MINUTE, anchorMinute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// --- Budget helpers (pure, unit-testable) ---

/** Sum of EXPENSE transactions of one calendar month, grouped by category. */
fun monthExpenseTotalsByCategory(
    transactions: List<Transaction>,
    year: Int,
    month: Int // 0-based, Calendar convention
): Map<String, Double> {
    val cal = Calendar.getInstance()
    return transactions
        .filter { it.type == "EXPENSE" }
        .filter { t ->
            cal.timeInMillis = t.dateMillis
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }
        .groupingBy { it.category }
        .fold(0.0) { acc, t -> acc + t.amount }
}

/** Result of comparing one budget against its actual spend. */
data class BudgetUsage(val budget: Budget, val spent: Double, val exceeded: Boolean)

/** Cross-checks every [Budget] against current-month expenses. */
fun computeBudgetUsages(
    transactions: List<Transaction>,
    budgets: List<Budget>,
    year: Int = Calendar.getInstance().get(Calendar.YEAR),
    month: Int = Calendar.getInstance().get(Calendar.MONTH)
): List<BudgetUsage> {
    val totals = monthExpenseTotalsByCategory(transactions, year, month)
    return budgets.map { b ->
        val spent = totals[b.category] ?: 0.0
        BudgetUsage(b, spent, b.monthlyLimit > 0 && spent >= b.monthlyLimit)
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
