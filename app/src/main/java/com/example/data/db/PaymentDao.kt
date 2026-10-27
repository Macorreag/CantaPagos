package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PaymentTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payment_transactions WHERE timestamp >= :shiftCutoff ORDER BY timestamp DESC")
    fun getTransactionsForShift(shiftCutoff: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions")
    suspend fun getAllTransactionsSnapshot(): List<PaymentTransaction>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payment_transactions WHERE timestamp >= :shiftCutoff")
    fun getTotalForShift(shiftCutoff: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM payment_transactions WHERE timestamp >= :shiftCutoff")
    fun getCountForShift(shiftCutoff: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PaymentTransaction): Long

    @Query("DELETE FROM payment_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM payment_transactions WHERE amount < 100 OR LOWER(rawNotificationText) LIKE '%uala%' OR LOWER(rawNotificationText) LIKE '%ualá%' OR LOWER(rawNotificationText) LIKE '%interés%' OR LOWER(rawNotificationText) LIKE '%interes%'")
    suspend fun purgeInvalidTransactions()

    @Query("DELETE FROM payment_transactions")
    suspend fun clearAll()
}
