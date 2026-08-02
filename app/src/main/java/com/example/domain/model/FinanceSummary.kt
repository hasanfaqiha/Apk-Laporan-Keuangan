package com.example.domain.model

import java.util.Date

/**
 * Domain model for financial summary and analytics.
 */
data class FinanceSummary(
    val cashOnHand: Double = 0.0,
    val bankBalance: Double = 0.0,
    val eWalletBalance: Double = 0.0,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val categoryExpenses: Map<String, CategoryExpense> = emptyMap(),
    val categoryIncomes: Map<String, Double> = emptyMap(),
    val upcomingBillsCount: Int = 0,
    val overdueBillsCount: Int = 0,
    val creditCardDebt: Double = 0.0,
    val monthlyBudget: Double? = null,
    val budgetUsed: Double = 0.0,
    val dailyAverageExpense: Double = 0.0,
    val projectedMonthEndExpense: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Daily expense for trend analysis
 */
data class DailyExpense(
    val date: Long,
    val amount: Double,
    val transactionCount: Int = 0
)

/**
 * Monthly comparison for year-over-year or month-over-month analysis
 */
data class MonthlyComparison(
    val month: String,
    val monthNumber: Int,
    val year: Int,
    val income: Double,
    val expense: Double,
    val netSavings: Double
)

/**
 * Category expense with percentage calculation
 */
data class CategoryExpense(
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val percentage: Double = 0.0,
    val budgetLimit: Double? = null,
    val isOverBudget: Boolean = false
)

/**
 * Time period filter for financial reports
 */
enum class TimePeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_QUARTER,
    THIS_YEAR,
    CUSTOM
}

/**
 * Calculate date range for a given time period
 */
fun TimePeriod.getDateRange(): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    return when (this) {
        TimePeriod.TODAY -> {
            val startOfDay = now - (now % (1000 * 60 * 60 * 24))
            startOfDay to now
        }
        TimePeriod.THIS_WEEK -> {
            val calendar = java.util.Calendar.getInstance()
            calendar.time = Date(now)
            calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.timeInMillis to now
        }
        TimePeriod.THIS_MONTH -> {
            val calendar = java.util.Calendar.getInstance()
            calendar.time = Date(now)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.timeInMillis to now
        }
        TimePeriod.THIS_QUARTER -> {
            val calendar = java.util.Calendar.getInstance()
            calendar.time = Date(now)
            val month = calendar.get(java.util.Calendar.MONTH)
            val quarterStartMonth = (month / 3) * 3
            calendar.set(java.util.Calendar.MONTH, quarterStartMonth)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.timeInMillis to now
        }
        TimePeriod.THIS_YEAR -> {
            val calendar = java.util.Calendar.getInstance()
            calendar.time = Date(now)
            calendar.set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.timeInMillis to now
        }
        TimePeriod.CUSTOM -> 0L to now
    }
}

/**
 * Create a FinanceSummary from transactions and bills
 */
fun createFinanceSummary(
    transactions: List<Transaction>,
    bills: List<Bill>,
    timePeriod: TimePeriod = TimePeriod.THIS_MONTH
): FinanceSummary {
    val (startDate, endDate) = timePeriod.getDateRange()
    
    val filteredTransactions = transactions.filter { 
        it.date.time in startDate..endDate && !it.isDeleted 
    }
    
    val cashTransactions = filteredTransactions.filter { it.accountType == AccountType.CASH }
    val bankTransactions = filteredTransactions.filter { it.accountType == AccountType.BANK_ACCOUNT }
    val eWalletTransactions = filteredTransactions.filter { it.accountType == AccountType.E_WALLET }
    val creditCardTransactions = filteredTransactions.filter { it.accountType == AccountType.CREDIT_CARD }
    
    val incomeTransactions = filteredTransactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.TRANSFER_IN }
    val expenseTransactions = filteredTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER_OUT }
    
    val totalIncome = incomeTransactions.sumOf { it.amount }
    val totalExpense = expenseTransactions.sumOf { it.amount }
    
    val categoryExpenseMap = expenseTransactions
        .groupBy { it.category }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    
    val categoryExpensesWithPercentage = categoryExpenseMap.map { (category, amount) ->
        val percentage = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
        CategoryExpense(
            categoryId = category.id,
            categoryName = category.name,
            amount = amount,
            percentage = percentage,
            budgetLimit = category.budgetLimit,
            isOverBudget = category.budgetLimit?.let { amount > it } ?: false
        )
    }.associateBy { it.categoryId }
    
    val activeBills = bills.filter { !it.isDeleted }
    val upcomingBills = activeBills.filter { !it.isPaid && it.getStatus() == BillStatus.UPCOMING }
    val overdueBills = activeBills.filter { !it.isPaid && it.getStatus() == BillStatus.OVERDUE }
    
    val creditCardDebt = creditCardTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (netSavings / totalIncome) * 100 else 0.0
    
    val daysElapsed = ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    val dailyAverageExpense = totalExpense / daysElapsed
    val daysInMonth = java.util.Calendar.getInstance().getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val projectedMonthEndExpense = dailyAverageExpense * daysInMonth
    
    return FinanceSummary(
        cashOnHand = cashTransactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.TRANSFER_IN }
            .sumOf { it.amount } - 
            cashTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER_OUT }
            .sumOf { it.amount },
        bankBalance = bankTransactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.TRANSFER_IN }
            .sumOf { it.amount } - 
            bankTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER_OUT }
            .sumOf { it.amount },
        eWalletBalance = eWalletTransactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.TRANSFER_IN }
            .sumOf { it.amount } - 
            eWalletTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.TRANSFER_OUT }
            .sumOf { it.amount },
        totalBalance = netSavings,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        netSavings = netSavings,
        savingsRate = savingsRate,
        categoryExpenses = categoryExpensesWithPercentage,
        upcomingBillsCount = upcomingBills.size,
        overdueBillsCount = overdueBills.size,
        creditCardDebt = creditCardDebt,
        dailyAverageExpense = dailyAverageExpense,
        projectedMonthEndExpense = projectedMonthEndExpense,
        lastUpdated = System.currentTimeMillis()
    )
}
