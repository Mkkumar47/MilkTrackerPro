package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MilkRecord
import com.example.ui.components.ColorNo
import com.example.ui.components.ColorYes
import com.example.viewmodel.MilkViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: MilkViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.recordsFlow.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val config by viewModel.configFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, YES, NO
    var sellerFilter by remember { mutableStateOf("ALL") }

    var showEditDialog by remember { mutableStateOf(false) }
    var dialogDate by remember { mutableStateOf("") }
    var dialogTaken by remember { mutableStateOf(true) }
    var dialogQty by remember { mutableStateOf("1.0") }
    var dialogRate by remember { mutableStateOf("40.0") }
    var dialogNotes by remember { mutableStateOf("") }

    var entryType by remember { mutableStateOf("Single") }
    var dialogEndDate by remember { mutableStateOf("") }
    var dialogSession by remember { mutableStateOf("Morning") }
    var dialogSellerName by remember { mutableStateOf("") }
    var dialogMilkType by remember { mutableStateOf("Cow Milk") }

    val sellers by viewModel.sellersFlow.collectAsState()
    var sellerExpanded by remember { mutableStateOf(false) }

    var showAddSellerDialog by remember { mutableStateOf(false) }
    var newSellerName by remember { mutableStateOf("") }
    var newSellerPhone by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(dialogSellerName, dialogMilkType) {
        val activeSeller = sellers.find { it.name == dialogSellerName }
        if (activeSeller != null) {
            val matchingRate = if (dialogMilkType == "Cow Milk") activeSeller.cowRate else activeSeller.buffaloRate
            if (matchingRate > 0.0) {
                dialogRate = matchingRate.toString()
            }
        }
    }

    // Filter list matches current selected month
    val pageRecords = remember(records, selectedMonth, searchQuery, statusFilter, sellerFilter) {
        records.filter { it.date.startsWith(selectedMonth) }
            .filter { rec ->
                if (statusFilter == "YES") rec.taken
                else if (statusFilter == "NO") !rec.taken
                else true
            }
            .filter { rec ->
                if (sellerFilter == "ALL") true
                else rec.sellerName.equals(sellerFilter, ignoreCase = true)
            }
            .filter { rec ->
                rec.notes.contains(searchQuery, ignoreCase = true) || rec.date.contains(searchQuery)
            }
            .sortedByDescending { it.date }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            // Header Titles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dairy Ledger",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${getMonthDisplayName(selectedMonth)} • ${pageRecords.size} records",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Month cycler buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val prev = selectAdjacentMonth(selectedMonth, -1)
                            viewModel.selectMonth(prev)
                        }
                    ) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Prev month", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            val next = selectAdjacentMonth(selectedMonth, 1)
                            viewModel.selectMonth(next)
                        }
                    ) {
                        Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Next month", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search comments, dates...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )

            // Filtering Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text("All Days") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = statusFilter == "YES",
                    onClick = { statusFilter = "YES" },
                    label = { Text("Taken (Yes)") },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedLabelColor = ColorYes, selectedContainerColor = ColorYes.copy(alpha = 0.1f))
                )
                FilterChip(
                    selected = statusFilter == "NO",
                    onClick = { statusFilter = "NO" },
                    label = { Text("Leaves (No)") },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedLabelColor = ColorNo, selectedContainerColor = ColorNo.copy(alpha = 0.1f))
                )
            }

            // Vendor Filtering Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vendor Filter:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                FilterChip(
                    selected = sellerFilter == "ALL",
                    onClick = { sellerFilter = "ALL" },
                    label = { Text("All") },
                    shape = RoundedCornerShape(8.dp)
                )
                sellers.forEach { seller ->
                    FilterChip(
                        selected = sellerFilter == seller.name,
                        onClick = { sellerFilter = seller.name },
                        label = { Text(seller.name) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Records List View
            if (pageRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No recorded journal entries.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Tap the '+' floating button to log entry.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 85.dp)
                ) {
                    items(pageRecords) { item ->
                        LedgerItemRow(
                            record = item,
                            onClick = {
                                dialogDate = item.date
                                dialogEndDate = item.date
                                entryType = "Single"
                                dialogTaken = item.taken
                                dialogQty = item.quantity.toString()
                                dialogRate = item.rate.toString()
                                dialogNotes = item.notes
                                dialogSession = item.session
                                dialogSellerName = item.sellerName
                                dialogMilkType = item.milkType
                                showEditDialog = true
                            },
                            currencySymbol = config.currencySymbol
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                dialogDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                dialogEndDate = dialogDate
                entryType = "Single"
                dialogTaken = true
                dialogQty = config.defaultQuantity.toString()
                dialogRate = config.defaultRate.toString()
                dialogNotes = ""
                dialogSession = "Morning"
                dialogSellerName = sellers.firstOrNull()?.name ?: ""
                dialogMilkType = "Cow Milk"
                showEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 75.dp, end = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Log Diary Entry", modifier = Modifier.size(28.dp))
        }
    }

    if (showAddSellerDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddSellerDialog = false 
                newSellerName = ""
                newSellerPhone = ""
            },
            title = {
                Text("Add New Seller", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSellerName,
                        onValueChange = { newSellerName = it },
                        label = { Text("Seller Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newSellerPhone,
                        onValueChange = { newSellerPhone = it },
                        label = { Text("Phone Number (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSellerName.isNotBlank()) {
                            viewModel.saveSeller(
                                name = newSellerName,
                                phone = newSellerPhone,
                                cowRate = dialogRate.toDoubleOrNull() ?: 40.0,
                                buffaloRate = dialogRate.toDoubleOrNull() ?: 40.0,
                                onSuccess = { generatedId ->
                                    dialogSellerName = newSellerName.trim()
                                }
                            )
                            showAddSellerDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSellerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Edit/Creation Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = if (entryType == "Range") "Add Milk Entry (Range)" else "Add Milk Entry",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ENTRY TYPE: Single vs. Range
                    Column {
                        Text(
                            text = "Entry Type",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { entryType = "Single" }
                            ) {
                                RadioButton(
                                    selected = (entryType == "Single"),
                                    onClick = { entryType = "Single" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Single Entry", fontSize = 14.sp)
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { entryType = "Range" }
                            ) {
                                RadioButton(
                                    selected = (entryType == "Range"),
                                    onClick = { entryType = "Range" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Date Range", fontSize = 14.sp)
                            }
                        }
                    }

                    // DATE / RANGE DISPLAY & PICKER
                    if (entryType == "Single") {
                        Column {
                            Text(
                                text = "Delivery Date",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDatePicker(dialogDate, context) { selectedDate ->
                                            dialogDate = selectedDate
                                        }
                                    }
                            ) {
                                OutlinedTextField(
                                    value = formatToDisplayDate(dialogDate),
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        Icon(Icons.Filled.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "From Date",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showDatePicker(dialogDate, context) { selectedDate ->
                                                dialogDate = selectedDate
                                            }
                                        }
                                ) {
                                    OutlinedTextField(
                                        value = formatToDisplayDate(dialogDate),
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Icon(Icons.Filled.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                                            disabledContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "To Date",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showDatePicker(dialogEndDate, context) { selectedDate ->
                                                dialogEndDate = selectedDate
                                            }
                                        }
                                ) {
                                    OutlinedTextField(
                                        value = formatToDisplayDate(dialogEndDate),
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Icon(Icons.Filled.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                                            disabledContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // SESSION: Morning vs. Evening
                    Column {
                        Text(
                            text = "Session",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { dialogSession = "Morning" }
                            ) {
                                RadioButton(
                                    selected = (dialogSession == "Morning"),
                                    onClick = { dialogSession = "Morning" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Morning", fontSize = 14.sp)
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { dialogSession = "Evening" }
                            ) {
                                RadioButton(
                                    selected = (dialogSession == "Evening"),
                                    onClick = { dialogSession = "Evening" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Evening", fontSize = 14.sp)
                            }
                        }
                    }

                    // SELLER DROPDOWN & ADD BUTTON
                    Column {
                        Text(
                            text = "Seller",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dialogSellerName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Seller") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    IconButton(onClick = { sellerExpanded = true }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            DropdownMenu(
                                expanded = sellerExpanded,
                                onDismissRequest = { sellerExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                if (sellers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No Sellers. Click 'Add New Seller' below!") },
                                        onClick = { sellerExpanded = false }
                                    )
                                } else {
                                    sellers.forEach { seller ->
                                        DropdownMenuItem(
                                            text = { Text(seller.name) },
                                            onClick = {
                                                dialogSellerName = seller.name
                                                sellerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedButton(
                            onClick = { showAddSellerDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(text = "Add New Seller", fontWeight = FontWeight.Bold)
                        }
                    }

                    // MILK TYPE: Cow Milk vs. Buffalo Milk
                    Column {
                        Text(
                            text = "Milk Type",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { dialogMilkType = "Cow Milk" }
                            ) {
                                RadioButton(
                                    selected = (dialogMilkType == "Cow Milk"),
                                    onClick = { dialogMilkType = "Cow Milk" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cow Milk", fontSize = 14.sp)
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { dialogMilkType = "Buffalo Milk" }
                            ) {
                                RadioButton(
                                    selected = (dialogMilkType == "Buffalo Milk"),
                                    onClick = { dialogMilkType = "Buffalo Milk" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buffalo Milk", fontSize = 14.sp)
                            }
                        }
                    }

                    // YES / NO Slider Switch Buttons (MILK RECEIVED vs ON LEAVE)
                    Column {
                        Text(
                            text = "Status",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { dialogTaken = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dialogTaken) ColorYes else ColorYes.copy(alpha = 0.15f),
                                    contentColor = if (dialogTaken) Color.White else ColorYes
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = if (!dialogTaken) BorderStroke(1.dp, ColorYes.copy(alpha = 0.6f)) else null
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("DELIVERED", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = { dialogTaken = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!dialogTaken) ColorNo else ColorNo.copy(alpha = 0.15f),
                                    contentColor = if (!dialogTaken) Color.White else ColorNo
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = if (dialogTaken) BorderStroke(1.dp, ColorNo.copy(alpha = 0.6f)) else null
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ON LEAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (dialogTaken) {
                        // Quick Add (Liters)
                        Column {
                            Text(
                                text = "Quick Add (Liters):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val quickAddOptions = listOf(
                                    "250 ml" to "0.25",
                                    "500 ml" to "0.5",
                                    "750 ml" to "0.75",
                                    "1 L" to "1.0",
                                    "1.5 L" to "1.5"
                                )
                                quickAddOptions.forEach { (label, value) ->
                                    val isSelected = dialogQty == value
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                dialogQty = value
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quantity (Liters) text field
                        Column {
                            Text(
                                text = "Quantity (Liters)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dialogQty,
                                onValueChange = { dialogQty = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                placeholder = { Text("0.0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pricing Field
                        Column {
                            Text(
                                text = "Rate (per Liter)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dialogRate,
                                onValueChange = { dialogRate = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                leadingIcon = {
                                    Text(
                                        text = config.currencySymbol,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 12.dp, end = 2.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    // Leave reason or customizable notes
                    Column {
                        Text(
                            text = if (dialogTaken) "Notes (Optional)" else "Reason for Leave",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = dialogNotes,
                            onValueChange = { dialogNotes = it },
                            placeholder = {
                                Text(
                                    text = if (dialogTaken) "Add notes..." else "e.g., Off-town / Holiday / Festivity",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = dialogQty.toDoubleOrNull() ?: 1.0
                        val rate = dialogRate.toDoubleOrNull() ?: 40.0
                        if (entryType == "Single") {
                            viewModel.saveRecord(
                                date = dialogDate,
                                taken = dialogTaken,
                                quantity = qty,
                                rate = rate,
                                notes = dialogNotes,
                                session = dialogSession,
                                sellerName = dialogSellerName,
                                milkType = dialogMilkType
                            )
                        } else {
                            viewModel.saveRecordRange(
                                startDate = dialogDate,
                                endDate = dialogEndDate,
                                taken = dialogTaken,
                                quantity = qty,
                                rate = rate,
                                notes = dialogNotes,
                                session = dialogSession,
                                sellerName = dialogSellerName,
                                milkType = dialogMilkType
                            )
                        }
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }
                    if (entryType == "Single") {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(contentColor = ColorNo),
                            onClick = {
                                viewModel.deleteRecord(dialogDate)
                                showEditDialog = false
                            }
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun LedgerItemRow(
    record: MilkRecord,
    onClick: () -> Unit,
    currencySymbol: String = "$"
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Indicator Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (record.taken) ColorYes.copy(alpha = 0.15f) else ColorNo.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (record.taken) Icons.Filled.WaterDrop else Icons.Filled.Block,
                    contentDescription = null,
                    tint = if (record.taken) ColorYes else ColorNo,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Central details section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatFriendlyDate(record.date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (record.taken) {
                        Text(
                            text = "${record.quantity} L @ ${currencySymbol}${record.rate}/L",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No delivery",
                            fontSize = 11.sp,
                            color = ColorNo,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (record.notes.isNotBlank()) {
                    Text(
                        text = "Notes: ${record.notes}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                        fontWeight = FontWeight.Light
                    )
                }
            }

            // Calculations section (price tag)
            if (record.taken) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%s%.2f", currencySymbol, record.totalExpense),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorYes
                    )
                    Text(
                        text = "Total Cost",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "${currencySymbol}0.00",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Friendly formatting: e.g. "Wed, May 27"
private fun formatFriendlyDate(dateStr: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)
        val date = input.parse(dateStr)
        if (date != null) output.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

private fun selectAdjacentMonth(current: String, offset: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(current) ?: Date()
        cal.add(Calendar.MONTH, offset)
        sdf.format(cal.time)
    } catch (e: Exception) {
        current
    }
}

private fun formatToDisplayDate(yyyyMmDd: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val date = input.parse(yyyyMmDd)
        if (date != null) output.format(date) else yyyyMmDd
    } catch (e: Exception) {
        yyyyMmDd
    }
}

private fun showDatePicker(
    currentDateStr: String,
    context: android.content.Context,
    onDateSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    try {
        val parsedDate = sdf.parse(currentDateStr)
        if (parsedDate != null) {
            calendar.time = parsedDate
        }
    } catch (e: Exception) {
    }
    
    val dpd = android.app.DatePickerDialog(
        context,
        { _, y, m, d ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            onDateSelected(formatted)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    dpd.show()
}
