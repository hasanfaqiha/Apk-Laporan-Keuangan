package com.example.domain.model

data class DailyExpense(
    val date: String,
    val totalAmount: Double,
    val transactionCount: Int
)
