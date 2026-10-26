package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.*

@Database(
    entities = [
        ProductEntity::class,
        ProductVariantEntity::class,
        StockMovementEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ReturnEntity::class,
        ReturnItemEntity::class,
        PurchaseOrderEntity::class,
        PurchaseItemEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        ExpenseEntity::class,
        CashRegisterClosingEntity::class,
        BranchEntity::class,
        StockTransferEntity::class,
        NotificationEntity::class,
        BusinessSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MerchSpringDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productVariantDao(): ProductVariantDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun orderDao(): OrderDao
    abstract fun returnDao(): ReturnDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashRegisterDao(): CashRegisterDao
    abstract fun branchDao(): BranchDao
    abstract fun stockTransferDao(): StockTransferDao
    abstract fun notificationDao(): NotificationDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MerchSpringDatabase? = null

        fun getInstance(context: Context): MerchSpringDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MerchSpringDatabase::class.java,
                    "merchspring_business.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
