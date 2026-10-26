package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Medication
import com.example.data.model.RefillSubscription
import com.example.ui.AppTab
import com.example.ui.PharmacyStats
import java.util.Locale

@Composable
fun DashboardScreen(
    stats: PharmacyStats,
    medications: List<Medication>,
    refills: List<RefillSubscription>,
    onNavigateTab: (AppTab) -> Unit,
    onQuickRestock: (Long, Int) -> Unit,
    onOpenAddMedication: () -> Unit,
    onOpenAddRefill: () -> Unit
) {
    val now = System.currentTimeMillis()
    val dueRefills = refills.filter { it.isDue(now) }
    val lowStockMeds = medications.filter { it.isLowStock }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Pharmacy Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mejan_pharmacy_hero_1792988364007),
                    contentDescription = "Drugs International Limited Dispensing Facility",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        color = Color(0xFF00BFA5),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "REAL-TIME PHARMACY OS",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Drugs International Limited",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Automated Refill Scheduler & Live Inventory Tracking",
                        fontSize = 12.sp,
                        color = Color(0xFFE0F2F1)
                    )
                }
            }
        }

        // Automated Refill Alert Banner (if any due)
        item {
            if (dueRefills.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateTab(AppTab.REFILLS) }
                        .testTag("dashboard_refill_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Refill Alert",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Action Required: ${dueRefills.size} Refill${if (dueRefills.size > 1) "s" else ""} Due",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Patients need notification dispatch or medication dispensing.",
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Go",
                            tint = Color(0xFFD97706)
                        )
                    }
                }
            }
        }

        // KPI Telemetry Grid
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pharmacy Telemetry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard(
                        title = "Inventory Value",
                        value = "$" + String.format(Locale.US, "%,.2f", stats.totalInventoryValue),
                        subtitle = "${stats.totalStockUnits} units in stock",
                        icon = Icons.Default.Inventory2,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Today's Sales",
                        value = "$" + String.format(Locale.US, "%,.2f", stats.todaySalesAmount),
                        subtitle = "${stats.todaySalesCount} prescriptions filled",
                        icon = Icons.Default.PointOfSale,
                        iconTint = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiCard(
                        title = "Low Stock Alerts",
                        value = "${stats.lowStockCount} items",
                        subtitle = "Needs reordering",
                        icon = Icons.Default.Warning,
                        iconTint = if (stats.lowStockCount > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AppTab.INVENTORY) }
                    )
                    KpiCard(
                        title = "Refills Pending",
                        value = "${stats.dueRefillsCount} due",
                        subtitle = "Automated reminders",
                        icon = Icons.Default.Autorenew,
                        iconTint = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AppTab.REFILLS) }
                    )
                }
            }
        }

        // Quick Action Bar
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Quick Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onNavigateTab(AppTab.DISPENSING) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_pos")
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Counter POS", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onOpenAddMedication,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_new_med")
                    ) {
                        Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Medicine", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onOpenAddRefill,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_new_refill")
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Refill", fontSize = 12.sp)
                    }
                }
            }
        }

        // Urgent Low Stock Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock Alert Spotlight",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { onNavigateTab(AppTab.INVENTORY) }) {
                    Text("View All Inventory", fontSize = 12.sp)
                }
            }
        }

        if (lowStockMeds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Stock Good",
                            tint = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("All medications are adequately stocked above safety thresholds.")
                    }
                }
            }
        } else {
            items(lowStockMeds.take(5)) { med ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (med.isOutOfStock) Color(0xFFFEE2E2) else Color(0xFFFFFBEB)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = med.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (med.isOutOfStock) Color(0xFFDC2626) else Color(0xFFD97706),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (med.isOutOfStock) "OUT OF STOCK" else "LOW STOCK",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Current: ${med.currentStock} ${med.unit} (Threshold: ${med.minThreshold})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onQuickRestock(med.id, 20) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("quick_restock_${med.id}")
                        ) {
                            Text("+20 Stock", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
