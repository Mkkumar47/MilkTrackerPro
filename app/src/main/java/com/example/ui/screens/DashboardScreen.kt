package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MilkRecord
import com.example.ui.components.*
import com.example.viewmodel.MilkViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MilkViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.recordsFlow.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val configState by viewModel.configFlow.collectAsState()

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Dynamically calculate Month stats
    val recordsInMonth = records.filter { it.date.startsWith(selectedMonth) }
    val milkTakenDays = recordsInMonth.filter { it.taken }
    val leaveDays = recordsInMonth.filter { !it.taken }

    val totalLitres = milkTakenDays.sumOf { it.quantity }
    val totalExpense = milkTakenDays.sumOf { it.quantity * it.rate }
    val leaveDaysCount = leaveDays.size
    val averageDaily = if (milkTakenDays.isNotEmpty()) totalLitres / milkTakenDays.size else 0.0

    // Month-over-month comparison
    val comparisonText = remember(selectedMonth, records, configState.currencySymbol) {
        calculateMoMComparison(selectedMonth, records, configState.currencySymbol)
    }

    // Peak month estimation
    val peakMonthLabel = remember(selectedYear, records) {
        calculatePeakMonth(selectedYear, records)
    }

    // Prepare data arrays for canvas graphs
    val barGraphData = remember(selectedYear, records) {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val data = months.mapIndexed { idx, name ->
            val monthKey = String.format(Locale.US, "%s-%02d", selectedYear, idx + 1)
            val monthLitres = records.filter { it.date.startsWith(monthKey) && it.taken }.sumOf { it.quantity }
            Pair(name, monthLitres)
        }
        data
    }

    val expenseTrendData = remember(selectedYear, records) {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val data = months.mapIndexed { idx, name ->
            val monthKey = String.format(Locale.US, "%s-%02d", selectedYear, idx + 1)
            val monthExpense = records.filter { it.date.startsWith(monthKey) && it.taken }.sumOf { it.quantity * it.rate }
            Pair(name, monthExpense)
        }
        data
    }

    // Filter selectors setup
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    val availableYears = listOf("2024", "2025", "2026", "2027", "2028")
    val availableMonths = (1..12).map { String.format(Locale.US, "%s-%02d", selectedYear, it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Header Block Layout conforming to design spec
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val displayName = configState.googleUserName ?: "User"
                Text(
                    text = "Welcome, $displayName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "MilkTrack Pro",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val file = viewModel.exportPdfReport(context, totalLitres, totalExpense, milkTakenDays.size, leaveDaysCount)
                        if (file != null) viewModel.shareReport(context, file, "application/pdf")
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Filled.PictureAsPdf,
                        contentDescription = "Export pdf statement",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                val initials = remember(configState.googleUserName) {
                    val rawName = configState.googleUserName
                    if (rawName.isNullOrBlank()) {
                        "U"
                    } else {
                        val parts = rawName.trim().split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            "${parts[0].take(1).uppercase()}${parts[1].take(1).uppercase()}"
                        } else {
                            rawName.take(2).uppercase()
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                        .clickable {
                            val accountInfo = if (configState.isGoogleSignedIn) {
                                "${configState.googleUserName} (${configState.googleEmail})"
                            } else {
                                "Guest Mode"
                            }
                            Toast.makeText(context, "Logged in as $accountInfo", Toast.LENGTH_LONG).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Year and Month Filters card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Year Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedCard(
                    onClick = { yearMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Year Filter", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedYear, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Expand Year Selection", modifier = Modifier.size(20.dp))
                    }
                }
                DropdownMenu(
                    expanded = yearMenuExpanded,
                    onDismissRequest = { yearMenuExpanded = false }
                ) {
                    availableYears.forEach { yr ->
                        DropdownMenuItem(
                            text = { Text(yr) },
                            onClick = {
                                viewModel.selectYear(yr)
                                viewModel.selectMonth("$yr-${selectedMonth.substringAfter("-")}")
                                yearMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Month Dropdown
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedCard(
                    onClick = { monthMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Selected Month", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(getMonthDisplayName(selectedMonth), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Expand Month Selection", modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false }
                ) {
                    availableMonths.forEach { mth ->
                        DropdownMenuItem(
                            text = { Text(getMonthDisplayName(mth)) },
                            onClick = {
                                viewModel.selectMonth(mth)
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Primary Summary Card styled exactly with #006495 matching Professional Polish
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF006495)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Semi-translucent decorative shape overlay for Material 3 depth
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 30.dp, y = (-40).dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(60.dp))
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "${getMonthDisplayName(selectedMonth)} Expenses",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%s%.2f", configState.currencySymbol, totalExpense),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Total Volume",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f Litres", totalLitres),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Avg. Daily",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "%.2fL / day", averageDaily),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leave Days Card
            Card(
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LEAVE DAYS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%02d", leaveDaysCount),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorNo
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "This Month",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Unit Price Card
            Card(
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "UNIT PRICE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%s%.2f", configState.currencySymbol, configState.defaultRate),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Per Litre",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Extra dynamic stats block: Peak Month and MOM Change
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Smart Analytics Insights",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comparisonText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = ColorYes, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Year $selectedYear Peak Month: $peakMonthLabel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Charts Display Section
        YesNoPieChart(yesDays = milkTakenDays.size, noDays = leaveDaysCount)

        MonthlyConsumptionBarGraph(monthlyLitres = barGraphData)

        YearlyExpenseTrendChart(monthlyExpenseList = expenseTrendData, currencySymbol = configState.currencySymbol)

        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// MoM calculator
private fun calculateMoMComparison(currentMonth: String, allRecords: List<MilkRecord>, currencySymbol: String): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        val curDate = sdf.parse(currentMonth) ?: return "Inconclusive historical comparison"
        cal.time = curDate
        cal.add(Calendar.MONTH, -1)
        val prevMonth = sdf.format(cal.time)

        val currentExpense = allRecords.filter { it.date.startsWith(currentMonth) && it.taken }.sumOf { it.quantity * it.rate }
        val prevExpense = allRecords.filter { it.date.startsWith(prevMonth) && it.taken }.sumOf { it.quantity * it.rate }

        if (prevExpense == 0.0) {
            return "No data logged for previous month (${getMonthDisplayName(prevMonth)}) to compare."
        }

        val diff = currentExpense - prevExpense
        val percent = (diff / prevExpense) * 100

        return if (diff >= 0) {
            String.format(
                Locale.US,
                "Expense is ▲ %s%.2f (+%.1f%%) higher than last month (%s).",
                currencySymbol, diff, percent, getMonthDisplayName(prevMonth)
            )
        } else {
            String.format(
                Locale.US,
                "Expense is ▼ %s%.2f (-%.1f%%) lower than last month (%s).",
                currencySymbol, Math.abs(diff), Math.abs(percent), getMonthDisplayName(prevMonth)
            )
        }
    } catch (e: Exception) {
        return "Comparison unavailable"
    }
}

// Peak month calculator
private fun calculatePeakMonth(year: String, allRecords: List<MilkRecord>): String {
    val recordsInYear = allRecords.filter { it.date.startsWith(year) && it.taken }
    if (recordsInYear.isEmpty()) return "No records found"

    val monthlyTotals = recordsInYear.groupBy { it.date.substring(0, 7) }
        .mapValues { (_, recs) -> recs.sumOf { it.quantity } }

    val peakEntry = monthlyTotals.maxByOrNull { it.value } ?: return "Inconclusive"
    return String.format(Locale.US, "%s (%.1f L)", getMonthDisplayName(peakEntry.key), peakEntry.value)
}

// Friendly Display Month string
fun getMonthDisplayName(yearMonth: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        val date = inputFormat.parse(yearMonth)
        if (date != null) outputFormat.format(date) else yearMonth
    } catch (e: Exception) {
        yearMonth
    }
}
