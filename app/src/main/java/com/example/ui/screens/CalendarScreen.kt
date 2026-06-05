package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
fun CalendarScreen(
    viewModel: MilkViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.recordsFlow.collectAsState()
    val payments by viewModel.paymentsFlow.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val config by viewModel.configFlow.collectAsState()

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

    LaunchedEffect(dialogSellerName, dialogMilkType) {
        val activeSeller = sellers.find { it.name == dialogSellerName }
        if (activeSeller != null) {
            val matchingRate = if (dialogMilkType == "Cow Milk") activeSeller.cowRate else activeSeller.buffaloRate
            if (matchingRate > 0.0) {
                dialogRate = matchingRate.toString()
            }
        }
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Derive calendar days list
    val calendarDaysList = remember(selectedMonth) {
        deriveMonthDays(selectedMonth)
    }

    val firstDayOffset = remember(selectedMonth) {
        deriveFirstDayOfWeekOffset(selectedMonth)
    }

    // Dynamic high-fidelity Stats computation for the active selected month
    val monthRecords = remember(records, selectedMonth) {
        records.filter { it.date.startsWith(selectedMonth) }
    }
    val totalLitres = remember(monthRecords) {
        monthRecords.filter { it.taken }.sumOf { it.quantity }
    }
    val totalExpense = remember(monthRecords) {
        monthRecords.filter { it.taken }.sumOf { it.quantity * it.rate }
    }
    val milkDays = remember(monthRecords) {
        monthRecords.count { it.taken }
    }
    val leaveDays = remember(monthRecords) {
        monthRecords.count { !it.taken }
    }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Premium Typography and Visual Header Bar
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Delivery Calendar",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Tap any date to log or update entries",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Glassmorphic Month Switcher
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = borderStrokeLight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val prev = getNeighboringMonth(selectedMonth, -1)
                        viewModel.selectMonth(prev)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.background,
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = getMonthDisplayName(selectedMonth),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedMonth.take(4),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                }

                IconButton(
                    onClick = {
                        val next = getNeighboringMonth(selectedMonth, +1)
                        viewModel.selectMonth(next)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.background,
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // High-Fidelity Stats Summary Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStatsCard(
                modifier = Modifier.weight(1f),
                title = "Total Milk",
                value = "${String.format(Locale.US, "%.1f", totalLitres)}L",
                subTitle = "$milkDays Active delivers",
                icon = Icons.Rounded.WaterDrop,
                tintColor = ColorYes
            )
            MiniStatsCard(
                modifier = Modifier.weight(1.1f),
                title = "Est. Bill",
                value = "${config.currencySymbol}${String.format(Locale.US, "%.2f", totalExpense)}",
                subTitle = "$leaveDays Leave Days",
                icon = Icons.Rounded.Payments,
                tintColor = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday headers styled cleanly in uppercase
        val daysOfWeek = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Calendar Grid System represented inside a scrollable column of rows
        val totalCells = remember(firstDayOffset, calendarDaysList) {
            List(firstDayOffset) { null as Int? } + calendarDaysList.map { it as Int? }
        }
        val gridRows = remember(totalCells) {
            totalCells.chunked(7)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gridRows.forEach { rowDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowDays.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            if (day != null) {
                                val dateStr = String.format(Locale.US, "%s-%02d", selectedMonth, day)
                                val record = records.find { it.date == dateStr }
                                val isToday = (dateStr == todayDateStr)

                                CalendarCell(
                                    day = day,
                                    record = record,
                                    isToday = isToday,
                                    onClick = {
                                        dialogDate = dateStr
                                        dialogEndDate = dateStr
                                        entryType = "Single"
                                        if (record != null) {
                                            dialogTaken = record.taken
                                            dialogQty = record.quantity.toString()
                                            dialogRate = record.rate.toString()
                                            dialogNotes = record.notes
                                            dialogSession = record.session
                                            dialogSellerName = record.sellerName
                                            dialogMilkType = record.milkType
                                        } else {
                                            dialogTaken = true
                                            dialogQty = config.defaultQuantity.toString()
                                            dialogRate = config.defaultRate.toString()
                                            dialogNotes = ""
                                            dialogSession = "Morning"
                                            dialogSellerName = sellers.firstOrNull()?.name ?: ""
                                            dialogMilkType = "Cow Milk"
                                        }
                                        showEditDialog = true
                                    }
                                )
                            }
                        }
                    }
                    if (rowDays.size < 7) {
                        repeat(7 - rowDays.size) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color Legends
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = borderStrokeLight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = ColorNo, label = "Leave (NO)")
                LegendIndicator(color = ColorYes, label = "Milk Taken")
                LegendIndicator(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    label = "Not Logged"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CARD 1: Selected Month Summary (E.g. April 2026 Summary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${getMonthDisplayName(selectedMonth)} ${selectedMonth.take(4)} Summary",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                SummaryFieldRow(label = "Total Days:", value = "$milkDays")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

                SummaryFieldRow(label = "Total Quantity:", value = String.format(Locale.US, "%.2f L", totalLitres))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

                val avgMilkPerDay = if (milkDays > 0) totalLitres / milkDays else 0.0
                SummaryFieldRow(label = "Average Milk/Day:", value = String.format(Locale.US, "%.2f L", avgMilkPerDay))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

                SummaryFieldRow(label = "Total:", value = "${config.currencySymbol}${String.format(Locale.US, "%.2f", totalExpense)}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

                val monthPayments = payments.filter { it.date.startsWith(selectedMonth) }
                val monthPaid = monthPayments.sumOf { it.amount }
                SummaryFieldRow(label = "Paid:", value = "${config.currencySymbol}${String.format(Locale.US, "%.2f", monthPaid)}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

                val monthPending = totalExpense - monthPaid
                SummaryFieldRow(
                    label = "Pending:",
                    value = "${config.currencySymbol}${String.format(Locale.US, "%.2f", monthPending)}",
                    valueColor = Color(0xFF00BFA5) // Cyan/teal color matching mock image
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CARD 2: Overall Summary (All Time)
        val allTimeRecords = records.filter { it.taken }
        val allTimeMilkAmount = allTimeRecords.sumOf { it.quantity * it.rate }
        val allTimePaid = payments.sumOf { it.amount }
        val allTimePending = allTimeMilkAmount - allTimePaid

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Overall Summary (All Time)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Milk Amount:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${config.currencySymbol}${String.format(Locale.US, "%.2f", allTimeMilkAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Paid:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${config.currencySymbol}${String.format(Locale.US, "%.2f", allTimePaid)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BFA5) // Beautiful cyan/teal from the mockup
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF0F2), RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFFFD5D9),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thick red vertical bar on left edge
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(30.dp)
                            .background(Color(0xFFD32F2F), RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Pending:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${config.currencySymbol}${String.format(Locale.US, "%.2f", allTimePending)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Milk purchased count centered subtitle from mockup
        Text(
            text = "Milk purchased on $milkDays days this month.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        Spacer(modifier = Modifier.height(85.dp))
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

    // Modal Sheet or Card Dialog for logging/editing entries
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Rounded.EditCalendar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (entryType == "Range") "Add Milk Entry (Range)" else "Add Milk Entry",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
                                text = "Date",
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
                                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

                    // YES / NO Slider Switch Buttons (MILK DELIVERED vs ON LEAVE)
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
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

// Attractive Squircle Calendar Cell Composable
@Composable
fun CalendarCell(
    day: Int,
    record: MilkRecord?,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val dayBrush = when {
        record == null -> Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
        )
        record.taken -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF67BE6E).copy(alpha = 0.85f),
                Color(0xFF67BE6E)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEA4335).copy(alpha = 0.85f),
                Color(0xFFB71C1C)
            )
        )
    }

    val cellShape = RoundedCornerShape(13.dp)

    // Today glow configuration using dynamic gradients
    val cellModifier = if (isToday) {
        Modifier
            .aspectRatio(1f)
            .shadow(4.dp, cellShape, clip = false)
            .background(dayBrush, cellShape)
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                ),
                shape = cellShape
            )
            .clickable { onClick() }
    } else {
        Modifier
            .aspectRatio(1f)
            .background(dayBrush, cellShape)
            .border(
                width = 1.dp,
                color = if (record == null) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f) else Color.Transparent,
                shape = cellShape
            )
            .clickable { onClick() }
    }

    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            // Little dot if note exists
            Box(modifier = Modifier.size(6.dp)) {
                if (record != null && record.notes.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color.White, CircleShape)
                            .align(Alignment.Center)
                    )
                }
            }

            // Central Calendar Day Text
            Text(
                text = day.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (record != null) Color.White else MaterialTheme.colorScheme.onSurface
            )

            // Inner visual tag
            if (record != null) {
                Text(
                    text = if (record.taken) "${String.format(Locale.US, "%.1f", record.quantity)}L" else "OFF",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (record.taken) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.85f),
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.size(10.dp))
            }
        }
    }
}

// Minimal Beautiful Legend Indicator
@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

// Glass-morphic mini statistic card
@Composable
fun MiniStatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subTitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = borderStrokeLight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        tintColor.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subTitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    }
}

// Utility light border helper
@Composable
private fun borderStrokeLight(strokeColor: Color? = null): androidx.compose.foundation.BorderStroke {
    val finalColor = strokeColor ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    return androidx.compose.foundation.BorderStroke(1.dp, finalColor)
}

// Helper date functions
private fun deriveMonthDays(yearMonth: String): List<Int> {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(yearMonth) ?: Date()
        val mMax = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        (1..mMax).toList()
    } catch (e: Exception) {
        (1..30).toList()
    }
}

private fun deriveFirstDayOfWeekOffset(yearMonth: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse("$yearMonth-01") ?: Date()
        val firstDay = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        firstDay - 1
    } catch (e: Exception) {
        0
    }
}

private fun getNeighboringMonth(current: String, offset: Int): String {
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

@Composable
fun SummaryFieldRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
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

