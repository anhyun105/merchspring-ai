package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = BrandTeal,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (bg, fg) = when (status) {
        OrderStatus.DRAFT -> Pair(Color(0xFFF1F5F9), DarkSlate)
        OrderStatus.CONFIRMED -> Pair(StatusBlueContainer, StatusBlue)
        OrderStatus.DISPATCHED -> Pair(StatusGreenContainer, StatusGreen)
        OrderStatus.CANCELLED -> Pair(StatusCoralContainer, StatusCoral)
    }
    StatusBadge(text = status.name, backgroundColor = bg, textColor = fg)
}

@Composable
fun PaymentStatusChip(status: PaymentStatus) {
    val (bg, fg) = when (status) {
        PaymentStatus.PAID -> Pair(StatusGreenContainer, StatusGreen)
        PaymentStatus.PARTIAL -> Pair(StatusAmberContainer, StatusAmber)
        PaymentStatus.UNPAID -> Pair(StatusCoralContainer, StatusCoral)
    }
    StatusBadge(text = status.name, backgroundColor = bg, textColor = fg)
}

@Composable
fun PurchaseStatusChip(status: PurchaseStatus) {
    val (bg, fg) = when (status) {
        PurchaseStatus.DRAFT -> Pair(Color(0xFFF1F5F9), DarkSlate)
        PurchaseStatus.ORDERED -> Pair(StatusBlueContainer, StatusBlue)
        PurchaseStatus.PARTIAL_RECEIVED -> Pair(StatusAmberContainer, StatusAmber)
        PurchaseStatus.RECEIVED -> Pair(StatusGreenContainer, StatusGreen)
        PurchaseStatus.CANCELLED -> Pair(StatusCoralContainer, StatusCoral)
    }
    StatusBadge(text = status.name.replace("_", " "), backgroundColor = bg, textColor = fg)
}

@Composable
fun RoleBadge(role: UserRole) {
    Surface(
        color = BrandTealContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = role.name.replace("_", " "),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrandTealLight
        )
    }
}
