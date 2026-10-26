package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MerchSpringTheme {
                MerchSpringApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchSpringApp(viewModel: MainViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle Snackbars
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessage.value = null
        }
    }

    // Android Hardware / Gesture BackHandler
    BackHandler(enabled = currentScreen != AppScreen.OVERVIEW) {
        viewModel.currentScreen.value = AppScreen.OVERVIEW
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightNavy)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandTeal
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = null,
                                tint = MidnightNavy,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MerchSpring AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Your Business, in Sync.",
                                fontSize = 11.sp,
                                color = BrandTealLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = settings?.businessName ?: "MerchSpring Retail",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "GSTIN: ${settings?.gstin ?: "29AAAAA0000A1Z5"}",
                        fontSize = 11.sp,
                        color = LightSlate
                    )
                }

                HorizontalDivider(color = LightBorder)

                // Navigation Items List
                val navigationItems = listOf(
                    Triple(AppScreen.OVERVIEW, "Overview", Icons.Default.Dashboard),
                    Triple(AppScreen.POS_BILLING, "Quick POS Billing", Icons.Default.PointOfSale),
                    Triple(AppScreen.INVENTORY, "Inventory & Dead Stock", Icons.Default.Inventory2),
                    Triple(AppScreen.SALES, "Sales & Orders", Icons.Default.ShoppingCart),
                    Triple(AppScreen.CASH_REGISTER, "Daily Cash Register", Icons.Default.AccountBalanceWallet),
                    Triple(AppScreen.STOCK_TRANSFERS, "Transfers & Reorder", Icons.Default.SwapHoriz),
                    Triple(AppScreen.PURCHASES, "Purchases & Procurement", Icons.Default.LocalShipping),
                    Triple(AppScreen.CUSTOMERS, "Customers & Dues", Icons.Default.People),
                    Triple(AppScreen.EXPENSES, "Expenses", Icons.Default.ReceiptLong),
                    Triple(AppScreen.INVOICES, "GST Invoices", Icons.Default.Description),
                    Triple(AppScreen.REPORTS, "Reports & Profit P&L", Icons.Default.Analytics),
                    Triple(AppScreen.AI_ASSISTANT, "AI Business Assistant", Icons.Default.AutoAwesome),
                    Triple(AppScreen.WAREHOUSE_3D, "3D Warehouse Twin", Icons.Default.ViewInAr),
                    Triple(AppScreen.NOTIFICATIONS, "Alerts & Notifications", Icons.Default.Notifications),
                    Triple(AppScreen.SETTINGS, "Settings & Roles", Icons.Default.Settings)
                )

                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    navigationItems.forEach { (screen, label, icon) ->
                        val isSelected = currentScreen == screen

                        NavigationDrawerItem(
                            label = { Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            badge = {
                                if (screen == AppScreen.NOTIFICATIONS && metrics.unreadNotificationsCount > 0) {
                                    Badge(containerColor = StatusCoral) {
                                        Text("${metrics.unreadNotificationsCount}")
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                viewModel.currentScreen.value = screen
                                coroutineScope.launch { drawerState.close() }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BrandTealContainer,
                                selectedTextColor = BrandTealLight,
                                selectedIconColor = BrandTealLight,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentScreen.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = settings?.businessName ?: "MerchSpring AI",
                                fontSize = 11.sp,
                                color = BrandTeal
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Quick 3D Twin Jump Icon
                        IconButton(
                            onClick = { viewModel.currentScreen.value = AppScreen.WAREHOUSE_3D }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "3D Twin",
                                tint = if (currentScreen == AppScreen.WAREHOUSE_3D) BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Notifications Icon with live unread badge
                        IconButton(
                            onClick = { viewModel.currentScreen.value = AppScreen.NOTIFICATIONS }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (metrics.unreadNotificationsCount > 0) {
                                        Badge(containerColor = StatusCoral) {
                                            Text("${metrics.unreadNotificationsCount}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (currentScreen == AppScreen.NOTIFICATIONS) BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val bottomNavDestinations = listOf(
                        Triple(AppScreen.OVERVIEW, "Overview", Icons.Default.Dashboard),
                        Triple(AppScreen.POS_BILLING, "POS Bill", Icons.Default.PointOfSale),
                        Triple(AppScreen.INVENTORY, "Inventory", Icons.Default.Inventory2),
                        Triple(AppScreen.WAREHOUSE_3D, "3D Twin", Icons.Default.ViewInAr),
                        Triple(AppScreen.AI_ASSISTANT, "AI Help", Icons.Default.AutoAwesome)
                    )

                    bottomNavDestinations.forEach { (screen, label, icon) ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.currentScreen.value = screen },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) MidnightNavy else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = BrandTeal
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.OVERVIEW -> OverviewDashboardScreen(viewModel = viewModel)
                    AppScreen.POS_BILLING -> PosQuickBillingScreen(viewModel = viewModel)
                    AppScreen.INVENTORY -> InventoryScreen(viewModel = viewModel)
                    AppScreen.SALES -> SalesOrdersScreen(viewModel = viewModel)
                    AppScreen.CASH_REGISTER -> CashClosingScreen(viewModel = viewModel)
                    AppScreen.STOCK_TRANSFERS -> ReorderAndTransfersScreen(viewModel = viewModel)
                    AppScreen.PURCHASES -> PurchasesScreen(viewModel = viewModel)
                    AppScreen.CUSTOMERS, AppScreen.SUPPLIERS -> CustomersSuppliersScreen(viewModel = viewModel)
                    AppScreen.EXPENSES -> ExpensesReportsScreen(viewModel = viewModel, initialTab = 0)
                    AppScreen.REPORTS -> ExpensesReportsScreen(viewModel = viewModel, initialTab = 1)
                    AppScreen.INVOICES -> GstInvoiceScreen(viewModel = viewModel)
                    AppScreen.AI_ASSISTANT -> AiAssistantScreen(viewModel = viewModel)
                    AppScreen.WAREHOUSE_3D -> Warehouse3DScreen(viewModel = viewModel)
                    AppScreen.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
