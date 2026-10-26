package com.example.data

import com.example.model.*

object SampleData {
    val initialSettings = BusinessSettingsEntity(
        id = "current_workspace",
        businessName = "MerchSpring Retail & Mart",
        tagline = "Your Business, in Sync.",
        ownerName = "Rajesh Verma",
        businessCategory = "Retail & Wholesale",
        industryProfile = IndustryProfile.CLOTHING_FASHION,
        activeBranchId = "BRANCH-MAIN",
        phone = "+91 98200 45678",
        email = "support@merchspring.in",
        address = "Plot 42, Commercial Zone, Indiranagar, Bengaluru - 560038",
        gstin = "29AAAAA0000A1Z5",
        pan = "AAAAA0000A",
        currencyCode = "INR",
        currencySymbol = "₹",
        invoicePrefix = "MS-INV-",
        poPrefix = "MS-PO-",
        deadStockDaysThreshold = 30,
        activeRole = UserRole.OWNER,
        isSetupComplete = true
    )

    val branches = listOf(
        BranchEntity(
            id = "BRANCH-MAIN",
            name = "Indiranagar Flagship Store",
            address = "Plot 42, 100ft Road, Indiranagar",
            city = "Bengaluru",
            phone = "+91 98200 45678",
            isPrimary = true
        ),
        BranchEntity(
            id = "BRANCH-GODOWN",
            name = "Central Mandi Godown",
            address = "Shed 12, APMC Mandi Complex, Yeshwanthpur",
            city = "Bengaluru",
            phone = "+91 98200 99881",
            isPrimary = false
        ),
        BranchEntity(
            id = "BRANCH-OUTLET2",
            name = "Commercial Street Outlet",
            address = "Shop 18, Commercial Street",
            city = "Bengaluru",
            phone = "+91 98200 33445",
            isPrimary = false
        )
    )

    val products = listOf(
        ProductEntity(
            id = "PRD-101",
            name = "Pure Cotton Anarkali Kurti (Size M/L)",
            sku = "APP-KT-001",
            barcode = "890123456001",
            description = "Handcrafted breathable Jaipur cotton kurti with zari embroidery.",
            category = "Apparel & Boutique",
            brand = "Virasat Jaipur",
            unit = "Pcs",
            costPricePaise = 45000L, // ₹450.00
            sellingPricePaise = 89900L, // ₹899.00
            physicalStock = 35.0,
            reservedStock = 4.0, // Available: 31
            minStockThreshold = 8.0,
            preferredSupplier = "Jaipur Textiles Hub",
            hsn = "6204",
            gstRate = 5.0,
            variantAttribute = "Sizes: M, L, XL",
            lastSoldTimestamp = System.currentTimeMillis() - 86400000L * 1
        ),
        ProductEntity(
            id = "PRD-102",
            name = "Wireless ENC Earbuds Pro",
            sku = "ELC-EB-002",
            barcode = "890123456002",
            description = "Bluetooth 5.3 earbuds with 40-hour playback and deep bass.",
            category = "Electronics",
            brand = "AuraSonic",
            unit = "Pcs",
            costPricePaise = 75000L, // ₹750.00
            sellingPricePaise = 149900L, // ₹1,499.00
            physicalStock = 18.0,
            reservedStock = 2.0, // Available: 16
            minStockThreshold = 5.0,
            preferredSupplier = "Apex Electronics Dist",
            hsn = "8518",
            gstRate = 18.0,
            serialNumber = "SN-AURA-2026-X8",
            warrantyMonths = 12,
            lastSoldTimestamp = System.currentTimeMillis() - 86400000L * 2
        ),
        ProductEntity(
            id = "PRD-103",
            name = "Organic Royal Basmati Rice (5 Kg)",
            sku = "GRO-BR-003",
            barcode = "890123456003",
            description = "Aged extra-long grain aromatic basmati rice for festive meals.",
            category = "Grocery & Food",
            brand = "Annapurna Naturals",
            unit = "Bag",
            costPricePaise = 42000L, // ₹420.00
            sellingPricePaise = 58000L, // ₹580.00
            physicalStock = 45.0,
            reservedStock = 0.0, // Available: 45
            minStockThreshold = 10.0,
            preferredSupplier = "Punjab Agro Grain Traders",
            hsn = "1006",
            gstRate = 0.0,
            batchNo = "BATCH-PUN-2026",
            expiryDate = "2027-04-30",
            lastSoldTimestamp = System.currentTimeMillis() - 86400000L * 1
        ),
        ProductEntity(
            id = "PRD-104",
            name = "Ayurvedic Kumkumadi Face Glow Serum (30ml)",
            sku = "COS-SR-004",
            barcode = "890123456004",
            description = "Traditional 26-herb saffron brightening elixir for skin glow.",
            category = "Cosmetics",
            brand = "Saundarya Veda",
            unit = "Bottle",
            costPricePaise = 32000L, // ₹320.00
            sellingPricePaise = 79900L, // ₹799.00
            physicalStock = 6.0,
            reservedStock = 0.0, // Available: 6 -> LOW STOCK!
            minStockThreshold = 10.0,
            preferredSupplier = "Vedic Herbs Corp",
            hsn = "3304",
            gstRate = 18.0,
            batchNo = "VEDA-B-89",
            expiryDate = "2027-09-15",
            lastSoldTimestamp = System.currentTimeMillis() - 86400000L * 3
        ),
        ProductEntity(
            id = "PRD-105",
            name = "Smart Fitness Watch Ultra (AMOLED)",
            sku = "ELC-SW-005",
            barcode = "890123456005",
            description = "1.96-inch HD display, SpO2 sensor, BT calling, 7-day battery.",
            category = "Electronics",
            brand = "AuraSonic",
            unit = "Pcs",
            costPricePaise = 120000L, // ₹1,200.00
            sellingPricePaise = 249900L, // ₹2,499.00
            physicalStock = 2.0,
            reservedStock = 2.0, // Available: 0 -> OUT OF STOCK!
            minStockThreshold = 5.0,
            preferredSupplier = "Apex Electronics Dist",
            hsn = "8517",
            gstRate = 18.0,
            serialNumber = "IMEI-867530901",
            warrantyMonths = 24,
            lastSoldTimestamp = System.currentTimeMillis() - 86400000L * 4
        ),
        ProductEntity(
            id = "PRD-106",
            name = "Single-Origin Robusta Coffee Beans (500g)",
            sku = "GRO-CF-006",
            barcode = "890123456006",
            description = "Dark roast aromatic plantation coffee beans from Coorg.",
            category = "Grocery & Food",
            brand = "Coorg Hill Blends",
            unit = "Pkt",
            costPricePaise = 28000L, // ₹280.00
            sellingPricePaise = 45000L, // ₹450.00
            physicalStock = 28.0,
            reservedStock = 0.0,
            minStockThreshold = 6.0,
            preferredSupplier = "Coorg Planters Direct",
            hsn = "0901",
            gstRate = 5.0,
            batchNo = "CRG-OCT-26",
            expiryDate = "2027-02-28",
            lastSoldTimestamp = System.currentTimeMillis() - 86400000L * 35 // > 30 days -> DEAD STOCK!
        ),
        ProductEntity(
            id = "PRD-107",
            name = "Raw Organic Honey (1 Kg)",
            sku = "GRO-HN-007",
            barcode = "890123456007",
            description = "Unpasteurized forest wildflower wild nectar honey.",
            category = "Grocery & Food",
            brand = "Annapurna Naturals",
            unit = "Jar",
            costPricePaise = 38000L, // ₹380.00
            sellingPricePaise = 59000L, // ₹590.00
            physicalStock = 14.0,
            reservedStock = 0.0,
            minStockThreshold = 5.0,
            preferredSupplier = "Punjab Agro Grain Traders",
            hsn = "0409",
            gstRate = 0.0,
            batchNo = "HNY-2026-F1",
            expiryDate = "2028-06-30",
            lastSoldTimestamp = System.currentTimeMillis() - 86400000L * 5
        )
    )

    val productVariants = listOf(
        ProductVariantEntity(
            id = "VAR-101-M",
            parentProductId = "PRD-101",
            variantName = "Size M (38) - Indigo",
            sku = "APP-KT-001-M",
            barcode = "890123456011",
            additionalPricePaise = 0L,
            physicalStock = 15.0,
            reservedStock = 2.0,
            size = "M",
            color = "Indigo"
        ),
        ProductVariantEntity(
            id = "VAR-101-L",
            parentProductId = "PRD-101",
            variantName = "Size L (40) - Indigo",
            sku = "APP-KT-001-L",
            barcode = "890123456012",
            additionalPricePaise = 0L,
            physicalStock = 12.0,
            reservedStock = 2.0,
            size = "L",
            color = "Indigo"
        ),
        ProductVariantEntity(
            id = "VAR-101-XL",
            parentProductId = "PRD-101",
            variantName = "Size XL (42) - Marigold",
            sku = "APP-KT-001-XL",
            barcode = "890123456013",
            additionalPricePaise = 5000L, // ₹50 premium
            physicalStock = 8.0,
            reservedStock = 0.0,
            size = "XL",
            color = "Marigold"
        )
    )

    val customers = listOf(
        CustomerEntity(
            id = "CUS-201",
            name = "Priya Sharma",
            phone = "+91 98450 11223",
            email = "priya.s@gmail.com",
            gstin = "29BZZPS1234A1Z1",
            address = "Flat 302, Green Glen Layout, Bellandur",
            city = "Bengaluru",
            state = "Karnataka",
            customerTier = "Retail",
            outstandingBalancePaise = 320000L, // ₹3,200.00
            totalPurchasesPaise = 1850000L // ₹18,500.00
        ),
        CustomerEntity(
            id = "CUS-202",
            name = "Anand Kumar",
            phone = "+91 97110 88990",
            email = "anand.kumar@retailbiz.in",
            gstin = "29CDEAK5678B1Z2",
            address = "Shop 5, Commercial Street",
            city = "Bengaluru",
            state = "Karnataka",
            customerTier = "Gold Wholesale",
            outstandingBalancePaise = 145000L, // ₹1,450.00
            totalPurchasesPaise = 4280000L // ₹42,800.00
        ),
        CustomerEntity(
            id = "CUS-203",
            name = "Sunita Menon",
            phone = "+91 94470 55443",
            email = "s.menon@gmail.com",
            address = "B-12, Palm Meadows, Whitefield",
            city = "Bengaluru",
            state = "Karnataka",
            customerTier = "Retail",
            outstandingBalancePaise = 0L,
            totalPurchasesPaise = 940000L
        )
    )

    val suppliers = listOf(
        SupplierEntity(
            id = "SUP-301",
            name = "Jaipur Textiles Hub",
            contactPerson = "Mohan Lal Rathore",
            phone = "+91 94140 22334",
            email = "sales@jaipurtextiles.com",
            gstin = "08AAACJ1234D1Z8",
            address = "Sanganer Road, RIICO Industrial Area",
            city = "Jaipur",
            leadTimeDays = 5,
            outstandingPayablePaise = 1850000L // ₹18,500.00
        ),
        SupplierEntity(
            id = "SUP-302",
            name = "Apex Electronics Dist",
            contactPerson = "Vikas Jain",
            phone = "+91 98210 66778",
            email = "vikas@apexelectronics.in",
            gstin = "27AABCA9876C1Z3",
            address = "Lamington Road, Grant Road East",
            city = "Mumbai",
            leadTimeDays = 3,
            outstandingPayablePaise = 3400000L // ₹34,000.00
        ),
        SupplierEntity(
            id = "SUP-303",
            name = "Punjab Agro Grain Traders",
            contactPerson = "Gurpreet Singh",
            phone = "+91 98150 44556",
            email = "agro.punjab@grainmandi.org",
            gstin = "03AAACP5555M1Z9",
            address = "New Grain Mandi, GT Road",
            city = "Amritsar",
            leadTimeDays = 6,
            outstandingPayablePaise = 0L
        )
    )

    val orders = listOf(
        OrderEntity(
            id = "ORD-1001",
            orderNumber = "MS-INV-1001",
            customerId = "CUS-201",
            customerName = "Priya Sharma",
            customerPhone = "+91 98450 11223",
            status = OrderStatus.DISPATCHED,
            subtotalPaise = 269700L,
            discountPaise = 10000L,
            taxPaise = 13485L,
            totalPaise = 273185L, // ₹2,731.85
            paidPaise = 273185L,
            paymentStatus = PaymentStatus.PAID,
            paymentMethod = "UPI",
            dueDate = "2026-10-25",
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            dispatchedAt = System.currentTimeMillis() - 86400000L * 2 + 3600000L
        ),
        OrderEntity(
            id = "ORD-1002",
            orderNumber = "MS-INV-1002",
            customerId = "CUS-202",
            customerName = "Anand Kumar",
            customerPhone = "+91 97110 88990",
            status = OrderStatus.CONFIRMED,
            subtotalPaise = 659600L,
            discountPaise = 25000L,
            taxPaise = 88600L,
            totalPaise = 723200L, // ₹7,232.00
            paidPaise = 400000L,
            paymentStatus = PaymentStatus.PARTIAL,
            paymentMethod = "Cash",
            dueDate = "2026-10-28",
            createdAt = System.currentTimeMillis() - 3600000L * 4
        ),
        OrderEntity(
            id = "ORD-1003",
            orderNumber = "MS-INV-1003",
            customerId = "CUS-203",
            customerName = "Sunita Menon",
            customerPhone = "+91 94470 55443",
            status = OrderStatus.DRAFT,
            subtotalPaise = 179800L,
            discountPaise = 0L,
            taxPaise = 8990L,
            totalPaise = 188790L, // ₹1,887.90
            paidPaise = 0L,
            paymentStatus = PaymentStatus.UNPAID,
            paymentMethod = "Cash",
            dueDate = "2026-10-30",
            createdAt = System.currentTimeMillis() - 3600000L * 1
        )
    )

    val orderItems = listOf(
        // For ORD-1001 (Dispatched)
        OrderItemEntity(
            id = "ITEM-001",
            orderId = "ORD-1001",
            productId = "PRD-101",
            productName = "Pure Cotton Anarkali Kurti (Size M/L)",
            sku = "APP-KT-001",
            quantity = 3.0,
            unitPricePaise = 89900L,
            costPriceSnapshotPaise = 45000L,
            gstRate = 5.0,
            taxAmountPaise = 13485L,
            totalPaise = 283185L
        ),
        // For ORD-1002 (Confirmed - reserves stock!)
        OrderItemEntity(
            id = "ITEM-002",
            orderId = "ORD-1002",
            productId = "PRD-102",
            productName = "Wireless ENC Earbuds Pro",
            sku = "ELC-EB-002",
            quantity = 2.0,
            unitPricePaise = 149900L,
            costPriceSnapshotPaise = 75000L,
            gstRate = 18.0,
            taxAmountPaise = 53964L,
            totalPaise = 353764L
        ),
        OrderItemEntity(
            id = "ITEM-003",
            orderId = "ORD-1002",
            productId = "PRD-101",
            productName = "Pure Cotton Anarkali Kurti (Size M/L)",
            sku = "APP-KT-001",
            quantity = 4.0,
            unitPricePaise = 89900L,
            costPriceSnapshotPaise = 45000L,
            gstRate = 5.0,
            taxAmountPaise = 17980L,
            totalPaise = 377580L
        ),
        OrderItemEntity(
            id = "ITEM-004",
            orderId = "ORD-1002",
            productId = "PRD-105",
            productName = "Smart Fitness Watch Ultra (AMOLED)",
            sku = "ELC-SW-005",
            quantity = 2.0,
            unitPricePaise = 249900L,
            costPriceSnapshotPaise = 120000L,
            gstRate = 18.0,
            taxAmountPaise = 89964L,
            totalPaise = 589764L
        )
    )

    val returns = listOf(
        ReturnEntity(
            id = "RET-301",
            returnNumber = "MS-RET-301",
            orderId = "ORD-1001",
            orderNumber = "MS-INV-1001",
            customerId = "CUS-201",
            customerName = "Priya Sharma",
            refundAmountPaise = 89900L, // ₹899
            adjustmentType = ReturnAdjustmentType.REFUND_UPI,
            reason = "Size exchange / alteration request",
            createdAt = System.currentTimeMillis() - 86400000L
        )
    )

    val returnItems = listOf(
        ReturnItemEntity(
            id = "RITEM-301-1",
            returnId = "RET-301",
            orderItemId = "ITEM-001",
            productId = "PRD-101",
            productName = "Pure Cotton Anarkali Kurti (Size M/L)",
            quantity = 1.0,
            refundUnitPricePaise = 89900L,
            restocked = true
        )
    )

    val stockTransfers = listOf(
        StockTransferEntity(
            id = "TRF-501",
            transferNumber = "TRF-2026-001",
            sourceBranchId = "BRANCH-GODOWN",
            sourceBranchName = "Central Mandi Godown",
            destinationBranchId = "BRANCH-MAIN",
            destinationBranchName = "Indiranagar Flagship Store",
            productId = "PRD-101",
            productName = "Pure Cotton Anarkali Kurti",
            sku = "APP-KT-001",
            quantity = 20.0,
            status = TransferStatus.IN_TRANSIT,
            notes = "Replenishing showroom weekend rack stock",
            createdAt = System.currentTimeMillis() - 3600000L * 2
        )
    )

    val cashRegisterClosings = listOf(
        CashRegisterClosingEntity(
            id = "CRC-001",
            date = "2026-10-24",
            openingCashPaise = 500000L, // ₹5,000 opening float
            recordedCashSalesPaise = 1845000L, // ₹18,450
            recordedNonCashSalesPaise = 3280000L, // ₹32,800
            cashRefundsPaise = 0L,
            cashExpensesPaise = 65000L, // ₹650 tea/snacks
            expectedCashPaise = 2280000L, // 5000 + 18450 - 650 = ₹22,800
            actualCountedCashPaise = 2280000L, // Exact match
            variancePaise = 0L,
            closingStaffName = "Rajesh Verma",
            notes = "Cash reconciled with register drawer drawer balance."
        )
    )

    val purchaseOrders = listOf(
        PurchaseOrderEntity(
            id = "PO-2001",
            poNumber = "MS-PO-2001",
            supplierId = "SUP-301",
            supplierName = "Jaipur Textiles Hub",
            status = PurchaseStatus.ORDERED,
            totalPaise = 2250000L, // ₹22,500.00
            paidPaise = 500000L,
            createdAt = System.currentTimeMillis() - 86400000L,
            expectedDate = "2026-10-31",
            notes = "Festive seasonal restock order of 50 kurtis."
        )
    )

    val purchaseItems = listOf(
        PurchaseItemEntity(
            id = "PITEM-001",
            purchaseOrderId = "PO-2001",
            productId = "PRD-101",
            productName = "Pure Cotton Anarkali Kurti (Size M/L)",
            orderedQuantity = 50.0,
            receivedQuantity = 0.0,
            unitCostPaise = 45000L,
            gstRate = 5.0,
            totalPaise = 2250000L
        )
    )

    val expenses = listOf(
        ExpenseEntity(
            id = "EXP-401",
            category = "Rent",
            amountPaise = 2500000L, // ₹25,000.00
            date = "2026-10-01",
            description = "Monthly showroom rent for Indiranagar premises",
            paymentMethod = "NetBanking"
        ),
        ExpenseEntity(
            id = "EXP-402",
            category = "Electricity",
            amountPaise = 425000L, // ₹4,250.00
            date = "2026-10-10",
            description = "BESCOM Commercial power bill",
            paymentMethod = "UPI"
        ),
        ExpenseEntity(
            id = "EXP-403",
            category = "Logistics",
            amountPaise = 185000L, // ₹1,850.00
            date = "2026-10-18",
            description = "Delhivery express client dispatches",
            paymentMethod = "Cash"
        ),
        ExpenseEntity(
            id = "EXP-404",
            category = "Tea & Refreshments",
            amountPaise = 65000L, // ₹650.00
            date = "2026-10-22",
            description = "Staff daily chai and snacks",
            paymentMethod = "Cash"
        )
    )

    val notifications = listOf(
        NotificationEntity(
            id = "NOTIF-001",
            title = "Stock Alert: Face Glow Serum",
            message = "Kumkumadi Serum is at 6 units (Threshold: 10). Reorder recommended from Vedic Herbs Corp.",
            type = NotificationType.LOW_STOCK,
            relatedId = "PRD-104"
        ),
        NotificationEntity(
            id = "NOTIF-002",
            title = "Out of Stock: Smart Watch Ultra",
            message = "All 2 physical units are reserved for confirmed Order MS-INV-1002. Available stock is 0.",
            type = NotificationType.OUT_OF_STOCK,
            relatedId = "PRD-105"
        ),
        NotificationEntity(
            id = "NOTIF-003",
            title = "Dead Stock Alert: Robusta Coffee",
            message = "28 bags have had no sales in 35 days. Review price promotion or bundle.",
            type = NotificationType.DEAD_STOCK,
            relatedId = "PRD-106"
        ),
        NotificationEntity(
            id = "NOTIF-004",
            title = "Pending Order: Anand Kumar",
            message = "Order MS-INV-1002 is confirmed awaiting dispatch. ₹3,232 balance due.",
            type = NotificationType.PENDING_ORDER,
            relatedId = "ORD-1002"
        )
    )

    val stockMovements = listOf(
        StockMovementEntity(
            id = "MOV-001",
            productId = "PRD-101",
            productName = "Pure Cotton Anarkali Kurti (Size M/L)",
            timestamp = System.currentTimeMillis() - 86400000L * 2,
            movementType = MovementType.OUTWARD_DISPATCH,
            deltaQuantity = -3.0,
            physicalBefore = 38.0,
            physicalAfter = 35.0,
            reservedBefore = 3.0,
            reservedAfter = 0.0,
            referenceDoc = "MS-INV-1001",
            reason = "Dispatched order to Priya Sharma"
        ),
        StockMovementEntity(
            id = "MOV-002",
            productId = "PRD-101",
            productName = "Pure Cotton Anarkali Kurti (Size M/L)",
            timestamp = System.currentTimeMillis() - 3600000L * 4,
            movementType = MovementType.ORDER_RESERVE,
            deltaQuantity = 0.0,
            physicalBefore = 35.0,
            physicalAfter = 35.0,
            reservedBefore = 0.0,
            reservedAfter = 4.0,
            referenceDoc = "MS-INV-1002",
            reason = "Stock reserved upon confirming Order MS-INV-1002"
        )
    )
}
