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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
                // 1. Sync Categories
                val localCategories = repository.allCategories.first()
                // Upload local categories
                for (cat in localCategories) {
                    val docData = hashMapOf("id" to cat.id, "name" to cat.name, "type" to cat.type)
                    db.collection("users").document(uid)
                        .collection("categories").document(cat.id.toString())
                        .set(docData, SetOptions.merge())
                }
                // Download cloud categories and save to local
                db.collection("users").document(uid).collection("categories").get()
                    .addOnSuccessListener { snapshot ->
                        scope.launch {
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id")?.toInt() ?: continue
                                val name = doc.getString("name") ?: continue
                                val type = doc.getString("type") ?: continue
                                repository.insertCategory(Category(id = id, name = name, type = type))
                            }
                        }
                    }

                // 2. Sync Transactions
                val localTransactions = repository.allTransactions.first()
                // Upload local transactions
                for (t in localTransactions) {
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
                    db.collection("users").document(uid)
                        .collection("transactions").document(t.id.toString())
                        .set(docData, SetOptions.merge())
                }
                // Download cloud transactions and save to local
                db.collection("users").document(uid).collection("transactions").get()
                    .addOnSuccessListener { snapshot ->
                        scope.launch {
                            for (doc in snapshot.documents) {
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
                        }
                    }

                // 3. Sync Bills
                val localBills = repository.allBills.first()
                // Upload local bills
                for (b in localBills) {
                    val docData = hashMapOf(
                        "id" to b.id,
                        "title" to b.title,
                        "amount" to b.amount,
                        "dueDateMillis" to b.dueDateMillis,
                        "isPaid" to b.isPaid,
                        "category" to b.category,
                        "note" to b.note
                    )
                    db.collection("users").document(uid)
                        .collection("bills").document(b.id.toString())
                        .set(docData, SetOptions.merge())
                }
                // Download cloud bills and save to local
                db.collection("users").document(uid).collection("bills").get()
                    .addOnSuccessListener { snapshot ->
                        scope.launch {
                            for (doc in snapshot.documents) {
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
                            // Notify UI on success
                            onSuccess()
                        }
                    }

            } catch (e: Exception) {
                Log.e("FirebaseSync", "Error in sync", e)
                onFailure(e)
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
