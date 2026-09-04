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
import com.example.data.Budget
import com.example.data.Category
import com.example.data.FinanceDatabase
import com.example.data.FinanceRepository
import com.example.data.RecurringRule
import com.example.data.Transaction
import com.example.data.FirebaseSyncManager
import com.example.data.computeBudgetUsages
import com.example.data.computeCreditCardBills
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    val syncManager = FirebaseSyncManager(repository)

    val isLoggedIn = MutableStateFlow(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null)
    val currentUserEmail = MutableStateFlow(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
    val hasSkippedAuth = MutableStateFlow(false)

    // State for the automatic sync-retry loop: when a cloud write fails while
    // offline, we keep retrying a full sync (with back-off) until it succeeds,
    // so data added on this phone eventually reaches the cloud and the other
    // device, without the user having to tap "sync" manually.
    private var pendingSyncRetry = false
    private var autoSyncAttempts = 0
    private var autoSyncJob: Job? = null

    init {
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            isLoggedIn.value = user != null
            currentUserEmail.value = user?.email
            if (user != null) {
                syncManager.startRealtimeSync(viewModelScope)
            } else {
                syncManager.stopRealtimeSync()
                hasSkippedAuth.value = false
                // No session: stop any pending automatic retry loop.
                pendingSyncRetry = false
                autoSyncAttempts = 0
                autoSyncJob?.cancel()
                autoSyncJob = null
            }
        }

        syncManager.onCloudWriteFailed = {
            requestAutomaticSyncRetry()
        }

        // Session already active at launch (no login screen is shown): converge
        // with the cloud once, so edits made offline on either phone flow in
        // and out even before the user opens the sync screen.
        viewModelScope.launch {
            delay(3_000)
            if (syncManager.isLoggedIn && !syncManager.isFullSyncRunning) {
                runSilentFullSync()
            }
        }
    }

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

    // List of recurring transaction rules
    val recurringRules: StateFlow<List<RecurringRule>> = repository.allRecurringRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Monthly category budgets
    val budgets: StateFlow<List<Budget>> = repository.allBudgets
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
            // Generate any due recurring transactions (idempotent)
            generateDueRecurringTransactions()
        }
    }

    // Computed balances and totals
    val financeSummary: StateFlow<FinanceSummary> = combine(transactions, bills) { transList, billList ->
        computeFinanceSummary(transList, billList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceSummary()
    )

    // Analysis period selector (used by the Analysis screen)
    val analysisPeriod = MutableStateFlow(SummaryPeriod.ALL)

    // Period-aware summary for the Analysis screen
    val analysisSummary: StateFlow<FinanceSummary> = combine(transactions, bills, analysisPeriod) { transList, billList, period ->
        computeFinanceSummary(filterTransactionsByPeriod(transList, period), billList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceSummary()
    )

    fun setAnalysisPeriod(period: SummaryPeriod) {
        analysisPeriod.value = period
    }

    // Transaction Actions
    fun addTransaction(title: String, amount: Double, type: String, accountType: String, category: String, dateMillis: Long, note: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val transaction = Transaction(
                id = kotlin.random.Random.nextInt(1000000, 2_000_000_000),
                title = title,
                amount = amount,
                type = type,
                accountType = accountType,
                category = category,
                dateMillis = dateMillis,
                note = note,
                updatedAt = now
            )
            repository.insertTransaction(transaction)
            if (syncManager.isLoggedIn) {
                syncManager.syncTransactionToCloud(transaction)
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

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val stamped = transaction.copy(updatedAt = System.currentTimeMillis())
            repository.updateTransaction(stamped)
            if (syncManager.isLoggedIn) {
                syncManager.syncTransactionToCloud(stamped)
            }
        }
    }

    // Bill Actions
    fun addBill(title: String, amount: Double, dueDateMillis: Long, category: String, note: String, context: Context? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val bill = Bill(
                id = kotlin.random.Random.nextInt(1000000, 2_000_000_000),
                title = title,
                amount = amount,
                dueDateMillis = dueDateMillis,
                category = category,
                note = note,
                updatedAt = now
            )
            repository.insertBill(bill)
            if (syncManager.isLoggedIn) {
                syncManager.syncBillToCloud(bill)
            }
            context?.let { triggerBillReminders(it) }
        }
    }

    fun toggleBillPaid(bill: Bill, context: Context? = null) {
        viewModelScope.launch {
            val updated = bill.copy(isPaid = !bill.isPaid, updatedAt = System.currentTimeMillis())
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

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            val stamped = bill.copy(updatedAt = System.currentTimeMillis())
            repository.updateBill(stamped)
            if (syncManager.isLoggedIn) {
                syncManager.syncBillToCloud(stamped)
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
                val newBills = computeCreditCardBills(transList, billList)
                newBills.forEach { bill ->
                    val stamped = bill.copy(updatedAt = System.currentTimeMillis())
                    repository.insertBill(stamped)
                    if (syncManager.isLoggedIn) {
                        syncManager.syncBillToCloud(stamped)
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
            val category = Category(
                id = kotlin.random.Random.nextInt(1000000, 2_000_000_000),
                name = name,
                type = type,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertCategory(category)
            if (syncManager.isLoggedIn) {
                syncManager.syncCategoryToCloud(category)
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            val stamped = category.copy(updatedAt = System.currentTimeMillis())
            repository.updateCategory(stamped)
            if (syncManager.isLoggedIn) {
                syncManager.syncCategoryToCloud(stamped)
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

    // --- Recurring transactions ------------------------------------------

    private var recurringGenerationRunning = false

    /** Generates all due recurring-rule transactions (idempotent by design). */
    fun generateDueRecurringTransactions() {
        if (recurringGenerationRunning) return
        recurringGenerationRunning = true
        viewModelScope.launch {
            try {
                val created = repository.generateDueRecurringTransactions()
                if (syncManager.isLoggedIn) {
                    created.forEach { syncManager.syncTransactionToCloud(it) }
                }
            } finally {
                recurringGenerationRunning = false
            }
        }
    }

    fun addRecurringRule(
        title: String,
        amount: Double,
        type: String,
        accountType: String,
        category: String,
        frequency: String,
        startMillis: Long,
        note: String
    ) {
        viewModelScope.launch {
            val rule = RecurringRule(
                title = title.trim(),
                amount = amount,
                type = type,
                accountType = accountType,
                category = category.ifBlank { "Lain-lain" },
                frequency = frequency,
                startDateMillis = startMillis,
                nextRunMillis = startMillis,
                isActive = true,
                note = note.trim()
            )
            repository.insertRecurringRule(rule)
            // A rule starting today (or earlier) becomes visible immediately.
            generateDueRecurringTransactions()
        }
    }

    fun toggleRecurringRule(rule: RecurringRule, active: Boolean) {
        viewModelScope.launch {
            repository.updateRecurringRule(rule.copy(isActive = active))
        }
    }

    fun deleteRecurringRule(id: Int) {
        viewModelScope.launch {
            repository.deleteRecurringRule(id)
        }
    }

    // --- Monthly category budgets ----------------------------------------

    /** Creates or updates the monthly limit for one category. */
    fun upsertBudget(category: String, monthlyLimit: Double) {
        viewModelScope.launch {
            if (monthlyLimit <= 0) {
                repository.getAllBudgetsDirect()
                    .find { it.category == category }
                    ?.let { repository.deleteBudget(it.id) }
                return@launch
            }
            val existing = repository.getAllBudgetsDirect().find { it.category == category }
            if (existing != null) {
                repository.updateBudget(existing.copy(monthlyLimit = monthlyLimit))
            } else {
                repository.insertBudget(Budget(category = category, monthlyLimit = monthlyLimit))
            }
        }
    }

    fun deleteBudget(id: Int) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    /** Posts a notification when one or more monthly budgets are exceeded. */
    fun checkBudgetsAndNotify(context: Context) {
        viewModelScope.launch {
            val allTx = repository.getAllTransactionsDirect()
            val budgetList = repository.getAllBudgetsDirect()
            if (budgetList.isEmpty()) return@launch
            val exceeded = computeBudgetUsages(allTx, budgetList).filter { it.exceeded }
            if (exceeded.isEmpty()) return@launch

            val channelId = "budget_alerts_channel"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(channelId, "Pengingat Budget", NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { description = "Notifikasi saat pengeluaran melewati budget bulanan kategori" }
                )
            }
            val details = exceeded.joinToString("\n") { usage ->
                "- ${usage.budget.category}: ${formatRupiah(usage.spent)} dari ${formatRupiah(usage.budget.monthlyLimit)}"
            }
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Budget bulanan terlampaui")
                .setContentText("${exceeded.size} kategori melebihi budget bulan ini.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(details))
                .setAutoCancel(true)
            manager.notify(9001, builder.build())
        }
    }

    // --- Automatic cloud sync retry --------------------------------------

    /**
     * Called whenever an individual cloud write fails (offline, etc.). Starts a
     * background loop that retries a full sync with increasing back-off until it
     * succeeds, guaranteeing that local changes are uploaded and both devices
     * converge even if the phone was offline for a while.
     */
    private fun requestAutomaticSyncRetry() {
        pendingSyncRetry = true
        if (autoSyncJob?.isActive == true) return
        autoSyncJob = viewModelScope.launch {
            while (pendingSyncRetry && isLoggedIn.value) {
                val retryDelay = when {
                    autoSyncAttempts <= 0 -> 10_000L
                    autoSyncAttempts == 1 -> 30_000L
                    autoSyncAttempts == 2 -> 60_000L
                    else -> 5 * 60_000L
                }
                delay(retryDelay)
                if (!pendingSyncRetry || !isLoggedIn.value) break
                if (runSilentFullSync()) {
                    pendingSyncRetry = false
                    autoSyncAttempts = 0
                    break
                }
                autoSyncAttempts++
            }
            autoSyncJob = null
        }
    }

    /** Runs a full bidirectional sync in the background and reports whether it succeeded. */
    private suspend fun runSilentFullSync(): Boolean {
        if (!syncManager.isLoggedIn || syncManager.isFullSyncRunning) return false
        val deferred = CompletableDeferred<Boolean>()
        syncManager.performFullSync(
            onSuccess = { deferred.complete(true) },
            onFailure = { deferred.complete(false) }
        )
        return deferred.await()
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

enum class SummaryPeriod {
    ALL, THIS_MONTH, LAST_MONTH, THIS_YEAR
}

private fun filterTransactionsByPeriod(transactions: List<Transaction>, period: SummaryPeriod): List<Transaction> {
    return when (period) {
        SummaryPeriod.ALL -> transactions
        SummaryPeriod.THIS_MONTH -> {
            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            transactions.filter { it.dateMillis >= start }
        }
        SummaryPeriod.LAST_MONTH -> {
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            transactions.filter { it.dateMillis >= start && it.dateMillis < end }
        }
        SummaryPeriod.THIS_YEAR -> {
            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            transactions.filter { it.dateMillis >= start }
        }
    }
}

private fun computeFinanceSummary(transList: List<Transaction>, billList: List<Bill>): FinanceSummary {
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

    return FinanceSummary(
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
}

// Helper to parse user-entered amounts that may use Indonesian thousand/decimal
// separators (e.g. "1.500", "25.000,50", "12,5"). Ambiguous bare dots with
// exactly 3 trailing digits are treated as thousand separators.
fun parseAmount(input: String): Double {
    var s = input.trim().replace("Rp", "").replace("rp", "").replace(" ", "")
    if (s.isEmpty()) return 0.0

    val lastComma = s.lastIndexOf(',')
    val lastDot = s.lastIndexOf('.')
    when {
        lastComma >= 0 && lastDot >= 0 -> {
            if (lastComma > lastDot) {
                s = s.replace(".", "").replace(",", ".")
            } else {
                s = s.replace(",", "")
            }
        }
        lastComma >= 0 -> s = s.replace(",", ".")
        lastDot >= 0 -> {
            val digitsAfterDot = s.substring(lastDot + 1)
            if (digitsAfterDot.length == 3) {
                s = s.replace(".", "")
            }
        }
    }
    return s.toDoubleOrNull() ?: 0.0
}

// Helper to format currency to Indonesian Rupiah
fun formatRupiah(amount: Double): String {
    return try {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        format.format(amount).replace("Rp", "Rp ").replace(",00", "")
    } catch (e: Exception) {
        "Rp " + String.format("%,.0f", amount)
    }
}

// Build a CSV document of all transactions (data portability / backup feature)
fun buildTransactionsCsv(transactions: List<Transaction>): String {
    val sb = StringBuilder()
    sb.append("Tanggal,Tipe,Judul,Kategori,Metode Bayar,Jumlah,Catatan\n")
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale("id", "ID"))
    transactions
        .sortedByDescending { it.dateMillis }
        .forEach { tx ->
            val type = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"
            val account = when (tx.accountType) {
                "CASH" -> "Tunai"
                "CREDIT_CARD" -> "Kartu Kredit"
                else -> "Bank"
            }
            sb.append(
                listOf(
                    dateFormat.format(java.util.Date(tx.dateMillis)),
                    type,
                    escapeCsv(tx.title),
                    escapeCsv(tx.category),
                    account,
                    String.format(java.util.Locale.US, "%.0f", tx.amount),
                    escapeCsv(tx.note)
                ).joinToString(",")
            ).append("\n")
        }
    return sb.toString()
}

// Build a CSV document of all bills (data portability / backup feature)
fun buildBillsCsv(bills: List<Bill>): String {
    val sb = StringBuilder()
    sb.append("Nama,Kategori,Jumlah,Jatuh Tempo,Status,Catatan\n")
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id", "ID"))
    bills
        .sortedByDescending { it.dueDateMillis }
        .forEach { bill ->
            sb.append(
                listOf(
                    escapeCsv(bill.title),
                    escapeCsv(bill.category),
                    String.format(java.util.Locale.US, "%.0f", bill.amount),
                    dateFormat.format(java.util.Date(bill.dueDateMillis)),
                    if (bill.isPaid) "Lunas" else "Belum Bayar",
                    escapeCsv(bill.note)
                ).joinToString(",")
            ).append("\n")
        }
    return sb.toString()
}

// Escape a CSV field per RFC 4180 (quotes doubled, wrapped when needed)
fun escapeCsv(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n')) {
        "\"$escaped\""
    } else {
        escaped
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
