package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PharmacyDatabase
import com.example.data.model.InventoryLog
import com.example.data.model.Medication
import com.example.data.model.RefillSubscription
import com.example.data.model.SaleRecord
import com.example.data.repository.PharmacyRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Overview"),
    INVENTORY("Inventory"),
    REFILLS("Refill Alerts"),
    DISPENSING("Dispensing POS"),
    ANALYTICS("Analytics")
}

enum class StockFilter(val label: String) {
    ALL("All Stock"),
    LOW_STOCK("Low Stock Alerts"),
    EXPIRING_SOON("Expiring Soon"),
    OUT_OF_STOCK("Out of Stock")
}

data class PharmacyStats(
    val totalItemsCount: Int = 0,
    val totalStockUnits: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val expiringCount: Int = 0,
    val dueRefillsCount: Int = 0,
    val todaySalesAmount: Double = 0.0,
    val todaySalesCount: Int = 0
)

class PharmacyViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PharmacyDatabase.getDatabase(application)
    private val repository = PharmacyRepository(
        medicationDao = db.medicationDao(),
        refillDao = db.refillDao(),
        saleDao = db.saleDao(),
        inventoryLogDao = db.inventoryLogDao()
    )

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _stockFilter = MutableStateFlow(StockFilter.ALL)
    val stockFilter: StateFlow<StockFilter> = _stockFilter.asStateFlow()

    // Dialog & UI interaction states
    private val _showAddMedModal = MutableStateFlow(false)
    val showAddMedModal: StateFlow<Boolean> = _showAddMedModal.asStateFlow()

    private val _medicationToEdit = MutableStateFlow<Medication?>(null)
    val medicationToEdit: StateFlow<Medication?> = _medicationToEdit.asStateFlow()

    private val _showAddRefillModal = MutableStateFlow(false)
    val showAddRefillModal: StateFlow<Boolean> = _showAddRefillModal.asStateFlow()

    private val _notificationTargetRefill = MutableStateFlow<RefillSubscription?>(null)
    val notificationTargetRefill: StateFlow<RefillSubscription?> = _notificationTargetRefill.asStateFlow()

    private val _digitalReceipt = MutableStateFlow<SaleRecord?>(null)
    val digitalReceipt: StateFlow<SaleRecord?> = _digitalReceipt.asStateFlow()

    private val _uiToastEvent = MutableSharedFlow<String>()
    val uiToastEvent = _uiToastEvent.asSharedFlow()

    // Base flows
    val allMedications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRefills: StateFlow<List<RefillSubscription>> = repository.allRefills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleRecord>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<InventoryLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered medications based on search, category, and stock filter
    val filteredMedications: StateFlow<List<Medication>> = combine(
        allMedications,
        _searchQuery,
        _selectedCategory,
        _stockFilter
    ) { meds, query, category, filter ->
        val now = System.currentTimeMillis()
        meds.filter { med ->
            val matchesQuery = query.isBlank() ||
                    med.name.contains(query, ignoreCase = true) ||
                    med.genericName.contains(query, ignoreCase = true) ||
                    med.sku.contains(query, ignoreCase = true) ||
                    med.batchNumber.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || med.category.equals(category, ignoreCase = true)

            val matchesFilter = when (filter) {
                StockFilter.ALL -> true
                StockFilter.LOW_STOCK -> med.isLowStock && !med.isOutOfStock
                StockFilter.EXPIRING_SOON -> med.isExpiringSoon(now, daysThreshold = 60)
                StockFilter.OUT_OF_STOCK -> med.isOutOfStock
            }

            matchesQuery && matchesCategory && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time calculated pharmacy statistics
    val pharmacyStats: StateFlow<PharmacyStats> = combine(
        allMedications,
        allRefills,
        allSales
    ) { meds, refills, sales ->
        val now = System.currentTimeMillis()
        val totalUnits = meds.sumOf { it.currentStock }
        val totalValue = meds.sumOf { it.currentStock * it.sellingPrice }
        val lowStock = meds.count { it.isLowStock }
        val expiring = meds.count { it.isExpiringSoon(now, 60) }
        val dueRefills = refills.count { it.isDue(now) }

        val oneDayAgo = now - (24L * 60 * 60 * 1000)
        val todaySales = sales.filter { it.timestampEpoch >= oneDayAgo }
        val todaySalesAmount = todaySales.sumOf { it.totalPaid }

        PharmacyStats(
            totalItemsCount = meds.size,
            totalStockUnits = totalUnits,
            totalInventoryValue = totalValue,
            lowStockCount = lowStock,
            expiringCount = expiring,
            dueRefillsCount = dueRefills,
            todaySalesAmount = todaySalesAmount,
            todaySalesCount = todaySales.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PharmacyStats())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setStockFilter(filter: StockFilter) {
        _stockFilter.value = filter
    }

    fun openAddMedicationDialog(med: Medication? = null) {
        _medicationToEdit.value = med
        _showAddMedModal.value = true
    }

    fun closeAddMedicationDialog() {
        _medicationToEdit.value = null
        _showAddMedModal.value = false
    }

    fun openAddRefillDialog() {
        _showAddRefillModal.value = true
    }

    fun closeAddRefillDialog() {
        _showAddRefillModal.value = false
    }

    fun openNotificationDialog(refill: RefillSubscription) {
        _notificationTargetRefill.value = refill
    }

    fun closeNotificationDialog() {
        _notificationTargetRefill.value = null
    }

    fun showReceipt(sale: SaleRecord) {
        _digitalReceipt.value = sale
    }

    fun closeReceipt() {
        _digitalReceipt.value = null
    }

    fun saveMedication(
        name: String,
        genericName: String,
        sku: String,
        batchNumber: String,
        category: String,
        stock: Int,
        threshold: Int,
        unit: String,
        cost: Double,
        price: Double,
        expiryDateEpoch: Long,
        manufacturer: String,
        storageCondition: String,
        dosageInstructions: String,
        requiresPrescription: Boolean
    ) {
        viewModelScope.launch {
            val existing = _medicationToEdit.value
            if (existing != null) {
                val updated = existing.copy(
                    name = name,
                    genericName = genericName,
                    sku = sku,
                    batchNumber = batchNumber,
                    category = category,
                    currentStock = stock,
                    minThreshold = threshold,
                    unit = unit,
                    unitCost = cost,
                    sellingPrice = price,
                    expiryDateEpoch = expiryDateEpoch,
                    manufacturer = manufacturer,
                    storageCondition = storageCondition,
                    dosageInstructions = dosageInstructions,
                    requiresPrescription = requiresPrescription
                )
                repository.updateMedication(updated)
                _uiToastEvent.emit("Updated ${updated.name}")
            } else {
                val newMed = Medication(
                    name = name,
                    genericName = genericName,
                    sku = sku.ifBlank { "SKU-" + (1000..9999).random() },
                    batchNumber = batchNumber.ifBlank { "MDI-BX-" + (100..999).random() },
                    category = category,
                    currentStock = stock,
                    minThreshold = threshold,
                    unit = unit,
                    unitCost = cost,
                    sellingPrice = price,
                    expiryDateEpoch = expiryDateEpoch,
                    manufacturer = manufacturer.ifBlank { "Drugs International Ltd" },
                    storageCondition = storageCondition.ifBlank { "Room Temp (15-25°C)" },
                    dosageInstructions = dosageInstructions,
                    requiresPrescription = requiresPrescription
                )
                repository.insertMedication(newMed)
                _uiToastEvent.emit("Added ${newMed.name} to inventory")
            }
            closeAddMedicationDialog()
        }
    }

    fun adjustStock(medicationId: Long, change: Int, reason: String = "MANUAL_ADJUSTMENT") {
        viewModelScope.launch {
            repository.adjustStock(
                medicationId = medicationId,
                change = change,
                actionType = if (change > 0) "RESTOCKED" else "STOCK_DEDUCTED",
                notes = reason
            )
            val action = if (change > 0) "Restocked +$change" else "Deducted $change"
            _uiToastEvent.emit("$action units successfully")
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
            _uiToastEvent.emit("Removed ${medication.name}")
        }
    }

    fun saveRefillSubscription(
        patientName: String,
        patientPhone: String,
        patientEmail: String,
        medicationId: Long,
        medicationName: String,
        dosagePerDay: Int,
        quantity: Int,
        cycleDays: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextEpoch = now + (cycleDays.toLong() * 24 * 60 * 60 * 1000)
            val refill = RefillSubscription(
                patientName = patientName,
                patientPhone = patientPhone,
                patientEmail = patientEmail,
                medicationId = medicationId,
                medicationName = medicationName,
                dosagePerDay = dosagePerDay,
                quantityDispensed = quantity,
                lastDispensedDateEpoch = now,
                nextRefillDateEpoch = nextEpoch,
                refillCycleDays = cycleDays,
                status = "ACTIVE",
                autoNotifyEnabled = true,
                notes = notes
            )
            repository.insertRefill(refill)
            _uiToastEvent.emit("Refill schedule activated for $patientName")
            closeAddRefillDialog()
        }
    }

    fun fulfillRefill(refillId: Long) {
        viewModelScope.launch {
            val result = repository.processRefillFulfillment(refillId)
            if (result.isSuccess) {
                val sale = result.getOrThrow()
                _digitalReceipt.value = sale
                _uiToastEvent.emit("Refill dispensed! Next cycle automatically scheduled.")
            } else {
                _uiToastEvent.emit("Refill failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun postponeRefill(refillId: Long, days: Int = 3) {
        viewModelScope.launch {
            repository.postponeRefill(refillId, days)
            _uiToastEvent.emit("Refill postponed by $days days")
        }
    }

    fun toggleAutoNotify(refill: RefillSubscription) {
        viewModelScope.launch {
            val updated = refill.copy(autoNotifyEnabled = !refill.autoNotifyEnabled)
            repository.updateRefill(updated)
            val status = if (updated.autoNotifyEnabled) "enabled" else "disabled"
            _uiToastEvent.emit("Automated notifications $status for ${refill.patientName}")
        }
    }

    fun sendRefillNotification(refill: RefillSubscription, channel: String = "SMS") {
        viewModelScope.launch {
            _uiToastEvent.emit("Sent automated $channel alert to ${refill.patientName} (${refill.patientPhone})")
            closeNotificationDialog()
        }
    }

    fun processPosDispense(
        customerName: String,
        doctorPrescriptionNumber: String,
        medicationId: Long,
        quantity: Int,
        discountPercent: Double,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            val result = repository.recordSale(
                customerName = customerName,
                doctorPrescriptionNumber = doctorPrescriptionNumber,
                medicationId = medicationId,
                quantity = quantity,
                discountPercent = discountPercent,
                paymentMethod = paymentMethod,
                isRefillOrder = false
            )
            if (result.isSuccess) {
                val sale = result.getOrThrow()
                _digitalReceipt.value = sale
                _uiToastEvent.emit("Dispensing recorded! Receipt #${sale.receiptNumber}")
            } else {
                _uiToastEvent.emit("Dispensing error: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
