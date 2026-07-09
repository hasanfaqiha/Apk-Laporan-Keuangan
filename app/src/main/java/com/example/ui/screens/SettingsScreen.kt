package com.example.ui.screens

import android.widget.Toast
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Category
import com.example.data.SyncLog
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val summary by viewModel.financeSummary.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Pengeluaran, 1 = Pemasukan, 2 = Info Cloud / CC

    // Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Category?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Category?>(null) }

    var newCategoryName by remember { mutableStateOf("") }
    var editCategoryName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pengaturan & Admin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        newCategoryName = ""
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("add_category_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Kategori")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pengeluaran", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFEF4444)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pemasukan", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF10B981)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Info & Fitur", fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Log Cloud", fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.Default.Dns, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Expense Categories List
                    val expenseCats = categories.filter { it.type == "EXPENSE" }
                    CategoryListSection(
                        categories = expenseCats,
                        onEditClick = { cat ->
                            editCategoryName = cat.name
                            showEditDialog = cat
                        },
                        onDeleteClick = { cat ->
                            showDeleteConfirmDialog = cat
                        }
                    )
                }
                1 -> {
                    // Income Categories List
                    val incomeCats = categories.filter { it.type == "INCOME" }
                    CategoryListSection(
                        categories = incomeCats,
                        onEditClick = { cat ->
                            editCategoryName = cat.name
                            showEditDialog = cat
                        },
                        onDeleteClick = { cat ->
                            showDeleteConfirmDialog = cat
                        }
                    )
                }
                2 -> {
                    // Credit Card Logic & Cloud Hosting FAQ Info Section
                    val selectedTheme by viewModel.selectedTheme.collectAsState()
                    InfoAndFaqSection(
                        viewModel = viewModel,
                        creditCardDebt = summary.creditCardDebt,
                        onCheckCCNow = {
                            viewModel.checkAndGenerateCreditCardBills()
                            Toast.makeText(context, "Sistem memeriksa & memperbarui tagihan kartu kredit...", Toast.LENGTH_SHORT).show()
                        },
                        selectedTheme = selectedTheme,
                        onThemeSelected = { theme ->
                            viewModel.setTheme(theme, context)
                        }
                    )
                }
                3 -> {
                    val syncLogs by viewModel.syncManager.syncLogs.collectAsState()
                    val lastError by viewModel.syncManager.lastError.collectAsState()
                    CloudLogMonitorSection(
                        syncLogs = syncLogs,
                        lastError = lastError,
                        onClearLogs = { viewModel.syncManager.clearLogs() },
                        onClearError = { viewModel.syncManager.clearLastError() }
                    )
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Kategori Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Kategori untuk " + (if (selectedTab == 0) "Pengeluaran" else "Pemasukan"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nama Kategori") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_category_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.trim().isNotEmpty()) {
                            val type = if (selectedTab == 0) "EXPENSE" else "INCOME"
                            viewModel.addCategory(newCategoryName.trim(), type)
                            Toast.makeText(context, "Kategori '${newCategoryName}' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Nama kategori tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_category_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Category Dialog
    showEditDialog?.let { cat ->
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Nama Kategori", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editCategoryName,
                        onValueChange = { editCategoryName = it },
                        label = { Text("Nama Kategori") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_category_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editCategoryName.trim().isNotEmpty()) {
                            viewModel.updateCategory(cat.copy(name = editCategoryName.trim()))
                            Toast.makeText(context, "Kategori diubah menjadi '${editCategoryName}'!", Toast.LENGTH_SHORT).show()
                            showEditDialog = null
                        } else {
                            Toast.makeText(context, "Nama kategori tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_edit_category_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Category Confirmation Dialog
    showDeleteConfirmDialog?.let { cat ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Hapus Kategori", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus kategori '${cat.name}'? Transaksi lama yang menggunakan kategori ini akan tetap aman.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat)
                        Toast.makeText(context, "Kategori '${cat.name}' dihapus!", Toast.LENGTH_SHORT).show()
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_category_button")
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun CategoryListSection(
    categories: List<Category>,
    onEditClick: (Category) -> Unit,
    onDeleteClick: (Category) -> Unit
) {
    if (categories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Belum ada kategori.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Klik tombol + untuk menambahkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories, key = { it.id }) { cat ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { onEditClick(cat) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Kategori",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteClick(cat) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Kategori",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
            }
        }
    }
}

@Composable
fun InfoAndFaqSection(
    viewModel: FinanceViewModel,
    creditCardDebt: Double,
    onCheckCCNow: () -> Unit,
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val syncManager = viewModel.syncManager

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userEmail by viewModel.currentUserEmail.collectAsState()

    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // THEME SELECTOR CARD
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Tema Tampilan Aplikasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pilih mode terang, mode gelap, atau ikuti pengaturan default sistem HP Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // System Theme Selector
                    ThemeModeOptionCard(
                        title = "Default",
                        icon = Icons.Default.SettingsSuggest,
                        selected = selectedTheme == "SYSTEM",
                        onClick = { onThemeSelected("SYSTEM") },
                        modifier = Modifier.weight(1f)
                    )

                    // Light Theme Selector
                    ThemeModeOptionCard(
                        title = "Terang",
                        icon = Icons.Default.LightMode,
                        selected = selectedTheme == "LIGHT",
                        onClick = { onThemeSelected("LIGHT") },
                        modifier = Modifier.weight(1f)
                    )

                    // Dark Theme Selector
                    ThemeModeOptionCard(
                        title = "Gelap",
                        icon = Icons.Default.DarkMode,
                        selected = selectedTheme == "DARK",
                        onClick = { onThemeSelected("DARK") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // FIREBASE CLOUD SYNC CARD
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Sinkronisasi Cloud (Firebase Live)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mengamankan data keuangan Anda secara online agar dapat diakses dari perangkat manapun.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))

                if (isLoggedIn) {
                    // Logged In UI
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Terhubung dengan Akun Cloud",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Email: $userEmail",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Status: Sinkronisasi otomatis aktif saat Anda menambah, mengubah, atau menghapus transaksi / tagihan lokal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    isProcessing = true
                                    syncManager.performFullSync(
                                        onSuccess = {
                                            isProcessing = false
                                            Toast.makeText(context, "Sinkronisasi data berhasil diselesaikan!", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { e ->
                                            isProcessing = false
                                            Toast.makeText(context, "Gagal sinkronisasi: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                enabled = !isProcessing,
                                modifier = Modifier.weight(1.3f).testTag("sync_now_button")
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sinkron Sekarang", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                    Toast.makeText(context, "Anda telah keluar dari akun cloud.", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isProcessing,
                                modifier = Modifier.weight(1f).testTag("logout_button")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Keluar", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Logged Out (Login/Register Form) UI
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { isRegisterMode = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "MASUK (LOGIN)",
                                    fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isRegisterMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            TextButton(
                                onClick = { isRegisterMode = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "DAFTAR BARU",
                                    fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isRegisterMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        OutlinedTextField(
                            value = emailText,
                            onValueChange = { emailText = it },
                            label = { Text("Email Firebase / Gmail") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("firebase_email_input")
                        )

                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it },
                            label = { Text("Password (Min 6 Karakter)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("firebase_password_input")
                        )

                        Button(
                            onClick = {
                                if (emailText.trim().isEmpty() || passwordText.trim().isEmpty()) {
                                    Toast.makeText(context, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (passwordText.length < 6) {
                                    Toast.makeText(context, "Password minimal harus 6 karakter!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isProcessing = true
                                val authInstance = com.google.firebase.auth.FirebaseAuth.getInstance()

                                if (isRegisterMode) {
                                    // Sign Up
                                    authInstance.createUserWithEmailAndPassword(emailText.trim(), passwordText.trim())
                                        .addOnSuccessListener {
                                            isProcessing = false
                                            Toast.makeText(context, "Akun berhasil didaftarkan dan masuk!", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            isProcessing = false
                                            Toast.makeText(context, "Gagal mendaftar: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    // Sign In
                                    authInstance.signInWithEmailAndPassword(emailText.trim(), passwordText.trim())
                                        .addOnSuccessListener {
                                            isProcessing = false
                                            Toast.makeText(context, "Selamat datang kembali!", Toast.LENGTH_SHORT).show()
                                            
                                            // Automatically perform full sync after successful login
                                            syncManager.performFullSync(
                                                onSuccess = {
                                                    Toast.makeText(context, "Sinkronisasi awal berhasil diselesaikan!", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = { syncError ->
                                                    Log.e("FirebaseSync", "Initial sync failed", syncError)
                                                }
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            isProcessing = false
                                            Toast.makeText(context, "Gagal masuk: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth().testTag("auth_submit_button")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (isRegisterMode) "Daftar Akun Baru" else "Masuk ke Akun Cloud", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (isLoggedIn) {
            val syncLogs by syncManager.syncLogs.collectAsState()
            val lastError by syncManager.lastError.collectAsState()
            
            CloudLogMonitorSection(
                syncLogs = syncLogs,
                lastError = lastError,
                onClearLogs = { syncManager.clearLogs() },
                onClearError = { syncManager.clearLastError() }
            )
        }

        // CREDIT CARD BILLING LOGIC CARD
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Automasi Kartu Kredit (CC)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                Text(
                    text = "Bulan Ini Belanja CC: " + formatRupiah(creditCardDebt),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "💡 Cara Kerja:\nSemua pengeluaran harian Anda dengan metode bayar \"Kartu Kredit\" akan terakumulasi otomatis. Di tanggal 1 awal bulan berikutnya, sistem akan mengompilasi total pengeluaran tersebut dan menambahkannya secara otomatis sebagai tagihan (Bill) baru bernama \"Tagihan Kartu Kredit - [Bulan Lalu]\" dengan jatuh tempo tanggal 10.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Button(
                    onClick = onCheckCCNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Periksa Tagihan CC Baru", fontWeight = FontWeight.Bold)
                }
            }
        }

        // CLOUD HOSTING & ONLINE SYNC FAQ CARD
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Panduan Online & Sinkronisasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))

                Text(
                    text = "Apakah Aplikasi ini Bisa Dionlinekan?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "Ya, Tentu Saja Bisa! Saat ini aplikasi menggunakan database lokal (Room SQLite) yang sangat cepat dan bekerja 100% offline di satu perangkat Anda.\n\nUntuk membuatnya bisa login dan tersinkronisasi di banyak HP / perangkat secara real-time, Anda dapat bermigrasi ke arsitektur cloud. Berikut adalah beberapa metode terbaik:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                // FAQ Steps
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FaqStepItem(
                        number = "1",
                        title = "Firebase Cloud Firestore & Auth (Tercepat & Mudah)",
                        desc = "Menggunakan Firebase Authentication untuk login (Google Login / Email) dan Firestore Database untuk mengganti Room. Data akan disinkronkan otomatis dari SDK Firebase secara real-time di semua perangkat HP Android Anda."
                    )
                    FaqStepItem(
                        number = "2",
                        title = "Supabase / PostgreSQL (Sangat Kuat & Open-Source)",
                        desc = "Supabase menyediakan database PostgreSQL cloud dan REST API instan. Anda bisa menggunakan library Ktor/Retrofit di Android untuk melakukan request data transaksi setelah login menggunakan Supabase Auth."
                    )
                    FaqStepItem(
                        number = "3",
                        title = "Backend API Sendiri (Ktor / Spring / Express)",
                        desc = "Membuat server backend mandiri (misal menggunakan Kotlin Ktor Server / Node.js) yang terhubung ke database cloud (seperti PostgreSQL / MySQL), lalu mengekspos REST API JSON untuk diakses oleh aplikasi Android ini."
                    )
                }
            }
        }
    }
}

@Composable
fun FaqStepItem(
    number: String,
    title: String,
    desc: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ThemeModeOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else contentColor
            )
        }
    }
}

fun formatLogTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun CloudLogMonitorSection(
    syncLogs: List<com.example.data.SyncLog>,
    lastError: String?,
    onClearLogs: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Log Monitoring Sinkronisasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Aktivitas data transfer hosting ke Firebase secara real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (syncLogs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearLogs,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Realtime Connection / Status Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = (if (lastError != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (lastError != null) Color(0xFFEF4444) else Color(0xFF10B981),
                            shape = CircleShape
                        )
                )
                Text(
                    text = if (lastError != null) "Ada Masalah Koneksi / Transfer Data" else "Koneksi Cloud Real-time Aktif",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            // Error Transfer Alert Banner
            if (lastError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("realtime_error_banner")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Error Real-time Data Transfer!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = "TUTUP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { onClearError() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            if (syncLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Belum ada aktivitas transaksi cloud.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Scrollable container for logs (limited to a max height)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    syncLogs.forEach { log ->
                        val (icon, tint) = when (log.status) {
                            "SUCCESS" -> Icons.Default.CheckCircle to Color(0xFF10B981)
                            "FAILED" -> Icons.Default.Cancel to Color(0xFFEF4444)
                            "RUNNING" -> Icons.Default.Sync to Color(0xFF3B82F6)
                            else -> Icons.Default.Info to MaterialTheme.colorScheme.primary
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF1F5F9).copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = log.status,
                                tint = tint,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 1.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val badgeLabel = when (log.type) {
                                        "UPLOAD" -> "UNGGAH"
                                        "DOWNLOAD" -> "UNDUH"
                                        "DELETE" -> "HAPUS"
                                        "SUCCESS" -> "SUKSES"
                                        "ERROR" -> "ERROR"
                                        else -> log.type
                                    }
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tint,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = formatLogTime(log.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
