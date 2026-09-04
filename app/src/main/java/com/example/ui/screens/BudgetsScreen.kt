package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Budget
import com.example.data.computeBudgetUsages
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.formatRupiah
import com.example.viewmodel.parseAmount

/**
 * Screen for setting a monthly spending limit per expense category. Usage is
 * computed live against the current calendar month; categories that already
 * crossed their limit are flagged red and trigger a notification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val expenseCategories by viewModel.categories.collectAsState()

    val usages = remember(transactions, budgets) { computeBudgetUsages(transactions, budgets) }
    val expenseCats = expenseCategories.filter { it.type == "EXPENSE" }.map { it.name }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Budget Bulanan",
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
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah budget kategori")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (usages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Belum ada budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tetapkan batas pengeluaran bulanan per kategori untuk mengendalikan anggaran. Ketuk + untuk mulai.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(usages, key = { it.budget.id }) { usage ->
                    BudgetUsageCard(
                        budget = usage.budget,
                        spent = usage.spent,
                        exceeded = usage.exceeded,
                        onDelete = {
                            viewModel.deleteBudget(usage.budget.id)
                            Toast.makeText(context, "Budget dihapus", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            categories = expenseCats,
            onDismiss = { showAddDialog = false },
            onSave = { category, limit ->
                viewModel.upsertBudget(category, limit)
                showAddDialog = false
                Toast.makeText(context, "Budget '$category' disimpan", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun BudgetUsageCard(
    budget: Budget,
    spent: Double,
    exceeded: Boolean,
    onDelete: () -> Unit
) {
    val ratio = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
    val barColor = when {
        exceeded -> Color(0xFFEF4444)
        ratio >= 0.8f -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = budget.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus budget",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatRupiah(spent)} dari ${formatRupiah(budget.monthlyLimit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%.0f", ratio.coerceIn(0f, 1f) * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            if (exceeded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ Melebihi budget bulan ini!",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun AddBudgetDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "") }
    var limitText by remember { mutableStateOf("") }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Budget Kategori", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Batas pengeluaran bulanan (reset otomatis tiap bulan).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Kategori", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedButton(
                        onClick = { categoryMenuOpen = true },
                        modifier = Modifier.fillMaxWidth().testTag("budget_category_picker")
                    ) {
                        Text(selectedCategory.ifEmpty { "Pilih kategori" })
                    }
                    DropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false }
                    ) {
                        categories.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedCategory = name
                                    categoryMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Batas bulanan (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kosongkan atau isi 0 untuk menghapus batas kategori ini.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = parseAmount(limitText)
                    if (selectedCategory.isBlank()) {
                        Toast.makeText(context, "Pilih kategori dulu!", Toast.LENGTH_SHORT).show()
                    } else if (limit < 0) {
                        Toast.makeText(context, "Jumlah tidak valid!", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(selectedCategory, limit)
                    }
                },
                modifier = Modifier.testTag("save_budget_button")
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
