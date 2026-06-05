package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milk_config")
data class MilkConfig(
    @PrimaryKey val id: Int = 1,
    val defaultQuantity: Double = 1.0,
    val defaultRate: Double = 40.0,
    val paymentReminderEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val themePreference: String = "SYSTEM",             // "SYSTEM", "LIGHT", "DARK"
    val paymentReminderDay: Int = 1,                  // Preferred payment day of the month (1 to 28)
    val paymentReminderDaysBefore: Int = 1,          // Reminder interval (e.g., 1 day before, 2 days before, etc.)
    val dailyReminderHour: Int = 20,                  // Custom daily reminder hour (0 to 23)
    val dailyReminderMinute: Int = 0,                 // Custom daily reminder minute (0 to 59)
    val currencyCode: String = "USD",                 // Selected Currency Code (e.g. "INR", "USD", "EUR")
    val currencySymbol: String = "$",                  // Selected Currency Symbol (e.g. "₹", "$", "€")
    val googleUserName: String? = null,
    val googleEmail: String? = null,
    val googlePhotoUrl: String? = null,
    val isGoogleSignedIn: Boolean = false
)
