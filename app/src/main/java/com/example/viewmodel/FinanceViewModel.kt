package com.example.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Bill
import com.example.data.Category
import com.example.data.FinanceDatabase
import com.example.data.FinanceRepository
import com.example.data.Transaction
import com.example.data.FirebaseSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    val syncManager = FirebaseSyncManager(repository)

    // List of transactions
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // List of bills
    val bills: StateFlow<List<Bill>> = repository.allBills
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // List of categories
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic App Theme Preference State
    val selectedTheme = MutableStateFlow("SYSTEM") // SYSTEM, LIGHT, DARK

    fun setTheme(theme: String, context: Context) {
        selectedTheme.value = theme
        val prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_theme", theme).apply()
    }

    fun loadTheme(context: Context) {
        val prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
        selectedTheme.value = prefs.getString("selected_theme", "SYSTEM") ?: "SYSTEM"
    }

    init {
        viewModelScope.launch {
            try {
                // Pre-populate default categories if empty
                val currentCats = repository.allCategories.first()
                if (currentCats.isEmpty()) {
                    val defaultExpenses = listOf("Makanan & Minuman", "Transportasi", "Sewa & Tagihan", "Belanja", "Hiburan", "Lain-lain")
                    val defaultIncomes = listOf("Gaji", "Investasi", "Bonus", "Hadiah", "Lain-lain")
                    defaultExpenses.forEach { name ->
                        repository.insertCategory(Category(name = name, type = "EXPENSE"))
                    }
                    defaultIncomes.forEach { name ->
                        repository.insertCategory(Category(name = name, type = "INCOME"))
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            }
            // Check and generate credit card bills if applicable
            checkAndGenerateCreditCardBills()
        }
    }

    // Computed balances and totals
    val financeSummary: StateFlow<FinanceSummary> = combine(transactions, bills) { transList, billList ->
        var cashIncome = 0.0
        var cashExpense = 0.0
        var bankIncome = 0.0
        var bankExpense = 0.0
        var creditCardDebt = 0.0
        var totalIncome = 0.0
        var totalExpense = 0.0

        val categoryExpMap = mutableMapOf<String, Double>()
        val categoryIncMap = mutableMapOf<String, Double>()

        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val calendar = Calendar.getInstance()

        for (t in transList) {
            val amt = t.amount
            if (t.type == "INCOME") {
                if (t.accountType == "CASH") {
                    cashIncome += amt
                } else if (t.accountType == "BANK") {
                    bankIncome += amt
                }
                categoryIncMap[t.category] = (categoryIncMap[t.category] ?: 0.0) + amt
                totalIncome += amt
            } else if (t.type == "EXPENSE") {
                if (t.accountType == "CASH") {
                    cashExpense += amt
                } else if (t.accountType == "BANK") {
                    bankExpense += amt
                } else if (t.accountType == "CREDIT_CARD") {
                    calendar.timeInMillis = t.dateMillis
                    if (calendar.get(Calendar.YEAR) == currentYear && calendar.get(Calendar.MONTH) == currentMonth) {
                        creditCardDebt += amt
                    }
                }
                categoryExpMap[t.category] = (categoryExpMap[t.category] ?: 0.0) + amt
                totalExpense += amt
            } else if (t.type == "WITHDRAWAL") {
                // Tarik tunai: mengurangi saldo bank, menambah saldo cash
                bankExpense += amt
                cashIncome += amt
            } else if (t.type == "DEPOSIT") {
                // Setor tunai: mengurangi saldo cash, menambah saldo bank
                cashExpense += amt
                bankIncome += amt
            }
        }

        val cashOnHand = cashIncome - cashExpense
        val bankBalance = bankIncome - bankExpense
        val totalBalance = cashOnHand + bankBalance

        // Add credit card debt to total expenses
        totalExpense += creditCardDebt

        // Upcoming unpaid bills due in next 3 days
        val now = System.currentTimeMillis()
        val threeDaysInMs = 3 * 24 * 60 * 60 * 1000L
        val upcomingBillsCount = billList.count { !it.isPaid && (it.dueDateMillis - now in 0..threeDaysInMs) }
        val overdueBillsCount = billList.count { !it.isPaid && (it.dueDateMillis < now) }

        FinanceSummary(
            cashOnHand = cashOnHand,
            bankBalance = bankBalance,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            categoryExpenses = categoryExpMap,
            categoryIncomes = categoryIncMap,
            upcomingBillsCount = upcomingBillsCount,
            overdueBillsCount = overdueBillsCount,
            creditCardDebt = creditCardDebt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceSummary()
    )

    // Transaction Actions
    fun addTransaction(title: String, amount: Double, type: String, accountType: String, category: String, dateMillis: Long, note: String) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title,
                amount = amount,
                type = type,
                accountType = accountType,
                category = category,
                dateMillis = dateMillis,
                note = note
            )
            val newId = repository.insertTransaction(transaction)
            if (syncManager.isLoggedIn) {
                syncManager.syncTransactionToCloud(transaction.copy(id = newId.toInt()))
            }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
            if (syncManager.isLoggedIn) {
                syncManager.deleteTransactionFromCloud(id)
            }
        }
    }

    // Bill Actions
    fun addBill(title: String, amount: Double, dueDateMillis: Long, category: String, note: String, context: Context? = null) {
        viewModelScope.launch {
            val bill = Bill(
                title = title,
                amount = amount,
                dueDateMillis = dueDateMillis,
                category = category,
                note = note
            )
            val newId = repository.insertBill(bill)
            if (syncManager.isLoggedIn) {
                syncManager.syncBillToCloud(bill.copy(id = newId.toInt()))
            }
            context?.let { triggerBillReminders(it) }
        }
    }

    fun toggleBillPaid(bill: Bill, context: Context? = null) {
        viewModelScope.launch {
            val updated = bill.copy(isPaid = !bill.isPaid)
            repository.updateBill(updated)
            if (syncManager.isLoggedIn) {
                syncManager.syncBillToCloud(updated)
            }
            context?.let { triggerBillReminders(it) }
        }
    }

    fun deleteBill(id: Int) {
        viewModelScope.launch {
            repository.deleteBillById(id)
            if (syncManager.isLoggedIn) {
                syncManager.deleteBillFromCloud(id)
            }
        }
    }

    // Trigger Native System Notifications for Upcoming Unpaid Bills
    fun triggerBillReminders(context: Context) {
        viewModelScope.launch {
            val list = repository.allBills.stateIn(viewModelScope).value
            if (list.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            val threeDaysInMs = 3 * 24 * 60 * 60 * 1000L
            val channelId = "bill_reminders_channel"

            // Create notification channel for Android O+
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Pengingat Tagihan",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi pengingat jatuh tempo tagihan"
                }
                manager.createNotificationChannel(channel)
            }

            list.forEachIndexed { index, bill ->
                if (!bill.isPaid) {
                    val diff = bill.dueDateMillis - now
                    val isOverdue = diff < 0
                    val isDueSoon = diff in 0..threeDaysInMs

                    if (isOverdue || isDueSoon) {
                        val title = if (isOverdue) "⚠️ Tagihan Melewati Batas Tempo!" else "⏰ Pengingat Tagihan Terdekat"
                        val formattedAmount = formatRupiah(bill.amount)
                        val content = if (isOverdue) {
                            "Tagihan '${bill.title}' sebesar $formattedAmount telah melewati jatuh tempo!"
                        } else {
                            "Tagihan '${bill.title}' sebesar $formattedAmount jatuh tempo dalam waktu dekat."
                        }

                        val builder = NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(android.R.drawable.ic_popup_reminder)
                            .setContentTitle(title)
                            .setContentText(content)
                            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)

                        // Unique ID for each bill notification
                        manager.notify(bill.id, builder.build())
                    }
                }
            }
        }
    }

    // CC billing logic
    fun checkAndGenerateCreditCardBills() {
        viewModelScope.launch {
            try {
                val transList = repository.allTransactions.first()
                val billList = repository.allBills.first()
                
                val ccExpenses = transList.filter { t ->
                    t.type == "EXPENSE" && t.accountType == "CREDIT_CARD"
                }
                if (ccExpenses.isEmpty()) return@launch

                val calendar = Calendar.getInstance()
                val grouped = ccExpenses.groupBy { t ->
                    calendar.timeInMillis = t.dateMillis
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH) // 0-indexed
                    Pair(year, month)
                }

                val currentCalendar = Calendar.getInstance()
                val currentYear = currentCalendar.get(Calendar.YEAR)
                val currentMonth = currentCalendar.get(Calendar.MONTH)

                val monthNames = arrayOf(
                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                )

                for ((yearMonth, transactionsInMonth) in grouped) {
                    val (year, month) = yearMonth
                    
                    // Check if we are at least in the next month
                    val isPastMonth = (currentYear > year) || (currentYear == year && currentMonth > month)
                    if (!isPastMonth) continue

                    val monthName = monthNames[month]
                    val billTitle = "Tagihan Kartu Kredit - $monthName $year"

                    // Check if this bill already exists
                    val alreadyExists = billList.any { b -> b.title == billTitle }
                    if (!alreadyExists) {
                        val totalAmount = transactionsInMonth.sumOf { it.amount }
                        if (totalAmount > 0) {
                            val dueCal = Calendar.getInstance()
                            dueCal.set(Calendar.YEAR, year)
                            dueCal.set(Calendar.MONTH, month)
                            dueCal.add(Calendar.MONTH, 1) // Add 1 month to get next month
                            dueCal.set(Calendar.DAY_OF_MONTH, 10) // Set due date to 10th
                            dueCal.set(Calendar.HOUR_OF_DAY, 12)
                            dueCal.set(Calendar.MINUTE, 0)
                            dueCal.set(Calendar.SECOND, 0)

                            repository.insertBill(
                                Bill(
                                    title = billTitle,
                                    amount = totalAmount,
                                    dueDateMillis = dueCal.timeInMillis,
                                    isPaid = false,
                                    category = "Sewa & Tagihan",
                                    note = "Akumulasi belanja Kartu Kredit selama bulan $monthName $year"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Category actions
    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            val category = Category(name = name, type = type)
            val newId = repository.insertCategory(category)
            if (syncManager.isLoggedIn) {
                syncManager.syncCategoryToCloud(category.copy(id = newId.toInt()))
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
            if (syncManager.isLoggedIn) {
                syncManager.syncCategoryToCloud(category)
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            if (syncManager.isLoggedIn) {
                syncManager.deleteCategoryFromCloud(category.id)
            }
        }
    }
}

// Data class to wrap calculated metrics
data class FinanceSummary(
    val cashOnHand: Double = 0.0,
    val bankBalance: Double = 0.0,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryExpenses: Map<String, Double> = emptyMap(),
    val categoryIncomes: Map<String, Double> = emptyMap(),
    val upcomingBillsCount: Int = 0,
    val overdueBillsCount: Int = 0,
    val creditCardDebt: Double = 0.0
)

// Helper to format currency to Indonesian Rupiah
fun formatRupiah(amount: Double): String {
    return try {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        format.format(amount).replace("Rp", "Rp ").replace(",00", "")
    } catch (e: Exception) {
        "Rp " + String.format("%,.0f", amount)
    }
}

// ViewModel Factory
class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
