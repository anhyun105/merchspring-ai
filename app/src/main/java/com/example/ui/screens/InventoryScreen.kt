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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val movements by viewModel.stockMovements.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedStatus by remember { mutableStateOf("All") }

    // Dialog States
    var productToAdjust by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productForHistory by remember { mutableStateOf<ProductEntity?>(null) }

    val settings by viewModel.settings.collectAsState()
    val thresholdDays = settings?.deadStockDaysThreshold ?: 30

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, selectedStatus, thresholdDays) {
        products.filter { p ->
            val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || p.category == selectedCategory
            val matchesStatus = when (selectedStatus) {
                "Low Stock" -> p.isLowStock
                "Out of Stock" -> p.isOutOfStock
                "Healthy" -> !p.isLowStock && !p.isOutOfStock
                "Dead Stock" -> p.isDeadStock(thresholdDays)
                else -> true
            }
            matchesSearch && matchesCategory && matchesStatus
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = BrandTeal,
                contentColor = MidnightNavy
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = { Text("Search by product name, SKU, or barcode...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 12.sp) }
                    )
                }
            }

            // Stock Status Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOptions = listOf("All", "Low Stock", "Out of Stock", "Healthy", "Dead Stock")
                items(statusOptions) { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status, fontSize = 11.sp) }
                    )
                }
            }

            // Product List
            if (filteredProducts.isEmpty()) {
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
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = LightSlate,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No products found matching filters",
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
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onAdjustStock = { productToAdjust = product },
                            onViewHistory = { productForHistory = product }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Adjust Stock Dialog (Enforces Central Business Rules)
    // -------------------------------------------------------------
    productToAdjust?.let { product ->
        var newStockText by remember { mutableStateOf(product.physicalStock.toInt().toString()) }
        var reasonText by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { productToAdjust = null },
            title = {
                Text(
                    text = "Adjust Stock: ${product.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current Physical: ${product.physicalStock.toInt()} ${product.unit} | Reserved: ${product.reservedStock.toInt()} ${product.unit}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newStockText,
                        onValueChange = {
                            newStockText = it
                            errorMessage = null
                        },
                        label = { Text("New Physical Count (${product.unit})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason for Adjustment (Required)") },
                        placeholder = { Text("e.g. Audit variance, Damaged box") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCount = newStockText.toDoubleOrNull()
                        if (newCount == null || newCount < 0) {
                            errorMessage = "Please enter a valid non-negative number"
                            return@Button
                        }
                        if (newCount < product.reservedStock) {
                            errorMessage = "Cannot adjust below reserved stock (${product.reservedStock.toInt()} units)!"
                            return@Button
                        }
                        if (reasonText.isBlank()) {
                            errorMessage = "A reason is mandatory for stock audit"
                            return@Button
                        }
                        viewModel.adjustStock(product.id, newCount, reasonText)
                        productToAdjust = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                ) {
                    Text("Confirm Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToAdjust = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Add Product Dialog
    // -------------------------------------------------------------
    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var sku by remember { mutableStateOf("SKU-${System.currentTimeMillis().toString().takeLast(5)}") }
        var category by remember { mutableStateOf("Apparel & Boutique") }
        var costRupees by remember { mutableStateOf("") }
        var sellingRupees by remember { mutableStateOf("") }
        var initialStock by remember { mutableStateOf("10") }
        var minThreshold by remember { mutableStateOf("5") }
        var gstRate by remember { mutableStateOf("18.0") }
        var formError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Add New Product", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Product Name *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU Code *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costRupees,
                                onValueChange = { costRupees = it },
                                label = { Text("Cost Price (₹)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sellingRupees,
                                onValueChange = { sellingRupees = it },
                                label = { Text("Selling Price (₹)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = initialStock,
                                onValueChange = { initialStock = it },
                                label = { Text("Initial Stock") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = minThreshold,
                                onValueChange = { minThreshold = it },
                                label = { Text("Min Alert Level") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = gstRate,
                            onValueChange = { gstRate = it },
                            label = { Text("GST Rate (%) e.g. 5, 12, 18") },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                        if (name.isBlank()) {
                            formError = "Product name is required"
                            return@Button
                        }
                        val cost = ((costRupees.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        val selling = ((sellingRupees.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        val stock = initialStock.toDoubleOrNull() ?: 0.0
                        val threshold = minThreshold.toDoubleOrNull() ?: 5.0
                        val gst = gstRate.toDoubleOrNull() ?: 18.0

                        val newProduct = ProductEntity(
                            id = "PRD-${UUID.randomUUID().toString().take(8)}",
                            name = name,
                            sku = sku,
                            category = category,
                            costPricePaise = cost,
                            sellingPricePaise = selling,
                            physicalStock = stock,
                            reservedStock = 0.0,
                            minStockThreshold = threshold,
                            gstRate = gst
                        )
                        viewModel.saveProduct(newProduct)
                        showAddProductDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                ) {
                    Text("Add Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Stock Movement History Dialog
    // -------------------------------------------------------------
    productForHistory?.let { product ->
        val productMovements = remember(movements, product) {
            movements.filter { it.productId == product.id }
        }

        AlertDialog(
            onDismissRequest = { productForHistory = null },
            title = {
                Text(
                    text = "Stock Movements: ${product.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (productMovements.isEmpty()) {
                    Text("No stock movement history recorded for this product yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(productMovements) { mov ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = mov.movementType.name.replace("_", " "),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandTeal
                                        )
                                        Text(
                                            text = "${if (mov.deltaQuantity > 0) "+" else ""}${mov.deltaQuantity.toInt()} units",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (mov.deltaQuantity >= 0) StatusGreen else StatusCoral
                                        )
                                    }
                                    Text(
                                        text = "${mov.reason} (${mov.referenceDoc})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Physical: ${mov.physicalBefore.toInt()} → ${mov.physicalAfter.toInt()} | Reserved: ${mov.reservedBefore.toInt()} → ${mov.reservedAfter.toInt()}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { productForHistory = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onAdjustStock: () -> Unit,
    onViewHistory: () -> Unit
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
            // Header Row: Category, SKU, and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandTeal
                    )
                    Text(
                        text = "SKU: ${product.sku}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val isDead = product.isDeadStock(30)
                val isNew = product.lastSoldTimestamp == 0L

                val (badgeText, badgeBg, badgeFg) = when {
                    isDead -> Triple("DEAD STOCK", StatusCoralContainer, StatusCoral)
                    product.isOutOfStock -> Triple("OUT OF STOCK", StatusCoralContainer, StatusCoral)
                    product.isLowStock -> Triple("LOW STOCK", StatusAmberContainer, StatusAmber)
                    isNew -> Triple("NEW ITEM", BrandTealContainer, BrandTeal)
                    else -> Triple("IN STOCK", StatusGreenContainer, StatusGreen)
                }
                StatusBadge(text = badgeText, backgroundColor = badgeBg, textColor = badgeFg)
            }

            // Title & Industry Module Badges
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Optional module indicators
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (product.serialNumber.isNotBlank()) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                            Text("IMEI: ${product.serialNumber}", fontSize = 10.sp, color = LightSlate, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (product.expiryDate.isNotBlank()) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                            Text("Exp: ${product.expiryDate}", fontSize = 10.sp, color = LightSlate, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (product.moq > 1.0) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                            Text("MOQ: ${product.moq.toInt()}", fontSize = 10.sp, color = BrandTeal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (product.hasVariants) {
                        Surface(color = BrandTealContainer, shape = RoundedCornerShape(4.dp)) {
                            Text("Variants", fontSize = 10.sp, color = BrandTealLight, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Selling Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = product.sellingPricePaise.toRupeesString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("Cost Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = product.costPricePaise.toRupeesString(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text("GST Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${product.gstRate.toInt()}%", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Stock Details Grid: Physical, Reserved, Available
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Physical", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${product.physicalStock.toInt()} ${product.unit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Reserved", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${product.reservedStock.toInt()} ${product.unit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.reservedStock > 0) StatusAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Available", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${product.availableStock.toInt()} ${product.unit}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (product.isOutOfStock) StatusCoral else BrandTeal
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewHistory) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Audit Log", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onAdjustStock,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust Stock", fontSize = 12.sp)
                }
            }
        }
    }
}
