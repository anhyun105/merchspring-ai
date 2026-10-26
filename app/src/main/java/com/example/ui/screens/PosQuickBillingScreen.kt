package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun PosQuickBillingScreen(viewModel: MainViewModel) {
    val products by viewModel.products.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf("") }
    var customerNameInput by remember { mutableStateOf("Walk-in Retail Customer") }
    var customerPhoneInput by remember { mutableStateOf("") }

    // Cart State: Map of Product to Quantity
    var cartItems by remember { mutableStateOf<Map<ProductEntity, Double>>(emptyMap()) }
    var discountRupees by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var showReturnDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Financial calculations in Paise
    val subtotalPaise = remember(cartItems) {
        cartItems.entries.sumOf { (product, qty) ->
            (product.sellingPricePaise * qty).toLong()
        }
    }
    val discountPaise = remember(discountRupees) {
        val d = discountRupees.toDoubleOrNull() ?: 0.0
        (d * 100).toLong().coerceIn(0L, subtotalPaise)
    }
    val taxablePaise = (subtotalPaise - discountPaise).coerceAtLeast(0L)
    val gstPaise = remember(taxablePaise) {
        (taxablePaise * 0.05).toLong() // 5% average GST
    }
    val totalBillPaise = taxablePaise + gstPaise

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.sku.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top POS Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Quick POS Billing Counter",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Keyboard & Barcode rapid checkout for Indian retail",
                    fontSize = 12.sp,
                    color = LightSlate
                )
            }

            Button(
                onClick = { showReturnDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("open_returns_dialog_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentReturn,
                    contentDescription = "Returns",
                    tint = StatusCoral,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Process Return", color = StatusCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Two Column or Split View: Left = Search & Catalog, Right = Active Cart & Bill
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT COLUMN: Barcode, Search & Product Quick Picker
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Barcode scanner input row
                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    label = { Text("Scan / Type Barcode (Enter to add)") },
                    placeholder = { Text("e.g. 890103000001") },
                    leadingIcon = {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode", tint = BrandTeal)
                    },
                    trailingIcon = {
                        if (barcodeInput.isNotBlank()) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    val matched = viewModel.repository.getProductByBarcode(barcodeInput.trim())
                                    if (matched != null) {
                                        val cur = cartItems[matched] ?: 0.0
                                        cartItems = cartItems + (matched to (cur + 1.0))
                                        barcodeInput = ""
                                    } else {
                                        viewModel.snackbarMessage.value = "No product found with barcode $barcodeInput"
                                    }
                                }
                            }) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Barcode", tint = BrandTeal)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        coroutineScope.launch {
                            val matched = viewModel.repository.getProductByBarcode(barcodeInput.trim())
                            if (matched != null) {
                                val cur = cartItems[matched] ?: 0.0
                                cartItems = cartItems + (matched to (cur + 1.0))
                                barcodeInput = ""
                            } else {
                                viewModel.snackbarMessage.value = "No product found with barcode $barcodeInput"
                            }
                        }
                    }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_barcode_input")
                )

                // Search product box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by Product Name or SKU") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = LightSlate) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Catalog Items (${filteredProducts.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightSlate
                )

                // Product items grid/list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts) { product ->
                        val inCartQty = cartItems[product] ?: 0.0
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (inCartQty > 0) BrandTealContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (product.availableStock > inCartQty) {
                                        cartItems = cartItems + (product to (inCartQty + 1.0))
                                    } else {
                                        viewModel.snackbarMessage.value = "Cannot add more. Available stock is ${product.availableStock.toInt()}."
                                    }
                                }
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
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "SKU: ${product.sku} | Stock: ${product.availableStock.toInt()} ${product.unit}",
                                        fontSize = 11.sp,
                                        color = if (product.availableStock <= 5) StatusCoral else LightSlate
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = product.sellingPricePaise.toRupeesString(),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandTeal,
                                        fontSize = 14.sp
                                    )
                                    if (inCartQty > 0) {
                                        Surface(
                                            color = BrandTeal,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "In Cart: ${inCartQty.toInt()}",
                                                color = MidnightNavy,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: Active Cart, Customer Info & Quick Checkout
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Current Checkout Bill",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Customer input
                    OutlinedTextField(
                        value = customerNameInput,
                        onValueChange = { customerNameInput = it },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_customer_input")
                    )

                    // Cart Items list
                    Text(
                        text = "Items in Cart (${cartItems.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightSlate
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (cartItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Scan or tap items on the left to add to bill", color = LightSlate, fontSize = 12.sp)
                                }
                            }
                        }

                        items(cartItems.entries.toList()) { (product, qty) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${product.sellingPricePaise.toRupeesString()} x ${qty.toInt()} = ${((product.sellingPricePaise * qty).toLong()).toRupeesString()}",
                                        fontSize = 11.sp,
                                        color = LightSlate
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (qty > 1) cartItems = cartItems + (product to (qty - 1.0))
                                            else cartItems = cartItems - product
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = StatusCoral)
                                    }

                                    Text(
                                        text = "${qty.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            if (product.availableStock > qty) {
                                                cartItems = cartItems + (product to (qty + 1.0))
                                            } else {
                                                viewModel.snackbarMessage.value = "Stock limit reached for ${product.name}"
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = BrandTeal)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = LightBorder)

                    // Bill Summary
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", fontSize = 12.sp, color = LightSlate)
                        Text(subtotalPaise.toRupeesString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discount (₹)", fontSize = 12.sp, color = LightSlate)
                        OutlinedTextField(
                            value = discountRupees,
                            onValueChange = { discountRupees = it },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(110.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GST (5% Est.)", fontSize = 12.sp, color = LightSlate)
                        Text(gstPaise.toRupeesString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Payable", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(totalBillPaise.toRupeesString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandTeal)
                    }

                    // Payment Method Chips
                    Text("Payment Method", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LightSlate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "UPI", "Card", "Credit").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandTeal,
                                    selectedLabelColor = MidnightNavy
                                )
                            )
                        }
                    }

                    // Complete Checkout Button
                    Button(
                        onClick = {
                            if (cartItems.isEmpty()) {
                                viewModel.snackbarMessage.value = "Cart is empty. Add items first."
                                return@Button
                            }

                            val orderId = "ORD-${UUID.randomUUID().toString().take(6).uppercase()}"
                            val itemsList = cartItems.map { (p, q) ->
                                val taxAmount = ((p.sellingPricePaise * q) * 0.05).toLong()
                                OrderItemEntity(
                                    id = UUID.randomUUID().toString(),
                                    orderId = orderId,
                                    productId = p.id,
                                    productName = p.name,
                                    sku = p.sku,
                                    quantity = q,
                                    returnedQuantity = 0.0,
                                    unitPricePaise = p.sellingPricePaise,
                                    costPriceSnapshotPaise = p.costPricePaise,
                                    gstRate = 5.0,
                                    taxAmountPaise = taxAmount,
                                    totalPaise = ((p.sellingPricePaise * q) + taxAmount).toLong()
                                )
                            }

                            val order = OrderEntity(
                                id = orderId,
                                orderNumber = orderId,
                                customerId = "CUS-WALKIN",
                                customerName = customerNameInput.ifBlank { "Walk-in Retail Customer" },
                                customerPhone = customerPhoneInput,
                                subtotalPaise = subtotalPaise,
                                discountPaise = discountPaise,
                                taxPaise = gstPaise,
                                totalPaise = totalBillPaise,
                                status = OrderStatus.CONFIRMED,
                                paymentStatus = PaymentStatus.PAID,
                                paymentMethod = paymentMethod,
                                paidPaise = totalBillPaise,
                                branchId = settings?.activeBranchId ?: "BRANCH-MAIN"
                            )

                            viewModel.dispatchQuickSale(order, itemsList)
                            cartItems = emptyMap()
                            discountRupees = ""
                        },
                        enabled = cartItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("complete_pos_sale_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MidnightNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Complete Sale & Print (${totalBillPaise.toRupeesString()})",
                            color = MidnightNavy,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Returns & Exchange Dialog
    if (showReturnDialog) {
        ReturnsProcessingDialog(
            orders = orders,
            onDismiss = { showReturnDialog = false },
            onConfirmReturn = { orderId, refundPaise, adjType, reason, returnItems ->
                viewModel.processReturn(
                    orderId = orderId,
                    refundAmountPaise = refundPaise,
                    adjustmentType = adjType,
                    reason = reason,
                    items = returnItems
                )
                showReturnDialog = false
            }
        )
    }
}

@Composable
fun ReturnsProcessingDialog(
    orders: List<OrderEntity>,
    onDismiss: () -> Unit,
    onConfirmReturn: (
        orderId: String,
        refundAmountPaise: Long,
        adjustmentType: ReturnAdjustmentType,
        reason: String,
        items: List<ReturnItemEntity>
    ) -> Unit
) {
    val completedOrders = remember(orders) {
        orders.filter { it.status == OrderStatus.DISPATCHED }
    }

    var selectedOrder by remember { mutableStateOf<OrderEntity?>(completedOrders.firstOrNull()) }
    var refundAmountRupees by remember { mutableStateOf("") }
    var adjustmentType by remember { mutableStateOf<ReturnAdjustmentType>(ReturnAdjustmentType.REFUND_CASH) }
    var shouldRestock by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("Customer size mismatch / Defective item") }

    val maxAllowedRefundPaise = remember(selectedOrder) {
        val o = selectedOrder ?: return@remember 0L
        (o.totalPaise - o.returnedTotalPaise).coerceAtLeast(0L)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Process Order Return & Refund",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Select original order to return. MerchSpring ensures double-returns and excess refunds are mathematically blocked.",
                    fontSize = 12.sp,
                    color = LightSlate
                )

                // Pick order
                Text("Select Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(completedOrders) { order ->
                        val isSelected = selectedOrder?.id == order.id
                        val remainingRefund = (order.totalPaise - order.returnedTotalPaise).coerceAtLeast(0L)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BrandTealContainer else Color.Transparent)
                                .clickable {
                                    selectedOrder = order
                                    refundAmountRupees = (remainingRefund / 100.0).toString()
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${order.orderNumber} • ${order.customerName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Total: ${order.totalPaise.toRupeesString()} | Already Returned: ${order.returnedTotalPaise.toRupeesString()}",
                                    fontSize = 10.sp,
                                    color = LightSlate
                                )
                            }
                            Text(
                                text = "Eligible: ${remainingRefund.toRupeesString()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingRefund > 0) BrandTeal else StatusCoral
                            )
                        }
                    }
                }

                // Refund Amount Input with Max Cap Label
                Column {
                    Text(
                        text = "Refund Amount (Max: ${maxAllowedRefundPaise.toRupeesString()})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = refundAmountRupees,
                        onValueChange = { refundAmountRupees = it },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Adjustment Type Selector
                Text("Refund / Adjustment Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val returnAdjustmentOptions = listOf(
                        Pair(ReturnAdjustmentType.REFUND_CASH, "Cash"),
                        Pair(ReturnAdjustmentType.REFUND_UPI, "UPI"),
                        Pair(ReturnAdjustmentType.STORE_CREDIT, "Credit"),
                        Pair(ReturnAdjustmentType.EXCHANGE, "Replace")
                    )
                    returnAdjustmentOptions.forEach { pair ->
                        val type = pair.first
                        val label = pair.second
                        FilterChip(
                            selected = adjustmentType == type,
                            onClick = { adjustmentType = type },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandTeal,
                                selectedLabelColor = MidnightNavy
                            )
                        )
                    }
                }

                // Restock checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { shouldRestock = !shouldRestock }
                ) {
                    Checkbox(checked = shouldRestock, onCheckedChange = { shouldRestock = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restock returned goods back to physical inventory", fontSize = 12.sp)
                }

                // Reason input
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Return") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Confirm button
                val currentEnteredPaise = ((refundAmountRupees.toDoubleOrNull() ?: 0.0) * 100).toLong()
                val isValid = selectedOrder != null && currentEnteredPaise > 0 && currentEnteredPaise <= maxAllowedRefundPaise

                Button(
                    onClick = {
                        val ord = selectedOrder ?: return@Button
                        val sampleItem = ReturnItemEntity(
                            id = UUID.randomUUID().toString(),
                            returnId = "",
                            orderItemId = "ITEM-1",
                            productId = "PROD-SAMPLE",
                            productName = "Returned Product",
                            quantity = 1.0,
                            refundUnitPricePaise = currentEnteredPaise,
                            restocked = shouldRestock
                        )
                        onConfirmReturn(ord.id, currentEnteredPaise, adjustmentType, reason, listOf(sampleItem))
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCoral),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isValid) "Apply Refund (${currentEnteredPaise.toRupeesString()})" else "Enter Valid Refund (≤ Max)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
