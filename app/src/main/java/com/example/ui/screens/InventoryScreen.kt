package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Medication
import com.example.ui.StockFilter
import com.example.ui.components.CATEGORIES
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryScreen(
    medications: List<Medication>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    stockFilter: StockFilter,
    onFilterChange: (StockFilter) -> Unit,
    onAdjustStock: (Long, Int) -> Unit,
    onEditMedication: (Medication) -> Unit,
    onDeleteMedication: (Medication) -> Unit,
    onOpenAddMedication: () -> Unit
) {
    val now = System.currentTimeMillis()
    val allCategories = listOf("All") + CATEGORIES

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("inventory_screen"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header & Search
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real-Time Inventory Tracking",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Live stock balances, batch validation, and reorder thresholds",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search by name, generic, SKU, or batch...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_search_field")
                    )
                }
            }

            // Category Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Stock Status Filter Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StockFilter.values().forEach { filter ->
                        FilterChip(
                            selected = stockFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter.label, fontSize = 12.sp) },
                            leadingIcon = if (stockFilter == filter) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Results count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${medications.size} medications cataloged",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (medications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No medications match your filter",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(medications, key = { it.id }) { med ->
                    MedicationInventoryCard(
                        med = med,
                        nowEpoch = now,
                        onAdjustStock = onAdjustStock,
                        onEdit = { onEditMedication(med) },
                        onDelete = { onDeleteMedication(med) }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onOpenAddMedication,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .testTag("add_medication_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Medication")
        }
    }
}

@Composable
fun MedicationInventoryCard(
    med: Medication,
    nowEpoch: Long,
    onAdjustStock: (Long, Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.US)
    val expiryStr = dateFormat.format(Date(med.expiryDateEpoch))
    val isExpiring = med.isExpiringSoon(nowEpoch, 60)
    val isExpired = med.isExpired(nowEpoch)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("med_card_${med.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Category tag, Status Pill & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = med.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        med.isOutOfStock -> {
                            StatusBadge(label = "OUT OF STOCK", bgColor = Color(0xFFEF4444), textColor = Color.White)
                        }
                        med.isLowStock -> {
                            StatusBadge(label = "LOW STOCK", bgColor = Color(0xFFF59E0B), textColor = Color.White)
                        }
                        else -> {
                            StatusBadge(label = "IN STOCK", bgColor = Color(0xFF10B981), textColor = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name & Generic
            Text(
                text = med.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = med.genericName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Stock Bar
            val maxDisplay = (med.minThreshold * 3).coerceAtLeast(100)
            val progress = (med.currentStock.toFloat() / maxDisplay.toFloat()).coerceIn(0f, 1f)

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Current: ${med.currentStock} ${med.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (med.isLowStock) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Min Safety Threshold: ${med.minThreshold}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        med.isOutOfStock -> Color(0xFFEF4444)
                        med.isLowStock -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details: SKU, Batch, Expiry, Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Batch: ${med.batchNumber} • SKU: ${med.sku}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Exp: $expiryStr",
                            fontSize = 11.sp,
                            fontWeight = if (isExpiring || isExpired) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isExpired -> Color(0xFFDC2626)
                                isExpiring -> Color(0xFFD97706)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (isExpiring) {
                            Text(" (Expiring)", fontSize = 10.sp, color = Color(0xFFD97706))
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$" + String.format(Locale.US, "%.2f", med.sellingPrice) + " / ${med.unit.singularize()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cost: $${String.format(Locale.US, "%.2f", med.unitCost)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Adjust Stock Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Stock Adjust:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { onAdjustStock(med.id, -1) },
                        enabled = med.currentStock > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("-1", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { onAdjustStock(med.id, -5) },
                        enabled = med.currentStock >= 5,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("-5", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onAdjustStock(med.id, 10) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+10", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onAdjustStock(med.id, 50) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+50", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(label: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun String.singularize(): String {
    return when {
        endsWith("s") -> dropLast(1)
        else -> this
    }
}
