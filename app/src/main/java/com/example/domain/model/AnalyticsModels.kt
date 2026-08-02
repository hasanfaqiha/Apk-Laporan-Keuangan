package com.example.domain.model

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
    val categoryIcon: String? = null,
    val categoryColor: String? = null,
    val amount: Double,
    val percentage: Double = 0.0,
    val budgetLimit: Double? = null,
    val isOverBudget: Boolean = false
)
