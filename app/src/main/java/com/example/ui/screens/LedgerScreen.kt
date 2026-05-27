package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    var showEditDialog by remember { mutableStateOf(false) }
    var dialogDate by remember { mutableStateOf("") }
    var dialogTaken by remember { mutableStateOf(true) }
    var dialogQty by remember { mutableStateOf("1.0") }
    var dialogRate by remember { mutableStateOf("40.0") }
    var dialogNotes by remember { mutableStateOf("") }

    // Filter list matches current selected month
    val pageRecords = remember(records, selectedMonth, searchQuery, statusFilter) {
        records.filter { it.date.startsWith(selectedMonth) }
            .filter { rec ->
                if (statusFilter == "YES") rec.taken
                else if (statusFilter == "NO") !rec.taken
                else true
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
                                dialogTaken = item.taken
                                dialogQty = item.quantity.toString()
                                dialogRate = item.rate.toString()
                                dialogNotes = item.notes
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                dialogDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                dialogTaken = true
                dialogQty = config.defaultQuantity.toString()
                dialogRate = config.defaultRate.toString()
                dialogNotes = ""
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

    // Modal Edit/Creation Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Save Entry Details",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Date field in Dialog can be customized
                    Column {
                        Text("Delivery Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dialogDate,
                            onValueChange = { dialogDate = it },
                            placeholder = { Text("yyyy-MM-dd") },
                            leadingIcon = { Icon(Icons.Filled.EditCalendar, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Toggles YES/NO
                    Column {
                        Text("Milk Received?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { dialogTaken = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dialogTaken) ColorYes else ColorYes.copy(alpha = 0.2f),
                                    contentColor = if (dialogTaken) Color.White else ColorYes
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("YES", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { dialogTaken = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!dialogTaken) ColorNo else ColorNo.copy(alpha = 0.2f),
                                    contentColor = if (!dialogTaken) Color.White else ColorNo
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("NO", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (dialogTaken) {
                        // Litres input with quick incremental counters
                        Column {
                            Text("Quantity (Litres)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val cur = dialogQty.toDoubleOrNull() ?: 1.0
                                        if (cur > 0.5) dialogQty = String.format(Locale.US, "%.1f", cur - 0.5)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedTextField(
                                    value = dialogQty,
                                    onValueChange = { dialogQty = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Button(
                                    onClick = {
                                        val cur = dialogQty.toDoubleOrNull() ?: 1.0
                                        dialogQty = String.format(Locale.US, "%.1f", cur + 0.5)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Rate cost input
                        Column {
                            Text("Rate Cost (Price/L)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = dialogRate,
                                onValueChange = { dialogRate = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Comments or Notes
                    Column {
                        Text(if (dialogTaken) "Notes" else "Reason for Leave", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dialogNotes,
                            onValueChange = { dialogNotes = it },
                            placeholder = { Text(if (dialogTaken) "Add notes..." else "e.g. Vacation / Double delivery") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = dialogQty.toDoubleOrNull() ?: 1.0
                        val rate = dialogRate.toDoubleOrNull() ?: 40.0
                        viewModel.saveRecord(dialogDate, dialogTaken, qty, rate, dialogNotes)
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = ColorNo),
                        onClick = {
                            viewModel.deleteRecord(dialogDate)
                            showEditDialog = false
                        }
                    ) {
                        Text("Delete")
                    }
                }
            }
        )
    }
}

@Composable
fun LedgerItemRow(
    record: MilkRecord,
    onClick: () -> Unit
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
                            text = "${record.quantity} L @ $${record.rate}/L",
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
                        text = String.format(Locale.US, "$%.2f", record.totalExpense),
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
                    text = "$0.00",
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
