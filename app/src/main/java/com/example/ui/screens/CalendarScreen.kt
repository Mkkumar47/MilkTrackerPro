package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val config by viewModel.configFlow.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var dialogDate by remember { mutableStateOf("") }
    var dialogTaken by remember { mutableStateOf(true) }
    var dialogQty by remember { mutableStateOf("1.0") }
    var dialogRate by remember { mutableStateOf("40.0") }
    var dialogNotes by remember { mutableStateOf("") }

    // Derive calendar days for month yyyy-MM
    val calendarDaysList = remember(selectedMonth) {
        deriveMonthDays(selectedMonth)
    }

    val firstDayOffset = remember(selectedMonth) {
        deriveFirstDayOfWeekOffset(selectedMonth)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        // Title block
        Text(
            text = "Delivery Calendar",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Visual Status Grid & Easy Logger",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Month switcher header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val prev = getNeighboringMonth(selectedMonth, -1)
                    viewModel.selectMonth(prev)
                }
            ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Month")
            }

            Text(
                text = getMonthDisplayName(selectedMonth),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = {
                    val next = getNeighboringMonth(selectedMonth, +1)
                    viewModel.selectMonth(next)
                }
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekdays Headers
        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid Calendar Cells
        val cellCount = firstDayOffset + calendarDaysList.size
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Spacer cells for month padding
            items(firstDayOffset) {
                Box(modifier = Modifier.size(42.dp))
            }

            items(calendarDaysList.size) { index ->
                val day = calendarDaysList[index]
                val dateStr = String.format(Locale.US, "%s-%02d", selectedMonth, day)
                val record = records.find { it.date == dateStr }

                CalendarCell(
                    day = day,
                    record = record,
                    onClick = {
                        dialogDate = dateStr
                        if (record != null) {
                            dialogTaken = record.taken
                            dialogQty = record.quantity.toString()
                            dialogRate = record.rate.toString()
                            dialogNotes = record.notes
                        } else {
                            dialogTaken = true
                            dialogQty = config.defaultQuantity.toString()
                            dialogRate = config.defaultRate.toString()
                            dialogNotes = ""
                        }
                        showEditDialog = true
                    }
                )
            }
        }

        // Indicator Legend block
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = ColorYes, label = "Taken (YES)")
                LegendIndicator(color = ColorNo, label = "Leave (NO)")
                LegendIndicator(color = Color.LightGray, label = "Not Tracked")
            }
        }
        
        Spacer(modifier = Modifier.height(55.dp))
    }

    // Modal Sheet or Card Dialog for editing entries
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Log Diary: $dialogDate",
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
                    // YES / NO Quick Action buttons
                    Column {
                        Text("Milk Delivered?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        // Litres count selection
                        Column {
                            Text("Quantity (Litres)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val curValue = dialogQty.toDoubleOrNull() ?: 1.0
                                        if (curValue > 0.5) {
                                            dialogQty = String.format(Locale.US, "%.1f", curValue - 0.5)
                                        }
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
                                        val curValue = dialogQty.toDoubleOrNull() ?: 1.0
                                        dialogQty = String.format(Locale.US, "%.1f", curValue + 0.5)
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

                        // Pricing Field
                        Column {
                            Text("Rate (Per Litre Cost)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    // Leave reason or customizable notes
                    Column {
                        Text(if (dialogTaken) "Notes (Optional)" else "Reason for Leave", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dialogNotes,
                            onValueChange = { dialogNotes = it },
                            placeholder = { Text(if (dialogTaken) "Add notes..." else "e.g., Vacation/Out of town") },
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
fun CalendarCell(
    day: Int,
    record: MilkRecord?,
    onClick: () -> Unit
) {
    val containerColor = when {
        record == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        record.taken -> ColorYes
        else -> ColorNo
    }

    val textColor = if (record != null) Color.White else MaterialTheme.colorScheme.onSurface

    val borderModifier = if (record == null) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
    } else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(45.dp)
            .background(containerColor, CircleShape)
            .then(borderModifier)
            .clickable { onClick() }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            // Little dot if note exists
            if (record != null && record.notes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Helper time functions
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
