package com.example.data

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET physicalStock = :physical, reservedStock = :reserved, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStock(id: String, physical: Double, reserved: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET lastSoldTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastSoldTime(id: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface ProductVariantDao {
    @Query("SELECT * FROM product_variants WHERE parentProductId = :parentId")
    fun getVariantsForProduct(parentId: String): Flow<List<ProductVariantEntity>>

    @Query("SELECT * FROM product_variants")
    fun getAllVariants(): Flow<List<ProductVariantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(variant: ProductVariantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(variants: List<ProductVariantEntity>)

    @Query("UPDATE product_variants SET physicalStock = :physical, reservedStock = :reserved WHERE id = :id")
    suspend fun updateVariantStock(id: String, physical: Double, reserved: Double)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovementEntity>)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrders(orders: List<OrderEntity>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItems(): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)
}

@Dao
interface ReturnDao {
    @Query("SELECT * FROM returns ORDER BY createdAt DESC")
    fun getAllReturns(): Flow<List<ReturnEntity>>

    @Query("SELECT * FROM returns WHERE orderId = :orderId")
    suspend fun getReturnsForOrder(orderId: String): List<ReturnEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnEntity: ReturnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<ReturnItemEntity>)

    @Query("SELECT * FROM return_items WHERE returnId = :returnId")
    suspend fun getItemsForReturn(returnId: String): List<ReturnItemEntity>
}

@Dao
interface CashRegisterDao {
    @Query("SELECT * FROM cash_register_closings ORDER BY closedAt DESC")
    fun getAllClosings(): Flow<List<CashRegisterClosingEntity>>

    @Query("SELECT * FROM cash_register_closings WHERE date = :date LIMIT 1")
    suspend fun getClosingForDate(date: String): CashRegisterClosingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosing(closing: CashRegisterClosingEntity)
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches ORDER BY isPrimary DESC, name ASC")
    fun getAllBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE id = :id")
    suspend fun getBranchById(id: String): BranchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(branches: List<BranchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(branch: BranchEntity)
}

@Dao
interface StockTransferDao {
    @Query("SELECT * FROM stock_transfers ORDER BY createdAt DESC")
    fun getAllTransfers(): Flow<List<StockTransferEntity>>

    @Query("SELECT * FROM stock_transfers WHERE id = :id")
    suspend fun getTransferById(id: String): StockTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(transfer: StockTransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transfers: List<StockTransferEntity>)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_orders ORDER BY createdAt DESC")
    fun getAllPurchaseOrders(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    suspend fun getPurchaseOrderById(id: String): PurchaseOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(po: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPOs(pos: List<PurchaseOrderEntity>)

    @Query("SELECT * FROM purchase_items WHERE purchaseOrderId = :poId")
    suspend fun getItemsForPO(poId: String): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Delete
    suspend fun delete(customer: CustomerEntity)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<SupplierEntity>)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM business_settings WHERE id = 'current_workspace'")
    fun getSettings(): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 'current_workspace'")
    suspend fun getSettingsSync(): BusinessSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: BusinessSettingsEntity)
}
