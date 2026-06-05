package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format "yyyy-MM-dd"
    val sellerId: Int,
    val sellerName: String,
    val amount: Double,
    val paymentMode: String, // "Cash", "UPI", "Bank", "Other"
    val notes: String = "",
    val session: String = "All Sessions", // "All Sessions", "Morning", "Evening"
    val milkType: String = "All Milk Types", // "All Milk Types", "Cow Milk", "Buffalo Milk"
    val timestamp: Long = System.currentTimeMillis()
)
