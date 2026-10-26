package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun OverviewDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val products by viewModel.products.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val pendingOrders = remember(orders) {
        orders.filter { it.status == OrderStatus.CONFIRMED }
    }
    val lowStockProducts = remember(products) {
        products.filter { it.isLowStock || it.isOutOfStock }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // Business Welcome Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings?.businessName ?: "MerchSpring Retail",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = settings?.tagline ?: "Your Business, in Sync.",
                            fontSize = 12.sp,
                            color = BrandTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GSTIN: ${settings?.gstin ?: "29AAAAA0000A1Z5"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    settings?.activeRole?.let { role ->
                        RoleBadge(role = role)
                    }
                }
            }
        }

        // Quick Operations Hub Shortcuts
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Operations Hub",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandTealContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.currentScreen.value = AppScreen.POS_BILLING }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, tint = BrandTealLight, modifier = Modifier.size(24.dp))
                            Text("POS Billing", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandTealLight)
                            Text("Fast checkout & returns", fontSize = 10.sp, color = LightSlate)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.currentScreen.value = AppScreen.CASH_REGISTER }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(24.dp))
                            Text("Cash Register", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Closing & variance count", fontSize = 10.sp, color = LightSlate)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.currentScreen.value = AppScreen.STOCK_TRANSFERS }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = StatusBlue, modifier = Modifier.size(24.dp))
                            Text("Transfers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Branch routing & reorder", fontSize = 10.sp, color = LightSlate)
                        }
                    }
                }
            }
        }

        // 3D Digital Twin Mini-Preview Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "3D Twin",
                            tint = BrandTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "3D Interactive Warehouse",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = { viewModel.currentScreen.value = AppScreen.WAREHOUSE_3D }
                    ) {
                        Text("Fullscreen 3D", color = BrandTeal, fontSize = 12.sp)
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = BrandTeal,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Warehouse3DView(
                    products = products,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    isInteractive = true
                )
            }
        }

        // Primary KPI Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Financial & Inventory Pulse",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Realized Sales",
                        value = metrics.todaySalesPaise.toRupeesString(),
                        subtitle = "${metrics.completedSalesCount} orders fulfilled",
                        icon = Icons.Default.TrendingUp,
                        iconTint = StatusGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.currentScreen.value = AppScreen.SALES }
                    )

                    MetricCard(
                        title = "Gross Profit",
                        value = metrics.grossProfitPaise.toRupeesString(),
                        subtitle = "${String.format(java.util.Locale.US, "%.1f", metrics.grossProfitMarginPercent)}% margin",
                        icon = Icons.Default.Savings,
                        iconTint = BrandTeal,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.currentScreen.value = AppScreen.REPORTS }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Stock Valuation",
                        value = metrics.totalValuationPaise.toRupeesString(),
                        subtitle = "${products.size} catalogue SKUs",
                        icon = Icons.Default.Inventory2,
                        iconTint = StatusBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.currentScreen.value = AppScreen.INVENTORY }
                    )

                    MetricCard(
                        title = "Customer Dues",
                        value = metrics.customerReceivablesPaise.toRupeesString(),
                        subtitle = "Pending receivables",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = if (metrics.customerReceivablesPaise > 0) StatusAmber else StatusGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.currentScreen.value = AppScreen.CUSTOMERS }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Recorded Expenses",
                        value = metrics.totalExpensesPaise.toRupeesString(),
                        subtitle = "Rent, Power, Chai",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = StatusCoral,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.currentScreen.value = AppScreen.EXPENSES }
                    )

                    MetricCard(
                        title = "Supplier Payables",
                        value = metrics.supplierPayablesPaise.toRupeesString(),
                        subtitle = "Outstanding POs",
                        icon = Icons.Default.LocalShipping,
                        iconTint = LightSlate,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.currentScreen.value = AppScreen.PURCHASES }
                    )
                }
            }
        }

        // Quick AI Business Assistant Prompt Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(BrandTeal, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = MidnightNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "MerchSpring AI Assistant",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        TextButton(
                            onClick = { viewModel.currentScreen.value = AppScreen.AI_ASSISTANT }
                        ) {
                            Text("Open Chat", color = BrandTeal, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Instant grounded answers on stock, sales trends, and Hindi payment reminders.",
                        fontSize = 12.sp,
                        color = LightSlate
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val prompts = listOf(
                            "Summarize today's sales",
                            "Which items need restock priority?",
                            "Draft Hinglish payment reminder",
                            "Explain gross profit margin"
                        )
                        items(prompts) { prompt ->
                            Surface(
                                modifier = Modifier.clickable {
                                    viewModel.currentScreen.value = AppScreen.AI_ASSISTANT
                                    viewModel.sendAiPrompt(prompt)
                                },
                                color = SurfaceDarkCard,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                            ) {
                                Text(
                                    text = prompt,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 11.sp,
                                    color = BrandTealLight
                                )
                            }
                        }
                    }
                }
            }
        }

        // Orders Requiring Attention (Pending Confirmed Orders)
        if (pendingOrders.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Orders Awaiting Dispatch (${pendingOrders.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { viewModel.currentScreen.value = AppScreen.SALES }) {
                            Text("View All", color = BrandTeal, fontSize = 12.sp)
                        }
                    }

                    pendingOrders.forEach { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = order.orderNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${order.customerName} • ${order.totalPaise.toRupeesString()}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { viewModel.dispatchOrder(order.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandTeal,
                                        contentColor = MidnightNavy
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Dispatch",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Dispatch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Low & Out of Stock Alerts
        if (lowStockProducts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Critical Inventory Alerts (${lowStockProducts.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { viewModel.currentScreen.value = AppScreen.INVENTORY }) {
                            Text("Inventory", color = BrandTeal, fontSize = 12.sp)
                        }
                    }

                    lowStockProducts.forEach { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (product.isOutOfStock) StatusCoralContainer else StatusAmberContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (product.isOutOfStock) StatusCoral else Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Available: ${product.availableStock.toInt()} ${product.unit} (Physical: ${product.physicalStock.toInt()}, Reserved: ${product.reservedStock.toInt()})",
                                        fontSize = 11.sp,
                                        color = DarkSlate
                                    )
                                }

                                StatusBadge(
                                    text = if (product.isOutOfStock) "OUT OF STOCK" else "LOW STOCK",
                                    backgroundColor = if (product.isOutOfStock) StatusCoral else StatusAmber,
                                    textColor = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
