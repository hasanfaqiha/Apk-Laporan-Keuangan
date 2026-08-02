package com.example.domain.model

data class CategoryExpense(
    val categoryId: String,
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Double,
    val transactionCount: Int
)
