package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Group
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.theme.PremiumPrimary
import com.example.ui.theme.PremiumPrimaryDark
import com.example.ui.theme.PremiumPrimaryLight
import com.example.ui.theme.PremiumSecondary
import com.example.ui.theme.PremiumGradientStart
import com.example.ui.theme.PremiumGradientMid
import com.example.ui.theme.PremiumGradientEnd
import com.example.ui.theme.PremiumDarkGradientStart
import com.example.ui.theme.PremiumDarkGradientEnd
import com.example.ui.theme.PremiumGreen
import com.example.ui.theme.PremiumRed
import com.example.ui.theme.PremiumAmber
import com.example.ui.theme.PremiumBlue
import com.example.ui.theme.PremiumGreenLight
import com.example.ui.theme.PremiumRedLight
import com.example.ui.theme.PremiumAmberLight
import com.example.ui.theme.PremiumBlueLight
import com.example.ui.theme.PremiumSurface
import com.example.ui.theme.PremiumSurfaceVariant
import com.example.ui.theme.PremiumBackground
import com.example.ui.theme.PremiumDarkBackground
import com.example.ui.theme.PremiumDarkSurface
import com.example.ui.theme.PremiumDarkSurfaceVariant
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.formatRupiah

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onQuickAddClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.financeSummary.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val recentTransactions = allTransactions.take(4)
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val lastSyncError by viewModel.syncManager.lastError.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 0.dp, 0.dp, 100.dp)
    ) {
        // Full Gradient Header Section
        item {
            GradientHeaderSection(
                totalBalance = summary.totalBalance,
                cashOnHand = summary.cashOnHand,
                bankBalance = summary.bankBalance,
                isLoggedIn = isLoggedIn,
                currentUserEmail = currentUserEmail,
                onNavigateToSettings = onNavigateToSettings,
                onQuickAddClick = onQuickAddClick
            )
        }

        // Upgrade Banner
        item {
            UpgradeBanner(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        // Quick Actions Grid
        item {
            QuickActionsGrid(
                modifier = Modifier.padding(horizontal = 20.dp),
                onQuickAddClick = onQuickAddClick,
                onNavigateToBills = onNavigateToBills,
                onNavigateToSavings = { /* TODO */ },
                onNavigateToCards = { /* TODO */ }
            )
        }

        // Recent Transactions
        item {
            RecentTransactionsSection(
                transactions = recentTransactions,
                onNavigateToAll = onNavigateToTransactions,
                onDelete = { viewModel.deleteTransaction(it) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // Cloud Sync Status Badge
        item {
            CloudConnectionStatusBadge(
                isLoggedIn = isLoggedIn,
                email = currentUserEmail,
                onClick = onNavigateToSettings,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Sync Error Banner
        if (lastSyncError != null) {
            item {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error Sync",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connection Error",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = lastSyncError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(onClick = { viewModel.syncManager.clearLastError() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacer for nav bar
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GradientHeaderSection(
    totalBalance: Double,
    cashOnHand: Double,
    bankBalance: Double,
    isLoggedIn: Boolean,
    currentUserEmail: String?,
    onNavigateToSettings: () -> Unit,
    onQuickAddClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var balanceVisible by remember { mutableStateOf(true) }

    val gradientColors = if (isDark) {
        listOf(PremiumDarkGradientStart, PremiumDarkGradientEnd)
    } else {
        listOf(PremiumGradientStart, PremiumGradientMid, PremiumGradientEnd)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(0.dp),
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
                .padding(horizontal = 24.dp, vertical = 56.dp)
                .padding(bottom = 32.dp)
        ) {
            // Subtle pattern overlay
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokeColor = Color.White.copy(alpha = 0.03f)
                val step = 40f
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(strokeColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 0.5f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(strokeColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 0.5f)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Top Row: Avatar + Greeting + Notification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Good Day!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Pengguna",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onNavigateToSettings() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profil",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Balance Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Account Balance",
                                style = MaterialTheme.typography.labelSmall,
color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (balanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle saldo",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { balanceVisible = !balanceVisible }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (balanceVisible) formatRupiah(totalBalance) else "••••••••",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Account no: **** **** 3569",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientActionButton(
                        text = "↙ Add Money",
                        onClick = { onQuickAddClick("INCOME") },
                        modifier = Modifier.weight(1f)
                    )
                    GradientActionButton(
                        text = "↗ Send Money",
                        onClick = { onQuickAddClick("EXPENSE") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun GradientActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.12f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = if (isDark) 0.25f else 0.2f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun UpgradeBanner(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PremiumAmberLight,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "⚠️", fontSize = 22.sp)
                    }
                }
                Column {
                    Text(
                        text = "Upgrade Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Upgrade your account for more features.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    onQuickAddClick: (String) -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onNavigateToCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionItem(
                icon = "💰",
                label = "Topup",
                iconBackground = PremiumSurfaceVariant,
                onClick = { onQuickAddClick("INCOME") },
                modifier = Modifier.weight(1f)
            )
            QuickActionItem(
                icon = "📋",
                label = "Bills",
                iconBackground = PremiumAmberLight,
                onClick = onNavigateToBills,
                modifier = Modifier.weight(1f)
            )
            QuickActionItem(
                icon = "🏦",
                label = "Savings",
                iconBackground = PremiumGreenLight,
                onClick = onNavigateToSavings,
                modifier = Modifier.weight(1f)
            )
            QuickActionItem(
                icon = "💳",
                label = "Cards",
                iconBackground = PremiumBlueLight,
                onClick = onNavigateToCards,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: String,
    label: String,
    iconBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp)
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = iconBackground,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = icon, fontSize = 20.sp)
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<Transaction>,
    onNavigateToAll: () -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Riwayat",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "View All →",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onNavigateToAll() }
            )
        }

        // Transaction List
        if (transactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Kosong",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = "No Transactions Yet",
                    description = "Tap Add Money or Send Money to start tracking your finances.",
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                transactions.forEach { trans ->
                    TransactionListItem(
                        transaction = trans,
                        onDelete = { onDelete(trans.id) },
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CloudConnectionStatusBadge(
    isLoggedIn: Boolean,
    email: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isLoggedIn) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLoggedIn) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cloud_connection_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = if (isLoggedIn) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isLoggedIn) {
                    PremiumGreen
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) "Cloud Sync Active" else "Offline Mode (Local Storage)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isLoggedIn && !email.isNullOrBlank()) {
                    Text(
                        text = "Connected as: $email",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Tap to connect & secure data online",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == "EXPENSE"
    val isTransfer = transaction.type == "WITHDRAWAL" || transaction.type == "DEPOSIT"

    val categoryColor = when (transaction.category) {
        "Makanan & Minuman" -> Color(0xFFF57C00)
        "Transportasi" -> Color(0xFF0288D1)
        "Sewa & Tagihan" -> Color(0xFF7B1FA2)
        "Belanja" -> Color(0xFFC2185B)
        "Hiburan" -> Color(0xFFE91E63)
        "Gaji" -> Color(0xFF388E3C)
        "Investasi" -> Color(0xFF1976D2)
        "Bonus" -> Color(0xFFFBC02D)
        else -> MaterialTheme.colorScheme.primary
    }

    val (arrowIcon, arrowColor, arrowBg) = when {
        isExpense -> Triple(Icons.Default.ArrowDownward, PremiumRed, PremiumRedLight)
        transaction.type == "INCOME" -> Triple(Icons.Default.ArrowUpward, PremiumGreen, PremiumGreenLight)
        transaction.type == "WITHDRAWAL" -> Triple(Icons.Default.ArrowDownward, PremiumAmber, PremiumAmberLight)
        else -> Triple(Icons.Default.ArrowUpward, PremiumBlue, PremiumBlueLight)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Arrow indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = arrowBg,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = arrowIcon,
                        contentDescription = transaction.category,
                        tint = arrowColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " • ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = when (transaction.type) {
                            "WITHDRAWAL" -> "Bank ➔ Cash"
                            "DEPOSIT" -> "Cash ➔ Bank"
                            else -> when (transaction.accountType) {
                                "CREDIT_CARD" -> "Credit Card"
                                "BANK" -> "Bank"
                                else -> "Cash"
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                val sign = if (isExpense || transaction.type == "WITHDRAWAL") "-" else "+"
                val amountColor = if (isExpense || transaction.type == "WITHDRAWAL") PremiumRed else PremiumGreen
                Text(
                    text = "$sign${formatRupiah(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = formatDate(transaction.dateMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
}
        }
    }
}

@Composable
fun QuickActionsSection(
    onQuickAddClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Log Expense (Pengeluaran)
            Button(
                onClick = { onQuickAddClick("EXPENSE") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else PremiumRedLight,
                    contentColor = if (isDark) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF991B1B)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("quick_add_expense_button"),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = "Pengeluaran",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pengeluaran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Log Income (Pemasukan)
            Button(
                onClick = { onQuickAddClick("INCOME") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else PremiumGreenLight,
                    contentColor = if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("quick_add_income_button"),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Pemasukan",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pemasukan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceMiniCard(
    title: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) else Color(0xFFF1F5F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatRupiah(amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BillAlertCard(
    overdueCount: Int,
    upcomingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = overdueCount > 0
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    } else {
        PremiumRedLight
    }

    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    } else {
        Color(0xFFFECACA)
    }

    val iconBgColor = PremiumRed
    val textColor = if (isDark) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        Color(0xFF7F1D1D)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onClick() }
            .testTag("bill_alert_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBgColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCritical) "Overdue Bills!" else "Upcoming Bills",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                val text = buildString {
                    if (overdueCount > 0) append("$overdueCount overdue! ")
                    if (upcomingCount > 0) append("$upcomingCount due soon.")
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconBgColor,
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = "PAY",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}