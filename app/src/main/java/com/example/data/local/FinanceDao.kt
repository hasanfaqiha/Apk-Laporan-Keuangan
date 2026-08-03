package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) for finance operations with Room
 */
@Dao
interface FinanceDao {
    
    // ==================== TRANSACTION OPERATIONS ====================
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE type = :type AND isDeleted = 0 ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDeleteTransaction(id: String)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    // ==================== CATEGORY OPERATIONS ====================
    
    @Query("SELECT * FROM categories ORDER BY type, name")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories ORDER BY type, name")
    suspend fun getAllCategoriesDirect(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    // ==================== BILL OPERATIONS ====================
    
    @Query("SELECT * FROM bills WHERE isDeleted = 0 ORDER BY dueDate ASC")
    fun getAllBills(): Flow<List<BillEntity>>
    
    @Query("SELECT * FROM bills WHERE isDeleted = 0 ORDER BY dueDate ASC")
    suspend fun getAllBillsDirect(): List<BillEntity>
    
    @Query("SELECT * FROM bills WHERE isPaid = 0 AND dueDate BETWEEN :now AND :futureDate AND isDeleted = 0 ORDER BY dueDate ASC")
    fun getUpcomingBills(now: Long, futureDate: Long): Flow<List<BillEntity>>
    
    @Query("SELECT * FROM bills WHERE isPaid = 0 AND dueDate < :now AND isDeleted = 0 ORDER BY dueDate ASC")
    fun getOverdueBills(now: Long): Flow<List<BillEntity>>
    
    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: String): BillEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long
    
    @Update
    suspend fun updateBill(bill: BillEntity)
    
    @Query("UPDATE bills SET isPaid = 1, paidDate = :paidDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markBillAsPaid(id: String, paidDate: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE bills SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteBill(id: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun hardDeleteBill(id: String)

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()

    // ==================== SYNC OPERATIONS ====================
    
    @Query("SELECT MAX(lastSyncedAt) FROM transactions")
    suspend fun getLastTransactionSyncTime(): Long?
    
    @Query("SELECT MAX(lastSyncedAt) FROM bills")
    suspend fun getLastBillSyncTime(): Long?
    
    @Query("SELECT MAX(lastSyncedAt) FROM categories")
    suspend fun getLastCategorySyncTime(): Long?
}
