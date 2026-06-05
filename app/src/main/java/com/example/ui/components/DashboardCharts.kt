package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// Vibrant UI colors matching beautiful design theme
val ColorYes = Color(0xFF67BE6E) // Gentle Leaf Green
val ColorNo = Color(0xFFE53935)  // Material Red
val ColorAccent = Color(0xFF006495) // Professional Polish Navy Blue
val ColorChartGrid = Color(0x339E9E9E)
val ColorBarBackground = Color(0xFFD0BCFF) // Accent Lavender for background track

@Composable
fun YesNoPieChart(
    yesDays: Int,
    noDays: Int,
    modifier: Modifier = Modifier
) {
    val total = yesDays + noDays
    val yesPercentage = if (total > 0) (yesDays.toFloat() / total * 100) else 0f
    val noPercentage = if (total > 0) (noDays.toFloat() / total * 100) else 0f

    // Animating percentages for fluid presentation
    var targetAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(yesDays, noDays) {
        targetAngle = if (total > 0) (360f * (yesDays.toFloat() / total)) else 0f
    }
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 800),
        label = "PieChartAngle"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Attendance Breakdown (YES vs NO)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Pie Canvas Drawing
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 24.dp.toPx()
                        val diameter = size.minDimension - stroke
                        val arcSize = Size(diameter, diameter)
                        val offset = Offset(stroke / 2, stroke / 2)

                        // Base slice for No tracking
                        drawArc(
                            color = ColorNo.copy(alpha = 0.2f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )

                        // Main No Slice
                        drawArc(
                            color = ColorNo,
                            startAngle = -90f + animatedAngle,
                            sweepAngle = 360f - animatedAngle,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        // Main Yes Slice
                        drawArc(
                            color = ColorYes,
                            startAngle = -90f,
                            sweepAngle = animatedAngle,
                            useCenter = false,
                            topLeft = offset,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }

                    // Inner summary label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (total > 0) "${yesPercentage.toInt()}%" else "0%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorYes
                        )
                        Text(
                            text = "Yes Ratio",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Legend layout
                Column(verticalArrangement = Arrangement.Center) {
                    LegendItem(color = ColorYes, title = "YES / Milk Days", value = "$yesDays Days (${yesPercentage.toInt()}%)")
                    Spacer(modifier = Modifier.height(10.dp))
                    LegendItem(color = ColorNo, title = "NO / Leave Days", value = "$noDays Days (${noPercentage.toInt()}%)")
                }
            }
        }
    }
}

@Composable
fun MonthlyConsumptionBarGraph(
    monthlyLitres: List<Pair<String, Double>>, // e.g., Pair("Jan", 30.5)
    modifier: Modifier = Modifier
) {
    val maxQty = (monthlyLitres.maxOfOrNull { it.second } ?: 10.0).coerceAtLeast(10.0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Monthly Consumption Trend (Litres)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomPadding = 30f
                    val leftPadding = 50f
                    val gridHeight = canvasHeight - bottomPadding
                    val gridWidth = canvasWidth - leftPadding

                    // Draw baseline gridlines & markers
                    val gridSegments = 4
                    for (i in 0..gridSegments) {
                        val y = gridHeight - (gridHeight * i / gridSegments)
                        drawLine(
                            color = ColorChartGrid,
                            start = Offset(leftPadding, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f
                        )
                    }

                    // Plot bars for each month
                    val numBars = monthlyLitres.size
                    val spacing = gridWidth / (numBars + 1)

                    monthlyLitres.forEachIndexed { index, (month, qty) ->
                        val barWidth = (spacing * 0.55f).coerceIn(10f, 40f)
                        val x = leftPadding + spacing * (index + 1) - (barWidth / 2f)
                        val barHeight = (qty.toFloat() / maxQty.toFloat()) * gridHeight

                        // Draw background track
                        drawRoundRect(
                            color = ColorBarBackground.copy(alpha = 0.35f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, gridHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                        )

                        // Draw active volume bar
                        drawRoundRect(
                            color = ColorAccent,
                            topLeft = Offset(x, gridHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                        )
                    }
                }

                // Month labels below the canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(start = 18.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    monthlyLitres.forEach { (month, _) ->
                        Text(
                            text = month,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YearlyExpenseTrendChart(
    monthlyExpenseList: List<Pair<String, Double>>, // e.g., ("Jan", 120.0)
    modifier: Modifier = Modifier,
    currencySymbol: String = "$"
) {
    val maxExpense = (monthlyExpenseList.maxOfOrNull { it.second } ?: 100.0).coerceAtLeast(100.0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Yearly Expense Curve ($currencySymbol)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val bottomPadding = 25f
                    val leftPadding = 45f
                    val chartHeight = canvasHeight - bottomPadding
                    val chartWidth = canvasWidth - leftPadding

                    // Gridlines
                    val segments = 3
                    for (i in 0..segments) {
                        val y = chartHeight - (chartHeight * i / segments)
                        drawLine(
                            color = ColorChartGrid,
                            start = Offset(leftPadding, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f
                        )
                    }

                    if (monthlyExpenseList.isNotEmpty()) {
                        val pointsCount = monthlyExpenseList.size
                        val spacing = chartWidth / (pointsCount - 1).coerceAtLeast(1)
                        
                        val path = Path()
                        val pointsCoordinates = mutableListOf<Offset>()

                        monthlyExpenseList.forEachIndexed { idx, (_, expense) ->
                            val x = leftPadding + spacing * idx
                            val y = chartHeight - ((expense.toFloat() / maxExpense.toFloat()) * chartHeight)
                            pointsCoordinates.add(Offset(x, y))
                            
                            if (idx == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        // Draw Curve Trend Line
                        drawPath(
                            path = path,
                            color = ColorYes,
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )

                        // Draw circle nodes
                        pointsCoordinates.forEach { pt ->
                            drawCircle(
                                color = ColorYes,
                                radius = 7f,
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5f,
                                center = pt
                            )
                        }
                    }
                }

                // Month labels below line chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    monthlyExpenseList.forEach { (month, _) ->
                        Text(
                            text = month,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(18.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
