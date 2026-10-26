package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.BusinessSettingsEntity
import com.example.model.UserRole
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var businessName by remember(settings) { mutableStateOf(settings?.businessName ?: "") }
    var ownerName by remember(settings) { mutableStateOf(settings?.ownerName ?: "") }
    var phone by remember(settings) { mutableStateOf(settings?.phone ?: "") }
    var gstin by remember(settings) { mutableStateOf(settings?.gstin ?: "") }
    var address by remember(settings) { mutableStateOf(settings?.address ?: "") }
    var invoicePrefix by remember(settings) { mutableStateOf(settings?.invoicePrefix ?: "MS-INV-") }

    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // Active Role Card (Owner, Manager, Sales Staff, Accountant)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Workspace Role", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Role-based permission gating", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        settings?.activeRole?.let { RoleBadge(it) }
                    }

                    Text("Switch Active Role for Testing / Staff View:", fontSize = 12.sp, color = DarkSlate)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UserRole.values().forEach { role ->
                            FilterChip(
                                selected = settings?.activeRole == role,
                                onClick = { viewModel.updateRole(role) },
                                label = { Text(role.name.replace("_", " "), fontSize = 10.sp) }
                            )
                        }
                    }

                    val roleDesc = when (settings?.activeRole) {
                        UserRole.OWNER -> "Full access: Manage inventory, sales, purchases, financial reports, settings, and team."
                        UserRole.MANAGER -> "Operational access: Stock audits, order dispatch, goods receipt, supplier management."
                        UserRole.SALES_STAFF -> "Storefront access: Create orders, customer lookups, view retail catalogue."
                        UserRole.ACCOUNTANT -> "Financial access: Reconcile payments, GST tax invoices, expenses, profit reports."
                        null -> ""
                    }
                    Text(text = roleDesc, fontSize = 11.sp, color = BrandTeal)
                }
            }
        }

        // Industry Profile Experience Customization
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
                    Text("Industry Profile Experience", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Customizes product fields and workflows for your trade domain:",
                        fontSize = 11.sp,
                        color = LightSlate
                    )

                    val currentProfile = settings?.industryProfile ?: com.example.model.IndustryProfile.CLOTHING_FASHION

                    com.example.model.IndustryProfile.values().forEach { profile ->
                        val isSelected = currentProfile == profile
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandTealContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BrandTeal else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateIndustryProfile(profile) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(profile.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) BrandTealLight else MaterialTheme.colorScheme.onSurface)
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text("Modules: ${profile.recommendedModules.joinToString()}", fontSize = 10.sp, color = LightSlate)
                            }
                        }
                    }
                }
            }
        }

        // Active Branch Multi-Location Selector
        item {
            val branches by viewModel.branches.collectAsState()
            val activeBranchId = settings?.activeBranchId ?: "BRANCH-MAIN"

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
                    Text("Active Store / Warehouse Branch", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("All local POS billing, registers, and dispatch are assigned to this location:", fontSize = 11.sp, color = LightSlate)

                    branches.forEach { branch ->
                        val isSelected = branch.id == activeBranchId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandTealContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateActiveBranch(branch.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${branch.city} • ${branch.address}", fontSize = 10.sp, color = LightSlate)
                                }
                                if (isSelected) {
                                    Surface(color = BrandTeal, shape = RoundedCornerShape(4.dp)) {
                                        Text("ACTIVE", color = MidnightNavy, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Business Profile Configuration
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
                    Text("Business Profile & GST Setup", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business / Store Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner / Managing Partner") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("GSTIN (India)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Store Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = invoicePrefix,
                        onValueChange = { invoicePrefix = it },
                        label = { Text("Invoice Prefix") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val current = settings ?: BusinessSettingsEntity()
                            val updated = current.copy(
                                businessName = businessName,
                                ownerName = ownerName,
                                phone = phone,
                                gstin = gstin,
                                address = address,
                                invoicePrefix = invoicePrefix
                            )
                            coroutineScope.launch {
                                viewModel.repository.saveSettings(updated)
                                viewModel.snackbarMessage.value = "Business settings saved."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Demo Data Management & Reset
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
                    Text("Workspace Data Controls", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Reload rich sample Indian store data (Apparel, Electronics, Grocery) for testing or reset to a fresh database.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetDemoData() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reload Demo Data", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCoral, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fresh Workspace", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Start Fresh Workspace?", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear all transactions, products, orders, and initialize a clean database for your store.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.clearAllToFreshWorkspace(businessName, ownerName, gstin)
                            viewModel.snackbarMessage.value = "Initialized fresh clean workspace."
                        }
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCoral, contentColor = Color.White)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
