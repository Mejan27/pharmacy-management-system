package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refill_subscriptions")
data class RefillSubscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val patientPhone: String,
    val patientEmail: String = "",
    val medicationId: Long,
    val medicationName: String,
    val dosagePerDay: Int, // e.g. 2 pills daily
    val quantityDispensed: Int, // e.g. 60 units
    val lastDispensedDateEpoch: Long,
    val nextRefillDateEpoch: Long,
    val refillCycleDays: Int = 30, // 30, 60, 90 days
    val status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED
    val autoNotifyEnabled: Boolean = true,
    val notes: String = ""
) {
    fun daysUntilRefill(currentEpoch: Long): Int {
        val diff = nextRefillDateEpoch - currentEpoch
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    fun isDue(currentEpoch: Long): Boolean {
        // Due if within 3 days or already passed
        val diff = nextRefillDateEpoch - currentEpoch
        val days = diff / (1000 * 60 * 60 * 24)
        return days <= 3 && status == "ACTIVE"
    }

    fun isOverdue(currentEpoch: Long): Boolean {
        return currentEpoch > nextRefillDateEpoch && status == "ACTIVE"
    }
}
