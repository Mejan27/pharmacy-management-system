package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Medication

val CATEGORIES = listOf(
    "Antibiotics",
    "Analgesics",
    "Cardiovascular",
    "Antidiabetic",
    "Respiratory",
    "Gastrointestinal",
    "Vitamins",
    "Dermatology"
)

val UNITS = listOf("Tablets", "Capsules", "Bottles", "Vials", "Blisters", "Tubes", "Inhaler Units")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationDialog(
    medicationToEdit: Medication?,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var name by remember { mutableStateOf(medicationToEdit?.name ?: "") }
    var genericName by remember { mutableStateOf(medicationToEdit?.genericName ?: "") }
    var sku by remember { mutableStateOf(medicationToEdit?.sku ?: "") }
    var batchNumber by remember { mutableStateOf(medicationToEdit?.batchNumber ?: "") }
    var category by remember { mutableStateOf(medicationToEdit?.category ?: CATEGORIES.first()) }
    var stockStr by remember { mutableStateOf(medicationToEdit?.currentStock?.toString() ?: "50") }
    var thresholdStr by remember { mutableStateOf(medicationToEdit?.minThreshold?.toString() ?: "15") }
    var unit by remember { mutableStateOf(medicationToEdit?.unit ?: "Tablets") }
    var costStr by remember { mutableStateOf(medicationToEdit?.unitCost?.toString() ?: "0.50") }
    var priceStr by remember { mutableStateOf(medicationToEdit?.sellingPrice?.toString() ?: "1.20") }
    var manufacturer by remember { mutableStateOf(medicationToEdit?.manufacturer ?: "Drugs International Ltd") }
    var storageCondition by remember { mutableStateOf(medicationToEdit?.storageCondition ?: "Room Temp (15-25°C)") }
    var dosageInstructions by remember { mutableStateOf(medicationToEdit?.dosageInstructions ?: "") }
    var requiresPrescription by remember { mutableStateOf(medicationToEdit?.requiresPrescription ?: true) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_medication_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (medicationToEdit == null) "Add Medication to Inventory" else "Edit Medication",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_med_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                errorMessage?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Brand / Trade Name *") },
                    placeholder = { Text("e.g. Amoxicillin 500mg") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = genericName,
                    onValueChange = { genericName = it },
                    label = { Text("Generic Formula *") },
                    placeholder = { Text("e.g. Amoxicillin Trihydrate") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_generic_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock Qty *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("med_stock_input")
                    )
                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it },
                        label = { Text("Min Alert Lvl *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("med_threshold_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unit dropdown
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dispensing Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        UNITS.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = {
                                    unit = u
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Cost Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("med_cost_input")
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Selling Price ($) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("med_price_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Manufacturer / Supplier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dosageInstructions,
                    onValueChange = { dosageInstructions = it },
                    label = { Text("Standard Dosage Instructions") },
                    placeholder = { Text("e.g. Take 1 tablet daily after food") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = requiresPrescription,
                        onCheckedChange = { requiresPrescription = it }
                    )
                    Text("Requires Doctor Prescription (Rx)", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || genericName.isBlank()) {
                            errorMessage = "Brand name and generic formula are required."
                            return@Button
                        }
                        val stock = stockStr.toIntOrNull() ?: 0
                        val threshold = thresholdStr.toIntOrNull() ?: 10
                        val cost = costStr.toDoubleOrNull() ?: 0.5
                        val price = priceStr.toDoubleOrNull() ?: 1.0

                        val expiryEpoch = medicationToEdit?.expiryDateEpoch
                            ?: (System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000))

                        onSave(
                            name,
                            genericName,
                            sku,
                            batchNumber,
                            category,
                            stock,
                            threshold,
                            unit,
                            cost,
                            price,
                            expiryEpoch,
                            manufacturer,
                            storageCondition,
                            dosageInstructions,
                            requiresPrescription
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_medication_button")
                ) {
                    Text(if (medicationToEdit == null) "Add to Stock Inventory" else "Save Changes")
                }
            }
        }
    }
}
