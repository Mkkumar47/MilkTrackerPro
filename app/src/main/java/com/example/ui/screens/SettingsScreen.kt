package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ColorNo
import com.example.ui.components.ColorYes
import com.example.viewmodel.MilkViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MilkViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.configFlow.collectAsState()
    val records by viewModel.recordsFlow.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val sellers by viewModel.sellersFlow.collectAsState()

    var showVendorDialog by remember { mutableStateOf(false) }
    var editingVendor by remember { mutableStateOf<com.example.data.Seller?>(null) }
    var vendorFormName by remember { mutableStateOf("") }
    var vendorFormPhone by remember { mutableStateOf("") }
    var vendorFormCowRate by remember { mutableStateOf("40.0") }
    var vendorFormBuffaloRate by remember { mutableStateOf("40.0") }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var vendorToDelete by remember { mutableStateOf<com.example.data.Seller?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Config form states initial values
    var defaultQty by remember(config) { mutableStateOf(config.defaultQuantity.toString()) }
    var defaultRateByLitre by remember(config) { mutableStateOf(config.defaultRate.toString()) }
    var dailyReminder by remember(config) { mutableStateOf(config.dailyReminderEnabled) }
    var paymentReminder by remember(config) { mutableStateOf(config.paymentReminderEnabled) }
    var themePref by remember(config) { mutableStateOf(config.themePreference) }
    var payDay by remember(config) { mutableStateOf(config.paymentReminderDay) }
    var leadDays by remember(config) { mutableStateOf(config.paymentReminderDaysBefore) }
    var currencyCode by remember(config) { mutableStateOf(config.currencyCode) }
    var currencySymbol by remember(config) { mutableStateOf(config.currencySymbol) }
    var dailyReminderHour by remember(config) { mutableStateOf(config.dailyReminderHour) }
    var dailyReminderMinute by remember(config) { mutableStateOf(config.dailyReminderMinute) }
    var showCustomCurrencyInput by remember(config) { mutableStateOf(config.currencyCode == "Custom") }

    // Backup restore inputs
    var textToRestore by remember { mutableStateOf("") }
    var showBackupBox by remember { mutableStateOf(false) }
    var showRestoreBox by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        // Screen Header
        Text(
            text = "App Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configure Preferences & Control Backups",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Google Account Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val userNameVal = config.googleUserName
                        val initials = if (!userNameVal.isNullOrBlank()) {
                            val parts = userNameVal.trim().split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                "${parts[0].take(1).uppercase()}${parts[1].take(1).uppercase()}"
                            } else {
                                userNameVal.take(2).uppercase()
                            }
                        } else {
                            "U"
                        }
                        Text(
                            text = initials,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Column {
                        Text(
                            text = config.googleUserName ?: "Google User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = config.googleEmail ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Button(
                    onClick = {
                        viewModel.updateGoogleSignIn(null, null, null, false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Vendor Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Manage Milk Vendors",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            editingVendor = null
                            vendorFormName = ""
                            vendorFormPhone = ""
                            vendorFormCowRate = "40.0"
                            vendorFormBuffaloRate = "40.0"
                            showVendorDialog = true
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Vendor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (sellers.isEmpty()) {
                    Text(
                        text = "No milk vendors configured. Add one to associate deliveries with specific sellers.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sellers.forEach { seller ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = seller.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (seller.phone.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Phone,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = seller.phone,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "Cow: ${config.currencySymbol}${seller.cowRate}/L",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Buffalo: ${config.currencySymbol}${seller.buffaloRate}/L",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingVendor = seller
                                                vendorFormName = seller.name
                                                vendorFormPhone = seller.phone
                                                vendorFormCowRate = seller.cowRate.toString()
                                                vendorFormBuffaloRate = seller.buffaloRate.toString()
                                                showVendorDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Edit Vendor",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                vendorToDelete = seller
                                                showDeleteConfirm = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete Vendor",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showVendorDialog) {
            AlertDialog(
                onDismissRequest = { showVendorDialog = false },
                title = {
                    Text(
                        text = if (editingVendor == null) "Add Milk Vendor" else "Edit Milk Vendor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = vendorFormName,
                            onValueChange = { vendorFormName = it },
                            label = { Text("Vendor Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = vendorFormPhone,
                            onValueChange = { vendorFormPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = vendorFormCowRate,
                                onValueChange = { vendorFormCowRate = it },
                                label = { Text("Cow Rate/L") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = vendorFormBuffaloRate,
                                onValueChange = { vendorFormBuffaloRate = it },
                                label = { Text("Buffalo Rate/L") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (vendorFormName.isNotBlank()) {
                                viewModel.saveSeller(
                                    id = editingVendor?.id ?: 0,
                                    name = vendorFormName,
                                    phone = vendorFormPhone,
                                    cowRate = vendorFormCowRate.toDoubleOrNull() ?: 0.0,
                                    buffaloRate = vendorFormBuffaloRate.toDoubleOrNull() ?: 0.0
                                )
                                showVendorDialog = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (editingVendor == null) "Add" else "Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVendorDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text("Delete Vendor?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete '${vendorToDelete?.name}'? Daily diary records and payment files linked to this vendor name will remain, but the vendor profile will be removed.",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            vendorToDelete?.let {
                                viewModel.deleteSeller(it)
                            }
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Theme Customization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "App Theme Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")
                    themes.forEach { (key, label) ->
                        val isSelected = themePref == key
                        Button(
                            onClick = {
                                themePref = key
                                val qty = defaultQty.toDoubleOrNull() ?: 1.0
                                val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                                viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, key, payDay, leadDays, currencyCode, currencySymbol)
                            },
                            colors = if (isSelected) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            } else {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        // Preferred Defaults segment
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Delivery Preferences",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = defaultQty,
                        onValueChange = { defaultQty = it },
                        label = { Text("Default Litres") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = ColorYes) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = defaultRateByLitre,
                        onValueChange = { defaultRateByLitre = it },
                        label = { Text("Rate Per Litre ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Currency Dropdown section with bespoke unique UI
                var currencyDropdownExpanded by remember { mutableStateOf(false) }
                val currencies = listOf(
                    Triple("USD", "$", "US Dollar ($)"),
                    Triple("INR", "₹", "Indian Rupee (₹)"),
                    Triple("EUR", "€", "Euro (€)"),
                    Triple("GBP", "£", "British Pound (£)"),
                    Triple("CAD", "$", "Canadian Dollar ($)"),
                    Triple("AUD", "$", "Australian Dollar ($)"),
                    Triple("AED", "AED", "UAE Dirham (AED)"),
                    Triple("SAR", "SR", "Saudi Riyal (SR)"),
                    Triple("PKR", "₨", "Pakistani Rupee (₨)"),
                    Triple("JPY", "¥", "Japanese Yen (¥)"),
                    Triple("CNY", "¥", "Chinese Yuan (¥)"),
                    Triple("NPR", "₨", "Nepalese Rupee (₨)"),
                    Triple("Custom", "Custom", "Custom Currency Symbol...")
                )

                Text(
                    text = "Local Currency Settings",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { currencyDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeSelection = currencies.find { it.first == currencyCode }
                            val activeLabel = activeSelection?.third ?: "Custom: $currencyCode ($currencySymbol)"
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(
                                    text = activeLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Expand Currency Dropdown", modifier = Modifier.size(20.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        currencies.forEach { (code, symbol, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(label, fontSize = 14.sp)
                                        if (code != "Custom") {
                                            Text(
                                                symbol,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    currencyDropdownExpanded = false
                                    if (code != "Custom") {
                                        currencyCode = code
                                        currencySymbol = symbol
                                        showCustomCurrencyInput = false
                                        val qty = defaultQty.toDoubleOrNull() ?: 1.0
                                        val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                                        viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays, code, symbol)
                                    } else {
                                        currencyCode = "Custom"
                                        currencySymbol = ""
                                        showCustomCurrencyInput = true
                                    }
                                }
                            )
                        }
                    }
                }

                if (showCustomCurrencyInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (currencyCode == "Custom") "" else currencyCode,
                            onValueChange = {
                                currencyCode = it
                                val qty = defaultQty.toDoubleOrNull() ?: 1.0
                                val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                                viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays, it, currencySymbol)
                            },
                            label = { Text("Code (e.g., CAD)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = {
                                currencySymbol = it
                                val qty = defaultQty.toDoubleOrNull() ?: 1.0
                                val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                                viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays, currencyCode, it)
                            },
                            label = { Text("Symbol (e.g., $)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val qty = defaultQty.toDoubleOrNull() ?: 1.0
                        val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                        viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays, currencyCode, currencySymbol)
                        Toast.makeText(context, "Preferences saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Default Diary Settings")
                }
            }
        }

        // Reminders notification switch card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Smart Notifications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Daily Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedTime = remember(dailyReminderHour, dailyReminderMinute) {
                        val hr = if (dailyReminderHour == 0 || dailyReminderHour == 12) 12 else dailyReminderHour % 12
                        val min = String.format(Locale.US, "%02d", dailyReminderMinute)
                        val amPm = if (dailyReminderHour < 12) "AM" else "PM"
                        "$hr:$min $amPm"
                    }
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Daily Log Reminder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Send a daily check-in at $formattedTime if you forget to enter milk log.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = dailyReminder,
                        onCheckedChange = {
                            dailyReminder = it
                            val qty = defaultQty.toDoubleOrNull() ?: 1.0
                            val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                            viewModel.saveConfig(qty, rate, it, paymentReminder, themePref, payDay, leadDays, currencyCode, currencySymbol)
                        }
                    )
                }

                if (dailyReminder) {
                    val timePickerDialog = android.app.TimePickerDialog(
                        context,
                        { _, selectedHour, selectedMinute ->
                            dailyReminderHour = selectedHour
                            dailyReminderMinute = selectedMinute
                            val qty = defaultQty.toDoubleOrNull() ?: 1.0
                            val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                            viewModel.saveConfig(
                                qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays, currencyCode, currencySymbol,
                                selectedHour, selectedMinute
                            )
                        },
                        dailyReminderHour,
                        dailyReminderMinute,
                        false
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customize Reminder Time",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { timePickerDialog.show() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Time", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                // Monthly Settle Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Monthly Invoice Reminder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Notify as your customized payment day approaches.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = paymentReminder,
                        onCheckedChange = {
                            paymentReminder = it
                            val qty = defaultQty.toDoubleOrNull() ?: 1.0
                            val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                            viewModel.saveConfig(qty, rate, dailyReminder, it, themePref, payDay, leadDays, currencyCode, currencySymbol)
                        }
                    )
                }

                // Monthly Settle Reminder Configs
                if (paymentReminder) {
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Preferred Settle Day selection
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Preferred Payment Day", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Day $payDay of Month", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = payDay.toFloat(),
                            onValueChange = { payDay = it.toInt() },
                            valueRange = 1f..28f,
                            steps = 26,
                            onValueChangeFinished = {
                                val qty = defaultQty.toDoubleOrNull() ?: 1.0
                                val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                                viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays, currencyCode, currencySymbol)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Lead interval selection
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reminder Lead Interval", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (leadDays == 0) "Settle Day Only" else "$leadDays days before", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = leadDays.toFloat(),
                            onValueChange = { leadDays = it.toInt() },
                            valueRange = 0f..7f,
                            steps = 6,
                            onValueChangeFinished = {
                                val qty = defaultQty.toDoubleOrNull() ?: 1.0
                                val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                                viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays, currencyCode, currencySymbol)
                            }
                        )
                        val triggerExplanation = if (leadDays == 0) {
                            "Will notify exactly on Day $payDay of the month."
                        } else {
                            val startDay = if (payDay - leadDays <= 0) 28 + (payDay - leadDays) else payDay - leadDays
                            "Will notify daily starting on Day $startDay as Day $payDay approaches."
                        }
                        Text(
                            text = triggerExplanation,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }
            }
        }

        // Exports Reports section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var reportSellerFilter by remember { mutableStateOf("ALL") }
                var reportSellerExpanded by remember { mutableStateOf(false) }

                Text(
                    text = "Export Statement ($selectedMonth)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Vendor selector for reports
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter by Vendor:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        TextButton(
                            onClick = { reportSellerExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (reportSellerFilter == "ALL") "All Vendors" else reportSellerFilter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = reportSellerExpanded,
                            onDismissRequest = { reportSellerExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Vendors") },
                                onClick = {
                                    reportSellerFilter = "ALL"
                                    reportSellerExpanded = false
                                }
                            )
                            sellers.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        reportSellerFilter = s.name
                                        reportSellerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val csvFile = viewModel.exportCsvReport(context, sellerFilter = reportSellerFilter)
                            if (csvFile != null) {
                                viewModel.shareReport(context, csvFile, "text/csv")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV Report", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            // Calculate metrics to build report
                            val recordsInMonth = records.filter { it.date.startsWith(selectedMonth) }
                                .filter { if (reportSellerFilter == "ALL") true else it.sellerName.equals(reportSellerFilter, ignoreCase = true) }
                            val takenOnly = recordsInMonth.filter { it.taken }
                            val mQtyRef = takenOnly.sumOf { it.quantity }
                            val mCostRef = takenOnly.sumOf { it.quantity * it.rate }
                            val mPdfFile = viewModel.exportPdfReport(
                                context = context,
                                totalLitres = mQtyRef,
                                totalExpense = mCostRef,
                                milkDays = takenOnly.size,
                                leaveDays = recordsInMonth.size - takenOnly.size,
                                sellerFilter = reportSellerFilter
                            )
                            if (mPdfFile != null) {
                                viewModel.shareReport(context, mPdfFile, "application/pdf")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Invoice", fontSize = 12.sp)
                    }
                }
            }
        }

        // Backup and Restore card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Backup & Recovery Services",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Copy backup payload structures to clone across devices, or load previously saved states below.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val backupStr = viewModel.getBackupString()
                            clipboardManager.setText(AnnotatedString(backupStr))
                            Toast.makeText(context, "Backup JSON payload copied to clipboard!", Toast.LENGTH_LONG).show()
                            showBackupBox = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Backup", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showRestoreBox = !showRestoreBox },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Input", fontSize = 12.sp)
                    }
                }

                if (showBackupBox) {
                    val currentBackStr = viewModel.getBackupString().take(300) + "..."
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("JSON Schema Generated Sample:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(currentBackStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                if (showRestoreBox) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = textToRestore,
                            onValueChange = { textToRestore = it },
                            placeholder = { Text("Paste JSON payload here...") },
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )

                        Button(
                            onClick = {
                                if (textToRestore.isNotBlank()) {
                                    viewModel.restoreBackup(textToRestore)
                                    textToRestore = ""
                                    showRestoreBox = false
                                } else {
                                    Toast.makeText(context, "Input code is blank", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorYes),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Database Now")
                        }
                    }
                }
            }
        }

        // Standard Seeding card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Standard 2026 Records",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Instantly pre-populate all days in 2026 up to April with 1 Litre @ 80.0 rate/L (all set as 'taken'), and all days of May 2026 with 1 Litre @ 90.0/L.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        viewModel.populateStandard2026Data()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Standard 2026 Dataset")
                }
            }
        }

        // Danger block
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Danger Zone",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorNo
                )
                Text(
                    text = "Wiping data clears all previously saved milk entries and resets preferences. This action is permanent.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Button(
                    onClick = { showWipeConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorNo),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Database Data", color = Color.White)
                }
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Confirm Wipeout", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Are you absolutely sure you want to delete all entries and restore default configurations? This action in irrevocable.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ColorNo),
                    onClick = {
                        viewModel.clearAllUserData()
                        defaultQty = "1.0"
                        defaultRateByLitre = "40.0"
                        dailyReminder = true
                        paymentReminder = true
                        showWipeConfirm = false
                    }
                ) {
                    Text("Wipe Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}
