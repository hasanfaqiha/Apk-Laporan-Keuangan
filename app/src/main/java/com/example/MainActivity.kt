package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FinanceDatabase
import com.example.data.FinanceRepository
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.BillsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.AuthGateScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.FinanceViewModelFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            
            // Initialize local persistence
            val database = remember { FinanceDatabase.getDatabase(context) }
            val repository = remember { FinanceRepository(database.financeDao) }
            val factory = remember { FinanceViewModelFactory(repository) }
            
            val viewModel: FinanceViewModel = viewModel(factory = factory)

            // Trigger active notifications of overdue/due bills & load saved theme preference on launch
            LaunchedEffect(Unit) {
                viewModel.loadTheme(context)
                viewModel.triggerBillReminders(context)
                viewModel.checkAndGenerateCreditCardBills()
            }

            val selectedTheme by viewModel.selectedTheme.collectAsState()
            val darkTheme = when (selectedTheme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val hasSkippedAuth by viewModel.hasSkippedAuth.collectAsState()

            MyApplicationTheme(darkTheme = darkTheme) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn || hasSkippedAuth) {
                        FinanceAppFrame(viewModel = viewModel)
                    } else {
                        AuthGateScreen(
                            viewModel = viewModel,
                            onSkip = {
                                viewModel.hasSkippedAuth.value = true
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class FinanceTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dasbor", Icons.Default.Dashboard, "tab_dashboard"),
    TRANSACTIONS("Transaksi", Icons.Default.ReceiptLong, "tab_transactions"),
    ANALYSIS("Analisis", Icons.Default.PieChart, "tab_analysis"),
    BILLS("Tagihan", Icons.Default.Notifications, "tab_bills"),
    SETTINGS("Admin/Set", Icons.Default.Settings, "tab_settings")
}

@Composable
fun FinanceAppFrame(viewModel: FinanceViewModel) {
    var currentTab by remember { mutableStateOf(FinanceTab.DASHBOARD) }
    
    // States for navigating from quick-actions and triggering form opening automatically
    var showAddFormInitially by remember { mutableStateOf(false) }
    var initialFormType by remember { mutableStateOf("EXPENSE") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_navigation_bar")
            ) {
                FinanceTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(text = tab.title) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()) // Standard edge-to-edge handling
        ) {
            when (currentTab) {
                FinanceTab.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTransactions = { currentTab = FinanceTab.TRANSACTIONS },
                        onNavigateToBills = { currentTab = FinanceTab.BILLS },
                        onNavigateToSettings = { currentTab = FinanceTab.SETTINGS },
                        onQuickAddClick = { type ->
                            initialFormType = type
                            showAddFormInitially = true
                            currentTab = FinanceTab.TRANSACTIONS
                        },
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                    )
                }
                FinanceTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        viewModel = viewModel,
                        showAddFormInitially = showAddFormInitially,
                        initialType = initialFormType,
                        onFormDismissed = {
                            showAddFormInitially = false
                        },
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                    )
                }
                FinanceTab.ANALYSIS -> {
                    AnalysisScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                    )
                }
                FinanceTab.BILLS -> {
                    BillsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                    )
                }
                FinanceTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                    )
                }
            }
        }
    }
}
