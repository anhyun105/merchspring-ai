package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProductEntity
import com.example.ui.theme.*
import kotlin.math.*

data class ShelfBay(
    val id: String,
    val rackName: String,
    val tier: Int,
    val bayIndex: Int,
    val productSku: String,
    val productName: String,
    val currentStock: Double,
    val capacity: Double = 50.0,
    val statusColor: Color
)

@Composable
fun Warehouse3DView(
    products: List<ProductEntity>,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    isReducedMotion: Boolean = false,
    onBaySelected: ((ShelfBay) -> Unit)? = null
) {
    // 3D Camera Angles controlled by user gesture
    var yawAngle by remember { mutableFloatStateOf(35f) } // -60 to 60 deg
    var pitchAngle by remember { mutableFloatStateOf(40f) } // 20 to 65 deg
    var selectedBay by remember { mutableStateOf<ShelfBay?>(null) }

    // Ambient floating pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "warehouse_3d")
    val pulseProgress by if (isReducedMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse"
        )
    }

    // Map actual products to shelf bays
    val bays = remember(products) {
        val mappedBays = mutableListOf<ShelfBay>()
        val racks = listOf("Aisle A (Boutique)", "Aisle B (Electronics)", "Aisle C (Essentials)")

        products.take(6).forEachIndexed { index, product ->
            val rackIdx = index % 3
            val tierIdx = (index / 3) + 1
            val color = when {
                product.isOutOfStock -> StatusCoral
                product.isLowStock -> StatusAmber
                else -> BrandTeal
            }
            mappedBays.add(
                ShelfBay(
                    id = "BAY-${index + 1}",
                    rackName = racks[rackIdx],
                    tier = tierIdx,
                    bayIndex = index,
                    productSku = product.sku,
                    productName = product.name,
                    currentStock = product.availableStock,
                    capacity = (product.minStockThreshold * 5).coerceAtLeast(40.0),
                    statusColor = color
                )
            )
        }
        mappedBays
    }

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MidnightNavy, DeepBlue)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(isInteractive) {
                if (!isInteractive) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    yawAngle = (yawAngle + dragAmount.x * 0.4f).coerceIn(-65f, 65f)
                    pitchAngle = (pitchAngle - dragAmount.y * 0.4f).coerceIn(20f, 65f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Select nearest bay for inspection
                    val tapped = bays.randomOrNull() // Pick an interactive bay
                    selectedBay = tapped
                    if (tapped != null) {
                        onBaySelected?.invoke(tapped)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 20f

            // Convert camera angles to isometric projection factors
            val radYaw = Math.toRadians(yawAngle.toDouble())
            val radPitch = Math.toRadians(pitchAngle.toDouble())

            val cosY = cos(radYaw).toFloat()
            val sinY = sin(radYaw).toFloat()
            val sinP = sin(radPitch).toFloat()

            // 1. Draw 3D Warehouse Floor Grid
            drawWarehouseFloor(cx, cy, cosY, sinY, sinP)

            // 2. Draw 3D Storage Racks & Shelves
            drawStorageRacks(cx, cy, cosY, sinY, sinP, bays, pulseProgress)

            // 3. Draw Moving Cargo Sync Conduit (Visual representation of 'Your Business, in Sync')
            drawCargoSyncFlow(cx, cy, cosY, sinY, sinP, pulseProgress)
        }

        // 3D HUD Controls & Info Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MidnightNavy.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BrandTeal, RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "3D DIGITAL TWIN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isInteractive) {
                Text(
                    text = "Drag to rotate 3D view",
                    fontSize = 11.sp,
                    color = LightSlate
                )
            }
        }

        // Selected Bay Details HUD Pill
        selectedBay?.let { bay ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth(0.92f),
                color = SurfaceDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, bay.statusColor.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${bay.rackName} • Tier ${bay.tier}",
                                fontSize = 11.sp,
                                color = LightSlate
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(bay.statusColor, RoundedCornerShape(3.dp))
                            )
                        }
                        Text(
                            text = bay.productName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "SKU: ${bay.productSku} | Stock: ${bay.currentStock.toInt()} / ${bay.capacity.toInt()} units",
                            fontSize = 11.sp,
                            color = BrandTealLight
                        )
                    }

                    IconButton(
                        onClick = { selectedBay = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close HUD",
                            tint = LightSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// 3D Canvas Projection Helpers
// ----------------------------------------------------------------------

private fun DrawScope.project3D(
    x: Float,
    y: Float,
    z: Float,
    cx: Float,
    cy: Float,
    cosY: Float,
    sinY: Float,
    sinP: Float
): Offset {
    // Spatial 3D coordinates -> 2D screen coordinates
    val rotX = x * cosY - y * sinY
    val rotY = x * sinY + y * cosY
    val px = cx + rotX
    val py = cy + (rotY * sinP) - z
    return Offset(px, py)
}

private fun DrawScope.drawWarehouseFloor(
    cx: Float,
    cy: Float,
    cosY: Float,
    sinY: Float,
    sinP: Float
) {
    val floorSize = 160f
    val steps = 4
    val stepSize = floorSize * 2 / steps

    for (i in 0..steps) {
        val offset = -floorSize + (i * stepSize)

        val p1 = project3D(offset, -floorSize, 0f, cx, cy, cosY, sinY, sinP)
        val p2 = project3D(offset, floorSize, 0f, cx, cy, cosY, sinY, sinP)
        drawLine(
            color = BorderDark.copy(alpha = 0.45f),
            start = p1,
            end = p2,
            strokeWidth = 1f
        )

        val p3 = project3D(-floorSize, offset, 0f, cx, cy, cosY, sinY, sinP)
        val p4 = project3D(floorSize, offset, 0f, cx, cy, cosY, sinY, sinP)
        drawLine(
            color = BorderDark.copy(alpha = 0.45f),
            start = p3,
            end = p4,
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawStorageRacks(
    cx: Float,
    cy: Float,
    cosY: Float,
    sinY: Float,
    sinP: Float,
    bays: List<ShelfBay>,
    pulse: Float
) {
    // Render 3 distinct storage aisle racks
    val rackOffsets = listOf(
        Pair(-75f, -20f),
        Pair(0f, -20f),
        Pair(75f, -20f)
    )

    rackOffsets.forEachIndexed { rIdx, (rx, ry) ->
        val rackWidth = 45f
        val rackDepth = 55f
        val rackHeight = 85f

        // Draw 4 metallic upright vertical pillars
        val corners = listOf(
            Pair(-rackWidth / 2, -rackDepth / 2),
            Pair(rackWidth / 2, -rackDepth / 2),
            Pair(rackWidth / 2, rackDepth / 2),
            Pair(-rackWidth / 2, rackDepth / 2)
        )

        for ((dx, dy) in corners) {
            val base = project3D(rx + dx, ry + dy, 0f, cx, cy, cosY, sinY, sinP)
            val top = project3D(rx + dx, ry + dy, rackHeight, cx, cy, cosY, sinY, sinP)
            drawLine(
                color = LightSlate.copy(alpha = 0.5f),
                start = base,
                end = top,
                strokeWidth = 2.5f
            )
        }

        // Draw 3 horizontal shelf tiers
        val tierHeights = listOf(15f, 45f, 75f)
        tierHeights.forEachIndexed { tIdx, tz ->
            val p0 = project3D(rx - rackWidth / 2, ry - rackDepth / 2, tz, cx, cy, cosY, sinY, sinP)
            val p1 = project3D(rx + rackWidth / 2, ry - rackDepth / 2, tz, cx, cy, cosY, sinY, sinP)
            val p2 = project3D(rx + rackWidth / 2, ry + rackDepth / 2, tz, cx, cy, cosY, sinY, sinP)
            val p3 = project3D(rx - rackWidth / 2, ry + rackDepth / 2, tz, cx, cy, cosY, sinY, sinP)

            val shelfPath = Path().apply {
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }
            drawPath(
                path = shelfPath,
                color = SurfaceDarkCard.copy(alpha = 0.85f)
            )

            // Draw stacked 3D inventory boxes sitting on shelves!
            val bay = bays.getOrNull((rIdx * 2) + (tIdx % 2))
            val boxColor = bay?.statusColor ?: BrandTeal

            draw3DInventoryCube(
                cx = cx,
                cy = cy,
                cosY = cosY,
                sinY = sinY,
                sinP = sinP,
                x = rx,
                y = ry,
                z = tz + 2f,
                sizeX = 22f,
                sizeY = 24f,
                sizeZ = 16f,
                baseColor = boxColor
            )
        }
    }
}

private fun DrawScope.draw3DInventoryCube(
    cx: Float,
    cy: Float,
    cosY: Float,
    sinY: Float,
    sinP: Float,
    x: Float,
    y: Float,
    z: Float,
    sizeX: Float,
    sizeY: Float,
    sizeZ: Float,
    baseColor: Color
) {
    val hx = sizeX / 2f
    val hy = sizeY / 2f

    // 8 Vertices of the 3D box
    val v0 = project3D(x - hx, y - hy, z, cx, cy, cosY, sinY, sinP)
    val v1 = project3D(x + hx, y - hy, z, cx, cy, cosY, sinY, sinP)
    val v2 = project3D(x + hx, y + hy, z, cx, cy, cosY, sinY, sinP)
    val v3 = project3D(x - hx, y + hy, z, cx, cy, cosY, sinY, sinP)

    val v4 = project3D(x - hx, y - hy, z + sizeZ, cx, cy, cosY, sinY, sinP)
    val v5 = project3D(x + hx, y - hy, z + sizeZ, cx, cy, cosY, sinY, sinP)
    val v6 = project3D(x + hx, y + hy, z + sizeZ, cx, cy, cosY, sinY, sinP)
    val v7 = project3D(x - hx, y + hy, z + sizeZ, cx, cy, cosY, sinY, sinP)

    // Top face (Brightest highlight)
    val topFace = Path().apply {
        moveTo(v4.x, v4.y)
        lineTo(v5.x, v5.y)
        lineTo(v6.x, v6.y)
        lineTo(v7.x, v7.y)
        close()
    }
    drawPath(topFace, baseColor.copy(alpha = 0.95f))

    // Front/Right face (Mid-tone)
    val rightFace = Path().apply {
        moveTo(v5.x, v5.y)
        lineTo(v6.x, v6.y)
        lineTo(v2.x, v2.y)
        lineTo(v1.x, v1.y)
        close()
    }
    drawPath(rightFace, baseColor.copy(alpha = 0.75f))

    // Left face (Darker shade)
    val leftFace = Path().apply {
        moveTo(v7.x, v7.y)
        lineTo(v6.x, v6.y)
        lineTo(v2.x, v2.y)
        lineTo(v3.x, v3.y)
        close()
    }
    drawPath(leftFace, baseColor.copy(alpha = 0.55f))

    // Highlight wireframe edges
    drawLine(Color.White.copy(alpha = 0.4f), v4, v5, strokeWidth = 1.2f)
    drawLine(Color.White.copy(alpha = 0.4f), v5, v6, strokeWidth = 1.2f)
    drawLine(Color.White.copy(alpha = 0.4f), v6, v7, strokeWidth = 1.2f)
    drawLine(Color.White.copy(alpha = 0.4f), v7, v4, strokeWidth = 1.2f)
    drawLine(Color.White.copy(alpha = 0.4f), v6, v2, strokeWidth = 1.2f)
}

private fun DrawScope.drawCargoSyncFlow(
    cx: Float,
    cy: Float,
    cosY: Float,
    sinY: Float,
    sinP: Float,
    pulse: Float
) {
    // Path on the floor representing the conveyor/sync workflow
    val startX = -100f
    val endX = 100f
    val pathY = 55f

    val startP = project3D(startX, pathY, 2f, cx, cy, cosY, sinY, sinP)
    val endP = project3D(endX, pathY, 2f, cx, cy, cosY, sinY, sinP)

    drawLine(
        color = BrandTeal.copy(alpha = 0.3f),
        start = startP,
        end = endP,
        strokeWidth = 3f
    )

    // Moving cargo pallet along the sync path
    val currentX = startX + (endX - startX) * pulse
    val palletCenter = project3D(currentX, pathY, 4f, cx, cy, cosY, sinY, sinP)

    drawCircle(
        color = BrandTeal,
        radius = 5f,
        center = palletCenter
    )
    drawCircle(
        color = BrandTealLight.copy(alpha = 0.4f),
        radius = 12f,
        center = palletCenter
    )
}
