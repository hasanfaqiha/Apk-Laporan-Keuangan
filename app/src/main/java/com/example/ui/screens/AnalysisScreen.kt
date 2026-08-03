package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.formatRupiah

@Composable
fun AnalysisScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.financeSummary.collectAsState()
    val scrollState = rememberScrollState()

    var analysisType by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME

    val totalIncome = summary.totalIncome
    val totalExpense = summary.totalExpense
    val savings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (savings / totalIncome) * 100 else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("analysis_screen")
    ) {
        // Headline
        Text(
            text = "Analisis Keuangan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp)
        )

        // General Info Cards (Income / Expense Side-by-Side)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FlowCard(
                title = "Total Pemasukan",
                amount = totalIncome,
                isIncome = true,
                modifier = Modifier.weight(1f)
            )
            FlowCard(
                title = "Total Pengeluaran",
                amount = totalExpense,
                isIncome = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Savings Rate Banner
        SavingsRateCard(
            savingsAmount = savings,
            savingsRate = savingsRate,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Graphic Tab selector (Pemasukan vs Pengeluaran)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TypeFilterChip(
                label = "Grafik Pengeluaran",
                selected = analysisType == "EXPENSE",
                onClick = { analysisType = "EXPENSE" },
                modifier = Modifier.weight(1f)
            )
            TypeFilterChip(
                label = "Grafik Pemasukan",
                selected = analysisType == "INCOME",
                onClick = { analysisType = "INCOME" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom Donut Chart Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (analysisType == "EXPENSE") "Proporsi Pengeluaran" else "Proporsi Pemasukan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Crossfade(targetState = analysisType) { type ->
                    val dataMap = if (type == "EXPENSE") summary.categoryExpenses else summary.categoryIncomes
                    if (dataMap.isEmpty()) {
                        EmptyState(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = "Grafik Kosong",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            title = "Belum Ada Riwayat",
                            description = "Data grafik akan muncul secara otomatis setelah Anda menambahkan beberapa transaksi.",
                            modifier = Modifier.height(200.dp)
                        )
                    } else {
                        DonutChart(data = dataMap)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Detailed Category Budgets Bars
        val activeMap = if (analysisType == "EXPENSE") summary.categoryExpenses else summary.categoryIncomes
        if (activeMap.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Rincian Kategori",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                val sortedList = activeMap.toList().sortedByDescending { it.second }
                val mapTotal = activeMap.values.sum()

                sortedList.forEachIndexed { index, (cat, amt) ->
                    val ratio = if (mapTotal > 0.0) (amt / mapTotal).toFloat() else 0f
                    val color = CategoryPalette.getOrElse(index) { Color.Gray }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = formatRupiah(amt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = color,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.1f", ratio * 100)}% dari total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun FlowCard(
    title: String,
    amount: Double,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    val themeColor = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.error
    val lightThemeColor = if (isIncome) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = lightThemeColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = themeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatRupiah(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = themeColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SavingsRateCard(
    savingsAmount: Double,
    savingsRate: Double,
    modifier: Modifier = Modifier
) {
    val (status, tips, tintColor, bgTint) = when {
        savingsRate >= 30.0 -> Quadruple(
            "Luar Biasa Hebat! 🎉",
            "Keuangan Anda sangat prima. Rasio tabungan Anda berada di atas standar aman 20%. Pertahankan kedisiplinan finansial ini untuk kebebasan finansial jangka panjang!",
            Color(0xFF10B981),
            Color(0xFFD1FAE5)
        )
        savingsRate in 10.0..30.0 -> Quadruple(
            "Keuangan Cukup Sehat 👍",
            "Bagus! Anda berhasil mengamankan surplus pemasukan. Pertahankan penghematan ini dan usahakan untuk mengalokasikan tabungan Anda ke rekening bank terpisah atau investasi.",
            Color(0xFF1565C0),
            Color(0xFFE3F2FD)
        )
        savingsRate in 0.0..10.0 -> Quadruple(
            "Waspada Defisit Uang ⚠️",
            "Hati-hati, sisa dana simpanan Anda sangat tipis bulan ini. Tinjau ulang pengeluaran sekunder Anda (seperti belanja hiburan atau kuliner berlebih) untuk memperbesar tabungan.",
            Color(0xFFE65100),
            Color(0xFFFFF3E0)
        )
        else -> Quadruple(
            "Keuangan Defisit Bahaya! 🚨",
            "Bahaya! Pengeluaran Anda melebihi total pemasukan bulan ini. Segera audit buku kas harian Anda, kurangi pengeluaran yang tidak mendesak, dan tunda pembelian barang mahal.",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgTint),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = "Tabungan",
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sisa Tabungan: ${formatRupiah(savingsAmount)} (${String.format("%.1f", savingsRate)}%)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tintColor
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = tintColor.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Saran",
                    tint = tintColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = tintColor
                    )
                    Text(
                        text = tips,
                        style = MaterialTheme.typography.bodySmall,
                        color = tintColor.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// Simple quadruple container helper
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
