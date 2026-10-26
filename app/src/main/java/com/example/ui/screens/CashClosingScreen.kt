package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CashClosingScreen(viewModel: MainViewModel) {
    val orders by viewModel.orders.collectAsState()
    val returns by viewModel.returns.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val cashClosings by viewModel.cashClosings.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var openingCashRupees by remember { mutableStateOf("5000") }
    var countedCashRupees by remember { mutableStateOf("5000") }
    var staffName by remember { mutableStateOf(settings?.ownerName ?: "Rajesh Verma") }
    var closingNotes by remember { mutableStateOf("Evening closing reconciled after shop shutter.") }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    // Cash Sales Collected Today
    val cashSalesPaise = remember(orders) {
        orders.filter { it.status == OrderStatus.DISPATCHED && it.paymentMethod.equals("Cash", ignoreCase = true) }
            .sumOf { it.totalPaise }
    }

    // Cash Refunds Paid Out
    val cashRefundsPaise = remember(returns) {
        returns.filter { it.adjustmentType == ReturnAdjustmentType.CASH_REFUND }
            .sumOf { it.refundAmountPaise }
    }

    // Cash Expenses Paid Out
    val cashExpensesPaise = remember(expenses) {
        expenses.filter { it.paymentMethod.equals("Cash", ignoreCase = true) }
            .sumOf { it.amountPaise }
    }

    val openingCashPaise = remember(openingCashRupees) {
        val d = openingCashRupees.toDoubleOrNull() ?: 0.0
        (d * 100).toLong()
    }

    val expectedClosingCashPaise = openingCashPaise + cashSalesPaise - cashRefundsPaise - cashExpensesPaise

    val countedCashPaise = remember(countedCashRupees) {
        val d = countedCashRupees.toDoubleOrNull() ?: 0.0
        (d * 100).toLong()
    }

    val variancePaise = countedCashPaise - expectedClosingCashPaise

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Daily Cash Register Closing",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Reconcile physical cash drawer with recorded cash sales, refunds, and petty expenses",
                    fontSize = 12.sp,
                    color = LightSlate
                )
            }
        }

        // Cash Flow Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cash Drawer Ledger Breakdown ($todayStr)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Opening Float Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Opening Cash Float", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Cash in register at morning opening", fontSize = 11.sp, color = LightSlate)
                        }
                        OutlinedTextField(
                            value = openingCashRupees,
                            onValueChange = { openingCashRupees = it },
                            prefix = { Text("₹", color = BrandTeal) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .width(130.dp)
                                .testTag("opening_cash_input")
                        )
                    }

                    HorizontalDivider(color = LightBorder)

                    // Line rows
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("(+) Recorded Cash Sales", fontSize = 13.sp)
                        Text(cashSalesPaise.toRupeesString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("(-) Cash Returns & Refunds Paid", fontSize = 13.sp)
                        Text("-${cashRefundsPaise.toRupeesString()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusCoral)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("(-) Petty Cash Expenses (Tea, Logistics, etc.)", fontSize = 13.sp)
                        Text("-${cashExpensesPaise.toRupeesString()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusCoral)
                    }

                    HorizontalDivider(color = LightBorder)

                    // Expected Closing Total
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MidnightNavy)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Expected Drawer Cash", fontSize = 12.sp, color = BrandTealLight)
                            Text("Formula: Opening + Sales - Refunds - Expenses", fontSize = 10.sp, color = LightSlate)
                        }
                        Text(
                            text = expectedClosingCashPaise.toRupeesString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Counted Cash & Variance Analysis Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Physical Cash Count & Variance",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Counted cash entry
                    OutlinedTextField(
                        value = countedCashRupees,
                        onValueChange = { countedCashRupees = it },
                        label = { Text("Actual Counted Cash in Hand (₹)") },
                        prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("counted_cash_input")
                    )

                    // Real-time Variance Banner
                    val varianceContainerColor = when {
                        variancePaise == 0L -> Color(0xFF1B5E20).copy(alpha = 0.2f)
                        variancePaise > 0L -> BrandTealContainer
                        else -> StatusCoralContainer
                    }
                    val varianceTextColor = when {
                        variancePaise == 0L -> Color(0xFF4CAF50)
                        variancePaise > 0L -> BrandTealLight
                        else -> StatusCoral
                    }
                    val varianceLabel = when {
                        variancePaise == 0L -> "Exact Match (No Variance)"
                        variancePaise > 0L -> "Cash Surplus: +${variancePaise.toRupeesString()}"
                        else -> "Cash Shortage: ${variancePaise.toRupeesString()}"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(varianceContainerColor)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (variancePaise >= 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = varianceTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Reconciliation Result", fontSize = 11.sp, color = LightSlate)
                                Text(varianceLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = varianceTextColor)
                            }
                        }

                        Text(
                            text = if (variancePaise == 0L) "BALANCED" else if (variancePaise > 0) "SURPLUS" else "DEFICIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = varianceTextColor
                        )
                    }

                    // Staff & Notes inputs
                    OutlinedTextField(
                        value = staffName,
                        onValueChange = { staffName = it },
                        label = { Text("Closing Staff Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = closingNotes,
                        onValueChange = { closingNotes = it },
                        label = { Text("Closing Notes / Remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Close & Lock Register Button
                    Button(
                        onClick = {
                            viewModel.performCashClosing(
                                openingCashPaise = openingCashPaise,
                                actualCountedCashPaise = countedCashPaise,
                                closingStaff = staffName,
                                notes = closingNotes,
                                branchId = settings?.activeBranchId ?: "BRANCH-MAIN"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_cash_closing_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MidnightNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Record & Lock Daily Cash Closing",
                            color = MidnightNavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Historical Closings Log
        item {
            Text(
                text = "Previous Cash Closings (${cashClosings.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(cashClosings) { closing ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Date: ${closing.closingDate} • Staff: ${closing.closingStaff}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Expected: ${closing.expectedClosingCashPaise.toRupeesString()} | Counted: ${closing.actualCountedCashPaise.toRupeesString()}",
                            fontSize = 11.sp,
                            color = LightSlate
                        )
                        if (closing.notes.isNotBlank()) {
                            Text(text = "Note: ${closing.notes}", fontSize = 10.sp, color = LightSlate)
                        }
                    }

                    val varColor = if (closing.variancePaise >= 0) BrandTeal else StatusCoral
                    val varSign = if (closing.variancePaise > 0) "+" else ""
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$varSign${closing.variancePaise.toRupeesString()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = varColor
                        )
                        Text(
                            text = if (closing.isReconciled) "Reconciled" else "Flagged",
                            fontSize = 10.sp,
                            color = LightSlate
                        )
                    }
                }
            }
        }
    }
}
