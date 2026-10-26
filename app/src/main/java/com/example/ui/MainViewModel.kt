package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.GeminiAnswer
import com.example.ai.GeminiService
import com.example.data.BusinessRepository
import com.example.data.MerchSpringDatabase
import com.example.data.SampleData
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen(val title: String) {
    OVERVIEW("Overview"),
    POS_BILLING("Quick POS Billing"),
    INVENTORY("Inventory"),
    SALES("Sales & Orders"),
    CASH_REGISTER("Daily Cash Register"),
    STOCK_TRANSFERS("Stock Transfers & Reorder"),
    PURCHASES("Purchases"),
    CUSTOMERS("Customers"),
    SUPPLIERS("Suppliers"),
    EXPENSES("Expenses"),
    INVOICES("GST Invoices"),
    REPORTS("Reports & Analytics"),
    AI_ASSISTANT("AI Business Assistant"),
    WAREHOUSE_3D("3D Warehouse Twin"),
    NOTIFICATIONS("Notifications"),
    SETTINGS("Settings & Roles")
}

data class DashboardMetrics(
    val todaySalesPaise: Long = 0L,
    val periodSalesPaise: Long = 0L,
    val completedSalesCount: Int = 0,
    val pendingOrdersCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalValuationPaise: Long = 0L,
    val totalExpensesPaise: Long = 0L,
    val customerReceivablesPaise: Long = 0L,
    val supplierPayablesPaise: Long = 0L,
    val grossProfitPaise: Long = 0L,
    val grossProfitMarginPercent: Double = 0.0,
    val unreadNotificationsCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MerchSpringDatabase.getInstance(application)
    val repository = BusinessRepository(db)
    val geminiService = GeminiService()

    // Navigation State
    val currentScreen = MutableStateFlow(AppScreen.OVERVIEW)

    // Data streams
    val products: StateFlow<List<ProductEntity>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.orders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseOrders: StateFlow<List<PurchaseOrderEntity>> = repository.purchaseOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.suppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.expenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.stockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<BusinessSettingsEntity?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val branches: StateFlow<List<BranchEntity>> = repository.branches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockTransfers: StateFlow<List<StockTransferEntity>> = repository.stockTransfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val returns: StateFlow<List<ReturnEntity>> = repository.returns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashClosings: StateFlow<List<CashRegisterClosingEntity>> = repository.cashClosings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productVariants: StateFlow<List<ProductVariantEntity>> = repository.productVariants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reorderRecommendations: StateFlow<List<ReorderRecommendation>> = products
        .map { pList -> repository.computeReorderRecommendations(pList) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deadStockProducts: StateFlow<List<ProductEntity>> = combine(products, settings) { pList, s ->
        val thresholdDays = s?.deadStockDaysThreshold ?: 30
        pList.filter { it.isDeadStock(thresholdDays) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Assistant State
    val chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                content = "Namaste! I am your MerchSpring AI Business Assistant. How can I help you sync your business today? You can ask me to summarize today's sales, check low stock items, or draft a polite payment reminder in English, Hindi, or Hinglish."
            )
        )
    )
    val isAiThinking = MutableStateFlow(false)
    val aiErrorMessage = MutableStateFlow<String?>(null)
    val enableThinkingMode = MutableStateFlow(false)
    val enableSearchGrounding = MutableStateFlow(false)

    // User Feedback Snackbars
    val snackbarMessage = MutableStateFlow<String?>(null)

    // Computed Dashboard Metrics
    @Suppress("UNCHECKED_CAST")
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        products, orders, expenses, customers, suppliers, notifications
    ) { args: Array<Any> ->
        val pList = args[0] as List<ProductEntity>
        val oList = args[1] as List<OrderEntity>
        val eList = args[2] as List<ExpenseEntity>
        val cList = args[3] as List<CustomerEntity>
        val sList = args[4] as List<SupplierEntity>
        val nList = args[5] as List<NotificationEntity>
        computeMetrics(pList, oList, eList, cList, sList, nList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    init {
        viewModelScope.launch {
            repository.initializeIfEmpty()
        }
    }

    private suspend fun computeMetrics(
        pList: List<ProductEntity>,
        oList: List<OrderEntity>,
        eList: List<ExpenseEntity>,
        cList: List<CustomerEntity>,
        sList: List<SupplierEntity>,
        nList: List<NotificationEntity>
    ): DashboardMetrics {
        val completedOrders = oList.filter { it.status == OrderStatus.DISPATCHED }
        val pendingOrders = oList.filter { it.status == OrderStatus.CONFIRMED }

        val totalSalesPaise = completedOrders.sumOf { it.totalPaise }
        val completedCount = completedOrders.size
        val pendingCount = pendingOrders.size

        val lowStockCount = pList.count { it.isLowStock }
        val outOfStockCount = pList.count { it.isOutOfStock }

        // Valuation: physicalStock * costPrice
        val valuationPaise = pList.sumOf { (it.physicalStock * it.costPricePaise).toLong() }

        val totalExpensesPaise = eList.sumOf { it.amountPaise }
        val customerReceivables = cList.sumOf { it.outstandingBalancePaise }
        val supplierPayables = sList.sumOf { it.outstandingPayablePaise }

        // Reconciled Gross Profit: from fulfilled order items snapshots
        val allOrderItems = db.orderDao().getAllOrderItems()
        val dispatchedOrderIds = completedOrders.map { it.id }.toSet()
        val fulfilledItems = allOrderItems.filter { it.orderId in dispatchedOrderIds }

        val grossProfit = fulfilledItems.sumOf { item ->
            val revenue = (item.unitPricePaise * item.quantity).toLong()
            val cost = (item.costPriceSnapshotPaise * item.quantity).toLong()
            (revenue - cost)
        }

        val margin = if (totalSalesPaise > 0) {
            (grossProfit.toDouble() / totalSalesPaise) * 100.0
        } else 0.0

        val unread = nList.count { !it.isRead }

        return DashboardMetrics(
            todaySalesPaise = totalSalesPaise, // Today/Period sales
            periodSalesPaise = totalSalesPaise,
            completedSalesCount = completedCount,
            pendingOrdersCount = pendingCount,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            totalValuationPaise = valuationPaise,
            totalExpensesPaise = totalExpensesPaise,
            customerReceivablesPaise = customerReceivables,
            supplierPayablesPaise = supplierPayables,
            grossProfitPaise = grossProfit,
            grossProfitMarginPercent = margin,
            unreadNotificationsCount = unread
        )
    }

    // -------------------------------------------------------------
    // Business Operations Actions
    // -------------------------------------------------------------

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
            snackbarMessage.value = "Product '${product.name}' saved successfully."
        }
    }

    fun adjustStock(productId: String, newPhysical: Double, reason: String) {
        viewModelScope.launch {
            val result = repository.adjustStock(productId, newPhysical, reason)
            if (result.isSuccess) {
                snackbarMessage.value = "Stock adjusted successfully."
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to adjust stock."
            }
        }
    }

    fun createDraftOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        viewModelScope.launch {
            repository.createDraftOrder(order, items)
            snackbarMessage.value = "Draft Order ${order.orderNumber} created. Confirm when ready to reserve stock."
        }
    }

    fun confirmOrder(orderId: String) {
        viewModelScope.launch {
            val result = repository.confirmOrder(orderId)
            if (result.isSuccess) {
                snackbarMessage.value = "Order confirmed. Inventory has been reserved."
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to confirm order."
            }
        }
    }

    fun dispatchOrder(orderId: String) {
        viewModelScope.launch {
            val result = repository.dispatchOrder(orderId)
            if (result.isSuccess) {
                snackbarMessage.value = "Order dispatched! Physical inventory updated and reservation released."
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to dispatch order."
            }
        }
    }

    fun cancelOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            val result = repository.cancelOrder(orderId, reason)
            if (result.isSuccess) {
                snackbarMessage.value = "Order cancelled and any reserved stock released."
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to cancel order."
            }
        }
    }

    fun recordOrderPayment(orderId: String, amountPaise: Long, method: String) {
        viewModelScope.launch {
            repository.recordOrderPayment(orderId, amountPaise, method)
            snackbarMessage.value = "Payment of ${amountPaise.toRupeesString()} recorded."
        }
    }

    fun receiveGoods(poId: String, receivedMap: Map<String, Double>) {
        viewModelScope.launch {
            val result = repository.receiveGoods(poId, receivedMap)
            if (result.isSuccess) {
                snackbarMessage.value = "Goods receipt recorded. Physical stock increased."
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to receive goods."
            }
        }
    }

    fun recordExpense(category: String, amountPaise: Long, description: String, method: String) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = "EXP-${UUID.randomUUID().toString().take(8)}",
                category = category,
                amountPaise = amountPaise,
                date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                description = description,
                paymentMethod = method
            )
            repository.recordExpense(expense)
            snackbarMessage.value = "Expense of ${amountPaise.toRupeesString()} recorded."
        }
    }

    fun addCustomer(name: String, phone: String, email: String, address: String, gstin: String) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                id = "CUS-${UUID.randomUUID().toString().take(8)}",
                name = name,
                phone = phone,
                email = email,
                address = address,
                gstin = gstin
            )
            repository.saveCustomer(customer)
            snackbarMessage.value = "Customer '$name' added."
        }
    }

    fun addSupplier(name: String, contactPerson: String, phone: String, email: String, gstin: String, address: String) {
        viewModelScope.launch {
            val supplier = SupplierEntity(
                id = "SUP-${UUID.randomUUID().toString().take(8)}",
                name = name,
                contactPerson = contactPerson,
                phone = phone,
                email = email,
                gstin = gstin,
                address = address
            )
            repository.saveSupplier(supplier)
            snackbarMessage.value = "Supplier '$name' added."
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetDatabase()
            snackbarMessage.value = "Database reloaded with sample retail store data."
        }
    }

    fun updateRole(newRole: UserRole) {
        viewModelScope.launch {
            val current = settings.value ?: SampleData.initialSettings
            repository.saveSettings(current.copy(activeRole = newRole))
            snackbarMessage.value = "Switched active workspace role to ${newRole.name}."
        }
    }

    fun updateIndustryProfile(profile: IndustryProfile) {
        viewModelScope.launch {
            val current = settings.value ?: SampleData.initialSettings
            repository.saveSettings(current.copy(industryProfile = profile))
            snackbarMessage.value = "Active industry profile updated to ${profile.displayName}."
        }
    }

    fun updateActiveBranch(branchId: String) {
        viewModelScope.launch {
            val current = settings.value ?: SampleData.initialSettings
            repository.saveSettings(current.copy(activeBranchId = branchId))
            val bName = branches.value.firstOrNull { it.id == branchId }?.name ?: branchId
            snackbarMessage.value = "Switched active branch to $bName."
        }
    }

    fun dispatchQuickSale(order: OrderEntity, items: List<OrderItemEntity>) {
        viewModelScope.launch {
            val result = repository.createAndDispatchQuickSale(order, items)
            if (result.isSuccess) {
                snackbarMessage.value = "Bill #${order.orderNumber} generated successfully (${order.totalPaise.toRupeesString()})!"
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Quick sale failed."
            }
        }
    }

    fun processReturn(
        orderId: String,
        refundAmountPaise: Long,
        adjustmentType: ReturnAdjustmentType,
        reason: String,
        items: List<ReturnItemEntity>
    ) {
        viewModelScope.launch {
            val result = repository.processReturn(
                orderId = orderId,
                refundAmountPaise = refundAmountPaise,
                adjustmentType = adjustmentType,
                reason = reason,
                items = items
            )
            if (result.isSuccess) {
                val ret = result.getOrNull()
                snackbarMessage.value = "Return #${ret?.returnNumber} recorded! Refund: ${refundAmountPaise.toRupeesString()} (${adjustmentType.name})."
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Return processing failed."
            }
        }
    }

    fun performCashClosing(
        openingCashPaise: Long,
        actualCountedCashPaise: Long,
        closingStaff: String,
        notes: String,
        branchId: String = "BRANCH-MAIN"
    ) {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val result = repository.performCashClosing(
                date = today,
                openingCashPaise = openingCashPaise,
                actualCountedCashPaise = actualCountedCashPaise,
                closingStaffName = closingStaff,
                notes = notes,
                branchId = branchId
            )
            if (result.isSuccess) {
                val closing = result.getOrNull()
                val status = if ((closing?.variancePaise ?: 0L) >= 0) "Surplus" else "Shortage"
                val varStr = Math.abs(closing?.variancePaise ?: 0L).toRupeesString()
                snackbarMessage.value = "Cash register closed! Expected: ${closing?.expectedClosingCashPaise?.toRupeesString()} | Counted: ${closing?.actualCountedCashPaise?.toRupeesString()} (Variance: $status $varStr)"
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to close cash register."
            }
        }
    }

    fun initiateTransfer(
        sourceBranchId: String,
        destBranchId: String,
        productId: String,
        quantity: Double,
        staffName: String,
        notes: String
    ) {
        viewModelScope.launch {
            val result = repository.initiateTransfer(
                sourceBranchId = sourceBranchId,
                destinationBranchId = destBranchId,
                productId = productId,
                quantity = quantity,
                staffName = staffName,
                notes = notes
            )
            if (result.isSuccess) {
                snackbarMessage.value = "Stock transfer initiated! Dispatched from source branch."
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to initiate transfer."
            }
        }
    }

    fun receiveTransfer(transferId: String, staffName: String) {
        viewModelScope.launch {
            val result = repository.receiveTransfer(transferId, staffName)
            if (result.isSuccess) {
                snackbarMessage.value = "Stock transfer received! Inventory incremented at destination branch."
            } else {
                snackbarMessage.value = result.exceptionOrNull()?.message ?: "Failed to receive transfer."
            }
        }
    }

    /**
     * Confirms and applies a structured action proposed by the AI Assistant.
     * Guaranteed safe: Never triggered automatically without user tap.
     */
    fun executeActionProposal(proposal: StructuredActionProposal) {
        viewModelScope.launch {
            when (proposal.type) {
                ActionProposalType.ADD_STOCK -> {
                    val p = products.value.firstOrNull { it.id == proposal.targetEntityId }
                    if (p != null) {
                        val newQty = p.physicalStock + proposal.quantity
                        val result = repository.adjustStock(p.id, newQty, "AI Assistant Confirmed Restock")
                        snackbarMessage.value = if (result.isSuccess) {
                            "Action Applied: Added ${proposal.quantity} units to '${p.name}'. New physical stock: ${newQty}."
                        } else {
                            result.exceptionOrNull()?.message ?: "Could not update stock. No change was confirmed."
                        }
                    } else {
                        snackbarMessage.value = "Target product not found."
                    }
                }
                ActionProposalType.CONFIRM_ORDER -> {
                    val res = repository.confirmOrder(proposal.targetEntityId)
                    if (res.isSuccess) {
                        snackbarMessage.value = "Action Applied: Confirmed order ${proposal.targetEntityName} and reserved stock."
                    } else {
                        snackbarMessage.value = res.exceptionOrNull()?.message ?: "Failed to confirm order."
                    }
                }
                ActionProposalType.RECORD_EXPENSE -> {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    val expense = ExpenseEntity(
                        id = "EXP-${UUID.randomUUID().toString().take(8)}",
                        category = proposal.targetEntityId.ifBlank { "Misc" },
                        amountPaise = proposal.amountPaise,
                        date = today,
                        description = proposal.summary,
                        paymentMethod = "Cash"
                    )
                    repository.recordExpense(expense)
                    snackbarMessage.value = "Action Applied: Recorded expense of ${proposal.amountPaise.toRupeesString()} (${expense.category})."
                }
                ActionProposalType.CREATE_PO, ActionProposalType.REORDER_RESTOCK -> {
                    val p = products.value.firstOrNull { it.id == proposal.targetEntityId }
                    if (p != null) {
                        val availableSuppliers = suppliers.value
                        val preferred = p.preferredSupplier.trim()
                        val preferredMatches = if (preferred.isBlank()) {
                            emptyList()
                        } else {
                            availableSuppliers.filter {
                                it.id.equals(preferred, ignoreCase = true) ||
                                    it.name.equals(preferred, ignoreCase = true)
                            }
                        }
                        val sup = preferredMatches.singleOrNull()
                            ?: availableSuppliers.singleOrNull().takeIf { preferredMatches.isEmpty() }
                        if (sup == null) {
                            snackbarMessage.value = when {
                                availableSuppliers.isEmpty() ->
                                    "Add a supplier before creating a purchase-order draft."
                                preferredMatches.size > 1 ->
                                    "More than one supplier matches this product. Open Purchases and select the supplier manually."
                                else ->
                                    "Set this product's preferred supplier, or create the purchase order manually to choose the supplier."
                            }
                            return@launch
                        }
                        val poId = "PO-${UUID.randomUUID().toString().take(6).uppercase()}"
                        // This is a tax-exclusive estimate. The owner must review tax and pricing
                        // in the draft before it can be ordered.
                        val total = (p.costPricePaise * proposal.quantity).toLong()
                        val po = PurchaseOrderEntity(
                            id = poId,
                            poNumber = poId,
                            supplierId = sup.id,
                            supplierName = sup.name,
                            totalPaise = total,
                            status = PurchaseStatus.DRAFT
                        )
                        val item = PurchaseItemEntity(
                            id = UUID.randomUUID().toString(),
                            purchaseOrderId = poId,
                            productId = p.id,
                            productName = p.name,
                            orderedQuantity = proposal.quantity,
                            unitCostPaise = p.costPricePaise,
                            gstRate = p.gstRate,
                            totalPaise = total
                        )
                        repository.createPurchaseOrder(po, listOf(item))
                        snackbarMessage.value = "Draft PO ${po.poNumber} created. Review supplier, quantities, and tax before ordering."
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // AI Business Assistant
    // -------------------------------------------------------------

    fun sendAiPrompt(prompt: String) {
        if (prompt.isBlank()) return

        val userMsg = ChatMessage(
            role = "user",
            content = prompt
        )
        chatMessages.value = chatMessages.value + userMsg
        isAiThinking.value = true
        aiErrorMessage.value = null

        viewModelScope.launch {
            // First, parse if this is a structured action proposal (Hindi/Hinglish/English)
            val proposal = geminiService.parseActionProposal(
                prompt = prompt,
                products = products.value,
                orders = orders.value
            )

            // Build grounded real context from actual database state
            val metrics = dashboardMetrics.value
            val pList = products.value
            val lowStockNames = pList.filter { it.isLowStock }.joinToString { "${it.name} (${it.availableStock.toInt()} left)" }
            val outOfStockNames = pList.filter { it.isOutOfStock }.joinToString { it.name }
            val oList = orders.value
            val pendingOrdersSummary = oList.filter { it.status == OrderStatus.CONFIRMED }
                .joinToString { "${it.orderNumber} for ${it.customerName} (${it.totalPaise.toRupeesString()})" }
            val cList = customers.value
            val debtors = cList.filter { it.outstandingBalancePaise > 0 }
                .joinToString { "${it.name}: ${it.outstandingBalancePaise.toRupeesString()}" }

            val contextText = """
Total Realized Sales: ${metrics.todaySalesPaise.toRupeesString()} (${metrics.completedSalesCount} orders)
Inventory Valuation: ${metrics.totalValuationPaise.toRupeesString()}
Total Recorded Expenses: ${metrics.totalExpensesPaise.toRupeesString()}
Gross Profit: ${metrics.grossProfitPaise.toRupeesString()} (${String.format(java.util.Locale.US, "%.1f", metrics.grossProfitMarginPercent)}% margin)
Customer Receivables Dues: ${metrics.customerReceivablesPaise.toRupeesString()}
Debtors List: ${debtors.ifBlank { "None" }}
Supplier Payables: ${metrics.supplierPayablesPaise.toRupeesString()}
Low Stock Items: ${lowStockNames.ifBlank { "None" }}
Out of Stock Items: ${outOfStockNames.ifBlank { "None" }}
Pending Confirmed Orders: ${pendingOrdersSummary.ifBlank { "None" }}
            """.trimIndent()

            val result = geminiService.sendChat(
                history = chatMessages.value,
                userMessage = prompt,
                businessContext = contextText,
                enableThinking = enableThinkingMode.value,
                enableSearch = enableSearchGrounding.value
            )

            isAiThinking.value = false
            if (result.isSuccess) {
                val answer = result.getOrNull() ?: GeminiAnswer(text = "")
                val modelMsg = ChatMessage(
                    role = "model",
                    content = answer.text,
                    isThinking = enableThinkingMode.value,
                    isGrounded = enableSearchGrounding.value && answer.sources.isNotEmpty(),
                    proposal = proposal,
                    groundingSources = answer.sources,
                    searchEntryHtml = answer.searchEntryHtml
                )
                chatMessages.value = chatMessages.value + modelMsg
            } else {
                // When Firebase AI Logic is unavailable, provide only deterministic local summaries where possible.
                val fallbackReply = when {
                    proposal != null -> "I parsed your request into a proposed business action. Please review the details below and tap 'Confirm & Execute' to safely apply it to your database."
                    prompt.lowercase().contains("summary") || prompt.lowercase().contains("sales") || prompt.lowercase().contains("bikri") ->
                        "📊 **Daily Business Summary**\n• Realized Sales: ${metrics.todaySalesPaise.toRupeesString()} (${metrics.completedSalesCount} fulfilled orders)\n• Recorded Expenses: ${metrics.totalExpensesPaise.toRupeesString()}\n• Estimated Gross Profit: ${metrics.grossProfitPaise.toRupeesString()} (${String.format(java.util.Locale.US, "%.1f", metrics.grossProfitMarginPercent)}%)\n• Customer Balances Pending: ${metrics.customerReceivablesPaise.toRupeesString()}"
                    prompt.lowercase().contains("stock") || prompt.lowercase().contains("reorder") || prompt.lowercase().contains("low") ->
                        "⚠️ **Low Stock Alert**\n" + (if (lowStockNames.isNotBlank()) "The following items require reordering:\n• $lowStockNames" else "All inventory levels are above reorder thresholds!")
                    prompt.lowercase().contains("debt") || prompt.lowercase().contains("customer") || prompt.lowercase().contains("udhaar") ->
                        "💼 **Overdue Customer Balances**\n" + (if (debtors.isNotBlank()) "Pending dues:\n• $debtors" else "No outstanding customer balances.")
                    else -> "The AI service is currently unavailable. Configure Firebase AI Logic and App Check to enable AI responses. Your saved business records remain available in the app."
                }

                val modelMsg = ChatMessage(
                    role = "model",
                    content = fallbackReply,
                    isThinking = false,
                    isGrounded = false,
                    proposal = proposal
                )
                chatMessages.value = chatMessages.value + modelMsg
            }
        }
    }
}
