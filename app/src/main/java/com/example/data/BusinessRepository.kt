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
        db.withTransaction {
            if (db.settingsDao().getSettingsSync() == null) {
                // Initialize only the missing workspace metadata. Never clear business
                // tables just because a settings row is absent: that could erase existing
                // products, orders, customers, or financial records.
                db.settingsDao().saveSettings(
                    BusinessSettingsEntity(
                        businessName = "My Business",
                        ownerName = "",
                        gstin = "",
                        industryProfile = IndustryProfile.CLOTHING_FASHION,
                        isSetupComplete = true
                    )
                )
                db.branchDao().insertOrUpdate(
                    BranchEntity(
                        id = "BRANCH-MAIN",
                        name = "Main Store",
                        address = "",
                        city = "",
                        phone = "",
                        isPrimary = true
                    )
                )
            }
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
        if (!newPhysicalStock.isFinite() || newPhysicalStock < 0.0) {
            return Result.failure(IllegalArgumentException("Physical stock must be a finite, non-negative quantity."))
        }
        val result = db.withTransaction {
            val product = db.productDao().getProductById(productId)
                ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Product not found."))
            if (newPhysicalStock < product.reservedStock) {
                return@withTransaction Result.failure<Unit>(
                    IllegalArgumentException("Physical stock cannot be less than currently reserved stock (${product.reservedStock}).")
                )
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
            db.productDao().updateStock(productId, newPhysicalStock, product.reservedStock)
            db.stockMovementDao().insert(movement)
            Result.success(Unit)
        }
        if (result.isSuccess) {
            db.productDao().getProductById(productId)?.let { checkAndTriggerLowStockAlert(it) }
        }
        return result
    }
    suspend fun addStock(productId: String, quantity: Double, reason: String): Result<Double> {
        if (!quantity.isFinite() || quantity <= 0.0) {
            return Result.failure(IllegalArgumentException("Stock addition must be a positive finite quantity."))
        }
        val result = db.withTransaction {
            val product = db.productDao().getProductById(productId)
                ?: return@withTransaction Result.failure<Double>(IllegalStateException("Product not found."))
            val newPhysical = product.physicalStock + quantity
            if (!newPhysical.isFinite() || newPhysical < product.reservedStock) {
                return@withTransaction Result.failure<Double>(IllegalStateException("Stock quantity would violate the current reservation."))
            }
            db.productDao().updateStock(product.id, newPhysical, product.reservedStock)
            db.stockMovementDao().insert(
                StockMovementEntity(
                    id = "MOV-${UUID.randomUUID().toString().take(8)}",
                    productId = product.id,
                    productName = product.name,
                    timestamp = System.currentTimeMillis(),
                    movementType = MovementType.ADJUSTMENT_CORRECTION,
                    deltaQuantity = quantity,
                    physicalBefore = product.physicalStock,
                    physicalAfter = newPhysical,
                    reservedBefore = product.reservedStock,
                    reservedAfter = product.reservedStock,
                    referenceDoc = "AI-STOCK-${UUID.randomUUID().toString().take(6).uppercase()}",
                    reason = reason.ifBlank { "AI-assisted stock addition confirmed by user" }
                )
            )
            Result.success(newPhysical)
        }
        if (result.isSuccess) {
            db.productDao().getProductById(productId)?.let { checkAndTriggerLowStockAlert(it) }
        }
        return result
    }
    // -------------------------------------------------------------
    // POS Quick Billing Checkout
    // -------------------------------------------------------------
    suspend fun createAndDispatchQuickSale(
        order: OrderEntity,
        items: List<OrderItemEntity>
    ): Result<Unit> {
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("A sale must contain at least one item."))
        if (order.totalPaise <= 0L || order.subtotalPaise < 0L || order.discountPaise < 0L || order.taxPaise < 0L) {
            return Result.failure(IllegalArgumentException("Sale totals are invalid."))
        }
        if (order.paidPaise !in 0L..order.totalPaise) {
            return Result.failure(IllegalArgumentException("The recorded payment must be between zero and the bill total."))
        }
        if (items.any { it.orderId != order.id || !it.quantity.isFinite() || it.quantity <= 0.0 || it.unitPricePaise < 0L || it.taxAmountPaise < 0L }) {
            return Result.failure(IllegalArgumentException("One or more sale items have invalid order IDs, quantities, or amounts."))
        }

        val groupedItems = items.groupBy { it.productId }
        val resultingOrder = db.withTransaction {
            if (db.orderDao().getOrderById(order.id) != null) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("This sale ID already exists. Please retry with a new checkout."))
            }
            val productsById = mutableMapOf<String, ProductEntity>()
            for ((productId, productItems) in groupedItems) {
                val product = db.productDao().getProductById(productId)
                    ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Product ${productItems.first().productName} no longer exists."))
                val requested = productItems.sumOf { it.quantity }
                if (!requested.isFinite() || requested <= 0.0 || product.availableStock < requested) {
                    return@withTransaction Result.failure<Unit>(
                        IllegalStateException("Insufficient current stock for '${product.name}'. Available: ${product.availableStock}, requested: $requested.")
                    )
                }
                productsById[productId] = product
            }

            val paidStatus = when {
                order.paidPaise >= order.totalPaise -> PaymentStatus.PAID
                order.paidPaise > 0L -> PaymentStatus.PARTIAL
                else -> PaymentStatus.UNPAID
            }
            val dispatchedOrder = order.copy(
                status = OrderStatus.DISPATCHED,
                dispatchedAt = System.currentTimeMillis(),
                paymentStatus = paidStatus
            )
            db.orderDao().insertOrUpdate(dispatchedOrder)
            db.orderDao().insertOrderItems(items)

            for ((productId, productItems) in groupedItems) {
                val product = productsById[productId]!!
                val quantity = productItems.sumOf { it.quantity }
                val newPhysical = product.physicalStock - quantity
                db.productDao().updateStock(product.id, newPhysical, product.reservedStock)
                db.productDao().updateLastSoldTime(product.id, System.currentTimeMillis())
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        id = "MOV-${UUID.randomUUID().toString().take(8)}",
                        productId = product.id,
                        productName = product.name,
                        timestamp = System.currentTimeMillis(),
                        movementType = MovementType.OUTWARD_DISPATCH,
                        deltaQuantity = -quantity,
                        physicalBefore = product.physicalStock,
                        physicalAfter = newPhysical,
                        reservedBefore = product.reservedStock,
                        reservedAfter = product.reservedStock,
                        referenceDoc = order.orderNumber,
                        reason = "POS checkout: ${order.orderNumber}"
                    )
                )
            }

            val balanceDue = dispatchedOrder.balanceDuePaise
            val customer = db.customerDao().getCustomerById(dispatchedOrder.customerId)
            when {
                customer != null -> db.customerDao().insertOrUpdate(
                    customer.copy(
                        totalPurchasesPaise = customer.totalPurchasesPaise + dispatchedOrder.totalPaise,
                        outstandingBalancePaise = customer.outstandingBalancePaise + balanceDue
                    )
                )
                dispatchedOrder.customerId != "CUS-WALKIN" -> db.customerDao().insertOrUpdate(
                    CustomerEntity(
                        id = dispatchedOrder.customerId,
                        name = dispatchedOrder.customerName,
                        phone = dispatchedOrder.customerPhone,
                        city = "",
                        state = "",
                        totalPurchasesPaise = dispatchedOrder.totalPaise,
                        outstandingBalancePaise = balanceDue
                    )
                )
            }
            Result.success(Unit)
        }

        if (resultingOrder.isFailure) return resultingOrder
        for (productId in groupedItems.keys) {
            db.productDao().getProductById(productId)?.let { checkAndTriggerLowStockAlert(it) }
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
        if (refundAmountPaise < 0L) return Result.failure(IllegalArgumentException("Refund cannot be negative."))
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("Select at least one item for the return or exchange."))
        if (items.any { !it.quantity.isFinite() || it.quantity <= 0.0 }) {
            return Result.failure(IllegalArgumentException("Every returned quantity must be a positive finite number."))
        }

        return db.withTransaction {
            val order = db.orderDao().getOrderById(orderId)
                ?: return@withTransaction Result.failure<ReturnEntity>(IllegalStateException("Order $orderId not found."))
            if (order.status != OrderStatus.DISPATCHED) {
                return@withTransaction Result.failure<ReturnEntity>(IllegalStateException("Only dispatched orders can be returned or exchanged."))
            }
            val maxAllowableRefund = (order.totalPaise - order.returnedTotalPaise).coerceAtLeast(0L)
            if (refundAmountPaise > maxAllowableRefund) {
                return@withTransaction Result.failure<ReturnEntity>(
                    IllegalArgumentException("Refund exceeds the remaining eligible order balance of ${maxAllowableRefund.toRupeesString()}.")
                )
            }

            val originalItems = db.orderDao().getItemsForOrder(orderId)
            val originalItemMap = originalItems.associateBy { it.id }
            val quantitiesByOrderItem = items.groupBy { it.orderItemId }
            for ((orderItemId, requestedItems) in quantitiesByOrderItem) {
                val original = originalItemMap[orderItemId]
                    ?: return@withTransaction Result.failure<ReturnEntity>(IllegalArgumentException("A selected return item does not belong to this order."))
                if (requestedItems.any { it.productId != original.productId }) {
                    return@withTransaction Result.failure<ReturnEntity>(IllegalArgumentException("Returned product does not match the original order item '${original.productName}'."))
                }
                val requestedQuantity = requestedItems.sumOf { it.quantity }
                val availableToReturn = (original.quantity - original.returnedQuantity).coerceAtLeast(0.0)
                if (!requestedQuantity.isFinite() || requestedQuantity > availableToReturn) {
                    return@withTransaction Result.failure<ReturnEntity>(
                        IllegalArgumentException("Cannot return $requestedQuantity units of '${original.productName}'. Maximum returnable: $availableToReturn.")
                    )
                }
            }

            val restockByProduct = items.filter { it.restocked }.groupBy { it.productId }
            for ((productId, _) in restockByProduct) {
                if (db.productDao().getProductById(productId) == null) {
                    return@withTransaction Result.failure<ReturnEntity>(IllegalStateException("A product selected for restocking no longer exists."))
                }
            }

            val returnId = "RET-${UUID.randomUUID().toString().take(8).uppercase()}"
            val returnNumber = "MS-RET-${UUID.randomUUID().toString().take(8).uppercase()}"
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

            db.returnDao().insertReturn(returnEntity)
            db.returnDao().insertReturnItems(items.map { it.copy(returnId = returnId) })

            for ((orderItemId, requestedItems) in quantitiesByOrderItem) {
                val original = originalItemMap[orderItemId]!!
                val quantityReturned = requestedItems.sumOf { it.quantity }
                db.orderDao().insertOrderItems(
                    listOf(original.copy(returnedQuantity = original.returnedQuantity + quantityReturned))
                )
            }

            for ((productId, returnedItems) in restockByProduct) {
                val product = db.productDao().getProductById(productId)!!
                val quantity = returnedItems.sumOf { it.quantity }
                val newPhysical = product.physicalStock + quantity
                db.productDao().updateStock(product.id, newPhysical, product.reservedStock)
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        id = "MOV-${UUID.randomUUID().toString().take(8)}",
                        productId = product.id,
                        productName = product.name,
                        timestamp = System.currentTimeMillis(),
                        movementType = MovementType.RETURN_RESTOCK,
                        deltaQuantity = quantity,
                        physicalBefore = product.physicalStock,
                        physicalAfter = newPhysical,
                        reservedBefore = product.reservedStock,
                        reservedAfter = product.reservedStock,
                        referenceDoc = returnNumber,
                        reason = "Restocked from return $returnNumber: ${reason.ifBlank { "Customer return / exchange" }}"
                    )
                )
            }

            db.orderDao().insertOrUpdate(
                order.copy(returnedTotalPaise = order.returnedTotalPaise + refundAmountPaise)
            )

            val customer = db.customerDao().getCustomerById(order.customerId)
            if (customer != null && adjustmentType == ReturnAdjustmentType.STORE_CREDIT) {
                val newBalance = (customer.outstandingBalancePaise - refundAmountPaise).coerceAtLeast(0L)
                db.customerDao().insertOrUpdate(customer.copy(outstandingBalancePaise = newBalance))
            }
            Result.success(returnEntity)
        }
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
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) {
            return Result.failure(IllegalArgumentException("Closing date must use yyyy-MM-dd format."))
        }
        if (openingCashPaise < 0L || actualCountedCashPaise < 0L ||
            listOfNotNull(recordedCashSalesPaise, recordedNonCashSalesPaise, cashRefundsPaise, cashExpensesPaise).any { it < 0L }) {
            return Result.failure(IllegalArgumentException("Cash closing amounts cannot be negative."))
        }

        return db.withTransaction {
            if (db.cashRegisterDao().getClosingForDateAndBranch(date, branchId) != null) {
                return@withTransaction Result.failure<CashRegisterClosingEntity>(
                    IllegalStateException("A cash closing already exists for $date at branch $branchId. Use the correction process instead of creating a duplicate.")
                )
            }
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fun fallsOnDate(timestamp: Long): Boolean = dateFormatter.format(Date(timestamp)) == date
            val orders = db.orderDao().getAllOrders().firstOrNull().orEmpty().filter {
                it.status == OrderStatus.DISPATCHED && it.branchId == branchId &&
                    fallsOnDate(it.dispatchedAt ?: it.createdAt)
            }
            val cashSales = recordedCashSalesPaise ?: orders
                .filter { it.paymentMethod.equals("Cash", ignoreCase = true) }
                .sumOf { it.paidPaise }
            val digitalSales = recordedNonCashSalesPaise ?: orders
                .filter { !it.paymentMethod.equals("Cash", ignoreCase = true) && !it.paymentMethod.equals("Credit", ignoreCase = true) }
                .sumOf { it.paidPaise }
            val returns = db.returnDao().getAllReturns().firstOrNull().orEmpty().filter {
                it.branchId == branchId && fallsOnDate(it.createdAt)
            }
            val cashRefunds = cashRefundsPaise ?: returns
                .filter { it.adjustmentType == ReturnAdjustmentType.CASH_REFUND || it.adjustmentType == ReturnAdjustmentType.REFUND_CASH }
                .sumOf { it.refundAmountPaise }
            val expenses = db.expenseDao().getAllExpenses().firstOrNull().orEmpty().filter {
                it.date == date && it.branchId == branchId
            }
            val cashExpenses = cashExpensesPaise ?: expenses
                .filter { it.paymentMethod.equals("Cash", ignoreCase = true) }
                .sumOf { it.amountPaise }

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
            Result.success(closing)
        }
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
        if (!quantity.isFinite() || quantity <= 0.0) {
            return Result.failure(IllegalArgumentException("Transfer quantity must be a positive finite number."))
        }
        if (sourceBranchId.isBlank() || destinationBranchId.isBlank() || sourceBranchId == destinationBranchId) {
            return Result.failure(IllegalArgumentException("Choose two different, valid branches for a stock transfer."))
        }

        return db.withTransaction {
            val sourceBranch = db.branchDao().getBranchById(sourceBranchId)
                ?: return@withTransaction Result.failure<StockTransferEntity>(IllegalStateException("Source branch not found."))
            val destinationBranch = db.branchDao().getBranchById(destinationBranchId)
                ?: return@withTransaction Result.failure<StockTransferEntity>(IllegalStateException("Destination branch not found."))
            val product = db.productDao().getProductById(productId)
                ?: return@withTransaction Result.failure<StockTransferEntity>(IllegalStateException("Product not found."))
            if (product.availableStock < quantity) {
                return@withTransaction Result.failure<StockTransferEntity>(
                    IllegalStateException("Insufficient source stock for '${product.name}'. Available: ${product.availableStock}, requested: $quantity.")
                )
            }

            val transferNumber = "TRF-${UUID.randomUUID().toString().take(8).uppercase()}"
            val transferId = "${transferNumber}-${UUID.randomUUID().toString().take(4).uppercase()}"
            val transfer = StockTransferEntity(
                id = transferId,
                transferNumber = transferNumber,
                sourceBranchId = sourceBranch.id,
                sourceBranchName = sourceBranch.name.ifBlank { sourceBranchName },
                destinationBranchId = destinationBranch.id,
                destinationBranchName = destinationBranch.name.ifBlank { destinationBranchName },
                productId = product.id,
                productName = product.name,
                sku = product.sku,
                quantity = quantity,
                status = TransferStatus.IN_TRANSIT,
                notes = notes
            )
            val newPhysical = product.physicalStock - quantity
            db.productDao().updateStock(product.id, newPhysical, product.reservedStock)
            db.stockMovementDao().insert(
                StockMovementEntity(
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
                    reason = "Transferred to ${destinationBranch.name} ($transferNumber)"
                )
            )
            db.stockTransferDao().insertOrUpdate(transfer)
            Result.success(transfer)
        }
    }


    suspend fun receiveTransfer(transferId: String, staffName: String = ""): Result<Unit> {
        return db.withTransaction {
            val transfer = db.stockTransferDao().getTransferById(transferId)
                ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Transfer $transferId not found."))
            if (transfer.status != TransferStatus.IN_TRANSIT) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Transfer is not in transit (current status: ${transfer.status})."))
            }
            if (!transfer.quantity.isFinite() || transfer.quantity <= 0.0) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Transfer quantity is invalid; reconcile this transfer manually."))
            }
            if (db.branchDao().getBranchById(transfer.destinationBranchId) == null) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Destination branch no longer exists."))
            }
            val product = db.productDao().getProductById(transfer.productId)
                ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Transferred product no longer exists."))
            val newPhysical = product.physicalStock + transfer.quantity
            if (!newPhysical.isFinite() || newPhysical < product.reservedStock) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Receiving this transfer would violate stock constraints."))
            }
            val now = System.currentTimeMillis()
            db.productDao().updateStock(product.id, newPhysical, product.reservedStock)
            db.stockMovementDao().insert(
                StockMovementEntity(
                    id = "MOV-${UUID.randomUUID().toString().take(8)}",
                    productId = product.id,
                    productName = product.name,
                    timestamp = now,
                    movementType = MovementType.BRANCH_TRANSFER_IN,
                    deltaQuantity = transfer.quantity,
                    physicalBefore = product.physicalStock,
                    physicalAfter = newPhysical,
                    reservedBefore = product.reservedStock,
                    reservedAfter = product.reservedStock,
                    referenceDoc = transfer.transferNumber,
                    reason = "Received at ${transfer.destinationBranchName} from ${transfer.sourceBranchName}${staffName.trim().takeIf { it.isNotBlank() }?.let { " by $it" } ?: ""}"
                )
            )
            db.stockTransferDao().insertOrUpdate(
                transfer.copy(status = TransferStatus.COMPLETED, receivedAt = now)
            )
            Result.success(Unit)
        }
    }


    // -------------------------------------------------------------
    // Smart Reorder Recommendations Engine
    // -------------------------------------------------------------
    suspend fun computeReorderRecommendations(productsList: List<ProductEntity>): List<ReorderRecommendation> {
        val suppliersList = db.supplierDao().getAllSuppliers().firstOrNull().orEmpty()
        val supplierMap = suppliersList.associateBy { it.name.trim().lowercase(Locale.US) }
        val now = System.currentTimeMillis()
        val windowStart = now - 30L * 24L * 60L * 60L * 1000L
        val salesOrders = db.orderDao().getAllOrders().firstOrNull().orEmpty().filter { order ->
            order.status == OrderStatus.DISPATCHED && (order.dispatchedAt ?: order.createdAt) in windowStart..now
        }
        val dispatchedIds = salesOrders.map { it.id }.toSet()
        val recentItems = db.orderDao().getAllOrderItems().filter { it.orderId in dispatchedIds }
        val dailyVelocityByProduct = recentItems.groupBy { it.productId }.mapValues { (_, lines) ->
            (lines.sumOf { (it.quantity - it.returnedQuantity).coerceAtLeast(0.0) } / 30.0)
        }

        return productsList.mapNotNull { product ->
            val supplier = suppliersList.firstOrNull { candidate ->
                candidate.id.equals(product.preferredSupplier.trim(), ignoreCase = true) ||
                    candidate.name.equals(product.preferredSupplier.trim(), ignoreCase = true)
            } ?: supplierMap[product.preferredSupplier.trim().lowercase(Locale.US)]
            val leadDays = supplier?.leadTimeDays?.coerceAtLeast(0) ?: 4
            val velocity = dailyVelocityByProduct[product.id]?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
            val daysOfCover = if (velocity > 0.0) product.availableStock / velocity else Double.POSITIVE_INFINITY
            val approachingReorderPoint = velocity > 0.0 && daysOfCover <= leadDays + 3.0

            if (product.isOutOfStock || product.isLowStock || approachingReorderPoint) {
                val moq = product.moq.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
                val targetBufferDays = 14.0
                val demandBasedNeed = (targetBufferDays * velocity - product.availableStock).coerceAtLeast(0.0)
                val thresholdRecoveryNeed = (product.minStockThreshold + moq - product.availableStock).coerceAtLeast(0.0)
                val requiredUnits = maxOf(demandBasedNeed, thresholdRecoveryNeed, moq)
                val roundedQty = kotlin.math.ceil(requiredUnits / moq) * moq
                val estimatedCost = (roundedQty * product.costPricePaise).toLong()

                val daysText = if (daysOfCover.isFinite()) String.format(Locale.US, "%.1f", daysOfCover) else "unknown"
                val reason = when {
                    product.isOutOfStock -> "Critical: available stock is zero. Recent 30-day sales velocity: ${String.format(Locale.US, "%.2f", velocity)} ${product.unit}/day."
                    product.isLowStock -> "Running low: available stock is below the configured threshold (${product.minStockThreshold} ${product.unit}). Estimated cover: $daysText days."
                    else -> "Lead-time risk: estimated cover is $daysText days against a supplier lead time of $leadDays days."
                } + if (supplier == null) " No preferred supplier matched; confirm the supplier manually." else " Preferred supplier: ${supplier.name}."

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
        return db.withTransaction {
            val order = db.orderDao().getOrderById(orderId)
                ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Order $orderId not found."))
            if (order.status != OrderStatus.DRAFT) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Only Draft orders can be confirmed. Current status: ${order.status}."))
            }
            val items = db.orderDao().getItemsForOrder(orderId)
            if (items.isEmpty()) return@withTransaction Result.failure<Unit>(IllegalStateException("Cannot confirm an empty order."))
            if (items.any { !it.quantity.isFinite() || it.quantity <= 0.0 }) {
                return@withTransaction Result.failure<Unit>(IllegalArgumentException("Order quantities must be positive finite numbers."))
            }

            val requestedByProduct = items.groupBy { it.productId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
            val productsById = mutableMapOf<String, ProductEntity>()
            for ((productId, requested) in requestedByProduct) {
                val product = db.productDao().getProductById(productId)
                    ?: return@withTransaction Result.failure<Unit>(IllegalStateException("An order product no longer exists."))
                if (!requested.isFinite() || product.availableStock < requested) {
                    return@withTransaction Result.failure<Unit>(
                        IllegalStateException("Insufficient stock for '${product.name}'. Available: ${product.availableStock}, requested: $requested.")
                    )
                }
                productsById[productId] = product
            }

            for ((productId, requested) in requestedByProduct) {
                val product = productsById[productId]!!
                val newReserved = product.reservedStock + requested
                db.productDao().updateStock(product.id, product.physicalStock, newReserved)
                db.stockMovementDao().insert(
                    StockMovementEntity(
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
                )
            }
            db.orderDao().insertOrUpdate(order.copy(status = OrderStatus.CONFIRMED))
            Result.success(Unit)
        }
    }


    suspend fun dispatchOrder(orderId: String): Result<Unit> {
        val result = db.withTransaction {
            val order = db.orderDao().getOrderById(orderId)
                ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Order $orderId not found."))
            if (order.status != OrderStatus.CONFIRMED) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Only Confirmed orders can be dispatched. Current status: ${order.status}."))
            }
            val items = db.orderDao().getItemsForOrder(orderId)
            if (items.isEmpty() || items.any { !it.quantity.isFinite() || it.quantity <= 0.0 }) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("The order has no valid items to dispatch."))
            }

            val requestedByProduct = items.groupBy { it.productId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
            val productsById = mutableMapOf<String, ProductEntity>()
            for ((productId, requested) in requestedByProduct) {
                val product = db.productDao().getProductById(productId)
                    ?: return@withTransaction Result.failure<Unit>(IllegalStateException("An order product no longer exists."))
                if (product.physicalStock < requested || product.reservedStock < requested) {
                    return@withTransaction Result.failure<Unit>(
                        IllegalStateException("Stock reservation mismatch for '${product.name}'. Physical: ${product.physicalStock}, reserved: ${product.reservedStock}, required: $requested. Reconcile inventory before dispatch.")
                    )
                }
                productsById[productId] = product
            }

            val dispatchTime = System.currentTimeMillis()
            for ((productId, requested) in requestedByProduct) {
                val product = productsById[productId]!!
                val newPhysical = product.physicalStock - requested
                val newReserved = product.reservedStock - requested
                db.productDao().updateStock(product.id, newPhysical, newReserved)
                db.productDao().updateLastSoldTime(product.id, dispatchTime)
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        id = "MOV-${UUID.randomUUID().toString().take(8)}",
                        productId = product.id,
                        productName = product.name,
                        timestamp = dispatchTime,
                        movementType = MovementType.OUTWARD_DISPATCH,
                        deltaQuantity = -requested,
                        physicalBefore = product.physicalStock,
                        physicalAfter = newPhysical,
                        reservedBefore = product.reservedStock,
                        reservedAfter = newReserved,
                        referenceDoc = order.orderNumber,
                        reason = "Dispatched Order ${order.orderNumber} to ${order.customerName}"
                    )
                )
            }

            val customer = db.customerDao().getCustomerById(order.customerId)
            if (customer != null) {
                db.customerDao().insertOrUpdate(
                    customer.copy(
                        totalPurchasesPaise = customer.totalPurchasesPaise + order.totalPaise,
                        outstandingBalancePaise = customer.outstandingBalancePaise + order.balanceDuePaise
                    )
                )
            }
            db.orderDao().insertOrUpdate(order.copy(status = OrderStatus.DISPATCHED, dispatchedAt = dispatchTime))
            Result.success(Unit)
        }
        if (result.isFailure) return result
        db.orderDao().getItemsForOrder(orderId).map { it.productId }.distinct().forEach { id ->
            db.productDao().getProductById(id)?.let { checkAndTriggerLowStockAlert(it) }
        }
        return Result.success(Unit)
    }


    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> {
        return db.withTransaction {
            val order = db.orderDao().getOrderById(orderId)
                ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Order $orderId not found."))
            if (order.status == OrderStatus.DISPATCHED) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Cannot cancel a dispatched order. Use the return workflow instead."))
            }
            if (order.status == OrderStatus.CANCELLED) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Order is already cancelled."))
            }
            val items = db.orderDao().getItemsForOrder(orderId)
            if (order.status == OrderStatus.CONFIRMED && items.isEmpty()) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Confirmed order has no items; reconcile it before cancellation."))
            }
            if (order.status == OrderStatus.CONFIRMED && items.any { !it.quantity.isFinite() || it.quantity <= 0.0 }) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Confirmed order has invalid quantities; reconcile it before cancellation."))
            }
            if (order.status == OrderStatus.CONFIRMED) {
                val releaseByProduct = items.groupBy { it.productId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
                val productsById = mutableMapOf<String, ProductEntity>()
                for ((productId, requested) in releaseByProduct) {
                    val product = db.productDao().getProductById(productId)
                        ?: return@withTransaction Result.failure<Unit>(IllegalStateException("A reserved order product no longer exists."))
                    if (product.reservedStock < requested) {
                        return@withTransaction Result.failure<Unit>(
                            IllegalStateException("Reservation mismatch for '${product.name}'. Cannot safely release $requested units from ${product.reservedStock} reserved.")
                        )
                    }
                    productsById[productId] = product
                }
                for ((productId, requested) in releaseByProduct) {
                    val product = productsById[productId]!!
                    val newReserved = product.reservedStock - requested
                    db.productDao().updateStock(product.id, product.physicalStock, newReserved)
                    db.stockMovementDao().insert(
                        StockMovementEntity(
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
                    )
                }
            }
            db.orderDao().insertOrUpdate(
                order.copy(status = OrderStatus.CANCELLED, cancelledAt = System.currentTimeMillis(), cancellationReason = reason)
            )
            Result.success(Unit)
        }
    }


    suspend fun recordOrderPayment(orderId: String, amountPaise: Long, method: String): Result<Unit> {
        if (amountPaise <= 0L) return Result.failure(IllegalArgumentException("Payment amount must be greater than zero."))
        if (method.isBlank()) return Result.failure(IllegalArgumentException("Select a payment method."))
        return db.withTransaction {
            val order = db.orderDao().getOrderById(orderId)
                ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Order $orderId not found."))
            if (order.status == OrderStatus.CANCELLED) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Cannot take payment for a cancelled order."))
            }
            val due = order.balanceDuePaise
            if (due <= 0L) return@withTransaction Result.failure<Unit>(IllegalStateException("This order has no outstanding balance."))
            if (amountPaise > due) {
                return@withTransaction Result.failure<Unit>(IllegalArgumentException("Payment exceeds the current balance due of ${due.toRupeesString()}."))
            }
            val newPaid = order.paidPaise + amountPaise
            val newStatus = when {
                newPaid + order.returnedTotalPaise >= order.totalPaise -> PaymentStatus.PAID
                newPaid > 0L -> PaymentStatus.PARTIAL
                else -> PaymentStatus.UNPAID
            }
            db.orderDao().insertOrUpdate(order.copy(paidPaise = newPaid, paymentStatus = newStatus, paymentMethod = method))
            val customer = db.customerDao().getCustomerById(order.customerId)
            if (customer != null) {
                db.customerDao().insertOrUpdate(
                    customer.copy(outstandingBalancePaise = (customer.outstandingBalancePaise - amountPaise).coerceAtLeast(0L))
                )
            }
            Result.success(Unit)
        }
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
        return db.withTransaction {
            val po = db.purchaseDao().getPurchaseOrderById(poId)
                ?: return@withTransaction Result.failure<Unit>(IllegalStateException("Purchase Order $poId not found."))
            if (po.status != PurchaseStatus.ORDERED && po.status != PurchaseStatus.PARTIAL_RECEIVED) {
                return@withTransaction Result.failure<Unit>(IllegalStateException("Only ordered or partially received purchase orders can receive goods."))
            }
            val items = db.purchaseDao().getItemsForPO(poId)
            if (items.isEmpty()) return@withTransaction Result.failure<Unit>(IllegalStateException("No items found for this purchase order."))
            val itemsById = items.associateBy { it.id }
            if (receivedMap.keys.any { it !in itemsById.keys }) {
                return@withTransaction Result.failure<Unit>(IllegalArgumentException("The receipt includes an item that does not belong to this purchase order."))
            }
            if (receivedMap.values.any { !it.isFinite() || it < 0.0 }) {
                return@withTransaction Result.failure<Unit>(IllegalArgumentException("Received quantities must be finite, non-negative numbers."))
            }
            val positiveReceipts = receivedMap.filterValues { it > 0.0 }
            if (positiveReceipts.isEmpty()) {
                return@withTransaction Result.failure<Unit>(IllegalArgumentException("Enter at least one positive received quantity."))
            }
            for ((itemId, quantity) in positiveReceipts) {
                val item = itemsById[itemId]!!
                val remaining = (item.orderedQuantity - item.receivedQuantity).coerceAtLeast(0.0)
                if (!item.orderedQuantity.isFinite() || item.orderedQuantity <= 0.0 || quantity > remaining) {
                    return@withTransaction Result.failure<Unit>(
                        IllegalArgumentException("Cannot receive $quantity units of '${item.productName}'. Remaining on this purchase order: $remaining.")
                    )
                }
                if (db.productDao().getProductById(item.productId) == null) {
                    return@withTransaction Result.failure<Unit>(IllegalStateException("Product '${item.productName}' no longer exists."))
                }
            }

            val oldReceivedValuePaise = items.sumOf { (it.unitCostPaise * it.receivedQuantity).toLong() }
            var addedReceivedValuePaise = 0L
            val updatedItems = items.map { item ->
                val quantity = positiveReceipts[item.id] ?: 0.0
                if (quantity <= 0.0) {
                    item
                } else {
                    val product = db.productDao().getProductById(item.productId)!!
                    val newReceivedQuantity = item.receivedQuantity + quantity
                    val newPhysical = product.physicalStock + quantity
                    db.productDao().updateStock(product.id, newPhysical, product.reservedStock)
                    val lineValue = (item.unitCostPaise * quantity).toLong()
                    addedReceivedValuePaise += lineValue
                    db.stockMovementDao().insert(
                        StockMovementEntity(
                            id = "MOV-${UUID.randomUUID().toString().take(8)}",
                            productId = product.id,
                            productName = product.name,
                            timestamp = System.currentTimeMillis(),
                            movementType = MovementType.INWARD_PURCHASE,
                            deltaQuantity = quantity,
                            physicalBefore = product.physicalStock,
                            physicalAfter = newPhysical,
                            reservedBefore = product.reservedStock,
                            reservedAfter = product.reservedStock,
                            referenceDoc = po.poNumber,
                            reason = "Received ${quantity} units under ${po.poNumber}; value is recorded at tax-exclusive unit cost."
                        )
                    )
                    item.copy(receivedQuantity = newReceivedQuantity)
                }
            }
            db.purchaseDao().insertPurchaseItems(updatedItems)

            val allComplete = updatedItems.all { it.receivedQuantity >= it.orderedQuantity }
            val newStatus = if (allComplete) PurchaseStatus.RECEIVED else PurchaseStatus.PARTIAL_RECEIVED
            db.purchaseDao().insertOrUpdate(po.copy(status = newStatus))

            // Increase supplier payables only by newly received value not already covered by
            // payments against this PO. Never add the whole PO balance again on each receipt.
            val newReceivedValuePaise = oldReceivedValuePaise + addedReceivedValuePaise
            val oldRecognizedPayablePaise = (oldReceivedValuePaise - po.paidPaise).coerceAtLeast(0L)
            val newRecognizedPayablePaise = (newReceivedValuePaise - po.paidPaise).coerceAtLeast(0L)
            val payableIncreasePaise = (newRecognizedPayablePaise - oldRecognizedPayablePaise).coerceAtLeast(0L)
            if (payableIncreasePaise > 0L) {
                val supplier = db.supplierDao().getSupplierById(po.supplierId)
                if (supplier != null) {
                    db.supplierDao().insertOrUpdate(
                        supplier.copy(outstandingPayablePaise = supplier.outstandingPayablePaise + payableIncreasePaise)
                    )
                }
            }
            Result.success(Unit)
        }
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
