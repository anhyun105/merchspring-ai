package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * Currency amount stored safely in integer paise (1 INR = 100 Paise)
 * Prevents floating point rounding errors in financial transactions.
 */
fun Long.toRupeesString(): String {
    val rupees = this / 100.0
    val isNegative = rupees < 0
    val absRupees = kotlin.math.abs(rupees)
    
    // Indian Numbering Formatting (e.g. 1,23,456.78)
    val parts = String.format(Locale.US, "%.2f", absRupees).split(".")
    val integerPart = parts[0]
    val decimalPart = parts[1]
    
    val formattedInteger = if (integerPart.length <= 3) {
        integerPart
    } else {
        val lastThree = integerPart.takeLast(3)
        val remaining = integerPart.dropLast(3)
        val chunks = remaining.reversed().chunked(2).joinToString(",").reversed()
        "$chunks,$lastThree"
    }
    
    return "${if (isNegative) "-" else ""}₹$formattedInteger.$decimalPart"
}

fun Double.toCurrencyRupees(): String {
    return (this * 100).toLong().toRupeesString()
}

enum class IndustryProfile(val label: String, val description: String) {
    CLOTHING_FASHION("Clothing & Fashion", "Sizes, color variants, fabrics, seasonal catalogues"),
    COSMETICS_CARE("Cosmetics & Care", "Batch tracking, expiry dates, skin care variants"),
    GROCERY_FOOD("Grocery & Kirana", "Weight units, perishable expiry, batch mandi procurement"),
    ELECTRONICS_ACCESSORIES("Electronics & Mobile", "Serial/IMEI numbers, warranty tracking, models"),
    GENERAL_RETAIL("General Retail", "Fast POS barcode billing, standard inventory counts"),
    WHOLESALE_DISTRIBUTION("Wholesale & B2B", "Bulk packaging, MOQs, customer tier pricing");

    val displayName: String get() = label
    val recommendedModules: List<String> get() = description.split(", ")
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val barcode: String = "",
    val description: String = "",
    val category: String,
    val brand: String = "",
    val unit: String = "Pcs", // Pcs, Kg, Meter, Box, Litre
    val costPricePaise: Long, // Integer paise
    val sellingPricePaise: Long, // Integer paise
    val physicalStock: Double, // Actual on-shelf physical quantity
    val reservedStock: Double = 0.0, // Stock reserved by confirmed orders
    val minStockThreshold: Double = 5.0,
    val preferredSupplier: String = "",
    val hsn: String = "",
    val gstRate: Double = 18.0, // 0, 5, 12, 18, 28%
    val batchNo: String = "",
    val expiryDate: String = "", // yyyy-MM-dd
    val serialNumber: String = "", // For Electronics
    val warrantyMonths: Int = 0,
    val moq: Double = 1.0, // Minimum order quantity for wholesale
    val parentProductId: String = "", // If variant
    val variantAttribute: String = "", // e.g. "Size: XL | Color: Navy"
    val lastSoldTimestamp: Long = System.currentTimeMillis() - 86400000L * 5, // for dead-stock tracking
    val isActive: Boolean = true,
    val branchId: String = "BRANCH-MAIN",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val availableStock: Double
        get() = (physicalStock - reservedStock).coerceAtLeast(0.0)

    val isLowStock: Boolean
        get() = availableStock <= minStockThreshold && availableStock > 0

    val isOutOfStock: Boolean
        get() = availableStock <= 0.0

    fun isDeadStock(thresholdDays: Int = 30): Boolean {
        val daysInactive = (System.currentTimeMillis() - lastSoldTimestamp) / (86400000L)
        // Only classify as dead stock if there is physical stock on hand and inactivity exceeds threshold
        return physicalStock > 0 && daysInactive >= thresholdDays
    }

    val hasVariants: Boolean
        get() = parentProductId.isNotBlank() || variantAttribute.isNotBlank()

    val daysSinceLastSold: Int
        get() = ((System.currentTimeMillis() - lastSoldTimestamp) / 86400000L).toInt()
}

@Entity(tableName = "product_variants")
data class ProductVariantEntity(
    @PrimaryKey val id: String,
    val parentProductId: String,
    val variantName: String, // e.g. "Size 40 / Teal"
    val sku: String,
    val barcode: String = "",
    val additionalPricePaise: Long = 0L,
    val physicalStock: Double = 10.0,
    val reservedStock: Double = 0.0,
    val size: String = "",
    val color: String = ""
) {
    val availableStock: Double
        get() = (physicalStock - reservedStock).coerceAtLeast(0.0)
}

enum class MovementType {
    INWARD_PURCHASE,
    OUTWARD_DISPATCH,
    ADJUSTMENT_CORRECTION,
    ORDER_RESERVE,
    ORDER_RELEASE,
    RETURN_RESTOCK,
    BRANCH_TRANSFER_OUT,
    BRANCH_TRANSFER_IN
}

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val productName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val movementType: MovementType,
    val deltaQuantity: Double,
    val physicalBefore: Double,
    val physicalAfter: Double,
    val reservedBefore: Double,
    val reservedAfter: Double,
    val referenceDoc: String, // e.g. ORD-1001, PO-2001, ADJ-001, RET-301
    val reason: String = "",
    val branchId: String = "BRANCH-MAIN"
)

enum class OrderStatus {
    DRAFT,
    CONFIRMED,
    DISPATCHED,
    CANCELLED
}

enum class PaymentStatus {
    UNPAID,
    PARTIAL,
    PAID
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String = "",
    val status: OrderStatus = OrderStatus.DRAFT,
    val subtotalPaise: Long = 0L,
    val discountPaise: Long = 0L,
    val taxPaise: Long = 0L,
    val totalPaise: Long = 0L,
    val paidPaise: Long = 0L,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val paymentMethod: String = "Cash", // Cash, UPI, Card, NetBanking, Split
    val dueDate: String = "",
    val returnedTotalPaise: Long = 0L, // Tracks total amount refunded/returned to prevent double returns
    val branchId: String = "BRANCH-MAIN",
    val createdAt: Long = System.currentTimeMillis(),
    val dispatchedAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String = ""
) {
    val balanceDuePaise: Long
        get() = (totalPaise - paidPaise - returnedTotalPaise).coerceAtLeast(0L)
}

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val sku: String,
    val quantity: Double,
    val returnedQuantity: Double = 0.0, // To prevent over-returning
    val unitPricePaise: Long,
    val costPriceSnapshotPaise: Long, // Preserved cost snapshot for historical gross profit!
    val gstRate: Double,
    val taxAmountPaise: Long,
    val totalPaise: Long
)

// -------------------------------------------------------------
// Returns & Exchanges Models
// -------------------------------------------------------------

enum class ReturnAdjustmentType {
    REFUND_CASH,
    REFUND_UPI,
    STORE_CREDIT,
    EXCHANGE,
    CASH_REFUND,
    UPI_REFUND,
    EXCHANGE_REPLACEMENT
}

@Entity(tableName = "returns")
data class ReturnEntity(
    @PrimaryKey val id: String,
    val returnNumber: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val customerName: String,
    val refundAmountPaise: Long,
    val adjustmentType: ReturnAdjustmentType,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis(),
    val branchId: String = "BRANCH-MAIN"
)

@Entity(tableName = "return_items")
data class ReturnItemEntity(
    @PrimaryKey val id: String,
    val returnId: String,
    val orderItemId: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val refundUnitPricePaise: Long,
    val restocked: Boolean = true // If true, returned item is put back on shelf
)

// -------------------------------------------------------------
// Daily Cash Register Closing Models
// -------------------------------------------------------------

@Entity(tableName = "cash_register_closings")
data class CashRegisterClosingEntity(
    @PrimaryKey val id: String,
    val date: String, // yyyy-MM-dd
    val openingCashPaise: Long,
    val recordedCashSalesPaise: Long,
    val recordedNonCashSalesPaise: Long, // UPI, Card, NetBanking
    val cashRefundsPaise: Long,
    val cashExpensesPaise: Long,
    val expectedCashPaise: Long,
    val actualCountedCashPaise: Long,
    val variancePaise: Long, // actualCounted - expectedCash (Surplus if > 0, Shortage if < 0)
    val closingStaffName: String,
    val notes: String = "",
    val closedAt: Long = System.currentTimeMillis(),
    val branchId: String = "BRANCH-MAIN"
) {
    val expectedClosingCashPaise: Long get() = expectedCashPaise
    val closingDate: String get() = date
    val closingStaff: String get() = closingStaffName
    val isReconciled: Boolean get() = variancePaise == 0L
}

// -------------------------------------------------------------
// Multi-Branch & Transfer Models
// -------------------------------------------------------------

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val city: String,
    val phone: String,
    val isPrimary: Boolean = false
)

enum class TransferStatus {
    INITIATED,
    IN_TRANSIT,
    RECEIVED,
    COMPLETED,
    CANCELLED
}

@Entity(tableName = "stock_transfers")
data class StockTransferEntity(
    @PrimaryKey val id: String,
    val transferNumber: String,
    val sourceBranchId: String,
    val sourceBranchName: String,
    val destinationBranchId: String,
    val destinationBranchName: String,
    val productId: String,
    val productName: String,
    val sku: String,
    val quantity: Double,
    val status: TransferStatus = TransferStatus.INITIATED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val receivedAt: Long? = null
)

// -------------------------------------------------------------
// Purchases & Procurement
// -------------------------------------------------------------

enum class PurchaseStatus {
    DRAFT,
    ORDERED,
    PARTIAL_RECEIVED,
    RECEIVED,
    CANCELLED
}

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey val id: String,
    val poNumber: String,
    val supplierId: String,
    val supplierName: String,
    val status: PurchaseStatus = PurchaseStatus.DRAFT,
    val totalPaise: Long = 0L,
    val paidPaise: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val expectedDate: String = "",
    val notes: String = "",
    val branchId: String = "BRANCH-MAIN"
) {
    val payableBalancePaise: Long
        get() = (totalPaise - paidPaise).coerceAtLeast(0L)
}

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey val id: String,
    val purchaseOrderId: String,
    val productId: String,
    val productName: String,
    val orderedQuantity: Double,
    val receivedQuantity: Double = 0.0,
    val unitCostPaise: Long,
    val gstRate: Double = 18.0,
    val totalPaise: Long
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val gstin: String = "",
    val address: String = "",
    val city: String = "Bengaluru",
    val state: String = "Karnataka",
    val customerTier: String = "Retail", // Retail, Silver Wholesale, Gold Wholesale
    val outstandingBalancePaise: Long = 0L,
    val totalPurchasesPaise: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val contactPerson: String = "",
    val phone: String,
    val email: String = "",
    val gstin: String = "",
    val address: String = "",
    val city: String = "Surat",
    val leadTimeDays: Int = 4,
    val outstandingPayablePaise: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val category: String, // Rent, Electricity, Logistics, Staff, Tea/Snacks, Packaging, Marketing, Misc
    val amountPaise: Long,
    val date: String,
    val description: String,
    val paymentMethod: String = "Cash", // Cash vs Digital
    val branchId: String = "BRANCH-MAIN",
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    LOW_STOCK,
    OUT_OF_STOCK,
    PENDING_ORDER,
    OVERDUE_PAYMENT,
    GOODS_RECEIPT,
    DEAD_STOCK,
    TRANSFER_ALERT,
    SYSTEM
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedId: String = ""
)

enum class UserRole {
    OWNER,
    MANAGER,
    SALES_STAFF,
    ACCOUNTANT
}

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: String = "current_workspace",
    val businessName: String = "MerchSpring Retail & Mart",
    val tagline: String = "Your Business, in Sync.",
    val ownerName: String = "Rajesh Verma",
    val businessCategory: String = "Retail & Wholesale",
    val industryProfile: IndustryProfile = IndustryProfile.CLOTHING_FASHION,
    val activeBranchId: String = "BRANCH-MAIN",
    val phone: String = "+91 98200 12345",
    val email: String = "store@merchspring.in",
    val address: String = "Shop 14, Commercial Complex, MG Road, Bengaluru",
    val gstin: String = "29AAAAA0000A1Z5",
    val pan: String = "AAAAA0000A",
    val currencyCode: String = "INR",
    val currencySymbol: String = "₹",
    val invoicePrefix: String = "MS-INV-",
    val poPrefix: String = "MS-PO-",
    val deadStockDaysThreshold: Int = 30,
    val activeRole: UserRole = UserRole.OWNER,
    val isSetupComplete: Boolean = true
)

// -------------------------------------------------------------
// AI Assistant Action Proposals & Document Extraction Models
// -------------------------------------------------------------

enum class ActionProposalType {
    ADD_STOCK,
    CONFIRM_ORDER,
    RECORD_EXPENSE,
    CREATE_PO,
    REORDER_RESTOCK
}

data class StructuredActionProposal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ActionProposalType,
    val title: String,
    val summary: String,
    val targetEntityId: String = "",
    val targetEntityName: String = "",
    val quantity: Double = 0.0,
    val amountPaise: Long = 0L,
    val details: Map<String, String> = emptyMap()
)

data class ExtractedInvoiceDraft(
    val supplierName: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val lineItems: List<ExtractedInvoiceItem>,
    val subtotalPaise: Long,
    val taxPaise: Long,
    val totalPaise: Long,
    val confidence: String = "High",
    val warnings: List<String> = emptyList()
)

data class ExtractedInvoiceItem(
    val description: String,
    val hsn: String = "",
    val quantity: Double,
    val unitPricePaise: Long,
    val gstRate: Double,
    val totalPaise: Long
)

data class ReorderRecommendation(
    val product: ProductEntity,
    val dailySalesVelocity: Double, // units per day
    val daysOfCoverRemaining: Double,
    val recommendedOrderQty: Double,
    val estimatedCostPaise: Long,
    val reasonExplanation: String
)
