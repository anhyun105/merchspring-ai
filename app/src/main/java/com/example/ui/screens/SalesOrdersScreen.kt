package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesOrdersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()

    var selectedTab by remember { mutableStateOf("All") }
    var showNewOrderDialog by remember { mutableStateOf(false) }
    var orderToPay by remember { mutableStateOf<OrderEntity?>(null) }
    var orderToCancel by remember { mutableStateOf<OrderEntity?>(null) }

    val filteredOrders = remember(orders, selectedTab) {
        when (selectedTab) {
            "Confirmed" -> orders.filter { it.status == OrderStatus.CONFIRMED }
            "Dispatched" -> orders.filter { it.status == OrderStatus.DISPATCHED }
            "Draft" -> orders.filter { it.status == OrderStatus.DRAFT }
            "Cancelled" -> orders.filter { it.status == OrderStatus.CANCELLED }
            else -> orders
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewOrderDialog = true },
                containerColor = BrandTeal,
                contentColor = MidnightNavy
            ) {
                Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "New Sale")
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
            // Filter Tabs
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("All", "Confirmed", "Dispatched", "Draft", "Cancelled")
                items(tabs) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab, fontSize = 12.sp) }
                    )
                }
            }

            // Orders List
            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = LightSlate,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No orders found in $selectedTab status",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onConfirm = { viewModel.confirmOrder(order.id) },
                            onDispatch = { viewModel.dispatchOrder(order.id) },
                            onCancel = { orderToCancel = order },
                            onRecordPayment = { orderToPay = order },
                            onViewInvoice = {
                                viewModel.currentScreen.value = AppScreen.INVOICES
                            }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Create New Order Dialog (Draft Sales Workflow)
    // -------------------------------------------------------------
    if (showNewOrderDialog) {
        var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
        var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
        var quantityText by remember { mutableStateOf("1") }
        var discountText by remember { mutableStateOf("0") }
        var paymentMethod by remember { mutableStateOf("UPI") }
        var formError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showNewOrderDialog = false },
            title = { Text("Create New Sale Order", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("1. Customer Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                        if (customers.isNotEmpty()) {
                            var customerExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { customerExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedCustomer?.name ?: "Select Customer")
                                }
                                DropdownMenu(
                                    expanded = customerExpanded,
                                    onDismissRequest = { customerExpanded = false }
                                ) {
                                    customers.forEach { cus ->
                                        DropdownMenuItem(
                                            text = { Text("${cus.name} (${cus.phone})") },
                                            onClick = {
                                                selectedCustomer = cus
                                                customerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("2. Select Item & Quantity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                        if (products.isNotEmpty()) {
                            var productExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { productExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedProduct?.name ?: "Select Product", maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = productExpanded,
                                    onDismissRequest = { productExpanded = false }
                                ) {
                                    products.forEach { prod ->
                                        DropdownMenuItem(
                                            text = { Text("${prod.name} (Avail: ${prod.availableStock.toInt()})") },
                                            onClick = {
                                                selectedProduct = prod
                                                productExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            selectedProduct?.let { prod ->
                                Text(
                                    text = "Available: ${prod.availableStock.toInt()} ${prod.unit} • Rate: ${prod.sellingPricePaise.toRupeesString()} + ${prod.gstRate.toInt()}% GST",
                                    fontSize = 11.sp,
                                    color = if (prod.isOutOfStock) StatusCoral else BrandTeal
                                )
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text("Quantity") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { discountText = it },
                                label = { Text("Discount (₹)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (formError != null) {
                        item {
                            Text(text = formError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customer = selectedCustomer
                        val product = selectedProduct
                        if (customer == null || product == null) {
                            formError = "Please select both a customer and a product"
                            return@Button
                        }
                        val qty = quantityText.toDoubleOrNull()
                        if (qty == null || qty <= 0) {
                            formError = "Enter a valid positive quantity"
                            return@Button
                        }

                        val discountPaise = ((discountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        val subtotalPaise = (product.sellingPricePaise * qty).toLong()
                        val taxPaise = ((subtotalPaise - discountPaise) * (product.gstRate / 100.0)).toLong().coerceAtLeast(0L)
                        val totalPaise = (subtotalPaise - discountPaise + taxPaise).coerceAtLeast(0L)

                        val orderId = "ORD-${UUID.randomUUID().toString().take(8)}"
                        val orderNumber = "MS-INV-${System.currentTimeMillis().toString().takeLast(4)}"

                        val newOrder = OrderEntity(
                            id = orderId,
                            orderNumber = orderNumber,
                            customerId = customer.id,
                            customerName = customer.name,
                            customerPhone = customer.phone,
                            status = OrderStatus.DRAFT,
                            subtotalPaise = subtotalPaise,
                            discountPaise = discountPaise,
                            taxPaise = taxPaise,
                            totalPaise = totalPaise,
                            paidPaise = 0L,
                            paymentStatus = PaymentStatus.UNPAID,
                            paymentMethod = paymentMethod
                        )

                        val item = OrderItemEntity(
                            id = "ITEM-${UUID.randomUUID().toString().take(8)}",
                            orderId = orderId,
                            productId = product.id,
                            productName = product.name,
                            sku = product.sku,
                            quantity = qty,
                            unitPricePaise = product.sellingPricePaise,
                            costPriceSnapshotPaise = product.costPricePaise, // Historical cost preserved!
                            gstRate = product.gstRate,
                            taxAmountPaise = taxPaise,
                            totalPaise = totalPaise
                        )

                        viewModel.createDraftOrder(newOrder, listOf(item))
                        showNewOrderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                ) {
                    Text("Save Draft Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewOrderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Record Payment Dialog
    // -------------------------------------------------------------
    orderToPay?.let { order ->
        var payRupees by remember { mutableStateOf((order.balanceDuePaise / 100.0).toString()) }
        var method by remember { mutableStateOf("UPI") }

        AlertDialog(
            onDismissRequest = { orderToPay = null },
            title = { Text("Record Payment for ${order.orderNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total: ${order.totalPaise.toRupeesString()} | Due: ${order.balanceDuePaise.toRupeesString()}")

                    OutlinedTextField(
                        value = payRupees,
                        onValueChange = { payRupees = it },
                        label = { Text("Amount Paid (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("UPI", "Cash", "Card", "NetBanking").forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaise = ((payRupees.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        if (amountPaise > 0) {
                            viewModel.recordOrderPayment(order.id, amountPaise, method)
                        }
                        orderToPay = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToPay = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Cancel Order Dialog
    // -------------------------------------------------------------
    orderToCancel?.let { order ->
        var reason by remember { mutableStateOf("Customer cancelled order") }

        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text("Cancel Order ${order.orderNumber}?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cancelling an undispatched order releases any reserved stock back to available inventory.")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Cancellation Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOrder(order.id, reason)
                        orderToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCoral, contentColor = Color.White)
                ) {
                    Text("Cancel Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToCancel = null }) {
                    Text("Back")
                }
            }
        )
    }
}

@Composable
fun OrderCard(
    order: OrderEntity,
    onConfirm: () -> Unit,
    onDispatch: () -> Unit,
    onCancel: () -> Unit,
    onRecordPayment: () -> Unit,
    onViewInvoice: () -> Unit
) {
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
            // Header Row: Order Number and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${order.customerName} (${order.customerPhone})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrderStatusChip(status = order.status)
                    PaymentStatusChip(status = order.paymentStatus)
                }
            }

            // Totals Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Invoice", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = order.totalPaise.toRupeesString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = order.paidPaise.toRupeesString(),
                        fontSize = 13.sp,
                        color = StatusGreen
                    )
                }
                Column {
                    Text("Balance Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = order.balanceDuePaise.toRupeesString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (order.balanceDuePaise > 0) StatusCoral else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Central Business Rules Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onViewInvoice) {
                    Icon(Icons.Default.Print, contentDescription = "Invoice", tint = BrandTeal)
                }

                if (order.balanceDuePaise > 0 && order.status != OrderStatus.CANCELLED) {
                    TextButton(onClick = onRecordPayment) {
                        Text("Record Pay", fontSize = 12.sp, color = BrandTeal)
                    }
                }

                when (order.status) {
                    OrderStatus.DRAFT -> {
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm & Reserve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    OrderStatus.CONFIRMED -> {
                        Button(
                            onClick = onDispatch,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dispatch Stock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }

                if (order.status == OrderStatus.DRAFT || order.status == OrderStatus.CONFIRMED) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = StatusCoral)
                    }
                }
            }
        }
    }
}
