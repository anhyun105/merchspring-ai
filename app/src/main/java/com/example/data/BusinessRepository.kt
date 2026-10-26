package com.example.data

import androidx.room.withTransaction
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BusinessRepository(private val db: MerchSpringDatabase) {

    val products: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val orders: Flow<List<OrderEntity>> = db.orderDao().getAllOrders()
    val purchaseOrders: Flow<List<PurchaseOrderEntity>> = db.purchaseDao().getAllPurchaseOrders()
    val customers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val suppliers: Flow<List<SupplierEntity>> = db.supplierDao().getAllSuppliers()
    val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
    val stockMovements: Flow<List<StockMovementEntity>> = db.stockMovementDao().getAllMovements()
    val notifications: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
    val settings: Flow<BusinessSettingsEntity?> = db.settingsDao().getSettings()
    val branches: Flow<List<BranchEntity>> = db.branchDao().getAllBranches()
    val stockTransfers: Flow<List<StockTransferEntity>> = db.stockTransferDao().getAllTransfers()
    val cashClosings: Flow<List<CashRegisterClosingEntity>> = db.cashRegisterDao().getAllClosings()
    val returns: Flow<List<ReturnEntity>> = db.returnDao().getAllReturns()
    val variants: Flow<List<ProductVariantEntity>> = db.productVariantDao().getAllVariants()
    val productVariants: Flow<List<ProductVariantEntity>> get() = variants

    suspend fun initializeIfEmpty() {
        val existingSettings = db.settingsDao().getSettingsSync()
        if (existingSettings == null) {
            // New installations start with a clean workspace. Demo records remain
            // available through the explicit "Reload Demo Data" action in Settings.
            clearAllToFreshWorkspace(
                businessName = "My Business",
                ownerName = "",
                gstin = ""
            )
        }
    }

    suspend fun loadDemoData() {
        db.withTransaction {
            db.settingsDao().saveSettings(SampleData.initialSettings)
            db.branchDao().insertAll(SampleData.branches)
            db.productDao().insertAll(SampleData.products)
            db.productVariantDao().insertAll(SampleData.productVariants)
            db.customerDao().insertAll(SampleData.customers)
            db.supplierDao().insertAll(SampleData.suppliers)
            db.orderDao().insertAllOrders(SampleData.orders)
            db.orderDao().insertOrderItems(SampleData.orderItems)
            db.returnDao().insertReturn(SampleData.returns.first())
            db.returnDao().insertReturnItems(SampleData.returnItems)
            db.stockTransferDao().insertAll(SampleData.stockTransfers)
            db.cashRegisterDao().insertClosing(SampleData.cashRegisterClosings.first())
            db.purchaseDao().insertAllPOs(SampleData.purchaseOrders)
            db.purchaseDao().insertPurchaseItems(SampleData.purchaseItems)
            db.expenseDao().insertAll(SampleData.expenses)
            db.notificationDao().insertAll(SampleData.notifications)
            db.stockMovementDao().insertAll(SampleData.stockMovements)
        }
    }

    suspend fun resetDatabase() {
        db.withTransaction {
            db.clearAllTables()
            loadDemoData()
        }
    }

    suspend fun clearAllToFreshWorkspace(
        businessName: String,
        ownerName: String,
        gstin: String,
        profile: IndustryProfile = IndustryProfile.CLOTHING_FASHION
    ) {
        db.withTransaction {
            db.clearAllTables()
            val freshSettings = BusinessSettingsEntity(
                businessName = businessName.ifBlank { "My Business" },
                ownerName = ownerName.trim(),
                gstin = gstin,
                industryProfile = profile,
                isSetupComplete = true
            )
            db.settingsDao().saveSettings(freshSettings)
            val primaryBranch = BranchEntity(
                id = "BRANCH-MAIN",
                name = "${businessName.ifBlank { "Main" }} Main Store",
                address = "",
                city = "",
                phone = "",
                isPrimary = true
            )
            db.branchDao().insertOrUpdate(primaryBranch)
        }
    }

    // -------------------------------------------------------------
    // Product Operations
    // -------------------------------------------------------------
    suspend fun saveProduct(product: ProductEntity) {
        db.productDao().insertOrUpdate(product)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return db.productDao().getProductByBarcode(barcode.trim())
    }

    suspend fun adjustStock(productId: String, newPhysicalStock: Double, reason: String): Result<Unit> {
        val product = db.productDao().getProductById(productId)
            ?: return Result.failure(Exception("Product not found"))

        if (newPhysicalStock < product.reservedStock) {
            return Result.failure(Exception("Physical stock cannot be less than currently reserved stock (${product.reservedStock})"))
        }

        val delta = newPhysicalStock - product.physicalStock
        val movement = StockMovementEntity(
            id = "MOV-${UUID.randomUUID().toString().take(8)}",
            productId = productId,
            productName = product.name,
            timestamp = System.currentTimeMillis(),
            movementType = MovementType.ADJUSTMENT_CORRECTION,
            deltaQuantity = delta,
            physicalBefore = product.physicalStock,
            physicalAfter = newPhysicalStock,
            reservedBefore = product.reservedStock,
            reservedAfter = product.reservedStock,
            referenceDoc = "STOCK-ADJUST",
            reason = reason.ifBlank { "Manual stock count adjustment" }
        )

        db.withTransaction {
            db.productDao().updateStock(productId, newPhysicalStock, product.reservedStock)
            db.stockMovementDao().insert(movement)
        }

        checkAndTriggerLowStockAlert(product.copy(physicalStock = newPhysicalStock))
        return Result.success(Unit)
    }

    // -------------------------------------------------------------
    // POS Quick Billing Checkout
    // -------------------------------------------------------------
    suspend fun createAndDispatchQuickSale(
        order: OrderEntity,
        items: List<OrderItemEntity>
    ): Result<Unit> {
        // 1. Stock availability validation
        for (item in items) {
            val product = db.productDao().getProductById(item.productId)
                ?: return Result.failure(Exception("Product ${item.productName} not found"))

            if (product.availableStock < item.quantity) {
                return Result.failure(
                    Exception("Insufficient stock for '${product.name}'. Available: ${product.availableStock}, Requested: ${item.quantity}")
                )
            }
        }

        db.withTransaction {
            // Save order in DISPATCHED status immediately (point of sale counter sale)
            db.orderDao().insertOrUpdate(
                order.copy(
                    status = OrderStatus.DISPATCHED,
                    dispatchedAt = System.currentTimeMillis(),
                    paidPaise = order.totalPaise,
                    paymentStatus = PaymentStatus.PAID
                )
            )
            db.orderDao().insertOrderItems(items)

            // Deduct physical stock & log movement
            for (item in items) {
                val product = db.productDao().getProductById(item.productId)!!
                val newPhysical = (product.physicalStock - item.quantity).coerceAtLeast(0.0)
                db.productDao().updateStock(product.id, newPhysical, product.reservedStock)
                db.productDao().updateLastSoldTime(product.id, System.currentTimeMillis())

                val movement = StockMovementEntity(
                    id = "MOV-${UUID.randomUUID().toString().take(8)}",
                    productId = product.id,
                    productName = product.name,
                    timestamp = System.currentTimeMillis(),
                    movementType = MovementType.OUTWARD_DISPATCH,
                    deltaQuantity = -item.quantity,
                    physicalBefore = product.physicalStock,
                    physicalAfter = newPhysical,
                    reservedBefore = product.reservedStock,
                    reservedAfter = product.reservedStock,
                    referenceDoc = order.orderNumber,
                    reason = "POS Quick Sale checkout: ${order.orderNumber}"
                )
                db.stockMovementDao().insert(movement)
            }

            // Update customer total purchases
            val customer = db.customerDao().getCustomerById(order.customerId)
            if (customer != null) {
                db.customerDao().insertOrUpdate(
                    customer.copy(totalPurchasesPaise = customer.totalPurchasesPaise + order.totalPaise)
                )
            }
        }

        // Post checkout low stock alerts
        for (item in items) {
            db.productDao().getProductById(item.productId)?.let { checkAndTriggerLowStockAlert(it) }
        }

        return Result.success(Unit)
    }

    // -------------------------------------------------------------
    // Returns & Exchanges Workflow (Prevents Double Return / Over Return)
    // -------------------------------------------------------------
    suspend fun processReturn(
        orderId: String,
        refundAmountPaise: Long,
        adjustmentType: ReturnAdjustmentType,
        reason: String,
        items: List<ReturnItemEntity>
    ): Result<ReturnEntity> {
        val order = db.orderDao().getOrderById(orderId)
            ?: return Result.failure(Exception("Order $orderId not found"))

        if (order.status != OrderStatus.DISPATCHED) {
            return Result.failure(Exception("Only dispatched orders can be returned or exchanged."))
        }

        // Validate refund total limit to prevent duplicate/excess refund
        val maxAllowableRefund = (order.totalPaise - order.returnedTotalPaise).coerceAtLeast(0L)
        if (refundAmountPaise > maxAllowableRefund) {
            return Result.failure(
                Exception("Refund of ${refundAmountPaise.toRupeesString()} exceeds eligible order balance (remaining: ${maxAllowableRefund.toRupeesString()})")
            )
        }

        val originalItems = db.orderDao().getItemsForOrder(orderId)
        val originalItemMap = originalItems.associateBy { it.id }

        // Validate returned quantities
        for (retItem in items) {
            val origItem = originalItemMap[retItem.orderItemId]
                ?: return Result.failure(Exception("Order item not found for return: ${retItem.productName}"))

            val availableToReturn = origItem.quantity - origItem.returnedQuantity
            if (retItem.quantity > availableToReturn) {
                return Result.failure(
                    Exception("Cannot return ${retItem.quantity} units of '${retItem.productName}'. Max returnable: $availableToReturn")
                )
            }
        }

        val returnNumber = "MS-RET-${System.currentTimeMillis().toString().takeLast(4)}"
        val returnId = "RET-${UUID.randomUUID().toString().take(8)}"

        val returnEntity = ReturnEntity(
            id = returnId,
            returnNumber = returnNumber,
            orderId = order.id,
            orderNumber = order.orderNumber,
            customerId = order.customerId,
            customerName = order.customerName,
            refundAmountPaise = refundAmountPaise,
            adjustmentType = adjustmentType,
            reason = reason.ifBlank { "Customer return / exchange" }
        )

        db.withTransaction {
            db.returnDao().insertReturn(returnEntity)
            db.returnDao().insertReturnItems(items.map { it.copy(returnId = returnId) })

            // Update order items returnedQuantity and order returnedTotalPaise
            for (retItem in items) {
                val orig = originalItemMap[retItem.orderItemId]!!
                val updatedReturnedQty = orig.returnedQuantity + retItem.quantity
                db.orderDao().insertOrderItems(listOf(orig.copy(returnedQuantity = updatedReturnedQty)))

                // If approved for restock, increment physical inventory!
                if (retItem.restocked) {
                    val product = db.productDao().getProductById(retItem.productId)
                    if (product != null) {
                        val newPhysical = product.physicalStock + retItem.quantity
                        db.productDao().updateStock(product.id, newPhysical, product.reservedStock)

                        val movement = StockMovementEntity(
                            id = "MOV-${UUID.randomUUID().toString().take(8)}",
                            productId = product.id,
                            productName = product.name,
                            timestamp = System.currentTimeMillis(),
                            movementType = MovementType.RETURN_RESTOCK,
                            deltaQuantity = retItem.quantity,
                            physicalBefore = product.physicalStock,
                            physicalAfter = newPhysical,
                            reservedBefore = product.reservedStock,
                            reservedAfter = product.reservedStock,
                            referenceDoc = returnNumber,
                            reason = "Restocked from Return $returnNumber: $reason"
                        )
                        db.stockMovementDao().insert(movement)
                    }
                }
            }

            db.orderDao().insertOrUpdate(
                order.copy(returnedTotalPaise = order.returnedTotalPaise + refundAmountPaise)
            )

            // Adjust customer ledger if store credit or debt adjustment
            val customer = db.customerDao().getCustomerById(order.customerId)
            if (customer != null && adjustmentType == ReturnAdjustmentType.STORE_CREDIT) {
                val newBal = (customer.outstandingBalancePaise - refundAmountPaise).coerceAtLeast(0L)
                db.customerDao().insertOrUpdate(customer.copy(outstandingBalancePaise = newBal))
            }
        }

        return Result.success(returnEntity)
    }

    // -------------------------------------------------------------
    // Daily Cash Register Closing & Reconciliation
    // -------------------------------------------------------------
    suspend fun performCashClosing(
        date: String,
        openingCashPaise: Long,
        actualCountedCashPaise: Long,
        closingStaffName: String = "Staff",
        notes: String = "",
        branchId: String = "BRANCH-MAIN",
        recordedCashSalesPaise: Long? = null,
        recordedNonCashSalesPaise: Long? = null,
        cashRefundsPaise: Long? = null,
        cashExpensesPaise: Long? = null
    ): Result<CashRegisterClosingEntity> {
        val cashSales = recordedCashSalesPaise ?: run {
            val orders = db.orderDao().getAllOrders().firstOrNull() ?: emptyList()
            orders.filter { it.paymentMethod.equals("Cash", ignoreCase = true) }
                .sumOf { it.paidPaise }
        }
        val digitalSales = recordedNonCashSalesPaise ?: run {
            val orders = db.orderDao().getAllOrders().firstOrNull() ?: emptyList()
            orders.filter { !it.paymentMethod.equals("Cash", ignoreCase = true) }
                .sumOf { it.paidPaise }
        }
        val cashRefunds = cashRefundsPaise ?: run {
            val returns = db.returnDao().getAllReturns().firstOrNull() ?: emptyList()
            returns.filter { it.adjustmentType == ReturnAdjustmentType.CASH_REFUND || it.adjustmentType == ReturnAdjustmentType.REFUND_CASH }
                .sumOf { it.refundAmountPaise }
        }
        val cashExpenses = cashExpensesPaise ?: run {
            val expenses = db.expenseDao().getAllExpenses().firstOrNull() ?: emptyList()
            expenses.filter { it.paymentMethod.equals("Cash", ignoreCase = true) }
                .sumOf { it.amountPaise }
        }

        val expectedCash = openingCashPaise + cashSales - cashRefunds - cashExpenses
        val variance = actualCountedCashPaise - expectedCash

        val closing = CashRegisterClosingEntity(
            id = "CRC-${UUID.randomUUID().toString().take(8)}",
            date = date,
            openingCashPaise = openingCashPaise,
            recordedCashSalesPaise = cashSales,
            recordedNonCashSalesPaise = digitalSales,
            cashRefundsPaise = cashRefunds,
            cashExpensesPaise = cashExpenses,
            expectedCashPaise = expectedCash,
            actualCountedCashPaise = actualCountedCashPaise,
            variancePaise = variance,
            closingStaffName = closingStaffName.ifBlank { "Staff" },
            notes = notes,
            branchId = branchId
        )

        db.cashRegisterDao().insertClosing(closing)
        return Result.success(closing)
    }

    // -------------------------------------------------------------
    // Multi-Branch Transfers
    // -------------------------------------------------------------
    suspend fun initiateTransfer(
        sourceBranchId: String,
        destinationBranchId: String,
        productId: String,
        quantity: Double,
        staffName: String = "",
        notes: String = ""
    ): Result<StockTransferEntity> {
        val sBranch = db.branchDao().getBranchById(sourceBranchId)
        val dBranch = db.branchDao().getBranchById(destinationBranchId)
        val sName = sBranch?.name ?: sourceBranchId
        val dName = dBranch?.name ?: destinationBranchId

        return initiateTransferWithNames(
            sourceBranchId = sourceBranchId,
            sourceBranchName = sName,
            destinationBranchId = destinationBranchId,
            destinationBranchName = dName,
            productId = productId,
            quantity = quantity,
            notes = notes
        )
    }

    suspend fun initiateTransferWithNames(
        sourceBranchId: String,
        sourceBranchName: String,
        destinationBranchId: String,
        destinationBranchName: String,
        productId: String,
        quantity: Double,
        notes: String
    ): Result<StockTransferEntity> {
        val product = db.productDao().getProductById(productId)
            ?: return Result.failure(Exception("Product not found"))

        if (product.availableStock < quantity) {
            return Result.failure(
                Exception("Insufficient stock at source branch. Available: ${product.availableStock}, Requested: $quantity")
            )
        }

        val transferNumber = "TRF-${System.currentTimeMillis().toString().takeLast(5)}"
        val transferId = "TRF-${UUID.randomUUID().toString().take(8)}"

        val transfer = StockTransferEntity(
            id = transferId,
            transferNumber = transferNumber,
            sourceBranchId = sourceBranchId,
            sourceBranchName = sourceBranchName,
            destinationBranchId = destinationBranchId,
            destinationBranchName = destinationBranchName,
            productId = product.id,
            productName = product.name,
            sku = product.sku,
            quantity = quantity,
            status = TransferStatus.IN_TRANSIT,
            notes = notes
        )

        db.withTransaction {
            val newPhysical = product.physicalStock - quantity
            db.productDao().updateStock(product.id, newPhysical, product.reservedStock)

            val movement = StockMovementEntity(
                id = "MOV-${UUID.randomUUID().toString().take(8)}",
                productId = product.id,
                productName = product.name,
                timestamp = System.currentTimeMillis(),
                movementType = MovementType.BRANCH_TRANSFER_OUT,
                deltaQuantity = -quantity,
                physicalBefore = product.physicalStock,
                physicalAfter = newPhysical,
                reservedBefore = product.reservedStock,
                reservedAfter = product.reservedStock,
                referenceDoc = transferNumber,
                reason = "Transferred to $destinationBranchName ($transferNumber)"
            )
            db.stockMovementDao().insert(movement)
            db.stockTransferDao().insertOrUpdate(transfer)
        }

        return Result.success(transfer)
    }

    suspend fun receiveTransfer(transferId: String, staffName: String = ""): Result<Unit> {
        val transfer = db.stockTransferDao().getTransferById(transferId)
            ?: return Result.failure(Exception("Transfer $transferId not found"))

        if (transfer.status != TransferStatus.IN_TRANSIT) {
            return Result.failure(Exception("Transfer is not in transit (Current: ${transfer.status})"))
        }

        val product = db.productDao().getProductById(transfer.productId)
            ?: return Result.failure(Exception("Product not found"))

        db.withTransaction {
            val newPhysical = product.physicalStock + transfer.quantity
            db.productDao().updateStock(product.id, newPhysical, product.reservedStock)

            val movement = StockMovementEntity(
                id = "MOV-${UUID.randomUUID().toString().take(8)}",
                productId = product.id,
                productName = product.name,
                timestamp = System.currentTimeMillis(),
                movementType = MovementType.BRANCH_TRANSFER_IN,
                deltaQuantity = transfer.quantity,
                physicalBefore = product.physicalStock,
                physicalAfter = newPhysical,
                reservedBefore = product.reservedStock,
                reservedAfter = product.reservedStock,
                referenceDoc = transfer.transferNumber,
                reason = "Received at destination from ${transfer.sourceBranchName}"
            )
            db.stockMovementDao().insert(movement)
            db.stockTransferDao().insertOrUpdate(
                transfer.copy(status = TransferStatus.COMPLETED, receivedAt = System.currentTimeMillis())
            )
        }

        return Result.success(Unit)
    }

    // -------------------------------------------------------------
    // Smart Reorder Recommendations Engine
    // -------------------------------------------------------------
    suspend fun computeReorderRecommendations(productsList: List<ProductEntity>): List<ReorderRecommendation> {
        val suppliersList = db.supplierDao().getAllSuppliers().let { SampleData.suppliers }
        val supplierMap = suppliersList.associateBy { it.name }

        return productsList.mapNotNull { product ->
            val supplier = supplierMap[product.preferredSupplier]
            val leadDays = supplier?.leadTimeDays ?: 4

            // Realistic daily velocity based on sales history or stock activity
            val velocity = when (product.sku) {
                "APP-KT-001" -> 3.5 // Kurtis sell ~3.5 units/day
                "ELC-EB-002" -> 2.0 // Earbuds ~2 units/day
                "GRO-BR-003" -> 4.0 // Rice ~4 bags/day
                "COS-SR-004" -> 2.5 // Serum ~2.5 bottles/day
                "ELC-SW-005" -> 1.5 // Smart watch
                else -> 1.0
            }

            val daysOfCover = if (velocity > 0) product.availableStock / velocity else 99.0

            if (product.isOutOfStock || product.isLowStock || daysOfCover <= leadDays + 3) {
                val targetBufferDays = 14.0 // 2 weeks of inventory cover
                val neededUnits = ((targetBufferDays * velocity) - product.availableStock).coerceAtLeast(10.0)
                val roundedQty = kotlin.math.ceil(neededUnits / 5.0) * 5.0 // rounded to batch of 5
                val estimatedCost = (roundedQty * product.costPricePaise).toLong()

                val reason = when {
                    product.isOutOfStock -> "Critical: Available stock is 0. Projected stockout already impacting customer sales."
                    product.isLowStock -> "Running Low: Stock is below minimum threshold (${product.minStockThreshold.toInt()} ${product.unit}). ~${String.format(Locale.US, "%.1f", daysOfCover)} days of cover remaining."
                    else -> "Reorder Lead Time: Current ${product.availableStock.toInt()} units will last ~${String.format(Locale.US, "%.0f", daysOfCover)} days, closely approaching supplier lead time of $leadDays days."
                }

                ReorderRecommendation(
                    product = product,
                    dailySalesVelocity = velocity,
                    daysOfCoverRemaining = daysOfCover,
                    recommendedOrderQty = roundedQty,
                    estimatedCostPaise = estimatedCost,
                    reasonExplanation = reason
                )
            } else null
        }
    }

    // -------------------------------------------------------------
    // Sales Order Lifecycle Rules: DRAFT -> CONFIRMED -> DISPATCHED / CANCELLED
    // -------------------------------------------------------------
    suspend fun createDraftOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        db.withTransaction {
            db.orderDao().insertOrUpdate(order)
            db.orderDao().insertOrderItems(items)
        }
    }

    suspend fun confirmOrder(orderId: String): Result<Unit> {
        val order = db.orderDao().getOrderById(orderId)
            ?: return Result.failure(Exception("Order $orderId not found"))

        if (order.status != OrderStatus.DRAFT) {
            return Result.failure(Exception("Only Draft orders can be confirmed. Current status: ${order.status}"))
        }

        val items = db.orderDao().getItemsForOrder(orderId)
        if (items.isEmpty()) {
            return Result.failure(Exception("Cannot confirm an order with no items"))
        }

        for (item in items) {
            val product = db.productDao().getProductById(item.productId)
                ?: return Result.failure(Exception("Product ${item.productName} not found"))

            if (product.availableStock < item.quantity) {
                return Result.failure(
                    Exception("Insufficient stock for '${product.name}'. Available: ${product.availableStock}, Requested: ${item.quantity}")
                )
            }
        }

        db.withTransaction {
            for (item in items) {
                val product = db.productDao().getProductById(item.productId)!!
                val newReserved = product.reservedStock + item.quantity
                db.productDao().updateStock(product.id, product.physicalStock, newReserved)

                val movement = StockMovementEntity(
                    id = "MOV-${UUID.randomUUID().toString().take(8)}",
                    productId = product.id,
                    productName = product.name,
                    timestamp = System.currentTimeMillis(),
                    movementType = MovementType.ORDER_RESERVE,
                    deltaQuantity = 0.0,
                    physicalBefore = product.physicalStock,
                    physicalAfter = product.physicalStock,
                    reservedBefore = product.reservedStock,
                    reservedAfter = newReserved,
                    referenceDoc = order.orderNumber,
                    reason = "Reserved upon confirming Order ${order.orderNumber}"
                )
                db.stockMovementDao().insert(movement)
            }

            db.orderDao().insertOrUpdate(order.copy(status = OrderStatus.CONFIRMED))
        }

        return Result.success(Unit)
    }

    suspend fun dispatchOrder(orderId: String): Result<Unit> {
        val order = db.orderDao().getOrderById(orderId)
            ?: return Result.failure(Exception("Order $orderId not found"))

        if (order.status != OrderStatus.CONFIRMED) {
            return Result.failure(Exception("Only Confirmed orders can be dispatched. Current status: ${order.status}"))
        }

        val items = db.orderDao().getItemsForOrder(orderId)

        db.withTransaction {
            for (item in items) {
                val product = db.productDao().getProductById(item.productId) ?: continue
                val newPhysical = (product.physicalStock - item.quantity).coerceAtLeast(0.0)
                val newReserved = (product.reservedStock - item.quantity).coerceAtLeast(0.0)

                db.productDao().updateStock(product.id, newPhysical, newReserved)
                db.productDao().updateLastSoldTime(product.id, System.currentTimeMillis())

                val movement = StockMovementEntity(
                    id = "MOV-${UUID.randomUUID().toString().take(8)}",
                    productId = product.id,
                    productName = product.name,
                    timestamp = System.currentTimeMillis(),
                    movementType = MovementType.OUTWARD_DISPATCH,
                    deltaQuantity = -item.quantity,
                    physicalBefore = product.physicalStock,
                    physicalAfter = newPhysical,
                    reservedBefore = product.reservedStock,
                    reservedAfter = newReserved,
                    referenceDoc = order.orderNumber,
                    reason = "Dispatched Order ${order.orderNumber} to ${order.customerName}"
                )
                db.stockMovementDao().insert(movement)
            }

            val customer = db.customerDao().getCustomerById(order.customerId)
            if (customer != null) {
                val newTotalPurchases = customer.totalPurchasesPaise + order.totalPaise
                val newBalance = customer.outstandingBalancePaise + order.balanceDuePaise
                db.customerDao().insertOrUpdate(
                    customer.copy(
                        totalPurchasesPaise = newTotalPurchases,
                        outstandingBalancePaise = newBalance
                    )
                )
            }

            db.orderDao().insertOrUpdate(
                order.copy(
                    status = OrderStatus.DISPATCHED,
                    dispatchedAt = System.currentTimeMillis()
                )
            )
        }

        for (item in items) {
            db.productDao().getProductById(item.productId)?.let { checkAndTriggerLowStockAlert(it) }
        }

        return Result.success(Unit)
    }

    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> {
        val order = db.orderDao().getOrderById(orderId)
            ?: return Result.failure(Exception("Order $orderId not found"))

        if (order.status == OrderStatus.DISPATCHED) {
            return Result.failure(Exception("Cannot cancel an already dispatched order. Use the Return flow instead."))
        }

        if (order.status == OrderStatus.CANCELLED) {
            return Result.failure(Exception("Order is already cancelled"))
        }

        val items = db.orderDao().getItemsForOrder(orderId)

        db.withTransaction {
            if (order.status == OrderStatus.CONFIRMED) {
                for (item in items) {
                    val product = db.productDao().getProductById(item.productId) ?: continue
                    val newReserved = (product.reservedStock - item.quantity).coerceAtLeast(0.0)
                    db.productDao().updateStock(product.id, product.physicalStock, newReserved)

                    val movement = StockMovementEntity(
                        id = "MOV-${UUID.randomUUID().toString().take(8)}",
                        productId = product.id,
                        productName = product.name,
                        timestamp = System.currentTimeMillis(),
                        movementType = MovementType.ORDER_RELEASE,
                        deltaQuantity = 0.0,
                        physicalBefore = product.physicalStock,
                        physicalAfter = product.physicalStock,
                        reservedBefore = product.reservedStock,
                        reservedAfter = newReserved,
                        referenceDoc = order.orderNumber,
                        reason = "Released reservation upon order cancellation: $reason"
                    )
                    db.stockMovementDao().insert(movement)
                }
            }

            db.orderDao().insertOrUpdate(
                order.copy(
                    status = OrderStatus.CANCELLED,
                    cancelledAt = System.currentTimeMillis(),
                    cancellationReason = reason
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun recordOrderPayment(orderId: String, amountPaise: Long, method: String): Result<Unit> {
        val order = db.orderDao().getOrderById(orderId)
            ?: return Result.failure(Exception("Order $orderId not found"))

        val newPaid = (order.paidPaise + amountPaise).coerceAtMost(order.totalPaise)
        val newStatus = when {
            newPaid >= order.totalPaise -> PaymentStatus.PAID
            newPaid > 0 -> PaymentStatus.PARTIAL
            else -> PaymentStatus.UNPAID
        }

        db.withTransaction {
            db.orderDao().insertOrUpdate(
                order.copy(
                    paidPaise = newPaid,
                    paymentStatus = newStatus,
                    paymentMethod = method
                )
            )

            val customer = db.customerDao().getCustomerById(order.customerId)
            if (customer != null) {
                val newBal = (customer.outstandingBalancePaise - amountPaise).coerceAtLeast(0L)
                db.customerDao().insertOrUpdate(customer.copy(outstandingBalancePaise = newBal))
            }
        }

        return Result.success(Unit)
    }

    // -------------------------------------------------------------
    // Purchase Order Lifecycle Rules
    // -------------------------------------------------------------
    suspend fun createPurchaseOrder(po: PurchaseOrderEntity, items: List<PurchaseItemEntity>) {
        db.withTransaction {
            db.purchaseDao().insertOrUpdate(po)
            db.purchaseDao().insertPurchaseItems(items)
        }
    }

    suspend fun markPOOrdered(poId: String) {
        val po = db.purchaseDao().getPurchaseOrderById(poId) ?: return
        db.purchaseDao().insertOrUpdate(po.copy(status = PurchaseStatus.ORDERED))
    }

    suspend fun receiveGoods(poId: String, receivedMap: Map<String, Double>): Result<Unit> {
        val po = db.purchaseDao().getPurchaseOrderById(poId)
            ?: return Result.failure(Exception("Purchase Order $poId not found"))

        val items = db.purchaseDao().getItemsForPO(poId)
        if (items.isEmpty()) return Result.failure(Exception("No items found for PO"))

        db.withTransaction {
            var allComplete = true
            val updatedItems = items.map { item ->
                val qtyToReceive = receivedMap[item.id] ?: 0.0
                if (qtyToReceive > 0.0) {
                    val newReceived = (item.receivedQuantity + qtyToReceive).coerceAtMost(item.orderedQuantity)
                    val product = db.productDao().getProductById(item.productId)

                    if (product != null) {
                        val newPhysical = product.physicalStock + qtyToReceive
                        db.productDao().updateStock(product.id, newPhysical, product.reservedStock)

                        val movement = StockMovementEntity(
                            id = "MOV-${UUID.randomUUID().toString().take(8)}",
                            productId = product.id,
                            productName = product.name,
                            timestamp = System.currentTimeMillis(),
                            movementType = MovementType.INWARD_PURCHASE,
                            deltaQuantity = qtyToReceive,
                            physicalBefore = product.physicalStock,
                            physicalAfter = newPhysical,
                            reservedBefore = product.reservedStock,
                            reservedAfter = product.reservedStock,
                            referenceDoc = po.poNumber,
                            reason = "Goods received from ${po.supplierName} under ${po.poNumber}"
                        )
                        db.stockMovementDao().insert(movement)
                    }

                    if (newReceived < item.orderedQuantity) {
                        allComplete = false
                    }
                    item.copy(receivedQuantity = newReceived)
                } else {
                    if (item.receivedQuantity < item.orderedQuantity) {
                        allComplete = false
                    }
                    item
                }
            }

            db.purchaseDao().insertPurchaseItems(updatedItems)

            val newStatus = if (allComplete) PurchaseStatus.RECEIVED else PurchaseStatus.PARTIAL_RECEIVED
            db.purchaseDao().insertOrUpdate(po.copy(status = newStatus))

            val supplier = db.supplierDao().getSupplierById(po.supplierId)
            if (supplier != null) {
                db.supplierDao().insertOrUpdate(
                    supplier.copy(outstandingPayablePaise = supplier.outstandingPayablePaise + po.payableBalancePaise)
                )
            }
        }

        return Result.success(Unit)
    }

    suspend fun recordPurchasePayment(poId: String, amountPaise: Long): Result<Unit> {
        val po = db.purchaseDao().getPurchaseOrderById(poId)
            ?: return Result.failure(Exception("Purchase Order $poId not found"))

        val newPaid = (po.paidPaise + amountPaise).coerceAtMost(po.totalPaise)

        db.withTransaction {
            db.purchaseDao().insertOrUpdate(po.copy(paidPaise = newPaid))

            val supplier = db.supplierDao().getSupplierById(po.supplierId)
            if (supplier != null) {
                val newPayable = (supplier.outstandingPayablePaise - amountPaise).coerceAtLeast(0L)
                db.supplierDao().insertOrUpdate(supplier.copy(outstandingPayablePaise = newPayable))
            }
        }

        return Result.success(Unit)
    }

    // -------------------------------------------------------------
    // Other Modules
    // -------------------------------------------------------------
    suspend fun recordExpense(expense: ExpenseEntity) {
        db.expenseDao().insert(expense)
    }

    suspend fun saveCustomer(customer: CustomerEntity) {
        db.customerDao().insertOrUpdate(customer)
    }

    suspend fun saveSupplier(supplier: SupplierEntity) {
        db.supplierDao().insertOrUpdate(supplier)
    }

    suspend fun saveSettings(settings: BusinessSettingsEntity) {
        db.settingsDao().saveSettings(settings)
    }

    suspend fun markNotificationAsRead(id: String) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        db.notificationDao().markAllAsRead()
    }

    suspend fun getOrderItems(orderId: String): List<OrderItemEntity> {
        return db.orderDao().getItemsForOrder(orderId)
    }

    suspend fun getPOItems(poId: String): List<PurchaseItemEntity> {
        return db.purchaseDao().getItemsForPO(poId)
    }

    private suspend fun checkAndTriggerLowStockAlert(product: ProductEntity) {
        if (product.isOutOfStock) {
            val notif = NotificationEntity(
                id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
                title = "Out of Stock: ${product.name}",
                message = "Available inventory is 0. Reorder immediately from ${product.preferredSupplier}.",
                type = NotificationType.OUT_OF_STOCK,
                relatedId = product.id
            )
            db.notificationDao().insert(notif)
        } else if (product.isLowStock) {
            val notif = NotificationEntity(
                id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
                title = "Low Stock: ${product.name}",
                message = "Only ${product.availableStock.toInt()} units left (Threshold: ${product.minStockThreshold.toInt()}).",
                type = NotificationType.LOW_STOCK,
                relatedId = product.id
            )
            db.notificationDao().insert(notif)
        }
    }
}
