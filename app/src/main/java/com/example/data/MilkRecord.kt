package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milk_records")
data class MilkRecord(
    @PrimaryKey val date: String, // format: "yyyy-MM-dd"
    val taken: Boolean,
    val quantity: Double, // in Litres
    val rate: Double, // cost per Litre
    val notes: String = ""
) {
    val totalExpense: Double
        get() = if (taken) quantity * rate else 0.0
}
