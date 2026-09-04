package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.RecurringRule
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.formatRupiah
import com.example.viewmodel.parseAmount

private val FREQUENCY_OPTIONS = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
private fun frequencyLabel(freq: String): String = when (freq) {
    "DAILY" -> "Harian"
    "WEEKLY" -> "Mingguan"
    "YEARLY" -> "Tahunan"
    else -> "Bulanan"
}

/**
 * Full screen for managing recurring transactions. Each active rule generates
 * its entries automatically when the scheduled date passes (see
 * FinanceViewModel.generateDueRecurringTransactions).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rules by viewModel.recurringRules.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Transaksi Berulang",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_recurring_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah aturan berulang")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (rules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Belum ada transaksi berulang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Contoh: gaji bulanan, cicilan, langganan WiFi, atau pengeluaran rutin lain.\nKetuk + untuk membuat aturan baru.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    RecurringRuleCard(
                        rule = rule,
                        onToggle = { active -> viewModel.toggleRecurringRule(rule, active) },
                        onDelete = {
                            viewModel.deleteRecurringRule(rule.id)
                            Toast.makeText(context, "Aturan berulang dihapus", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecurringRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, type, accountType, category, frequency, note ->
                viewModel.addRecurringRule(
                    title = title,
                    amount = amount,
                    type = type,
                    accountType = accountType,
                    category = category,
                    frequency = frequency,
                    startMillis = System.currentTimeMillis(),
                    note = note
                )
                showAddDialog = false
                Toast.makeText(context, "Aturan berulang ditambahkan", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun RecurringRuleCard(
    rule: RecurringRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${frequencyLabel(rule.frequency)} · ${formatRupiah(rule.amount)} · ${rule.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Berikutnya: ${formatDate(rule.nextRunMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (rule.isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus aturan",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = rule.isActive,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("recurring_switch_${rule.id}")
            )
        }
    }
}

@Composable
private fun AddRecurringRuleDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, accountType: String, category: String, frequency: String, note: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Lain-lain") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var accountType by remember { mutableStateOf("CASH") }
    var frequency by remember { mutableStateOf("MONTHLY") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aturan Transaksi Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Berjalan otomatis mulai hari ini sesuai frekuensi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama (mis. Gaji, WiFi, Cicilan)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_title_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Jumlah (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Tipe", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Pengeluaran") }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Pemasukan") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Akun", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = accountType == "CASH",
                        onClick = { accountType = "CASH" },
                        label = { Text("Tunai") }
                    )
                    FilterChip(
                        selected = accountType == "BANK",
                        onClick = { accountType = "BANK" },
                        label = { Text("Bank") }
                    )
                    FilterChip(
                        selected = accountType == "CREDIT_CARD",
                        onClick = { accountType = "CREDIT_CARD" },
                        label = { Text("Kartu Kredit") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Frekuensi", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FREQUENCY_OPTIONS.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(frequencyLabel(freq)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_category_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = parseAmount(amountText)
                    if (title.trim().isEmpty()) {
                        Toast.makeText(context, "Nama transaksi wajib diisi!", Toast.LENGTH_SHORT).show()
                    } else if (amount <= 0) {
                        Toast.makeText(context, "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(title.trim(), amount, type, accountType, category.trim().ifEmpty { "Lain-lain" }, frequency, note.trim())
                    }
                },
                modifier = Modifier.testTag("save_recurring_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
