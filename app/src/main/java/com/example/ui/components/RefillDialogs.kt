package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Medication
import com.example.data.model.RefillSubscription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRefillDialog(
    medications: List<Medication>,
    onDismiss: () -> Unit,
    onSave: (
        patientName: String,
        patientPhone: String,
        patientEmail: String,
        medicationId: Long,
        medicationName: String,
        dosagePerDay: Int,
        quantity: Int,
        cycleDays: Int,
        notes: String
    ) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var patientEmail by remember { mutableStateOf("") }
    var selectedMedication by remember { mutableStateOf(medications.firstOrNull()) }
    var medDropdownExpanded by remember { mutableStateOf(false) }
    var dosagePerDayStr by remember { mutableStateOf("1") }
    var quantityStr by remember { mutableStateOf("30") }
    var cycleDays by remember { mutableIntStateOf(30) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_refill_dialog")
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
                        text = "New Refill Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_refill_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Full Name *") },
                    placeholder = { Text("e.g. John Doe") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("refill_patient_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = patientPhone,
                    onValueChange = { patientPhone = it },
                    label = { Text("Mobile Phone (for SMS/WhatsApp alerts) *") },
                    placeholder = { Text("+880 1712-345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("refill_patient_phone")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = patientEmail,
                    onValueChange = { patientEmail = it },
                    label = { Text("Email (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Medication selector
                ExposedDropdownMenuBox(
                    expanded = medDropdownExpanded,
                    onExpandedChange = { medDropdownExpanded = !medDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedMedication?.name ?: "Select Prescribed Medication",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Prescribed Medication *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = medDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("refill_med_select")
                    )
                    ExposedDropdownMenu(
                        expanded = medDropdownExpanded,
                        onDismissRequest = { medDropdownExpanded = false }
                    ) {
                        medications.forEach { med ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(med.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${med.category} • In stock: ${med.currentStock} ${med.unit}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedMedication = med
                                    medDropdownExpanded = false
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
                        value = dosagePerDayStr,
                        onValueChange = { dosagePerDayStr = it },
                        label = { Text("Dose / Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Dispensed Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Refill Cycle Schedule:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 60, 90).forEach { days ->
                        FilterChip(
                            selected = cycleDays == days,
                            onClick = { cycleDays = days },
                            label = { Text("$days Days") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Clinical Notes / Reminders") },
                    placeholder = { Text("e.g. Hypertension maintenance. Patient prefers morning contact.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val med = selectedMedication
                        if (patientName.isBlank() || patientPhone.isBlank() || med == null) {
                            errorMessage = "Please enter patient name, phone, and select medication."
                            return@Button
                        }
                        val dosage = dosagePerDayStr.toIntOrNull() ?: 1
                        val qty = quantityStr.toIntOrNull() ?: 30

                        onSave(
                            patientName,
                            patientPhone,
                            patientEmail,
                            med.id,
                            med.name,
                            dosage,
                            qty,
                            cycleDays,
                            notes
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_refill_button")
                ) {
                    Text("Activate Refill Tracking")
                }
            }
        }
    }
}

@Composable
fun NotificationPreviewDialog(
    refill: RefillSubscription,
    onDismiss: () -> Unit,
    onSendNotification: (channel: String) -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US)
    val dueDateStr = dateFormat.format(Date(refill.nextRefillDateEpoch))

    val messageBody = """
        [Drugs International Limited - Automated Refill Notice]
        Dear ${refill.patientName}, your prescription refill for ${refill.medicationName} is scheduled for ${dueDateStr}. 
        We have your medicine prepared for pickup or express courier delivery.
        Contact: +880 1712-345678 | Address: Drugs International Medical Complex
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("notification_preview_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automated Refill Alert",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Recipient: ${refill.patientName} (${refill.patientPhone})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Refill Item: ${refill.medicationName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = messageBody,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSendNotification("WhatsApp") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_whatsapp_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp")
                    }
                    Button(
                        onClick = { onSendNotification("SMS") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_sms_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "SMS", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS Dispatch")
                    }
                }
            }
        }
    }
}
