package com.example.data.repository_impl

import com.example.domain.model.Bill
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.repository.FinanceRepository
import com.example.data.local.FinanceDao
import com.example.data.local.TransactionEntity
import com.example.data.local.BillEntity
import com.example.data.local.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(private val financeDao: FinanceDao) : FinanceRepository {

    override val transactions: Flow<List<Transaction>> = 
        financeDao.getAllTransactions().map { entities -> 
            entities.map { it.toDomain() } 
        }

    override val bills: Flow<List<Bill>> = 
        financeDao.getAllBills().map { entities -> 
            entities.map { it.toDomain() } 
        }

    override val categories: Flow<List<Category>> = 
        financeDao.getAllCategories().map { entities -> 
            entities.map { it.toDomain() } 
        }

    override suspend fun getTransactionsDirect(): List<Transaction> =
        financeDao.getAllTransactionsDirect().map { it.toDomain() }

    override suspend fun getBillsDirect(): List<Bill> =
        financeDao.getAllBillsDirect().map { it.toDomain() }

    override suspend fun getCategoriesDirect(): List<Category> =
        financeDao.getAllCategoriesDirect().map { it.toDomain() }

    override suspend fun insertTransaction(transaction: Transaction): Long =
        financeDao.insertTransaction(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) =
        financeDao.updateTransaction(transaction.toEntity())

    override suspend fun deleteTransactionById(id: Int) =
        financeDao.deleteTransactionById(id)

    override suspend fun insertBill(bill: Bill): Long =
        financeDao.insertBill(bill.toEntity())

    override suspend fun updateBill(bill: Bill) =
        financeDao.updateBill(bill.toEntity())

    override suspend fun deleteBillById(id: Int) =
        financeDao.deleteBillById(id)

    override suspend fun insertCategory(category: Category): Long =
        financeDao.insertCategory(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        financeDao.updateCategory(category.toEntity())

    override suspend fun deleteCategory(category: Category) =
        financeDao.deleteCategory(category.toEntity())
}

// Extension functions for mapping between domain and entity
fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    title = title,
    amount = amount,
    type = TransactionType.valueOf(type),
    accountType = AccountType.valueOf(accountType),
    category = category,
    dateMillis = dateMillis,
    note = note
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    title = title,
    amount = amount,
    type = type.name,
    accountType = accountType.name,
    category = category,
    dateMillis = dateMillis,
    note = note
)

fun BillEntity.toDomain(): Bill = Bill(
    id = id,
    title = title,
    amount = amount,
    dueDateMillis = dueDateMillis,
    isPaid = isPaid,
    category = category,
    note = note
)

fun Bill.toEntity(): BillEntity = BillEntity(
    id = id,
    title = title,
    amount = amount,
    dueDateMillis = dueDateMillis,
    isPaid = isPaid,
    category = category,
    note = note
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = CategoryType.valueOf(type)
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type.name
)

// Import domain enums
import com.example.domain.model.TransactionType
import com.example.domain.model.AccountType
import com.example.domain.model.CategoryType
