package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@Composable
fun ReorderAndTransfersScreen(viewModel: MainViewModel) {
    val reorderList by viewModel.reorderRecommendations.collectAsState()
    val transfers by viewModel.stockTransfers.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val products by viewModel.products.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Smart Reorder, 1 = Branch Transfers

    // New Transfer dialog/form state
    var showNewTransferSheet by remember { mutableStateOf(false) }
    var selectedSourceBranchId by remember { mutableStateOf(branches.firstOrNull()?.id ?: "BRANCH-MAIN") }
    var selectedDestBranchId by remember { mutableStateOf(branches.getOrNull(1)?.id ?: "BRANCH-IND") }
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: "") }
    var transferQuantityText by remember { mutableStateOf("10") }
    var transferNotes by remember { mutableStateOf("Urgent weekend inventory replenishment.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Procurement & Inter-Branch Transfers",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "AI sales-velocity reordering and multi-warehouse stock routing",
                    fontSize = 12.sp,
                    color = LightSlate
                )
            }

            if (selectedTab == 1) {
                Button(
                    onClick = { showNewTransferSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("new_transfer_button")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MidnightNavy)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Transfer", color = MidnightNavy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BrandTeal
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Smart Reorder Engine (${reorderList.size})")
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.StoreMallDirectory, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Branch Transfers (${transfers.size})")
                    }
                }
            )
        }

        if (selectedTab == 0) {
            // TAB 0: SMART REORDER ENGINE
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandTealContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = BrandTealLight, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Transparent AI Reorder Formula",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BrandTealLight
                                )
                                Text(
                                    text = "Recommendations consider: Daily Sales Velocity x (Supplier Lead Time + Target Buffer Days) - Current Stock - Pending POs.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (reorderList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("All inventory items currently have sufficient days of cover!", color = LightSlate)
                        }
                    }
                }

                items(reorderList) { rec ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rec.product.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "SKU: ${rec.product.sku} • Current Stock: ${rec.product.availableStock.toInt()} ${rec.product.unit}",
                                        fontSize = 12.sp,
                                        color = if (rec.product.isLowStock) StatusCoral else LightSlate
                                    )
                                }

                                Surface(
                                    color = if (rec.daysOfCoverRemaining.isFinite() && rec.daysOfCoverRemaining <= 3) StatusCoralContainer else BrandTealContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (rec.daysOfCoverRemaining.isFinite()) "${String.format(java.util.Locale.US, "%.1f", rec.daysOfCoverRemaining)} Days of Cover Left" else "No Recent Sales Data",
                                        color = if (rec.daysOfCoverRemaining.isFinite() && rec.daysOfCoverRemaining <= 3) StatusCoral else BrandTealLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Metrics Grid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Sales Velocity", fontSize = 10.sp, color = LightSlate)
                                    Text(if (rec.dailySalesVelocity > 0.0) "${String.format(java.util.Locale.US, "%.2f", rec.dailySalesVelocity)}/day" else "No sales velocity", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Recommended Order", fontSize = 10.sp, color = LightSlate)
                                    Text("${rec.recommendedOrderQty.toInt()} units", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandTeal)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Est. Procurement Cost", fontSize = 10.sp, color = LightSlate)
                                    Text(rec.estimatedCostPaise.toRupeesString(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Text(
                                text = "Rationale: ${rec.reasonExplanation}",
                                fontSize = 11.sp,
                                color = LightSlate
                            )

                            // Action button: Quick Create PO
                            Button(
                                onClick = {
                                    val proposal = StructuredActionProposal(
                                        type = ActionProposalType.CREATE_PO,
                                        title = "Draft Reorder PO",
                                        summary = "Create PO for ${rec.recommendedOrderQty.toInt()} units of '${rec.product.name}'",
                                        targetEntityId = rec.product.id,
                                        targetEntityName = rec.product.name,
                                        quantity = rec.recommendedOrderQty,
                                        amountPaise = rec.estimatedCostPaise
                                    )
                                    viewModel.executeActionProposal(proposal)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = MidnightNavy, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Purchase Order", color = MidnightNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 1: BRANCH TRANSFERS
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Transfer Movements (${transfers.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightSlate
                    )
                }

                if (transfers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No inter-branch transfers recorded yet.", color = LightSlate)
                        }
                    }
                }

                items(transfers) { transfer ->
                    val isPending = transfer.status == TransferStatus.IN_TRANSIT
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${transfer.transferNumber} • ${transfer.productName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Quantity: ${transfer.quantity.toInt()} units",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = BrandTeal
                                    )
                                }

                                Surface(
                                    color = if (isPending) StatusAmberContainer else BrandTealContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isPending) "IN TRANSIT" else "COMPLETED",
                                        color = if (isPending) StatusAmber else BrandTealLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Branch Route Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Source Branch", fontSize = 10.sp, color = LightSlate)
                                    Text(transfer.sourceBranchName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Icon(Icons.Default.ArrowForward, contentDescription = "To", tint = BrandTeal)

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Destination Branch", fontSize = 10.sp, color = LightSlate)
                                    Text(transfer.destinationBranchName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (transfer.notes.isNotBlank()) {
                                Text("Note: ${transfer.notes}", fontSize = 11.sp, color = LightSlate)
                            }

                            // If In Transit, allow receiving staff to confirm receipt
                            if (isPending) {
                                Button(
                                    onClick = {
                                        viewModel.receiveTransfer(
                                            transferId = transfer.id,
                                            staffName = settings?.ownerName ?: "Store Manager"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .testTag("confirm_receive_transfer_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MidnightNavy, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirm Goods Received & Restock", color = MidnightNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog to create new transfer
    if (showNewTransferSheet) {
        AlertDialog(
            onDismissRequest = { showNewTransferSheet = false },
            title = { Text("Initiate Stock Transfer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select source and destination branches:", fontSize = 12.sp, color = LightSlate)

                    // Source
                    Text("Source Branch (Goods sent from)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    branches.forEach { b ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSourceBranchId = b.id }
                        ) {
                            RadioButton(selected = selectedSourceBranchId == b.id, onClick = { selectedSourceBranchId = b.id })
                            Text("${b.name} (${b.city})", fontSize = 12.sp)
                        }
                    }

                    // Destination
                    Text("Destination Branch (Goods sent to)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    branches.forEach { b ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDestBranchId = b.id }
                        ) {
                            RadioButton(selected = selectedDestBranchId == b.id, onClick = { selectedDestBranchId = b.id })
                            Text("${b.name} (${b.city})", fontSize = 12.sp)
                        }
                    }

                    // Product & Quantity
                    OutlinedTextField(
                        value = transferQuantityText,
                        onValueChange = { transferQuantityText = it },
                        label = { Text("Quantity to Transfer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = transferNotes,
                        onValueChange = { transferNotes = it },
                        label = { Text("Transfer Reason / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = transferQuantityText.toDoubleOrNull() ?: 10.0
                        val prodId = selectedProductId.ifBlank { products.firstOrNull()?.id ?: "" }
                        viewModel.initiateTransfer(
                            sourceBranchId = selectedSourceBranchId,
                            destBranchId = selectedDestBranchId,
                            productId = prodId,
                            quantity = qty,
                            staffName = settings?.ownerName ?: "Store Manager",
                            notes = transferNotes
                        )
                        showNewTransferSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Text("Dispatch Stock", color = MidnightNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTransferSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
