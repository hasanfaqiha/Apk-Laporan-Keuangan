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
import com.google.android.gms.tasks.Tasks
import java.lang.Exception

class FirebaseSyncManager(private val repository: FinanceRepository) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private var transactionsListener: ListenerRegistration? = null
    private var billsListener: ListenerRegistration? = null
    private var categoriesListener: ListenerRegistration? = null

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
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to sync transaction to cloud", e)
            }
    }

    fun deleteTransactionFromCloud(id: Int) {
        val uid = userId ?: return
        db.collection("users").document(uid)
            .collection("transactions").document(id.toString())
            .delete()
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to delete transaction from cloud", e)
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
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to sync bill to cloud", e)
            }
    }

    fun deleteBillFromCloud(id: Int) {
        val uid = userId ?: return
        db.collection("users").document(uid)
            .collection("bills").document(id.toString())
            .delete()
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to delete bill from cloud", e)
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
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to sync category to cloud", e)
            }
    }

    fun deleteCategoryFromCloud(id: Int) {
        val uid = userId ?: return
        db.collection("users").document(uid)
            .collection("categories").document(id.toString())
            .delete()
            .addOnFailureListener { e ->
                Log.e("FirebaseSync", "Failed to delete category from cloud", e)
            }
    }

    // --- Full Bi-directional Sync Engine ---
    fun performFullSync(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = userId
        if (uid == null) {
            onFailure(Exception("User tidak terdaftar atau belum login"))
            return
        }

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                // 1. Re-index local categories if they have auto-increment IDs (< 1000000) to avoid cloud clashing
                val localCategories = repository.allCategories.first()
                for (cat in localCategories) {
                    if (cat.id < 1000000) {
                        val newId = kotlin.random.Random.nextInt(1000000, 2_000_000_000)
                        repository.deleteCategory(cat)
                        repository.insertCategory(cat.copy(id = newId))
                    }
                }

                // 2. Re-index local transactions if they have auto-increment IDs (< 1000000)
                val localTransactions = repository.allTransactions.first()
                for (t in localTransactions) {
                    if (t.id < 1000000) {
                        val newId = kotlin.random.Random.nextInt(1000000, 2_000_000_000)
                        repository.deleteTransactionById(t.id)
                        repository.insertTransaction(t.copy(id = newId))
                    }
                }

                // 3. Re-index local bills if they have auto-increment IDs (< 1000000)
                val localBills = repository.allBills.first()
                for (b in localBills) {
                    if (b.id < 1000000) {
                        val newId = kotlin.random.Random.nextInt(1000000, 2_000_000_000)
                        repository.deleteBillById(b.id)
                        repository.insertBill(b.copy(id = newId))
                    }
                }

                // --- START SEQUENTIAL SYNC WITH CLOUD ---

                // 1. Sync Categories
                val updatedLocalCategories = repository.allCategories.first()
                // Upload local categories to cloud
                for (cat in updatedLocalCategories) {
                    val docData = hashMapOf("id" to cat.id, "name" to cat.name, "type" to cat.type)
                    val setTask = db.collection("users").document(uid)
                        .collection("categories").document(cat.id.toString())
                        .set(docData, SetOptions.merge())
                    Tasks.await(setTask)
                }
                // Download cloud categories and save to local
                val getCatTask = db.collection("users").document(uid).collection("categories").get()
                val catSnapshot = Tasks.await(getCatTask)
                for (doc in catSnapshot.documents) {
                    val id = doc.getLong("id")?.toInt() ?: continue
                    val name = doc.getString("name") ?: continue
                    val type = doc.getString("type") ?: continue
                    repository.insertCategory(Category(id = id, name = name, type = type))
                }

                // 2. Sync Transactions
                val updatedLocalTransactions = repository.allTransactions.first()
                // Upload local transactions to cloud
                for (t in updatedLocalTransactions) {
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
                    val setTransTask = db.collection("users").document(uid)
                        .collection("transactions").document(t.id.toString())
                        .set(docData, SetOptions.merge())
                    Tasks.await(setTransTask)
                }
                // Download cloud transactions and save to local
                val getTransTask = db.collection("users").document(uid).collection("transactions").get()
                val transSnapshot = Tasks.await(getTransTask)
                for (doc in transSnapshot.documents) {
                    val id = doc.getLong("id")?.toInt() ?: continue
                    val title = doc.getString("title") ?: continue
                    val amount = doc.getDouble("amount") ?: 0.0
                    val type = doc.getString("type") ?: continue
                    val accountType = doc.getString("accountType") ?: continue
                    val category = doc.getString("category") ?: continue
                    val dateMillis = doc.getLong("dateMillis") ?: System.currentTimeMillis()
                    val note = doc.getString("note") ?: ""
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
                }

                // 3. Sync Bills
                val updatedLocalBills = repository.allBills.first()
                // Upload local bills to cloud
                for (b in updatedLocalBills) {
                    val docData = hashMapOf(
                        "id" to b.id,
                        "title" to b.title,
                        "amount" to b.amount,
                        "dueDateMillis" to b.dueDateMillis,
                        "isPaid" to b.isPaid,
                        "category" to b.category,
                        "note" to b.note
                    )
                    val setBillTask = db.collection("users").document(uid)
                        .collection("bills").document(b.id.toString())
                        .set(docData, SetOptions.merge())
                    Tasks.await(setBillTask)
                }
                // Download cloud bills and save to local
                val getBillTask = db.collection("users").document(uid).collection("bills").get()
                val billSnapshot = Tasks.await(getBillTask)
                for (doc in billSnapshot.documents) {
                    val id = doc.getLong("id")?.toInt() ?: continue
                    val title = doc.getString("title") ?: continue
                    val amount = doc.getDouble("amount") ?: 0.0
                    val dueDateMillis = doc.getLong("dueDateMillis") ?: System.currentTimeMillis()
                    val isPaid = doc.getBoolean("isPaid") ?: false
                    val category = doc.getString("category") ?: continue
                    val note = doc.getString("note") ?: ""
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
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error in sync", e)
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }

    // --- Real-time Bi-directional Firestore Listener ---
    fun startRealtimeSync(coroutineScope: CoroutineScope) {
        val uid = userId ?: return
        
        // Stop any existing listeners first to avoid memory leaks
        stopRealtimeSync()

        Log.d("FirebaseSync", "Starting real-time synchronization for user: $uid")

        // 1. Listen for Transactions
        transactionsListener = db.collection("users").document(uid).collection("transactions")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FirebaseSync", "Transactions listener failed", error)
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
                                }
                                DocumentChange.Type.REMOVED -> {
                                    repository.deleteTransactionById(id)
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
                                }
                                DocumentChange.Type.REMOVED -> {
                                    repository.deleteBillById(id)
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
                                    
                                    repository.insertCategory(
                                        Category(
                                            id = id,
                                            name = name,
                                            type = type
                                        )
                                    )
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val name = doc.getString("name") ?: ""
                                    val type = doc.getString("type") ?: ""
                                    repository.deleteCategory(Category(id = id, name = name, type = type))
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
