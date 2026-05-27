package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MilkViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.configFlow.collectAsState()
    val records by viewModel.recordsFlow.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

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
                                viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, key, payDay, leadDays)
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
                        label = { Text("Rate Per Litre ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Button(
                    onClick = {
                        val qty = defaultQty.toDoubleOrNull() ?: 1.0
                        val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                        viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays)
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
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Daily Log Reminder", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Send a daily check-in at 8:00 PM if you forget to enter milk log.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = dailyReminder,
                        onCheckedChange = {
                            dailyReminder = it
                            val qty = defaultQty.toDoubleOrNull() ?: 1.0
                            val rate = defaultRateByLitre.toDoubleOrNull() ?: 40.0
                            viewModel.saveConfig(qty, rate, it, paymentReminder, themePref, payDay, leadDays)
                        }
                    )
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
                            viewModel.saveConfig(qty, rate, dailyReminder, it, themePref, payDay, leadDays)
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
                                viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays)
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
                                viewModel.saveConfig(qty, rate, dailyReminder, paymentReminder, themePref, payDay, leadDays)
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
                Text(
                    text = "Export Statement ($selectedMonth)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val csvFile = viewModel.exportCsvReport(context)
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
                            val takenOnly = recordsInMonth.filter { it.taken }
                            val mQtyRef = takenOnly.sumOf { it.quantity }
                            val mCostRef = takenOnly.sumOf { it.quantity * it.rate }
                            val mPdfFile = viewModel.exportPdfReport(
                                context = context,
                                totalLitres = mQtyRef,
                                totalExpense = mCostRef,
                                milkDays = takenOnly.size,
                                leaveDays = recordsInMonth.size - takenOnly.size
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
