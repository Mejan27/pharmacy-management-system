package com.example.data.repository

import com.example.data.local.InventoryLogDao
import com.example.data.local.MedicationDao
import com.example.data.local.RefillDao
import com.example.data.local.SaleDao
import com.example.data.model.InventoryLog
import com.example.data.model.Medication
import com.example.data.model.RefillSubscription
import com.example.data.model.SaleRecord
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PharmacyRepository(
    private val medicationDao: MedicationDao,
    private val refillDao: RefillDao,
    private val saleDao: SaleDao,
    private val inventoryLogDao: InventoryLogDao
) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
    val lowStockMedications: Flow<List<Medication>> = medicationDao.getLowStockMedications()
    val allRefills: Flow<List<RefillSubscription>> = refillDao.getAllRefills()
    val activeRefills: Flow<List<RefillSubscription>> = refillDao.getActiveRefills()
    val allSales: Flow<List<SaleRecord>> = saleDao.getAllSales()
    val recentLogs: Flow<List<InventoryLog>> = inventoryLogDao.getRecentLogs()

    suspend fun insertMedication(medication: Medication): Long {
        val id = medicationDao.insertMedication(medication)
        inventoryLogDao.insertLog(
            InventoryLog(
                medicationId = id,
                medicationName = medication.name,
                changeAmount = medication.currentStock,
                finalStock = medication.currentStock,
                actionType = "INITIAL_STOCK",
                notes = "Added new medication catalog item",
                timestampEpoch = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun updateMedication(medication: Medication) {
        medicationDao.updateMedication(medication)
    }

    suspend fun deleteMedication(medication: Medication) {
        medicationDao.deleteMedication(medication)
    }

    suspend fun adjustStock(medicationId: Long, change: Int, actionType: String, notes: String = "") {
        val med = medicationDao.getMedicationById(medicationId) ?: return
        val newStock = (med.currentStock + change).coerceAtLeast(0)
        medicationDao.updateStock(medicationId, newStock)
        inventoryLogDao.insertLog(
            InventoryLog(
                medicationId = medicationId,
                medicationName = med.name,
                changeAmount = change,
                finalStock = newStock,
                actionType = actionType,
                notes = notes,
                timestampEpoch = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordSale(
        customerName: String,
        doctorPrescriptionNumber: String,
        medicationId: Long,
        quantity: Int,
        discountPercent: Double = 0.0,
        paymentMethod: String = "CASH",
        isRefillOrder: Boolean = false
    ): Result<SaleRecord> {
        val med = medicationDao.getMedicationById(medicationId)
            ?: return Result.failure(Exception("Medication not found"))

        if (med.currentStock < quantity) {
            return Result.failure(Exception("Insufficient stock! Available: ${med.currentStock} ${med.unit}"))
        }

        val subtotal = med.sellingPrice * quantity
        val totalPaid = subtotal * (1.0 - (discountPercent / 100.0))
        val receiptNumber = "DIL-" + SimpleDateFormat("yyMMdd", Locale.US).format(Date()) + "-" +
                (1000..9999).random()

        val sale = SaleRecord(
            receiptNumber = receiptNumber,
            customerName = customerName.ifBlank { "Walk-in Customer" },
            doctorPrescriptionNumber = doctorPrescriptionNumber,
            medicationId = medicationId,
            medicationName = med.name,
            quantity = quantity,
            unitPrice = med.sellingPrice,
            subtotal = subtotal,
            discountPercent = discountPercent,
            totalPaid = totalPaid,
            paymentMethod = paymentMethod,
            timestampEpoch = System.currentTimeMillis(),
            isRefillOrder = isRefillOrder
        )

        saleDao.insertSale(sale)
        adjustStock(
            medicationId = medicationId,
            change = -quantity,
            actionType = if (isRefillOrder) "REFILL_DISPENSED" else "DISPENSED",
            notes = "Receipt #$receiptNumber for $customerName"
        )

        return Result.success(sale)
    }

    suspend fun insertRefill(refill: RefillSubscription): Long {
        return refillDao.insertRefill(refill)
    }

    suspend fun updateRefill(refill: RefillSubscription) {
        refillDao.updateRefill(refill)
    }

    suspend fun deleteRefill(refill: RefillSubscription) {
        refillDao.deleteRefill(refill)
    }

    suspend fun processRefillFulfillment(refillId: Long): Result<SaleRecord> {
        val refill = refillDao.getRefillById(refillId)
            ?: return Result.failure(Exception("Refill record not found"))

        val saleResult = recordSale(
            customerName = refill.patientName,
            doctorPrescriptionNumber = "REFILL-${refill.id}",
            medicationId = refill.medicationId,
            quantity = refill.quantityDispensed,
            discountPercent = 5.0, // 5% chronic refill courtesy discount
            paymentMethod = "CASH",
            isRefillOrder = true
        )

        if (saleResult.isSuccess) {
            val now = System.currentTimeMillis()
            val nextRefillEpoch = now + (refill.refillCycleDays.toLong() * 24 * 60 * 60 * 1000)
            val updated = refill.copy(
                lastDispensedDateEpoch = now,
                nextRefillDateEpoch = nextRefillEpoch,
                status = "ACTIVE"
            )
            refillDao.updateRefill(updated)
        }

        return saleResult
    }

    suspend fun postponeRefill(refillId: Long, days: Int = 3) {
        val refill = refillDao.getRefillById(refillId) ?: return
        val postponement = days.toLong() * 24 * 60 * 60 * 1000
        val updated = refill.copy(
            nextRefillDateEpoch = refill.nextRefillDateEpoch + postponement
        )
        refillDao.updateRefill(updated)
    }

    suspend fun seedInitialDataIfNeeded() {
        if (medicationDao.getCount() > 0) return

        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        val initialMeds = listOf(
            Medication(
                name = "Amoxicillin 500mg",
                genericName = "Amoxicillin Trihydrate",
                sku = "MED-AMX-500",
                batchNumber = "MDI-BX-902",
                category = "Antibiotics",
                currentStock = 120,
                minThreshold = 30,
                unit = "Capsules",
                unitCost = 0.40,
                sellingPrice = 0.95,
                expiryDateEpoch = now + (400 * oneDay),
                manufacturer = "Drugs International Ltd",
                storageCondition = "Room Temp (15-25°C)",
                dosageInstructions = "Take 1 capsule every 8 hours with food",
                requiresPrescription = true
            ),
            Medication(
                name = "Metformin HCl 850mg",
                genericName = "Metformin Hydrochloride",
                sku = "MED-MET-850",
                batchNumber = "MDI-BX-311",
                category = "Antidiabetic",
                currentStock = 18, // LOW STOCK
                minThreshold = 25,
                unit = "Tablets",
                unitCost = 0.20,
                sellingPrice = 0.60,
                expiryDateEpoch = now + (320 * oneDay),
                manufacturer = "Novartis Pharma",
                storageCondition = "Dry Place (< 25°C)",
                dosageInstructions = "Take 1 tablet twice daily with breakfast & dinner",
                requiresPrescription = true
            ),
            Medication(
                name = "Atorvastatin 20mg",
                genericName = "Atorvastatin Calcium",
                sku = "MED-ATV-020",
                batchNumber = "MDI-BX-844",
                category = "Cardiovascular",
                currentStock = 85,
                minThreshold = 20,
                unit = "Tablets",
                unitCost = 0.65,
                sellingPrice = 1.45,
                expiryDateEpoch = now + (280 * oneDay),
                manufacturer = "Pfizer Inc.",
                storageCondition = "Room Temp (15-25°C)",
                dosageInstructions = "Take 1 tablet at bedtime daily",
                requiresPrescription = true
            ),
            Medication(
                name = "Azithromycin 500mg",
                genericName = "Azithromycin Monohydrate",
                sku = "MED-AZI-500",
                batchNumber = "MDI-BX-109",
                category = "Antibiotics",
                currentStock = 8, // CRITICAL LOW STOCK
                minThreshold = 15,
                unit = "Tablets",
                unitCost = 1.10,
                sellingPrice = 2.50,
                expiryDateEpoch = now + (45 * oneDay), // EXPIRING SOON
                manufacturer = "Sanofi Health",
                storageCondition = "Dry & Cool (< 30°C)",
                dosageInstructions = "Take 1 tablet once daily 1 hour before meals",
                requiresPrescription = true
            ),
            Medication(
                name = "Omeprazole 20mg",
                genericName = "Omeprazole Delayed-Release",
                sku = "MED-OMP-020",
                batchNumber = "MDI-BX-772",
                category = "Gastrointestinal",
                currentStock = 140,
                minThreshold = 30,
                unit = "Capsules",
                unitCost = 0.30,
                sellingPrice = 0.80,
                expiryDateEpoch = now + (500 * oneDay),
                manufacturer = "AstraZeneca",
                storageCondition = "Room Temp (15-25°C)",
                dosageInstructions = "Take 1 capsule 30 minutes before breakfast",
                requiresPrescription = false
            ),
            Medication(
                name = "Salbutamol Inhaler 100mcg",
                genericName = "Albuterol Sulfate CFC-Free",
                sku = "MED-SLB-100",
                batchNumber = "MDI-BX-519",
                category = "Respiratory",
                currentStock = 14, // LOW STOCK
                minThreshold = 15,
                unit = "Inhaler Units",
                unitCost = 4.20,
                sellingPrice = 8.75,
                expiryDateEpoch = now + (360 * oneDay),
                manufacturer = "GlaxoSmithKline",
                storageCondition = "Protect from direct sunlight & heat",
                dosageInstructions = "1-2 puffs as needed every 4-6 hours for bronchospasm",
                requiresPrescription = true
            ),
            Medication(
                name = "Losartan Potassium 50mg",
                genericName = "Losartan Potassium",
                sku = "MED-LOS-050",
                batchNumber = "MDI-BX-633",
                category = "Cardiovascular",
                currentStock = 92,
                minThreshold = 25,
                unit = "Tablets",
                unitCost = 0.35,
                sellingPrice = 0.85,
                expiryDateEpoch = now + (420 * oneDay),
                manufacturer = "Merck Sharp & Dohme",
                storageCondition = "Room Temp (15-30°C)",
                dosageInstructions = "Take 1 tablet every morning with water",
                requiresPrescription = true
            ),
            Medication(
                name = "Paracetamol Extra 500/65mg",
                genericName = "Acetaminophen & Caffeine",
                sku = "MED-PCM-500",
                batchNumber = "MDI-BX-412",
                category = "Analgesics",
                currentStock = 310,
                minThreshold = 50,
                unit = "Tablets",
                unitCost = 0.12,
                sellingPrice = 0.35,
                expiryDateEpoch = now + (600 * oneDay),
                manufacturer = "Drugs International Ltd",
                storageCondition = "Room Temp (15-25°C)",
                dosageInstructions = "1-2 tablets every 6 hours as needed for pain/fever",
                requiresPrescription = false
            ),
            Medication(
                name = "Insulin Glargine 100U/ml",
                genericName = "Recombinant Human Insulin Glargine",
                sku = "MED-INS-100",
                batchNumber = "MDI-BX-220",
                category = "Antidiabetic",
                currentStock = 6, // LOW STOCK
                minThreshold = 10,
                unit = "Vials",
                unitCost = 18.00,
                sellingPrice = 32.50,
                expiryDateEpoch = now + (200 * oneDay),
                manufacturer = "Sanofi Aventis",
                storageCondition = "Cold Storage (2-8°C) Do NOT freeze",
                dosageInstructions = "Subcutaneous injection once daily at bedtime as prescribed",
                requiresPrescription = true
            ),
            Medication(
                name = "Cetirizine 10mg",
                genericName = "Cetirizine Dihydrochloride",
                sku = "MED-CTZ-010",
                batchNumber = "MDI-BX-904",
                category = "Respiratory",
                currentStock = 160,
                minThreshold = 30,
                unit = "Tablets",
                unitCost = 0.15,
                sellingPrice = 0.45,
                expiryDateEpoch = now + (550 * oneDay),
                manufacturer = "Drugs International Ltd",
                storageCondition = "Room Temp (15-25°C)",
                dosageInstructions = "Take 1 tablet daily in the evening",
                requiresPrescription = false
            ),
            Medication(
                name = "Hydrocortisone 1% Cream",
                genericName = "Hydrocortisone Topical",
                sku = "MED-HYD-001",
                batchNumber = "MDI-BX-815",
                category = "Dermatology",
                currentStock = 24,
                minThreshold = 15,
                unit = "Tubes",
                unitCost = 2.10,
                sellingPrice = 4.80,
                expiryDateEpoch = now + (300 * oneDay),
                manufacturer = "Bayer Healthcare",
                storageCondition = "Store below 25°C",
                dosageInstructions = "Apply sparingly to affected skin area 2-3 times daily",
                requiresPrescription = false
            ),
            Medication(
                name = "Vitamin D3 50,000 IU",
                genericName = "Cholecalciferol High Potency",
                sku = "MED-VTD-050",
                batchNumber = "MDI-BX-104",
                category = "Vitamins",
                currentStock = 75,
                minThreshold = 20,
                unit = "Capsules",
                unitCost = 0.90,
                sellingPrice = 2.20,
                expiryDateEpoch = now + (450 * oneDay),
                manufacturer = "Drugs International Ltd",
                storageCondition = "Room Temp (15-25°C)",
                dosageInstructions = "Take 1 capsule once weekly with high-fat meal",
                requiresPrescription = true
            )
        )

        medicationDao.insertAll(initialMeds)

        // Seed initial refill subscriptions
        val initialRefills = listOf(
            RefillSubscription(
                patientName = "Haji Abdul Karim",
                patientPhone = "+880 1712-345678",
                patientEmail = "abdul.karim@email.com",
                medicationId = 2,
                medicationName = "Metformin HCl 850mg",
                dosagePerDay = 2,
                quantityDispensed = 60,
                lastDispensedDateEpoch = now - (29 * oneDay),
                nextRefillDateEpoch = now + (1 * oneDay), // DUE TOMORROW
                refillCycleDays = 30,
                status = "ACTIVE",
                autoNotifyEnabled = true,
                notes = "Type 2 Diabetes ongoing care. Patient requested morning reminders."
            ),
            RefillSubscription(
                patientName = "Mrs. Sarah Jenkins",
                patientPhone = "+1 (555) 482-9901",
                patientEmail = "sarah.j@email.com",
                medicationId = 3,
                medicationName = "Atorvastatin 20mg",
                dosagePerDay = 1,
                quantityDispensed = 30,
                lastDispensedDateEpoch = now - (31 * oneDay),
                nextRefillDateEpoch = now - (1 * oneDay), // OVERDUE BY 1 DAY!
                refillCycleDays = 30,
                status = "ACTIVE",
                autoNotifyEnabled = true,
                notes = "Lipid management. Urgent refill reminder needed."
            ),
            RefillSubscription(
                patientName = "Mohammed Rafiq",
                patientPhone = "+880 1819-876543",
                patientEmail = "rafiq.m@email.com",
                medicationId = 7,
                medicationName = "Losartan Potassium 50mg",
                dosagePerDay = 1,
                quantityDispensed = 30,
                lastDispensedDateEpoch = now - (27 * oneDay),
                nextRefillDateEpoch = now + (3 * oneDay), // DUE IN 3 DAYS
                refillCycleDays = 30,
                status = "ACTIVE",
                autoNotifyEnabled = true,
                notes = "Hypertension protocol. Doctor Dr. Shamsul Islam."
            ),
            RefillSubscription(
                patientName = "Elena Rostova",
                patientPhone = "+1 (555) 234-7819",
                patientEmail = "elena.r@email.com",
                medicationId = 6,
                medicationName = "Salbutamol Inhaler 100mcg",
                dosagePerDay = 1,
                quantityDispensed = 1,
                lastDispensedDateEpoch = now - (50 * oneDay),
                nextRefillDateEpoch = now + (10 * oneDay), // DUE IN 10 DAYS
                refillCycleDays = 60,
                status = "ACTIVE",
                autoNotifyEnabled = true,
                notes = "Seasonal bronchial asthma."
            )
        )
        refillDao.insertAll(initialRefills)

        // Seed some past sales
        val initialSales = listOf(
            SaleRecord(
                receiptNumber = "MDI-261025-4190",
                customerName = "Tariq Mahmood",
                doctorPrescriptionNumber = "RX-88219",
                medicationId = 1,
                medicationName = "Amoxicillin 500mg",
                quantity = 21,
                unitPrice = 0.95,
                subtotal = 19.95,
                discountPercent = 0.0,
                totalPaid = 19.95,
                paymentMethod = "CARD",
                timestampEpoch = now - (4 * 3600 * 1000),
                isRefillOrder = false
            ),
            SaleRecord(
                receiptNumber = "MDI-261025-7832",
                customerName = "Mrs. Sarah Jenkins",
                doctorPrescriptionNumber = "REFILL-2",
                medicationId = 3,
                medicationName = "Atorvastatin 20mg",
                quantity = 30,
                unitPrice = 1.45,
                subtotal = 43.50,
                discountPercent = 5.0,
                totalPaid = 41.32,
                paymentMethod = "CASH",
                timestampEpoch = now - (31 * oneDay),
                isRefillOrder = true
            )
        )
        saleDao.insertAll(initialSales)
    }
}
