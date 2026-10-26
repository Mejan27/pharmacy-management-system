package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.PharmacyViewModel
import com.example.ui.components.AddEditMedicationDialog
import com.example.ui.components.AddRefillDialog
import com.example.ui.components.NotificationPreviewDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DispensingPosScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.RefillNotificationsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PharmacyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyApp(viewModel: PharmacyViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val stats by viewModel.pharmacyStats.collectAsStateWithLifecycle()
    val allMedications by viewModel.allMedications.collectAsStateWithLifecycle()
    val filteredMedications by viewModel.filteredMedications.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val stockFilter by viewModel.stockFilter.collectAsStateWithLifecycle()
    val allRefills by viewModel.allRefills.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()

    val showAddMedModal by viewModel.showAddMedModal.collectAsStateWithLifecycle()
    val medicationToEdit by viewModel.medicationToEdit.collectAsStateWithLifecycle()
    val showAddRefillModal by viewModel.showAddRefillModal.collectAsStateWithLifecycle()
    val notificationTargetRefill by viewModel.notificationTargetRefill.collectAsStateWithLifecycle()
    val digitalReceipt by viewModel.digitalReceipt.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle back button: if not on Dashboard, return to Dashboard
    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        viewModel.setTab(AppTab.DASHBOARD)
    }

    // Collect UI toast events
    LaunchedEffect(Unit) {
        viewModel.uiToastEvent.collectLatest { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalPharmacy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DRUGS INTERNATIONAL LTD",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                text = "Real-Time Inventory & Automated Refills",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    },
                    actions = {
                        // Quick badge button to jump to refill alerts
                        IconButton(
                            onClick = { viewModel.setTab(AppTab.REFILLS) },
                            modifier = Modifier.testTag("top_refill_alert_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (stats.dueRefillsCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFEF4444),
                                            contentColor = Color.White
                                        ) {
                                            Text("${stats.dueRefillsCount}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Refill Alerts",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        AppTab.values().forEach { tab ->
                            val icon = getTabIcon(tab)
                            val hasBadge = tab == AppTab.REFILLS && stats.dueRefillsCount > 0

                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { viewModel.setTab(tab) },
                                icon = {
                                    if (hasBadge) {
                                        BadgedBox(badge = {
                                            Badge(
                                                containerColor = Color(0xFFEF4444),
                                                contentColor = Color.White
                                            ) {
                                                Text("${stats.dueRefillsCount}")
                                            }
                                        }) {
                                            Icon(icon, contentDescription = tab.title)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = tab.title)
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Responsive Navigation Rail for tablets/wide screens
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.values().forEach { tab ->
                            val icon = getTabIcon(tab)
                            val hasBadge = tab == AppTab.REFILLS && stats.dueRefillsCount > 0

                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { viewModel.setTab(tab) },
                                icon = {
                                    if (hasBadge) {
                                        BadgedBox(badge = {
                                            Badge(
                                                containerColor = Color(0xFFEF4444),
                                                contentColor = Color.White
                                            ) {
                                                Text("${stats.dueRefillsCount}")
                                            }
                                        }) {
                                            Icon(icon, contentDescription = tab.title)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = tab.title)
                                    }
                                },
                                label = { Text(tab.title, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Main Screen Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        AppTab.DASHBOARD -> DashboardScreen(
                            stats = stats,
                            medications = allMedications,
                            refills = allRefills,
                            onNavigateTab = { viewModel.setTab(it) },
                            onQuickRestock = { id, amount -> viewModel.adjustStock(id, amount, "DASHBOARD_QUICK_RESTOCK") },
                            onOpenAddMedication = { viewModel.openAddMedicationDialog() },
                            onOpenAddRefill = { viewModel.openAddRefillDialog() }
                        )

                        AppTab.INVENTORY -> InventoryScreen(
                            medications = filteredMedications,
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { viewModel.setSelectedCategory(it) },
                            stockFilter = stockFilter,
                            onFilterChange = { viewModel.setStockFilter(it) },
                            onAdjustStock = { id, amount -> viewModel.adjustStock(id, amount, "INVENTORY_QUICK_ADJUST") },
                            onEditMedication = { viewModel.openAddMedicationDialog(it) },
                            onDeleteMedication = { viewModel.deleteMedication(it) },
                            onOpenAddMedication = { viewModel.openAddMedicationDialog() }
                        )

                        AppTab.REFILLS -> RefillNotificationsScreen(
                            refills = allRefills,
                            onFulfillRefill = { viewModel.fulfillRefill(it) },
                            onPostponeRefill = { id, days -> viewModel.postponeRefill(id, days) },
                            onToggleAutoNotify = { viewModel.toggleAutoNotify(it) },
                            onOpenNotificationModal = { viewModel.openNotificationDialog(it) },
                            onOpenAddRefill = { viewModel.openAddRefillDialog() }
                        )

                        AppTab.DISPENSING -> DispensingPosScreen(
                            medications = allMedications,
                            recentSales = allSales,
                            onProcessSale = { customer, rx, medId, qty, disc, payMethod ->
                                viewModel.processPosDispense(customer, rx, medId, qty, disc, payMethod)
                            },
                            onViewReceipt = { viewModel.showReceipt(it) }
                        )

                        AppTab.ANALYTICS -> AnalyticsScreen(
                            stats = stats,
                            medications = allMedications,
                            sales = allSales,
                            inventoryLogs = recentLogs
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddMedModal) {
        AddEditMedicationDialog(
            medicationToEdit = medicationToEdit,
            onDismiss = { viewModel.closeAddMedicationDialog() },
            onSave = { name, generic, sku, batch, cat, stock, thresh, unit, cost, price, exp, mfg, storage, dosage, rx ->
                viewModel.saveMedication(
                    name = name,
                    genericName = generic,
                    sku = sku,
                    batchNumber = batch,
                    category = cat,
                    stock = stock,
                    threshold = thresh,
                    unit = unit,
                    cost = cost,
                    price = price,
                    expiryDateEpoch = exp,
                    manufacturer = mfg,
                    storageCondition = storage,
                    dosageInstructions = dosage,
                    requiresPrescription = rx
                )
            }
        )
    }

    if (showAddRefillModal) {
        AddRefillDialog(
            medications = allMedications,
            onDismiss = { viewModel.closeAddRefillDialog() },
            onSave = { patient, phone, email, medId, medName, dosage, qty, cycle, notes ->
                viewModel.saveRefillSubscription(
                    patientName = patient,
                    patientPhone = phone,
                    patientEmail = email,
                    medicationId = medId,
                    medicationName = medName,
                    dosagePerDay = dosage,
                    quantity = qty,
                    cycleDays = cycle,
                    notes = notes
                )
            }
        )
    }

    notificationTargetRefill?.let { refill ->
        NotificationPreviewDialog(
            refill = refill,
            onDismiss = { viewModel.closeNotificationDialog() },
            onSendNotification = { channel ->
                viewModel.sendRefillNotification(refill, channel)
            }
        )
    }

    digitalReceipt?.let { sale ->
        ReceiptDialog(
            sale = sale,
            onDismiss = { viewModel.closeReceipt() }
        )
    }
}

fun getTabIcon(tab: AppTab): ImageVector {
    return when (tab) {
        AppTab.DASHBOARD -> Icons.Default.Dashboard
        AppTab.INVENTORY -> Icons.Default.Inventory2
        AppTab.REFILLS -> Icons.Default.NotificationsActive
        AppTab.DISPENSING -> Icons.Default.PointOfSale
        AppTab.ANALYTICS -> Icons.Default.BarChart
    }
}
