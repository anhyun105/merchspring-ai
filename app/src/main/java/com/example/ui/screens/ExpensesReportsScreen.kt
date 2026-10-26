package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.MetricCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesReportsScreen(
    viewModel: MainViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenses.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val products by viewModel.products.collectAsState()
    val orders by viewModel.orders.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0: Expenses, 1: Reports
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = BrandTeal,
                    contentColor = MidnightNavy
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandTeal
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Expenses (${expenses.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reports & Reconciliation", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // Expenses Tracker
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Expenses Recorded:", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = metrics.totalExpensesPaise.toRupeesString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusCoral
                                )
                            }
                        }
                    }

                    items(expenses, key = { it.id }) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Surface(
                                        color = BrandTealContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = expense.category,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandTealLight
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = expense.description,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${expense.date} • Mode: ${expense.paymentMethod}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = expense.amountPaise.toRupeesString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = StatusCoral
                                )
                            }
                        }
                    }
                }
            } else {
                // Reports & Reconciliation View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Text(
                            text = "Business Reconciliation Summary",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Grounded exclusively in actual database records and preserved cost snapshots.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Reconciled P&L Snapshot", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Net Sales (excl. GST, net of returns):", fontSize = 13.sp)
                                    Text(metrics.allTimeNetRevenuePaise.toRupeesString(), fontWeight = FontWeight.Bold, color = StatusGreen)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Gross Margin on Dispatches:", fontSize = 13.sp)
                                    Text(
                                        "${metrics.grossProfitPaise.toRupeesString()} (${String.format(java.util.Locale.US, "%.1f", metrics.grossProfitMarginPercent)}%)",
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTeal
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Operating Expenses Deducted:", fontSize = 13.sp)
                                    Text(metrics.totalExpensesPaise.toRupeesString(), fontWeight = FontWeight.Bold, color = StatusCoral)
                                }
                                HorizontalDivider()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val netOperating = metrics.grossProfitPaise - metrics.totalExpensesPaise
                                    Text("Net Operating Earnings:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        netOperating.toRupeesString(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (netOperating >= 0) StatusGreen else StatusCoral
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Inventory Valuation Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Total Warehouse Assets: ${metrics.totalValuationPaise.toRupeesString()}", fontSize = 13.sp, color = BrandTeal)

                                val categories = products.groupBy { it.category }
                                categories.forEach { (cat, pGroup) ->
                                    val catVal = pGroup.sumOf { (it.physicalStock * it.costPricePaise).toLong() }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, fontSize = 12.sp, color = DarkSlate)
                                        Text(catVal.toRupeesString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        var category by remember { mutableStateOf("Rent") }
        var amountText by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("UPI") }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Record Business Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Expense Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (e.g. Shop electric bill)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Rent", "Power", "Logistics", "Tea", "Staff").forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = { Text(c, fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Payment Method:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("UPI", "Cash", "NetBanking").forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m },
                                label = { Text(m, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaise = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        if (amountPaise > 0 && description.isNotBlank()) {
                            viewModel.recordExpense(category, amountPaise, description, method)
                            showAddExpenseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                ) {
                    Text("Record Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
