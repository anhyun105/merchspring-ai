package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GstInvoiceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedOrder by remember { mutableStateOf(orders.firstOrNull()) }
    var orderItems by remember { mutableStateOf<List<OrderItemEntity>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(orders) {
        if (selectedOrder == null && orders.isNotEmpty()) {
            selectedOrder = orders.first()
        }
    }

    LaunchedEffect(selectedOrder) {
        selectedOrder?.let { ord ->
            coroutineScope.launch {
                orderItems = viewModel.repository.getOrderItems(ord.id)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Order Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Invoice Preview (Draft)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (orders.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(selectedOrder?.orderNumber ?: "Select Order", fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        orders.forEach { o ->
                            DropdownMenuItem(
                                text = { Text("${o.orderNumber} - ${o.customerName}") },
                                onClick = {
                                    selectedOrder = o
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        selectedOrder?.let { order ->
            // Printable GST Invoice Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Business Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings?.businessName?.takeIf { it.isNotBlank() } ?: "Business name not configured",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MidnightNavy
                                )
                                Text(
                                    text = settings?.address?.takeIf { it.isNotBlank() } ?: "Business address not configured",
                                    fontSize = 11.sp,
                                    color = DarkSlate
                                )
                                Text(
                                    text = "GSTIN: ${settings?.gstin?.takeIf { it.isNotBlank() } ?: "Not configured"} | PAN: ${settings?.pan?.takeIf { it.isNotBlank() } ?: "Not configured"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTealDark
                                )
                                Text(
                                    text = "Phone: ${settings?.phone?.takeIf { it.isNotBlank() } ?: "Not configured"}",
                                    fontSize = 11.sp,
                                    color = DarkSlate
                                )
                            }

                            Surface(
                                color = BrandTealContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "DRAFT PREVIEW",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTeal
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 10.dp), color = LightBorder)
                    }

                    // Metadata Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Invoice No:", fontSize = 11.sp, color = MutedSlate)
                                Text(order.orderNumber, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightNavy)
                                Text("Payment Mode:", fontSize = 11.sp, color = MutedSlate)
                                Text(order.paymentMethod, fontSize = 12.sp, color = MidnightNavy)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Date:", fontSize = 11.sp, color = MutedSlate)
                                Text(
                                    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(java.util.Date(order.createdAt)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightNavy
                                )
                                Text("Place of Supply:", fontSize = 11.sp, color = MutedSlate)
                                Text("Not configured", fontSize = 12.sp, color = MidnightNavy)
                            }
                        }
                    }

                    // Bill To
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = LightBackground,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("BILL TO:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedSlate)
                                Text(order.customerName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightNavy)
                                Text("Contact: ${order.customerPhone.ifBlank { "Not provided" }}", fontSize = 11.sp, color = DarkSlate)
                            }
                        }
                    }

                    // Line Items Table Header
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = DeepBlue,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(2.5f))
                                Text("Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(0.8f))
                                Text("Rate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.2f))
                                Text("GST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(0.8f))
                                Text("Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.3f))
                            }
                        }
                    }

                    // Line Items List
                    items(orderItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(2.5f)) {
                                Text(item.productName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MidnightNavy)
                                Text("SKU: ${item.sku}", fontSize = 9.sp, color = MutedSlate)
                            }
                            Text(if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else String.format(java.util.Locale.US, "%.3f", item.quantity).trimEnd('0').trimEnd('.'), fontSize = 11.sp, color = DarkSlate, modifier = Modifier.weight(0.8f))
                            Text(item.unitPricePaise.toRupeesString(), fontSize = 11.sp, color = DarkSlate, modifier = Modifier.weight(1.2f))
                            Text("${item.gstRate.toInt()}%", fontSize = 11.sp, color = DarkSlate, modifier = Modifier.weight(0.8f))
                            Text(item.totalPaise.toRupeesString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MidnightNavy, modifier = Modifier.weight(1.3f))
                        }
                        HorizontalDivider(color = LightBorder.copy(alpha = 0.5f))
                    }

                    // Taxes & Grand Total Breakdown
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Item Subtotal:", fontSize = 11.sp, color = DarkSlate)
                                Text(order.subtotalPaise.toRupeesString(), fontSize = 11.sp, color = MidnightNavy)
                            }
                            if (order.discountPaise > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount:", fontSize = 11.sp, color = StatusCoral)
                                    Text("-${order.discountPaise.toRupeesString()}", fontSize = 11.sp, color = StatusCoral)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tax (configured product rates):", fontSize = 11.sp, color = DarkSlate)
                                Text(order.taxPaise.toRupeesString(), fontSize = 11.sp, color = MidnightNavy)
                            }
                            HorizontalDivider(color = MidnightNavy, thickness = 1.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total (INR):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MidnightNavy)
                                Text(order.totalPaise.toRupeesString(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandTealDark)
                            }
                        }
                    }

                    // Authorized Signatory & Stamp
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("Draft only — verify tax, place of supply, and terms.", fontSize = 9.sp, color = MutedSlate)
                                Text("Thank you for your business!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandTealDark)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier
                                        .size(70.dp, 35.dp)
                                        .border(1.dp, BrandTeal, RoundedCornerShape(4.dp)),
                                    color = BrandTeal.copy(alpha = 0.05f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("DRAFT\nNOT VERIFIED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("For ${settings?.businessName?.takeIf { it.isNotBlank() } ?: "Business"}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MidnightNavy)
                                Text("Authorized Signatory", fontSize = 9.sp, color = MutedSlate)
                            }
                        }
                    }
                }
            }

            // Print & Share Floating Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.snackbarMessage.value = "GST invoice ${order.orderNumber} ready for printing."
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print Invoice")
                }

                Button(
                    onClick = {
                        viewModel.snackbarMessage.value = "Invoice ${order.orderNumber} sent via WhatsApp to ${order.customerName}."
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Invoice")
                }
            }
        }
    }
}
