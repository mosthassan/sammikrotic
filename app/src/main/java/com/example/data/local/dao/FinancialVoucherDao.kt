package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FinancialVoucherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialVoucherDao {
    @Query("SELECT * FROM financial_vouchers ORDER BY dateMillis DESC")
    fun getAllVouchers(): Flow<List<FinancialVoucherEntity>>

    @Query("SELECT * FROM financial_vouchers WHERE voucherType = :type ORDER BY dateMillis DESC")
    fun getVouchersByType(type: String): Flow<List<FinancialVoucherEntity>>

    @Query("SELECT * FROM financial_vouchers WHERE retailerId = :retailerId ORDER BY dateMillis DESC")
    fun getVouchersForRetailer(retailerId: Long): Flow<List<FinancialVoucherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: FinancialVoucherEntity): Long

    @Update
    suspend fun updateVoucher(voucher: FinancialVoucherEntity)

    @Delete
    suspend fun deleteVoucher(voucher: FinancialVoucherEntity)

    @Query("SELECT SUM(amount) FROM financial_vouchers WHERE voucherType = 'RECEIPT'")
    fun getTotalReceipts(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_vouchers WHERE voucherType = 'PAYMENT'")
    fun getTotalPayments(): Flow<Double?>

    @Query("DELETE FROM financial_vouchers")
    suspend fun deleteAllVouchers()
}
