package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Medication
import com.example.data.model.SaleRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispensingPosScreen(
    medications: List<Medication>,
    recentSales: List<SaleRecord>,
    onProcessSale: (
        customerName: String,
        doctorPrescriptionNumber: String,
        medicationId: Long,
        quantity: Int,
        discountPercent: Double,
        paymentMethod: String
    ) -> Unit,
    onViewReceipt: (SaleRecord) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var prescriptionNumber by remember { mutableStateOf("") }
    var selectedMedication by remember { mutableStateOf(medications.firstOrNull()) }
    var medDropdownExpanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableIntStateOf(1) }
    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // If selectedMedication is null or updated, sync
    LaunchedEffect(medications) {
        if (selectedMedication == null && medications.isNotEmpty()) {
            selectedMedication = medications.first()
        } else if (selectedMedication != null) {
            selectedMedication = medications.find { it.id == selectedMedication?.id } ?: medications.firstOrNull()
        }
    }

    val currentMed = selectedMedication
    val unitPrice = currentMed?.sellingPrice ?: 0.0
    val subtotal = unitPrice * quantity
    val totalDiscount = subtotal * (discountPercent / 100.0)
    val grandTotal = subtotal - totalDiscount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dispensing_pos_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // POS Header
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Counter Dispensing & POS Register",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Instant prescription fulfillment, live stock deduction, and digital receipting",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dispensing Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Prescription Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Patient / Customer Name") },
                            placeholder = { Text("Walk-in Customer") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("pos_customer_input")
                        )

                        OutlinedTextField(
                            value = prescriptionNumber,
                            onValueChange = { prescriptionNumber = it },
                            label = { Text("Rx / Doctor #") },
                            placeholder = { Text("e.g. RX-9042") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pos_rx_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Medication selector dropdown
                    ExposedDropdownMenuBox(
                        expanded = medDropdownExpanded,
                        onExpandedChange = { medDropdownExpanded = !medDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentMed?.name ?: "Select Medication to Dispense",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Prescribed Medication") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = medDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("pos_med_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = medDropdownExpanded,
                            onDismissRequest = { medDropdownExpanded = false }
                        ) {
                            medications.forEach { med ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(med.name, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "${med.category} • $${String.format(Locale.US, "%.2f", med.sellingPrice)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                color = if (med.currentStock > med.minThreshold) Color(0xFF10B981) else Color(0xFFEF4444),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "${med.currentStock} in stock",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedMedication = med
                                        medDropdownExpanded = false
                                        if (quantity > med.currentStock && med.currentStock > 0) {
                                            quantity = med.currentStock
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Selected item stock telemetry strip
                    currentMed?.let { med ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Available Stock: ${med.currentStock} ${med.unit}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (med.isLowStock) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Unit Price: $" + String.format(Locale.US, "%.2f", med.sellingPrice),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quantity Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dispense Quantity:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "$quantity",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            FilledTonalButton(
                                onClick = {
                                    val max = currentMed?.currentStock ?: 999
                                    if (quantity < max) quantity++
                                },
                                enabled = currentMed != null && quantity < currentMed.currentStock,
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Discount presets
                    Text("Apply Discount:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.0, 5.0, 10.0, 15.0).forEach { disc ->
                            FilterChip(
                                selected = discountPercent == disc,
                                onClick = { discountPercent = disc },
                                label = { Text(if (disc == 0.0) "None" else "${disc.toInt()}%", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Payment Method
                    Text("Payment Method:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CASH", "CARD", "DIGITAL_PAY").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = {
                                    Text(
                                        when (method) {
                                            "CASH" -> "Cash"
                                            "CARD" -> "Card / POS"
                                            else -> "Digital Pay"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Total Calculation Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal:", fontSize = 12.sp)
                                Text("$" + String.format(Locale.US, "%.2f", subtotal), fontSize = 12.sp)
                            }
                            if (discountPercent > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discount (${discountPercent.toInt()}%):", fontSize = 12.sp, color = Color(0xFF10B981))
                                    Text("-$" + String.format(Locale.US, "%.2f", totalDiscount), fontSize = 12.sp, color = Color(0xFF10B981))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "$" + String.format(Locale.US, "%.2f", grandTotal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val med = currentMed
                            if (med == null) {
                                errorMessage = "Please select a medication to dispense."
                                return@Button
                            }
                            if (med.currentStock < quantity) {
                                errorMessage = "Insufficient stock! Only ${med.currentStock} units available."
                                return@Button
                            }
                            errorMessage = null
                            onProcessSale(
                                customerName.ifBlank { "Walk-in Customer" },
                                prescriptionNumber,
                                med.id,
                                quantity,
                                discountPercent,
                                paymentMethod
                            )
                            // Reset quantity
                            quantity = 1
                        },
                        enabled = currentMed != null && (currentMed?.currentStock ?: 0) >= quantity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dispense_pos_submit")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm & Dispense Medication", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent Sales History Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Dispensed Prescriptions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${recentSales.size} transactions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (recentSales.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sales logged yet today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(recentSales.take(10), key = { it.id }) { sale ->
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
                val timeStr = timeFormat.format(Date(sale.timestampEpoch))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = sale.medicationName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (sale.isRefillOrder) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF00897B),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            "REFILL",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${sale.customerName} • ${sale.quantity} units • $timeStr",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$" + String.format(Locale.US, "%.2f", sale.totalPaid),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { onViewReceipt(sale) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Receipt", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
