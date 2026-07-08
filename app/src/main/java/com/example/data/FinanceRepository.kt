package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val allBills: Flow<List<Bill>> = financeDao.getAllBills()
    val allCategories: Flow<List<Category>> = financeDao.getAllCategories()

    suspend fun insertTransaction(transaction: Transaction) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        financeDao.updateTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        financeDao.deleteTransactionById(id)
    }

    suspend fun insertBill(bill: Bill) {
        financeDao.insertBill(bill)
    }

    suspend fun updateBill(bill: Bill) {
        financeDao.updateBill(bill)
    }

    suspend fun deleteBillById(id: Int) {
        financeDao.deleteBillById(id)
    }

    suspend fun insertCategory(category: Category) {
        financeDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        financeDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        financeDao.deleteCategory(category)
    }
}
