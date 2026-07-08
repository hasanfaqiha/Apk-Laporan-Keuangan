package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    text = { Text("Info & Fitur", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) }
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
    creditCardDebt: Double,
    onCheckCCNow: () -> Unit,
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
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
