package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_records")
data class SaleRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String,
    val customerName: String,
    val doctorPrescriptionNumber: String = "",
    val medicationId: Long,
    val medicationName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val discountPercent: Double = 0.0,
    val totalPaid: Double,
    val paymentMethod: String = "CASH", // CASH, CARD, DIGITAL_PAY
    val timestampEpoch: Long,
    val isRefillOrder: Boolean = false
)

@Entity(tableName = "inventory_logs")
data class InventoryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val changeAmount: Int, // e.g. +50 for restock, -2 for dispensed
    val finalStock: Int,
    val actionType: String, // RESTOCKED, DISPENSED, AUDIT_ADJUSTMENT, EXPIRED_REMOVED
    val notes: String = "",
    val timestampEpoch: Long
)
