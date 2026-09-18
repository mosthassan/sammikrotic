package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CardDao
import com.example.data.local.dao.CardPackageDao
import com.example.data.local.dao.FinancialVoucherDao
import com.example.data.local.dao.InventoryDao
import com.example.data.local.dao.NetworkAssetDao
import com.example.data.local.dao.NetworkDeviceDao
import com.example.data.local.dao.NetworkIdentityDao
import com.example.data.local.dao.PartnerDao
import com.example.data.local.dao.RetailerDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        NetworkDeviceEntity::class,
        CardBatchEntity::class,
        CardEntity::class,
        CardPackageEntity::class,
        RetailerEntity::class,
        FinancialVoucherEntity::class,
        UserEntity::class,
        NetworkIdentityEntity::class,
        PartnerEntity::class,
        NetworkAssetEntity::class,
        PartnerTransactionEntity::class,
        InventoryItemEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkDeviceDao(): NetworkDeviceDao
    abstract fun cardDao(): CardDao
    abstract fun cardPackageDao(): CardPackageDao
    abstract fun retailerDao(): RetailerDao
    abstract fun financialVoucherDao(): FinancialVoucherDao
    abstract fun userDao(): UserDao
    abstract fun networkIdentityDao(): NetworkIdentityDao
    abstract fun partnerDao(): PartnerDao
    abstract fun networkAssetDao(): NetworkAssetDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sam_mikrotik_db"
                ).fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback : RoomDatabase.Callback() {
            private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch {
                    try {
                        INSTANCE?.let { database ->
                            InitialDataSeeder.seedDatabase(database)
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("AppDatabase", "Error seeding database on create: ${e.message}", e)
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                scope.launch {
                    try {
                        INSTANCE?.let { database ->
                            InitialDataSeeder.seedDatabase(database)
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("AppDatabase", "Error seeding database on migration: ${e.message}", e)
                    }
                }
            }
        }
    }
}
