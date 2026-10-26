package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NotificationType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
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
            Text(
                text = "Notifications & Operational Alerts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (notifications.any { !it.isRead }) {
                TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                    Text("Mark all read", fontSize = 12.sp, color = BrandTeal)
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No notifications right now. Everything in sync!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    val (icon, tint) = when (notif.type) {
                        NotificationType.OUT_OF_STOCK -> Pair(Icons.Default.Error, StatusCoral)
                        NotificationType.LOW_STOCK -> Pair(Icons.Default.Warning, StatusAmber)
                        NotificationType.PENDING_ORDER -> Pair(Icons.Default.PendingActions, StatusBlue)
                        NotificationType.OVERDUE_PAYMENT -> Pair(Icons.Default.AccountBalanceWallet, StatusAmber)
                        NotificationType.GOODS_RECEIPT -> Pair(Icons.Default.Download, StatusGreen)
                        NotificationType.DEAD_STOCK -> Pair(Icons.Default.HourglassBottom, StatusCoral)
                        NotificationType.TRANSFER_ALERT -> Pair(Icons.Default.SwapHoriz, StatusBlue)
                        NotificationType.SYSTEM -> Pair(Icons.Default.Info, BrandTeal)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.markNotificationAsRead(notif.id)
                                when (notif.type) {
                                    NotificationType.LOW_STOCK, NotificationType.OUT_OF_STOCK -> {
                                        viewModel.currentScreen.value = AppScreen.INVENTORY
                                    }
                                    NotificationType.PENDING_ORDER -> {
                                        viewModel.currentScreen.value = AppScreen.SALES
                                    }
                                    NotificationType.OVERDUE_PAYMENT -> {
                                        viewModel.currentScreen.value = AppScreen.CUSTOMERS
                                    }
                                    NotificationType.GOODS_RECEIPT -> {
                                        viewModel.currentScreen.value = AppScreen.PURCHASES
                                    }
                                    else -> {}
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = notif.title,
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = notif.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!notif.isRead) {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = BrandTeal
                                ) {}
                            }
                        }
                    }
                }
            }
        }
    }
}
