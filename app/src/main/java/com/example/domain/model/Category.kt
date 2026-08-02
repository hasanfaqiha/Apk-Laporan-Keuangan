package com.example.domain.model

import java.util.UUID

/**
 * Domain model for expense/income categories.
 */
data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "attach_money",
    val color: String = "#4F46E5",
    val type: CategoryType = CategoryType.EXPENSE,
    val isSystem: Boolean = false,
    val budgetLimit: Double? = null,
    val spentAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CategoryType {
    INCOME,
    EXPENSE
}

/**
 * Predefined system categories for quick setup
 */
object DefaultCategories {
    val expenseCategories = listOf(
        Category(id = "food", name = "Makanan & Minuman", icon = "restaurant", color = "#FF9800", type = CategoryType.EXPENSE, isSystem = true),
        Category(id = "transport", name = "Transportasi", icon = "directions_car", color = "#2196F3", type = CategoryType.EXPENSE, isSystem = true),
        Category(id = "bills", name = "Sewa & Tagihan", icon = "receipt", color = "#9C27B0", type = CategoryType.EXPENSE, isSystem = true),
        Category(id = "shopping", name = "Belanja", icon = "shopping_bag", color = "#E91E63", type = CategoryType.EXPENSE, isSystem = true),
        Category(id = "entertainment", name = "Hiburan", icon = "movie", color = "#F44336", type = CategoryType.EXPENSE, isSystem = true),
        Category(id = "health", name = "Kesehatan", icon = "favorite", color = "#4CAF50", type = CategoryType.EXPENSE, isSystem = true),
        Category(id = "education", name = "Pendidikan", icon = "school", color = "#3F51B5", type = CategoryType.EXPENSE, isSystem = true),
        Category(id = "others_expense", name = "Lainnya", icon = "more_horiz", color = "#9E9E9E", type = CategoryType.EXPENSE, isSystem = true)
    )

    val incomeCategories = listOf(
        Category(id = "salary", name = "Gaji", icon = "work", color = "#4CAF50", type = CategoryType.INCOME, isSystem = true),
        Category(id = "investment", name = "Investasi", icon = "trending_up", color = "#1976D2", type = CategoryType.INCOME, isSystem = true),
        Category(id = "bonus", name = "Bonus", icon = "card_giftcard", color = "#FFC107", type = CategoryType.INCOME, isSystem = true),
        Category(id = "others_income", name = "Lainnya", icon = "more_horiz", color = "#9E9E9E", type = CategoryType.INCOME, isSystem = true)
    )

    fun getAll(): List<Category> = expenseCategories + incomeCategories
}
