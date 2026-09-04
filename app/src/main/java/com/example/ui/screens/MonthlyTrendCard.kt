package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.viewmodel.formatRupiah
import java.util.Calendar

private val TrendIncomeColor = Color(0xFF10B981)
private val TrendExpenseColor = Color(0xFFEF4444)

/** One month bucket used by the trend card. */
data class MonthTrendPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

/**
 * Pure helper (unit-testable): buckets transactions into the last [count]
 * calendar months, oldest first.
 */
fun computeMonthlyTrend(
    transactions: List<Transaction>,
    count: Int = 6
): List<MonthTrendPoint> {
    val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )
    val cal = Calendar.getInstance()
    val nowYear = cal.get(Calendar.YEAR)
    val nowMonth = cal.get(Calendar.MONTH)

    // Build (year, month) pairs oldest -> newest for the last `count` months.
    val monthPairs = mutableListOf<Pair<Int, Int>>()
    var y = nowYear
    var m = nowMonth
    for (i in 0 until count) {
        monthPairs.add(0, y to m)
        m--
        if (m < 0) {
            m = 11
            y--
        }
    }

    return monthPairs.map { (year, month) ->
        val start = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.MONTH, 1)
        }.timeInMillis

        var income = 0.0
        var expense = 0.0
        for (t in transactions) {
            if (t.dateMillis < start || t.dateMillis >= end) continue
            when (t.type) {
                "INCOME" -> income += t.amount
                "EXPENSE" -> expense += t.amount
            }
        }
        MonthTrendPoint(label = "${monthNames[month]} ${year % 100}", income = income, expense = expense)
    }
}

/**
 * Interactive bar chart of the last 6 months. Tap a column to inspect that
 * month's income vs expense in detail.
 */
@Composable
fun MonthlyTrendCard(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val points = remember(transactions) { computeMonthlyTrend(transactions, 6) }
    var selectedIndex by remember { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    val selected = points.getOrNull(selectedIndex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Tren 6 Bulan Terakhir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(TrendIncomeColor, "Pemasukan")
                LegendDot(TrendExpenseColor, "Pengeluaran")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bar chart area
            val maxValue = points.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0
            val chartMax = if (maxValue > 0) maxValue else 1.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                points.forEachIndexed { index, point ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedIndex = index }
                            .background(
                                if (index == selectedIndex) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Two bars (expense overlaid, income) scaled to chartMax.
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TrendBar(value = point.income, max = chartMax, color = TrendIncomeColor)
                            TrendBar(value = point.expense, max = chartMax, color = TrendExpenseColor)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = point.label,
                            fontSize = 10.sp,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selected month detail
            if (selected != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = selected.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Masuk ${formatRupiah(selected.income)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TrendIncomeColor
                            )
                            Text(
                                text = "Keluar ${formatRupiah(selected.expense)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TrendExpenseColor
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Belum ada data transaksi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrendBar(value: Double, max: Double, color: Color) {
    val fraction = (value / max).toFloat().coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .width(10.dp)
            .height((120 * fraction).dp.coerceAtLeast(if (fraction > 0f) 4.dp else 2.dp))
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(if (value > 0) color else color.copy(alpha = 0.15f))
    )
}
