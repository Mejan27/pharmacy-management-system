package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val genericName: String,
    val sku: String,
    val batchNumber: String,
    val category: String, // Antibiotics, Analgesics, Cardiovascular, Antidiabetic, Respiratory, Gastrointestinal, Vitamins, Dermatology
    val currentStock: Int,
    val minThreshold: Int, // Low stock alert threshold
    val unit: String, // Tablets, Capsules, Bottles, Vials, Blisters
    val unitCost: Double,
    val sellingPrice: Double,
    val expiryDateEpoch: Long, // Timestamp ms
    val manufacturer: String,
    val storageCondition: String, // e.g. "Room Temp (15-25°C)", "Cold Storage (2-8°C)"
    val dosageInstructions: String,
    val requiresPrescription: Boolean = true
) {
    val isLowStock: Boolean
        get() = currentStock <= minThreshold

    val isOutOfStock: Boolean
        get() = currentStock <= 0

    fun isExpiringSoon(nowEpoch: Long, daysThreshold: Int = 60): Boolean {
        val daysInMillis = daysThreshold.toLong() * 24 * 60 * 60 * 1000
        return (expiryDateEpoch - nowEpoch) in 0..daysInMillis
    }

    fun isExpired(nowEpoch: Long): Boolean {
        return expiryDateEpoch < nowEpoch
    }
}
