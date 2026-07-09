package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.google.android.gms.tasks.Tasks
import java.lang.Exception

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,     // "UPLOAD", "DOWNLOAD", "DELETE", "ERROR", "SUCCESS", "INFO"
    val status: String,   // "SUCCESS", "FAILED", "RUNNING", "INFO"
    val message: String
)

class FirebaseSyncManager(private val repository: FinanceRepository) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private var transactionsListener: ListenerRegistration? = null
    private var billsListener: ListenerRegistration? = null
    private var categoriesListener: ListenerRegistration? = null
    private var activeScope: CoroutineScope? = null
    var isFullSyncRunning: Boolean = false
        private set

    private val _syncLogs = MutableStateFlow<List<SyncLog>>(emptyList())
    val syncLogs: StateFlow<List<SyncLog>> = _syncLogs.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun addLog(type: String, status: String, message: String) {
        val newLog = SyncLog(type = type, status = status, message = message)
        val current = _syncLogs.value.toMutableList()
        current.add(0, newLog)
        if (current.size > 100) {
            current.removeAt(current.size - 1)
        }
        _syncLogs.value = current

        if (status == "FAILED" || type == "ERROR") {
            _lastError.value = message
        }
    }

    fun clearLastError() {
        _lastError.value = null
    }

    fun clearLogs() {
        _syncLogs.value = emptyList()
    }

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val userId: String?
        get() = auth.currentUser?.uid

    // --- Core Cloud Sync Operations for Transactions ---
    fun syncTransactionToCloud(transaction: Transaction) {
        val uid = userId ?: return
        val docData = hashMapOf(
            "id" to transaction.id,
            "title" to transaction.title,
            "amount" to transaction.amount,
            "type" to transaction.type,
            "accountType" to transaction.accountType,
            "category" to transaction.category,
            "dateMillis" to transaction.dateMillis,
            "note" to transaction.note
        )
        db.collection("users").document(uid)
            .collection("transactions").document(transaction.id.toString())
            .set(docData, SetOptions.merge())
            .addOnSuccessListener {
                addLog("UPLOAD", "SUCCESS", "Transaksi '${transaction.title}' berhasil diunggah ke cloud.")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to sync transaction to cloud", e)
                addLog("UPLOAD", "FAILED", "Gagal mengunggah transaksi '${transaction.title}': ${e.message}")
            }
    }

    fun deleteTransactionFromCloud(id: Int) {
        val uid = userId ?: return
        db.collection("users").document(uid)
            .collection("transactions").document(id.toString())
            .delete()
            .addOnSuccessListener {
                addLog("DELETE", "SUCCESS", "Transaksi ID $id berhasil dihapus dari cloud.")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to delete transaction from cloud", e)
                addLog("DELETE", "FAILED", "Gagal menghapus transaksi ID $id dari cloud: ${e.message}")
            }
    }

    // --- Core Cloud Sync Operations for Bills ---
    fun syncBillToCloud(bill: Bill) {
        val uid = userId ?: return
        val docData = hashMapOf(
            "id" to bill.id,
            "title" to bill.title,
            "amount" to bill.amount,
            "dueDateMillis" to bill.dueDateMillis,
            "isPaid" to bill.isPaid,
            "category" to bill.category,
            "note" to bill.note
        )
        db.collection("users").document(uid)
            .collection("bills").document(bill.id.toString())
            .set(docData, SetOptions.merge())
            .addOnSuccessListener {
                addLog("UPLOAD", "SUCCESS", "Tagihan '${bill.title}' berhasil diunggah ke cloud.")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to sync bill to cloud", e)
                addLog("UPLOAD", "FAILED", "Gagal mengunggah tagihan '${bill.title}': ${e.message}")
            }
    }

    fun deleteBillFromCloud(id: Int) {
        val uid = userId ?: return
        db.collection("users").document(uid)
            .collection("bills").document(id.toString())
            .delete()
            .addOnSuccessListener {
                addLog("DELETE", "SUCCESS", "Tagihan ID $id berhasil dihapus dari cloud.")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to delete bill from cloud", e)
                addLog("DELETE", "FAILED", "Gagal menghapus tagihan ID $id dari cloud: ${e.message}")
            }
    }

    // --- Core Cloud Sync Operations for Categories ---
    fun syncCategoryToCloud(category: Category) {
        val uid = userId ?: return
        val docData = hashMapOf(
            "id" to category.id,
            "name" to category.name,
            "type" to category.type
        )
        db.collection("users").document(uid)
            .collection("categories").document(category.id.toString())
            .set(docData, SetOptions.merge())
            .addOnSuccessListener {
                addLog("UPLOAD", "SUCCESS", "Kategori '${category.name}' berhasil diunggah ke cloud.")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to sync category to cloud", e)
                addLog("UPLOAD", "FAILED", "Gagal mengunggah kategori '${category.name}': ${e.message}")
            }
    }

    fun deleteCategoryFromCloud(id: Int) {
        val uid = userId ?: return
        db.collection("users").document(uid)
            .collection("categories").document(id.toString())
            .delete()
            .addOnSuccessListener {
                addLog("DELETE", "SUCCESS", "Kategori ID $id berhasil dihapus dari cloud.")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to delete category from cloud", e)
                addLog("DELETE", "FAILED", "Gagal menghapus kategori ID $id dari cloud: ${e.message}")
            }
    }

    // --- Full Bi-directional Sync Engine ---
    fun performFullSync(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = userId
        if (uid == null) {
            onFailure(Exception("User tidak terdaftar atau belum login"))
            return
        }

        isFullSyncRunning = true
        addLog("INFO", "RUNNING", "Memulai sinkronisasi penuh dengan Firebase...")
        // Stop active listeners to prevent race conditions or concurrent writes during bulk sync
        stopRealtimeSync()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // Force Auth token refresh to synchronize credentials with the Firestore client cache.
                // This prevents transient PERMISSION_DENIED errors immediately after signing in.
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    try {
                        Tasks.await(currentUser.getIdToken(true))
                        Log.d("FirebaseSync", "Successfully refreshed and synchronized auth token with Firestore client")
                    } catch (tokenEx: Exception) {
                        Log.w("FirebaseSync", "Transient auth token sync warning: ${tokenEx.localizedMessage}")
                    }
                }

                // Helper to execute tasks with simple retry to handle transient Firestore/Auth propagation delays
                suspend fun <T> runWithRetry(maxAttempts: Int = 3, block: () -> com.google.android.gms.tasks.Task<T>): T {
                    var lastException: Exception? = null
                    for (attempt in 1..maxAttempts) {
                        try {
                            return Tasks.await(block())
                        } catch (e: Exception) {
                            lastException = e
                            Log.w("FirebaseSync", "Firestore operation attempt $attempt failed: ${e.localizedMessage}. Retrying...")
                            delay(1000)
                        }
                    }
                    throw lastException ?: Exception("Firestore operation failed after $maxAttempts attempts")
                }

                // --- 1. SYNC CATEGORIES ---
                Log.d("FirebaseSync", "Syncing categories...")
                addLog("DOWNLOAD", "RUNNING", "Mengunduh kategori dari cloud...")
                val cloudCatSnapshot = runWithRetry {
                    db.collection("users").document(uid).collection("categories").get()
                }
                val cloudCategories = cloudCatSnapshot.documents.mapNotNull { doc ->
                    val id = doc.getLong("id")?.toInt() ?: return@mapNotNull null
                    val name = doc.getString("name") ?: "Kategori Tanpa Nama"
                    val type = doc.getString("type") ?: "EXPENSE"
                    Category(id = id, name = name, type = type)
                }
                addLog("DOWNLOAD", "SUCCESS", "Selesai mengunduh ${cloudCategories.size} kategori.")

                val localCategories = repository.getAllCategoriesDirect()
                for (localCat in localCategories) {
                    val matchingCloudCat = cloudCategories.find {
                        it.name.equals(localCat.name, ignoreCase = true) && it.type == localCat.type
                    }

                    if (matchingCloudCat != null) {
                        if (localCat.id != matchingCloudCat.id) {
                            // Align local ID with cloud ID to avoid duplicates
                            repository.deleteCategory(localCat)
                            repository.insertCategory(matchingCloudCat)
                        }
                    } else {
                        // Not in cloud. If it has a local auto-increment ID (< 1000000), re-index it first to avoid collision
                        val finalCat = if (localCat.id < 1000000) {
                            val newId = kotlin.random.Random.nextInt(1000000, 2_000_000_000)
                            repository.deleteCategory(localCat)
                            val newCat = localCat.copy(id = newId)
                            repository.insertCategory(newCat)
                            newCat
                        } else {
                            localCat
                        }

                        // Upload to cloud
                        val docData = hashMapOf("id" to finalCat.id, "name" to finalCat.name, "type" to finalCat.type)
                        runWithRetry {
                            db.collection("users").document(uid)
                                .collection("categories").document(finalCat.id.toString())
                                .set(docData, SetOptions.merge())
                        }
                        addLog("UPLOAD", "SUCCESS", "Mengunggah kategori lokal '${finalCat.name}' ke cloud.")
                    }
                }

                // Download and insert any cloud categories not present locally
                val postLocalCats = repository.getAllCategoriesDirect()
                for (cloudCat in cloudCategories) {
                    val alreadyExistsLocally = postLocalCats.any {
                        it.id == cloudCat.id || (it.name.equals(cloudCat.name, ignoreCase = true) && it.type == cloudCat.type)
                    }
                    if (!alreadyExistsLocally) {
                        repository.insertCategory(cloudCat)
                    }
                }


                // --- 2. SYNC TRANSACTIONS ---
                Log.d("FirebaseSync", "Syncing transactions...")
                addLog("DOWNLOAD", "RUNNING", "Mengunduh transaksi dari cloud...")
                val cloudTransSnapshot = runWithRetry {
                    db.collection("users").document(uid).collection("transactions").get()
                }
                val cloudTransactions = cloudTransSnapshot.documents.mapNotNull { doc ->
                    val id = doc.getLong("id")?.toInt() ?: return@mapNotNull null
                    val title = doc.getString("title") ?: "Transaksi Tanpa Judul"
                    val amount = doc.getDouble("amount") ?: 0.0
                    val type = doc.getString("type") ?: "EXPENSE"
                    val accountType = doc.getString("accountType") ?: "CASH"
                    val category = doc.getString("category") ?: "Lain-lain"
                    val dateMillis = doc.getLong("dateMillis") ?: System.currentTimeMillis()
                    val note = doc.getString("note") ?: ""
                    Transaction(
                        id = id,
                        title = title,
                        amount = amount,
                        type = type,
                        accountType = accountType,
                        category = category,
                        dateMillis = dateMillis,
                        note = note
                    )
                }
                addLog("DOWNLOAD", "SUCCESS", "Selesai mengunduh ${cloudTransactions.size} transaksi.")
                val cloudTransMap = cloudTransactions.associateBy { it.id }

                val localTransactions = repository.getAllTransactionsDirect()
                val keepLocalTransIds = mutableSetOf<Int>()

                for (t in localTransactions) {
                    if (t.id < 1000000) {
                        // Re-index guest offline transaction
                        val newId = kotlin.random.Random.nextInt(1000000, 2_000_000_000)
                        repository.deleteTransactionById(t.id)
                        val newT = t.copy(id = newId)
                        repository.insertTransaction(newT)
                        
                        // Upload immediately
                        val docData = hashMapOf(
                            "id" to newT.id,
                            "title" to newT.title,
                            "amount" to newT.amount,
                            "type" to newT.type,
                            "accountType" to newT.accountType,
                            "category" to newT.category,
                            "dateMillis" to newT.dateMillis,
                            "note" to newT.note
                        )
                        runWithRetry {
                            db.collection("users").document(uid)
                                .collection("transactions").document(newT.id.toString())
                                .set(docData, SetOptions.merge())
                        }
                        keepLocalTransIds.add(newId)
                        addLog("UPLOAD", "SUCCESS", "Mengunggah transaksi offline '${newT.title}' ke cloud.")
                    } else {
                        // Check if exists in cloud
                        if (cloudTransMap.containsKey(t.id)) {
                            keepLocalTransIds.add(t.id)
                            // Upload local to merge edits or just make sure cloud is up to date
                            val docData = hashMapOf(
                                "id" to t.id,
                                "title" to t.title,
                                "amount" to t.amount,
                                "type" to t.type,
                                "accountType" to t.accountType,
                                "category" to t.category,
                                "dateMillis" to t.dateMillis,
                                "note" to t.note
                            )
                            runWithRetry {
                                db.collection("users").document(uid)
                                    .collection("transactions").document(t.id.toString())
                                    .set(docData, SetOptions.merge())
                            }
                        } else {
                            // Was deleted in cloud on another device, delete locally
                            repository.deleteTransactionById(t.id)
                            addLog("DELETE", "SUCCESS", "Menghapus transaksi lokal '${t.title}' (dihapus dari cloud).")
                        }
                    }
                }

                // Insert missing cloud transactions
                for (cloudT in cloudTransactions) {
                    if (!keepLocalTransIds.contains(cloudT.id)) {
                        repository.insertTransaction(cloudT)
                    }
                }


                // --- 3. SYNC BILLS ---
                Log.d("FirebaseSync", "Syncing bills...")
                addLog("DOWNLOAD", "RUNNING", "Mengunduh tagihan dari cloud...")
                val cloudBillSnapshot = runWithRetry {
                    db.collection("users").document(uid).collection("bills").get()
                }
                val cloudBills = cloudBillSnapshot.documents.mapNotNull { doc ->
                    val id = doc.getLong("id")?.toInt() ?: return@mapNotNull null
                    val title = doc.getString("title") ?: "Tagihan Tanpa Judul"
                    val amount = doc.getDouble("amount") ?: 0.0
                    val dueDateMillis = doc.getLong("dueDateMillis") ?: System.currentTimeMillis()
                    val isPaid = doc.getBoolean("isPaid") ?: false
                    val category = doc.getString("category") ?: "Lain-lain"
                    val note = doc.getString("note") ?: ""
                    Bill(
                        id = id,
                        title = title,
                        amount = amount,
                        dueDateMillis = dueDateMillis,
                        isPaid = isPaid,
                        category = category,
                        note = note
                    )
                }
                addLog("DOWNLOAD", "SUCCESS", "Selesai mengunduh ${cloudBills.size} tagihan.")
                val cloudBillsMap = cloudBills.associateBy { it.id }

                val localBills = repository.getAllBillsDirect()
                val keepLocalBillIds = mutableSetOf<Int>()

                for (b in localBills) {
                    if (b.id < 1000000) {
                        // Re-index guest offline bill
                        val newId = kotlin.random.Random.nextInt(1000000, 2_000_000_000)
                        repository.deleteBillById(b.id)
                        val newB = b.copy(id = newId)
                        repository.insertBill(newB)

                        // Upload immediately
                        val docData = hashMapOf(
                            "id" to newB.id,
                            "title" to newB.title,
                            "amount" to newB.amount,
                            "dueDateMillis" to newB.dueDateMillis,
                            "isPaid" to newB.isPaid,
                            "category" to newB.category,
                            "note" to newB.note
                        )
                        runWithRetry {
                            db.collection("users").document(uid)
                                .collection("bills").document(newB.id.toString())
                                .set(docData, SetOptions.merge())
                        }
                        keepLocalBillIds.add(newId)
                        addLog("UPLOAD", "SUCCESS", "Mengunggah tagihan offline '${newB.title}' ke cloud.")
                    } else {
                        // Check if exists in cloud
                        if (cloudBillsMap.containsKey(b.id)) {
                            keepLocalBillIds.add(b.id)
                            // Upload local to merge edits
                            val docData = hashMapOf(
                                "id" to b.id,
                                "title" to b.title,
                                "amount" to b.amount,
                                "dueDateMillis" to b.dueDateMillis,
                                "isPaid" to b.isPaid,
                                "category" to b.category,
                                "note" to b.note
                            )
                            runWithRetry {
                                db.collection("users").document(uid)
                                    .collection("bills").document(b.id.toString())
                                    .set(docData, SetOptions.merge())
                            }
                        } else {
                            // Was deleted on another device, delete locally
                            repository.deleteBillById(b.id)
                            addLog("DELETE", "SUCCESS", "Menghapus tagihan lokal '${b.title}' (dihapus dari cloud).")
                        }
                    }
                }

                // Insert missing cloud bills
                for (cloudB in cloudBills) {
                    if (!keepLocalBillIds.contains(cloudB.id)) {
                        repository.insertBill(cloudB)
                    }
                }

                Log.d("FirebaseSync", "Sequential full sync completed successfully!")
                isFullSyncRunning = false
                addLog("SUCCESS", "SUCCESS", "Sinkronisasi penuh berhasil diselesaikan!")

                // Re-start realtime sync safely now that the initial full sync is finished
                activeScope?.let { startRealtimeSync(it) }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error during full sync", e)
                isFullSyncRunning = false
                addLog("ERROR", "FAILED", "Gagal sinkronisasi penuh: ${e.message}")

                // Try to start realtime sync as fallback to keep real-time features alive
                activeScope?.let { startRealtimeSync(it) }

                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }

    // --- Real-time Bi-directional Firestore Listener ---
    fun startRealtimeSync(coroutineScope: CoroutineScope) {
        val uid = userId ?: return
        this.activeScope = coroutineScope

        if (isFullSyncRunning) {
            Log.d("FirebaseSync", "startRealtimeSync skipped because full sync is currently running.")
            return
        }
        
        // Stop any existing listeners first to avoid memory leaks
        stopRealtimeSync()

        Log.d("FirebaseSync", "Starting real-time synchronization for user: $uid")
        addLog("INFO", "INFO", "Sinkronisasi real-time Firebase aktif.")

        // 1. Listen for Transactions
        transactionsListener = db.collection("users").document(uid).collection("transactions")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FirebaseSync", "Transactions listener failed", error)
                    addLog("DOWNLOAD", "FAILED", "Gagal sinkronisasi transaksi real-time: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    coroutineScope.launch {
                        for (dc in snapshots.documentChanges) {
                            val doc = dc.document
                            val id = doc.getLong("id")?.toInt() ?: continue
                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val title = doc.getString("title") ?: ""
                                    val amount = doc.getDouble("amount") ?: 0.0
                                    val type = doc.getString("type") ?: ""
                                    val accountType = doc.getString("accountType") ?: ""
                                    val category = doc.getString("category") ?: ""
                                    val dateMillis = doc.getLong("dateMillis") ?: System.currentTimeMillis()
                                    val note = doc.getString("note") ?: ""
                                    
                                    val isNew = dc.type == DocumentChange.Type.ADDED
                                    val operationName = if (isNew) "diterima" else "diperbarui"
                                    
                                    repository.insertTransaction(
                                        Transaction(
                                            id = id,
                                            title = title,
                                            amount = amount,
                                            type = type,
                                            accountType = accountType,
                                            category = category,
                                            dateMillis = dateMillis,
                                            note = note
                                        )
                                    )
                                    addLog("DOWNLOAD", "SUCCESS", "Transaksi '${title}' $operationName dari cloud.")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    repository.deleteTransactionById(id)
                                    addLog("DELETE", "SUCCESS", "Transaksi ID $id dihapus dari cloud oleh perangkat lain.")
                                }
                            }
                        }
                    }
                }
            }

        // 2. Listen for Bills
        billsListener = db.collection("users").document(uid).collection("bills")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FirebaseSync", "Bills listener failed", error)
                    addLog("DOWNLOAD", "FAILED", "Gagal sinkronisasi tagihan real-time: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    coroutineScope.launch {
                        for (dc in snapshots.documentChanges) {
                            val doc = dc.document
                            val id = doc.getLong("id")?.toInt() ?: continue
                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val title = doc.getString("title") ?: ""
                                    val amount = doc.getDouble("amount") ?: 0.0
                                    val dueDateMillis = doc.getLong("dueDateMillis") ?: System.currentTimeMillis()
                                    val isPaid = doc.getBoolean("isPaid") ?: false
                                    val category = doc.getString("category") ?: ""
                                    val note = doc.getString("note") ?: ""
                                    
                                    val isNew = dc.type == DocumentChange.Type.ADDED
                                    val operationName = if (isNew) "diterima" else "diperbarui"
                                    
                                    repository.insertBill(
                                        Bill(
                                            id = id,
                                            title = title,
                                            amount = amount,
                                            dueDateMillis = dueDateMillis,
                                            isPaid = isPaid,
                                            category = category,
                                            note = note
                                        )
                                    )
                                    addLog("DOWNLOAD", "SUCCESS", "Tagihan '${title}' $operationName dari cloud.")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    repository.deleteBillById(id)
                                    addLog("DELETE", "SUCCESS", "Tagihan ID $id dihapus dari cloud oleh perangkat lain.")
                                }
                            }
                        }
                    }
                }
            }

        // 3. Listen for Categories
        categoriesListener = db.collection("users").document(uid).collection("categories")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FirebaseSync", "Categories listener failed", error)
                    addLog("DOWNLOAD", "FAILED", "Gagal sinkronisasi kategori real-time: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    coroutineScope.launch {
                        for (dc in snapshots.documentChanges) {
                            val doc = dc.document
                            val id = doc.getLong("id")?.toInt() ?: continue
                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val name = doc.getString("name") ?: ""
                                    val type = doc.getString("type") ?: ""
                                    
                                    val isNew = dc.type == DocumentChange.Type.ADDED
                                    val operationName = if (isNew) "diterima" else "diperbarui"
                                    
                                    repository.insertCategory(
                                        Category(
                                            id = id,
                                            name = name,
                                            type = type
                                        )
                                    )
                                    addLog("DOWNLOAD", "SUCCESS", "Kategori '${name}' $operationName dari cloud.")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val name = doc.getString("name") ?: ""
                                    val type = doc.getString("type") ?: ""
                                    repository.deleteCategory(Category(id = id, name = name, type = type))
                                    addLog("DELETE", "SUCCESS", "Kategori '${name}' dihapus dari cloud oleh perangkat lain.")
                                }
                            }
                        }
                    }
                }
            }
    }

    fun stopRealtimeSync() {
        Log.d("FirebaseSync", "Stopping real-time synchronization")
        transactionsListener?.remove()
        transactionsListener = null
        billsListener?.remove()
        billsListener = null
        categoriesListener?.remove()
        categoriesListener = null
    }
}
