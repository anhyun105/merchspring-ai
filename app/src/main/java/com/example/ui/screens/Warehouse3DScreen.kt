package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.model.toRupeesString
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.ShelfBay
import com.example.ui.components.Warehouse3DView
import com.example.ui.theme.*

@Composable
fun Warehouse3DScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()

    var isReducedMotion by remember { mutableStateOf(false) }
    var selectedBayDetail by remember { mutableStateOf<ShelfBay?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D Digital Twin Operations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Real-time spatial visualization of shelf racks, storage tiers & available bays",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilterChip(
                selected = isReducedMotion,
                onClick = { isReducedMotion = !isReducedMotion },
                label = { Text("Reduced Motion", fontSize = 10.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isReducedMotion) Icons.Default.MotionPhotosOff else Icons.Default.MotionPhotosOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }

        // Live Facility Metrics Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Total Assets", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(metrics.totalValuationPaise.toRupeesString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandTeal)
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Out of Stock", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${metrics.outOfStockCount} Bays", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (metrics.outOfStockCount > 0) StatusCoral else StatusGreen)
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Active Racks", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("3 Aisles", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Full Interactive 3D Canvas
        Warehouse3DView(
            products = products,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp)),
            isInteractive = true,
            isReducedMotion = isReducedMotion,
            onBaySelected = { bay -> selectedBayDetail = bay }
        )

        // Bottom Bay Inspector Card
        selectedBayDetail?.let { bay ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${bay.rackName} • Bay Tier ${bay.tier}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTeal
                        )
                        Text(
                            text = bay.productName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SKU: ${bay.productSku} • In-Bay: ${bay.currentStock.toInt()} units",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.currentScreen.value = AppScreen.PURCHASES
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal, contentColor = MidnightNavy),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reorder PO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
