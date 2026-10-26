package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.BusinessRepository
import com.example.data.MerchSpringDatabase
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MerchSpringBusinessRulesTest {

    private lateinit var db: MerchSpringDatabase
    private lateinit var repository: BusinessRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MerchSpringDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BusinessRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testAppStringIsMerchSpringAI() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MerchSpring AI", appName)
    }

    @Test
    fun testB_orderConfirmationReservesStock() = runBlocking {
        // Initial state: Product with 10 physical units
        val product = ProductEntity(
            id = "PRD-TEST-1",
            name = "Test Cotton Kurti",
            sku = "SKU-TEST-1",
            category = "Apparel",
            costPricePaise = 40000L,
            sellingPricePaise = 80000L,
            physicalStock = 10.0,
            reservedStock = 0.0
        )
        repository.saveProduct(product)

        // Draft Order for 3 units
        val order = OrderEntity(
            id = "ORD-TEST-1",
            orderNumber = "MS-INV-TEST1",
            customerId = "CUS-1",
            customerName = "Test Buyer",
            status = OrderStatus.DRAFT,
            totalPaise = 240000L
        )
        val item = OrderItemEntity(
            id = "ITEM-1",
            orderId = "ORD-TEST-1",
            productId = "PRD-TEST-1",
            productName = "Test Cotton Kurti",
            sku = "SKU-TEST-1",
            quantity = 3.0,
            unitPricePaise = 80000L,
            costPriceSnapshotPaise = 40000L,
            gstRate = 5.0,
            taxAmountPaise = 12000L,
            totalPaise = 252000L
        )
        repository.createDraftOrder(order, listOf(item))

        // Before confirmation: Draft does not reserve stock
        var p = db.productDao().getProductById("PRD-TEST-1")!!
        assertEquals(10.0, p.physicalStock, 0.001)
        assertEquals(0.0, p.reservedStock, 0.001)
        assertEquals(10.0, p.availableStock, 0.001)

        // Confirm order for 3 units
        val result = repository.confirmOrder("ORD-TEST-1")
        assertTrue(result.isSuccess)

        // Expected state: physical stock 10, reserved stock 3, available stock 7
        p = db.productDao().getProductById("PRD-TEST-1")!!
        assertEquals(10.0, p.physicalStock, 0.001)
        assertEquals(3.0, p.reservedStock, 0.001)
        assertEquals(7.0, p.availableStock, 0.001)
    }

    @Test
    fun testC_dispatchDeductsPhysicalAndReleasesReservation() = runBlocking {
        // Setup confirmed order of 3 units with 10 physical stock
        val product = ProductEntity(
            id = "PRD-TEST-2",
            name = "Test Wireless Earbuds",
            sku = "SKU-TEST-2",
            category = "Electronics",
            costPricePaise = 70000L,
            sellingPricePaise = 140000L,
            physicalStock = 10.0,
            reservedStock = 0.0
        )
        repository.saveProduct(product)

        val order = OrderEntity(
            id = "ORD-TEST-2",
            orderNumber = "MS-INV-TEST2",
            customerId = "CUS-2",
            customerName = "Priya S",
            status = OrderStatus.DRAFT,
            totalPaise = 420000L
        )
        val item = OrderItemEntity(
            id = "ITEM-2",
            orderId = "ORD-TEST-2",
            productId = "PRD-TEST-2",
            productName = "Test Wireless Earbuds",
            sku = "SKU-TEST-2",
            quantity = 3.0,
            unitPricePaise = 140000L,
            costPriceSnapshotPaise = 70000L,
            gstRate = 18.0,
            taxAmountPaise = 75600L,
            totalPaise = 495600L
        )
        repository.createDraftOrder(order, listOf(item))
        repository.confirmOrder("ORD-TEST-2")

        // Dispatch those 3 units
        val dispatchResult = repository.dispatchOrder("ORD-TEST-2")
        assertTrue(dispatchResult.isSuccess)

        // Expected state: physical stock 7, reserved stock 0, available stock 7
        val p = db.productDao().getProductById("PRD-TEST-2")!!
        assertEquals(7.0, p.physicalStock, 0.001)
        assertEquals(0.0, p.reservedStock, 0.001)
        assertEquals(7.0, p.availableStock, 0.001)
    }

    @Test
    fun testD_purchaseOrderReceiptIncreasesPhysicalStock() = runBlocking {
        // Initial state: Product with 7 physical units
        val product = ProductEntity(
            id = "PRD-TEST-3",
            name = "Basmati Rice 5kg",
            sku = "SKU-TEST-3",
            category = "Grocery",
            costPricePaise = 40000L,
            sellingPricePaise = 60000L,
            physicalStock = 7.0,
            reservedStock = 0.0
        )
        repository.saveProduct(product)

        // Create PO for 5 units
        val po = PurchaseOrderEntity(
            id = "PO-TEST-1",
            poNumber = "MS-PO-TEST1",
            supplierId = "SUP-1",
            supplierName = "Punjab Grain Traders",
            status = PurchaseStatus.ORDERED,
            totalPaise = 200000L
        )
        val poItem = PurchaseItemEntity(
            id = "PITEM-TEST-1",
            purchaseOrderId = "PO-TEST-1",
            productId = "PRD-TEST-3",
            productName = "Basmati Rice 5kg",
            orderedQuantity = 5.0,
            receivedQuantity = 0.0,
            unitCostPaise = 40000L,
            totalPaise = 200000L
        )
        repository.createPurchaseOrder(po, listOf(poItem))

        // Confirm PO creation did NOT change physical stock
        var p = db.productDao().getProductById("PRD-TEST-3")!!
        assertEquals(7.0, p.physicalStock, 0.001)

        // Record receipt of all 5 units
        val receiptResult = repository.receiveGoods("PO-TEST-1", mapOf("PITEM-TEST-1" to 5.0))
        assertTrue(receiptResult.isSuccess)

        // Expected physical stock: 7 + 5 = 12 units
        p = db.productDao().getProductById("PRD-TEST-3")!!
        assertEquals(12.0, p.physicalStock, 0.001)
        assertEquals(0.0, p.reservedStock, 0.001)
        assertEquals(12.0, p.availableStock, 0.001)
    }

    @Test
    fun testE_cancellingConfirmedOrderReleasesReservation() = runBlocking {
        val product = ProductEntity(
            id = "PRD-TEST-4",
            name = "Serum Bottle",
            sku = "SKU-TEST-4",
            category = "Cosmetics",
            costPricePaise = 30000L,
            sellingPricePaise = 70000L,
            physicalStock = 10.0,
            reservedStock = 0.0
        )
        repository.saveProduct(product)

        val order = OrderEntity(
            id = "ORD-TEST-4",
            orderNumber = "MS-INV-TEST4",
            customerId = "CUS-4",
            customerName = "Anand K",
            status = OrderStatus.DRAFT,
            totalPaise = 140000L
        )
        val item = OrderItemEntity(
            id = "ITEM-4",
            orderId = "ORD-TEST-4",
            productId = "PRD-TEST-4",
            productName = "Serum Bottle",
            sku = "SKU-TEST-4",
            quantity = 4.0,
            unitPricePaise = 70000L,
            costPriceSnapshotPaise = 30000L,
            gstRate = 18.0,
            taxAmountPaise = 50400L,
            totalPaise = 330400L
        )
        repository.createDraftOrder(order, listOf(item))
        repository.confirmOrder("ORD-TEST-4")

        // Before cancellation: reserved 4
        var p = db.productDao().getProductById("PRD-TEST-4")!!
        assertEquals(4.0, p.reservedStock, 0.001)

        // Cancel order
        val cancelResult = repository.cancelOrder("ORD-TEST-4", "Buyer changed mind")
        assertTrue(cancelResult.isSuccess)

        // After cancellation: reserved returned to 0, available back to 10
        p = db.productDao().getProductById("PRD-TEST-4")!!
        assertEquals(10.0, p.physicalStock, 0.001)
        assertEquals(0.0, p.reservedStock, 0.001)
        assertEquals(10.0, p.availableStock, 0.001)
    }

    @Test
    fun testF_oversizedOrderIsRejectedWithoutStockChange() = runBlocking {
        val product = ProductEntity(
            id = "PRD-TEST-5",
            name = "Smart Watch",
            sku = "SKU-TEST-5",
            category = "Electronics",
            costPricePaise = 100000L,
            sellingPricePaise = 200000L,
            physicalStock = 3.0,
            reservedStock = 0.0
        )
        repository.saveProduct(product)

        // Attempt order for 5 units when only 3 are available
        val order = OrderEntity(
            id = "ORD-TEST-5",
            orderNumber = "MS-INV-TEST5",
            customerId = "CUS-5",
            customerName = "Sunita M",
            status = OrderStatus.DRAFT,
            totalPaise = 1000000L
        )
        val item = OrderItemEntity(
            id = "ITEM-5",
            orderId = "ORD-TEST-5",
            productId = "PRD-TEST-5",
            productName = "Smart Watch",
            sku = "SKU-TEST-5",
            quantity = 5.0,
            unitPricePaise = 200000L,
            costPriceSnapshotPaise = 100000L,
            gstRate = 18.0,
            taxAmountPaise = 180000L,
            totalPaise = 1180000L
        )
        repository.createDraftOrder(order, listOf(item))

        val confirmResult = repository.confirmOrder("ORD-TEST-5")
        assertTrue(confirmResult.isFailure)
        assertTrue(confirmResult.exceptionOrNull()?.message?.contains("Insufficient stock") == true)

        // Confirm stock was not modified
        val p = db.productDao().getProductById("PRD-TEST-5")!!
        assertEquals(3.0, p.physicalStock, 0.001)
        assertEquals(0.0, p.reservedStock, 0.001)
        assertEquals(3.0, p.availableStock, 0.001)
    }

    @Test
    fun testG_returnRestockIncreasesPhysicalStock() = runBlocking {
        // Setup product with 10 units
        val product = ProductEntity(
            id = "PRD-RET-1",
            name = "Linen Shirt",
            sku = "SKU-RET-1",
            category = "Apparel",
            costPricePaise = 50000L,
            sellingPricePaise = 100000L,
            physicalStock = 10.0,
            reservedStock = 0.0
        )
        repository.saveProduct(product)

        // Quick Sale of 2 units (dispatches immediately -> stock 8)
        val order = OrderEntity(
            id = "ORD-RET-1",
            orderNumber = "ORD-RET-1",
            customerId = "CUS-WALKIN",
            customerName = "Walk-in Buyer",
            status = OrderStatus.CONFIRMED,
            totalPaise = 200000L,
            paymentMethod = "Cash",
            paidPaise = 200000L
        )
        val orderItem = OrderItemEntity(
            id = "ITEM-RET-1",
            orderId = "ORD-RET-1",
            productId = "PRD-RET-1",
            productName = "Linen Shirt",
            sku = "SHIRT-L",
            quantity = 2.0,
            unitPricePaise = 100000L,
            costPriceSnapshotPaise = 50000L,
            gstRate = 5.0,
            taxAmountPaise = 5000L,
            totalPaise = 200000L
        )
        repository.createAndDispatchQuickSale(order, listOf(orderItem))

        var p = db.productDao().getProductById("PRD-RET-1")!!
        assertEquals(8.0, p.physicalStock, 0.001)

        // Customer returns 1 unit with restock = true
        val returnItem = ReturnItemEntity(
            id = "RET-ITEM-1",
            returnId = "",
            orderItemId = "ITEM-RET-1",
            productId = "PRD-RET-1",
            productName = "Linen Shirt",
            quantity = 1.0,
            refundUnitPricePaise = 100000L,
            restocked = true
        )
        val returnResult = repository.processReturn(
            orderId = "ORD-RET-1",
            refundAmountPaise = 100000L,
            adjustmentType = ReturnAdjustmentType.CASH_REFUND,
            reason = "Size mismatch",
            items = listOf(returnItem)
        )
        assertTrue(returnResult.isSuccess)

        // Physical stock should now be 8 + 1 = 9
        p = db.productDao().getProductById("PRD-RET-1")!!
        assertEquals(9.0, p.physicalStock, 0.001)

        // Order record should reflect returnedTotalPaise = 100000L
        val updatedOrder = db.orderDao().getOrderById("ORD-RET-1")!!
        assertEquals(100000L, updatedOrder.returnedTotalPaise)
    }

    @Test
    fun testH_excessRefundAndOverReturnAreRejected() = runBlocking {
        // A ₹1,000 dispatched line has already had ₹800 / 0.8 units returned.
        val product = ProductEntity(
            id = "PRD-2",
            name = "Sample",
            sku = "SKU-2",
            category = "Test",
            costPricePaise = 40000L,
            sellingPricePaise = 100000L,
            physicalStock = 9.8,
            reservedStock = 0.0
        )
        repository.saveProduct(product)
        val order = OrderEntity(
            id = "ORD-EXCESS-1",
            orderNumber = "ORD-EXCESS-1",
            customerId = "CUS-1",
            customerName = "Buyer",
            status = OrderStatus.DISPATCHED,
            totalPaise = 100000L,
            returnedTotalPaise = 80000L
        )
        db.orderDao().insertOrUpdate(order)
        db.orderDao().insertOrderItems(
            listOf(
                OrderItemEntity(
                    id = "ITEM-2",
                    orderId = "ORD-EXCESS-1",
                    productId = "PRD-2",
                    productName = "Sample",
                    sku = "SKU-2",
                    quantity = 1.0,
                    returnedQuantity = 0.8,
                    unitPricePaise = 100000L,
                    costPriceSnapshotPaise = 40000L,
                    gstRate = 0.0,
                    taxAmountPaise = 0L,
                    totalPaise = 100000L
                )
            )
        )

        // Only ₹200 remains eligible, so a ₹300 refund must fail.
        val excessRefund = repository.processReturn(
            orderId = "ORD-EXCESS-1",
            refundAmountPaise = 30000L,
            adjustmentType = ReturnAdjustmentType.CASH_REFUND,
            reason = "Excess attempt",
            items = listOf(
                ReturnItemEntity(
                    id = "RET-ITEM-2",
                    returnId = "",
                    orderItemId = "ITEM-2",
                    productId = "PRD-2",
                    productName = "Sample",
                    quantity = 0.2,
                    refundUnitPricePaise = 100000L,
                    restocked = false
                )
            )
        )
        assertTrue(excessRefund.isFailure)
        assertTrue(
            excessRefund.exceptionOrNull()?.message
                ?.contains("remaining eligible order balance") == true
        )

        // The remaining quantity is 0.2; trying to return 0.3 must also fail.
        val excessiveQuantity = repository.processReturn(
            orderId = "ORD-EXCESS-1",
            refundAmountPaise = 10000L,
            adjustmentType = ReturnAdjustmentType.CASH_REFUND,
            reason = "Duplicate return attempt",
            items = listOf(
                ReturnItemEntity(
                    id = "RET-ITEM-3",
                    returnId = "",
                    orderItemId = "ITEM-2",
                    productId = "PRD-2",
                    productName = "Sample",
                    quantity = 0.3,
                    refundUnitPricePaise = 100000L,
                    restocked = false
                )
            )
        )
        assertTrue(excessiveQuantity.isFailure)
        assertTrue(
            excessiveQuantity.exceptionOrNull()?.message
                ?.contains("Maximum returnable: 0.19999999999999996") == true ||
                excessiveQuantity.exceptionOrNull()?.message
                    ?.contains("Maximum returnable: 0.2") == true
        )
        assertEquals(80000L, db.orderDao().getOrderById("ORD-EXCESS-1")!!.returnedTotalPaise)
    }

    @Test
    fun testI_dailyCashClosingCalculatesVarianceCorrectly() = runBlocking {
        // Opening cash: ₹5,000
        val openingCash = 500000L
        // Actual counted in drawer: ₹4,800 (deficit of ₹200)
        val countedCash = 480000L

        val result = repository.performCashClosing(
            date = "2026-03-25",
            openingCashPaise = openingCash,
            actualCountedCashPaise = countedCash,
            closingStaffName = "Rajesh Verma",
            notes = "Shortage investigated",
            branchId = "BRANCH-MAIN"
        )
        assertTrue(result.isSuccess)
        val closing = result.getOrNull()!!

        // Expected variance = 480,000 - 500,000 = -20,000 paise (-₹200)
        assertEquals(-20000L, closing.variancePaise)
        assertFalse(closing.isReconciled) // non-zero variance flags for audit
    }

    @Test
    fun testJ_multiBranchTransferDeductsSourceAndIncrementsDestination() = runBlocking {
        // Transfers must reference real branches; the production code validates both IDs.
        db.branchDao().insertAll(
            listOf(
                BranchEntity(
                    id = "BRANCH-MAIN",
                    name = "Main Branch",
                    address = "Test Address",
                    city = "Patna",
                    phone = "",
                    isPrimary = true
                ),
                BranchEntity(
                    id = "BRANCH-IND",
                    name = "Indiranagar Branch",
                    address = "Test Address",
                    city = "Bengaluru",
                    phone = ""
                )
            )
        )

        // Product with 20 units
        val product = ProductEntity(
            id = "PRD-TRF-1",
            name = "Silk Saree",
            sku = "SKU-TRF-1",
            category = "Apparel",
            costPricePaise = 200000L,
            sellingPricePaise = 450000L,
            physicalStock = 20.0,
            reservedStock = 0.0
        )
        repository.saveProduct(product)

        // Initiate transfer of 5 units from Main Branch to Indiranagar Branch
        val initResult = repository.initiateTransfer(
            sourceBranchId = "BRANCH-MAIN",
            destinationBranchId = "BRANCH-IND",
            productId = "PRD-TRF-1",
            quantity = 5.0,
            staffName = "Rajesh",
            notes = "Inter-branch restock"
        )
        assertTrue(initResult.isSuccess)
        val transfer = initResult.getOrNull()!!

        // Source physical stock decreased by 5: 20 - 5 = 15
        var p = db.productDao().getProductById("PRD-TRF-1")!!
        assertEquals(15.0, p.physicalStock, 0.001)

        // Receive transfer at destination
        val receiveResult = repository.receiveTransfer(transfer.id, "Indiranagar Staff")
        assertTrue(receiveResult.isSuccess)

        // Stock is global in this prototype: receipt restores the shared balance but is not branch-isolated accounting.
        p = db.productDao().getProductById("PRD-TRF-1")!!
        assertEquals(20.0, p.physicalStock, 0.001)

        val updatedTransfer = db.stockTransferDao().getTransferById(transfer.id)!!
        assertEquals(TransferStatus.COMPLETED, updatedTransfer.status)
    }
}
