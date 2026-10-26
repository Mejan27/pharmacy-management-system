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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RefillSubscription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RefillFilter(val label: String) {
    ALL("All Schedules"),
    DUE_OVERDUE("Due & Overdue"),
    UPCOMING("Upcoming (7-30d)"),
    PAUSED("Paused")
}

@Composable
fun RefillNotificationsScreen(
    refills: List<RefillSubscription>,
    onFulfillRefill: (Long) -> Unit,
    onPostponeRefill: (Long, Int) -> Unit,
    onToggleAutoNotify: (RefillSubscription) -> Unit,
    onOpenNotificationModal: (RefillSubscription) -> Unit,
    onOpenAddRefill: () -> Unit
) {
    val now = System.currentTimeMillis()
    var selectedFilter by remember { mutableStateOf(RefillFilter.ALL) }

    val filteredRefills = refills.filter { refill ->
        when (selectedFilter) {
            RefillFilter.ALL -> true
            RefillFilter.DUE_OVERDUE -> refill.isDue(now)
            RefillFilter.UPCOMING -> !refill.isDue(now) && refill.status == "ACTIVE"
            RefillFilter.PAUSED -> refill.status == "PAUSED"
        }
    }

    val dueCount = refills.count { it.isDue(now) }
    val overdueCount = refills.count { it.isOverdue(now) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("refill_notifications_screen"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Automated Refill Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Proactive chronic medication management & smart patient alert dispatch",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Intelligent Scheduler Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ScheduleSend,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Automated Dispatch Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "System monitors pill supply countdowns daily and dispatches SMS/WhatsApp alerts 3 days before supply exhaustion.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Summary Badges
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = if (overdueCount > 0) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Overdue Patients", fontSize = 11.sp, color = Color(0xFFDC2626))
                            Text(
                                "$overdueCount urgent",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = if (dueCount > 0) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Due Within 3 Days", fontSize = 11.sp, color = Color(0xFFD97706))
                            Text(
                                "$dueCount due now",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Subscriptions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${refills.size} active",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RefillFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (filteredRefills.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No refill records in this category", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                items(filteredRefills, key = { it.id }) { refill ->
                    RefillSubscriptionCard(
                        refill = refill,
                        nowEpoch = now,
                        onFulfill = { onFulfillRefill(refill.id) },
                        onPostpone = { onPostponeRefill(refill.id, 3) },
                        onToggleAutoNotify = { onToggleAutoNotify(refill) },
                        onSendAlert = { onOpenNotificationModal(refill) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenAddRefill,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .testTag("add_refill_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Schedule New Refill")
        }
    }
}

@Composable
fun RefillSubscriptionCard(
    refill: RefillSubscription,
    nowEpoch: Long,
    onFulfill: () -> Unit,
    onPostpone: () -> Unit,
    onToggleAutoNotify: () -> Unit,
    onSendAlert: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val nextDateStr = dateFormat.format(Date(refill.nextRefillDateEpoch))
    val daysLeft = refill.daysUntilRefill(nowEpoch)
    val isOverdue = refill.isOverdue(nowEpoch)
    val isDue = refill.isDue(nowEpoch)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("refill_card_${refill.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverdue -> Color(0xFFFFF1F2)
                isDue -> Color(0xFFFFFBEB)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Patient Name & Due Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = refill.patientName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                when {
                    isOverdue -> {
                        Surface(
                            color = Color(0xFFDC2626),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "OVERDUE (${-daysLeft}d)",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    isDue -> {
                        Surface(
                            color = Color(0xFFD97706),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (daysLeft == 0) "DUE TODAY" else "DUE IN ${daysLeft}d",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    else -> {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "IN $daysLeft DAYS",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Medication info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = refill.medicationName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dosage: ${refill.dosagePerDay} per day • Standard supply: ${refill.quantityDispensed} units (${refill.refillCycleDays}-day cycle)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Next Refill Scheduled: $nextDateStr • Contact: ${refill.patientPhone}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (refill.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${refill.notes}",
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Auto-notify toggle & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = refill.autoNotifyEnabled,
                        onCheckedChange = { onToggleAutoNotify() },
                        modifier = Modifier.scale(0.8f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (refill.autoNotifyEnabled) "Auto-Alerts ON" else "Alerts Off",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onSendAlert,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("send_alert_btn_${refill.id}")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Alert", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onFulfill,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("dispense_refill_btn_${refill.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispense Refill", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.size((scale * 48).dp)
)
