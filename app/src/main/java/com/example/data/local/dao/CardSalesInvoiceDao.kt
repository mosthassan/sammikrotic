package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CardSalesInvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardSalesInvoiceDao {
    @Query("SELECT * FROM card_sales_invoices ORDER BY invoiceDateMillis DESC")
    fun getAllSalesInvoices(): Flow<List<CardSalesInvoiceEntity>>

    @Query("SELECT * FROM card_sales_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): CardSalesInvoiceEntity?

    @Query("SELECT * FROM card_sales_invoices WHERE retailerId = :retailerId ORDER BY invoiceDateMillis DESC")
    fun getInvoicesForRetailer(retailerId: Long): Flow<List<CardSalesInvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: CardSalesInvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: CardSalesInvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: CardSalesInvoiceEntity)

    @Query("SELECT SUM(totalAmount) FROM card_sales_invoices")
    fun getTotalSalesAmount(): Flow<Double?>

    @Query("SELECT SUM(totalCardsCount) FROM card_sales_invoices")
    fun getTotalSoldCardsCount(): Flow<Int?>

    @Query("SELECT SUM(remainingAmount) FROM card_sales_invoices")
    fun getTotalCreditRemaining(): Flow<Double?>

    @Query("DELETE FROM card_sales_invoices")
    suspend fun deleteAllInvoices()

    @Query("DELETE FROM card_sales_invoices WHERE invoiceNumber LIKE 'INV-DELIV-%'")
    suspend fun deleteSyntheticInvoices()
}
