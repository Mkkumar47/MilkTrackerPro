package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sellers")
data class Seller(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val milkType: String = "Both", // "Cow Milk", "Buffalo Milk", "Both"
    val cowRate: Double = 0.0,
    val buffaloRate: Double = 0.0
)
