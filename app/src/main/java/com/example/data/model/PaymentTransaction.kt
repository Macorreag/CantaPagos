package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val wallet: String, // "Nequi", "Daviplata", "Bancolombia", "SMS Bancario"
    val amount: Long, // COP amount e.g. 15000
    val formattedAmount: String, // "$ 15.000"
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String? = null,
    val rawNotificationText: String = "",
    val shiftId: Long = 0L // Shift cutoff timestamp
)
