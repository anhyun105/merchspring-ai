package com.example.ui.screens

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
import com.example.ui.components.PurchaseStatusChip
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val purchaseOrders by viewModel.purchaseOrders.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()

    var showNewPODialog by remember { mutableStateOf(false) }
    var showInvoiceOcrDialog by remember { mutableStateOf(false) }
    var poForGoodsReceipt by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var poItemsForReceipt by remember { mutableStateOf<List<PurchaseItemEntity>>(emptyList()) }
    var poForPayment by remember { mutableStateOf<PurchaseOrderEntity?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewPODialog = true },
                containerColor = BrandTeal,
                contentColor = MidnightNavy
            ) {
                Icon(imageVector = Icons.Default.AddBusiness, contentDescription = "New Purchase Order")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Purchase Orders & Procurement",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Supplier procurement and invoice auditing",
                        fontSize = 11.sp,
                        color = LightSlate
                    )
                }

                Button(
                    onClick = { showInvoiceOcrDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Invoice OCR", color = BrandTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (purchaseOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No purchase orders found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(purchaseOrders, key = { it.id }) { po ->
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = po.poNumber,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Supplier: ${po.supplierName}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    PurchaseStatusChip(status = po.status)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total PO Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = po.totalPaise.toRupeesString(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column {
                                        Text("Paid Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = po.paidPaise.toRupeesString(),
                                            fontSize = 13.sp,
                                            color = StatusGreen
                                        )
                                    }
                                    Column {
                                        Text("Payable Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = po.payableBalancePaise.toRupeesString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (po.payableBalancePaise > 0) StatusAmber else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (po.payableBalancePaise > 0) {
                                        TextButton(onClick = { poForPayment = po }) {
                                            Text("Record Pay", fontSize = 12.sp, color = BrandTeal)
                                        }
                                    }

                                    when (po.status) {
                                        PurchaseStatus.DRAFT -> {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        viewModel.repository.markPOOrdered(po.id)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                                            ) {
                                                Text("Place Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        PurchaseStatus.ORDERED, PurchaseStatus.PARTIAL_RECEIVED -> {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        poItemsForReceipt = viewModel.repository.getPOItems(po.id)
                                                        poForGoodsReceipt = po
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Receive Goods", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        PurchaseStatus.RECEIVED -> {
                                            StatusBadge(text = "FULL SHIPMENT RECEIVED", backgroundColor = StatusGreenContainer, textColor = StatusGreen)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Goods Receipt Dialog (Increases Physical Stock)
    // -------------------------------------------------------------
    poForGoodsReceipt?.let { po ->
        val receivedInputs = remember(poItemsForReceipt) {
            mutableStateMapOf<String, String>().apply {
                poItemsForReceipt.forEach { item ->
                    val remaining = item.orderedQuantity - item.receivedQuantity
                    this[item.id] = remaining.toInt().toString()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { poForGoodsReceipt = null },
            title = {
                Text(
                    text = "Receive Goods: ${po.poNumber}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recording goods receipt increments physical on-shelf stock and logs an INWARD_PURCHASE stock movement.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    poItemsForReceipt.forEach { item ->
                        val remaining = item.orderedQuantity - item.receivedQuantity
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "${item.productName} (Ordered: ${item.orderedQuantity.toInt()}, Already Received: ${item.receivedQuantity.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = receivedInputs[item.id] ?: "0",
                                onValueChange = { receivedInputs[item.id] = it },
                                label = { Text("Quantity Arrived Now (Max: ${remaining.toInt()})") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val resultMap = mutableMapOf<String, Double>()
                        poItemsForReceipt.forEach { item ->
                            val qty = receivedInputs[item.id]?.toDoubleOrNull() ?: 0.0
                            if (qty > 0) {
                                resultMap[item.id] = qty
                            }
                        }
                        viewModel.receiveGoods(po.id, resultMap)
                        poForGoodsReceipt = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White)
                ) {
                    Text("Confirm Goods Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { poForGoodsReceipt = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Create New Purchase Order Dialog
    // -------------------------------------------------------------
    if (showNewPODialog) {
        var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
        var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
        var orderedQty by remember { mutableStateOf("20") }
        var unitCostRupees by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewPODialog = false },
            title = { Text("Create Purchase Order", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Note: Creating a PO does not increase inventory until goods actually arrive.", fontSize = 11.sp, color = BrandTeal)

                    if (suppliers.isNotEmpty()) {
                        var supExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { supExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedSupplier?.name ?: "Select Supplier")
                            }
                            DropdownMenu(expanded = supExpanded, onDismissRequest = { supExpanded = false }) {
                                suppliers.forEach { s ->
                                    DropdownMenuItem(text = { Text(s.name) }, onClick = {
                                        selectedSupplier = s
                                        supExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    if (products.isNotEmpty()) {
                        var prodExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { prodExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedProduct?.name ?: "Select Product", maxLines = 1)
                            }
                            DropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false }) {
                                products.forEach { p ->
                                    DropdownMenuItem(text = { Text(p.name) }, onClick = {
                                        selectedProduct = p
                                        unitCostRupees = (p.costPricePaise / 100.0).toString()
                                        prodExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = orderedQty,
                            onValueChange = { orderedQty = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitCostRupees,
                            onValueChange = { unitCostRupees = it },
                            label = { Text("Unit Cost (₹)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sup = selectedSupplier
                        val prod = selectedProduct
                        val qty = orderedQty.toDoubleOrNull() ?: 0.0
                        val costPaise = ((unitCostRupees.toDoubleOrNull() ?: (prod?.costPricePaise?.toDouble() ?: 0.0) / 100.0) * 100).toLong()

                        if (sup != null && prod != null && qty > 0) {
                            val poId = "PO-${UUID.randomUUID().toString().take(8)}"
                            val totalPaise = (costPaise * qty).toLong()

                            val po = PurchaseOrderEntity(
                                id = poId,
                                poNumber = "MS-PO-${System.currentTimeMillis().toString().takeLast(4)}",
                                supplierId = sup.id,
                                supplierName = sup.name,
                                status = PurchaseStatus.ORDERED,
                                totalPaise = totalPaise,
                                paidPaise = 0L
                            )

                            val item = PurchaseItemEntity(
                                id = "PITEM-${UUID.randomUUID().toString().take(8)}",
                                purchaseOrderId = poId,
                                productId = prod.id,
                                productName = prod.name,
                                orderedQuantity = qty,
                                receivedQuantity = 0.0,
                                unitCostPaise = costPaise,
                                totalPaise = totalPaise
                            )

                            coroutineScope.launch {
                                viewModel.repository.createPurchaseOrder(po, listOf(item))
                                viewModel.snackbarMessage.value = "PO ${po.poNumber} created. Receive goods upon physical delivery."
                            }
                        }
                        showNewPODialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                ) {
                    Text("Issue Purchase Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPODialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Record Supplier Payment Dialog
    // -------------------------------------------------------------
    poForPayment?.let { po ->
        var payRupees by remember { mutableStateOf((po.payableBalancePaise / 100.0).toString()) }

        AlertDialog(
            onDismissRequest = { poForPayment = null },
            title = { Text("Pay Supplier: ${po.supplierName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Payable Balance: ${po.payableBalancePaise.toRupeesString()}")
                    OutlinedTextField(
                        value = payRupees,
                        onValueChange = { payRupees = it },
                        label = { Text("Amount Paid (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaise = ((payRupees.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        if (amountPaise > 0) {
                            coroutineScope.launch {
                                viewModel.repository.recordPurchasePayment(po.id, amountPaise)
                            }
                        }
                        poForPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy)
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { poForPayment = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // AI Invoice Extraction Review Dialog (Confidence Scores, Audit, Editability)
    // -------------------------------------------------------------
    if (showInvoiceOcrDialog) {
        val draft = remember { viewModel.geminiService.extractInvoiceDraft(0) }
        var editableSupplier by remember { mutableStateOf(draft.supplierName) }
        var editableInvoiceNumber by remember { mutableStateOf(draft.invoiceNumber) }
        var editableDate by remember { mutableStateOf(draft.invoiceDate) }

        val lineItemsSum = draft.lineItems.sumOf { it.totalPaise }
        val calculatedTotal = draft.subtotalPaise + draft.taxPaise
        val isMathValid = Math.abs(calculatedTotal - draft.totalPaise) < 100L

        AlertDialog(
            onDismissRequest = { showInvoiceOcrDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Invoice Extraction Review", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Surface(
                        color = BrandTealContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = draft.confidence,
                            color = BrandTealLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Review extracted fields before creating Purchase Order. All fields are editable and totals verified against GST rules.",
                            fontSize = 11.sp,
                            color = LightSlate
                        )
                    }

                    // Editable supplier and invoice fields
                    item {
                        OutlinedTextField(
                            value = editableSupplier,
                            onValueChange = { editableSupplier = it },
                            label = { Text("Extracted Supplier") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editableInvoiceNumber,
                                onValueChange = { editableInvoiceNumber = it },
                                label = { Text("Invoice No") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = editableDate,
                                onValueChange = { editableDate = it },
                                label = { Text("Date") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Extracted Line Items
                    item {
                        Text("Extracted Line Items (${draft.lineItems.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    items(draft.lineItems) { item ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.description, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("HSN: ${item.hsn} • ${item.quantity.toInt()} units @ ${item.unitPricePaise.toRupeesString()} (+${item.gstRate.toInt()}% GST)", fontSize = 10.sp, color = LightSlate)
                                }
                                Text(item.totalPaise.toRupeesString(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTeal)
                            }
                        }
                    }

                    // Math Reconciled Check Alert
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMathValid) Color(0xFF1B5E20).copy(alpha = 0.2f) else StatusCoralContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isMathValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isMathValid) Color(0xFF4CAF50) else StatusCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isMathValid) "Reconciled: Line Items (${draft.subtotalPaise.toRupeesString()}) + GST (${draft.taxPaise.toRupeesString()}) = ${draft.totalPaise.toRupeesString()}"
                                    else "Audit Warning: Line items do not match total invoice amount!",
                                    fontSize = 11.sp,
                                    color = if (isMathValid) Color(0xFF4CAF50) else StatusCoral
                                )
                            }
                        }
                    }

                    // Warnings
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            draft.warnings.forEach { w ->
                                Text("• $w", fontSize = 10.sp, color = LightSlate)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val poId = "PO-${UUID.randomUUID().toString().take(6).uppercase()}"
                        val sup = suppliers.firstOrNull { it.name.contains(editableSupplier, ignoreCase = true) }
                            ?: suppliers.firstOrNull()
                            ?: com.example.data.SampleData.suppliers.first()

                        val po = PurchaseOrderEntity(
                            id = poId,
                            poNumber = poId,
                            supplierId = sup.id,
                            supplierName = editableSupplier,
                            totalPaise = draft.totalPaise,
                            status = PurchaseStatus.ORDERED
                        )

                        val pItems = draft.lineItems.map { li ->
                            PurchaseItemEntity(
                                id = UUID.randomUUID().toString(),
                                purchaseOrderId = poId,
                                productId = products.firstOrNull()?.id ?: "PROD-SAMPLE",
                                productName = li.description,
                                orderedQuantity = li.quantity,
                                unitCostPaise = li.unitPricePaise,
                                gstRate = li.gstRate,
                                totalPaise = li.totalPaise
                            )
                        }

                        coroutineScope.launch {
                            viewModel.repository.createPurchaseOrder(po, pItems)
                            viewModel.snackbarMessage.value = "Created Purchase Order ${po.poNumber} from extracted invoice."
                        }
                        showInvoiceOcrDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Text("Import to Purchase Order", color = MidnightNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvoiceOcrDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
