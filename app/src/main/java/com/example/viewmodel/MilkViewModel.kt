package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MilkConfig
import com.example.data.MilkRecord
import com.example.data.MilkRepository
import com.example.utils.AlarmReceiver
import com.example.utils.ReportUtils
import com.example.utils.MilkWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MilkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MilkRepository

    // Selected state
    private val _selectedMonth = MutableStateFlow(getCurrentYearMonth()) // e.g. "2026-05"
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(getCurrentYear()) // e.g. "2026"
    val selectedYear = _selectedYear.asStateFlow()

    val recordsFlow: StateFlow<List<MilkRecord>>
    val configFlow: StateFlow<MilkConfig>

    // UI Feedback
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MilkRepository(database)

        recordsFlow = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        configFlow = repository.configFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MilkConfig()
        )

        // Seed dummy records if database is empty
        viewModelScope.launch {
            repository.allRecords.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedSampleRecords()
                }
            }
            // Sync Alarm system matching settings
            val cfg = repository.getConfig()
            if (cfg.dailyReminderEnabled || cfg.paymentReminderEnabled) {
                AlarmReceiver.scheduleReminders(application)
            }
        }
    }

    // Records management
    fun saveRecord(date: String, taken: Boolean, quantity: Double, rate: Double, notes: String) {
        viewModelScope.launch {
            val rec = MilkRecord(
                date = date,
                taken = taken,
                quantity = quantity,
                rate = rate,
                notes = notes
            )
            repository.insertRecord(rec)
            updateWidgets()
            _toastMessage.emit("Record for $date saved!")
        }
    }

    fun deleteRecord(date: String) {
        viewModelScope.launch {
            repository.deleteRecordByDate(date)
            updateWidgets()
            _toastMessage.emit("Record deleted")
        }
    }

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun selectYear(year: String) {
        _selectedYear.value = year
    }

    // Config management
    fun saveConfig(
        defaultQty: Double,
        defaultRate: Double,
        dailyNotify: Boolean,
        payNotify: Boolean,
        themePref: String = "SYSTEM",
        payDay: Int = 1,
        payDaysBefore: Int = 1
    ) {
        viewModelScope.launch {
            val newCfg = MilkConfig(
                id = 1,
                defaultQuantity = defaultQty,
                defaultRate = defaultRate,
                dailyReminderEnabled = dailyNotify,
                paymentReminderEnabled = payNotify,
                themePreference = themePref,
                paymentReminderDay = payDay,
                paymentReminderDaysBefore = payDaysBefore
            )
            repository.saveConfig(newCfg)
            
            // Manage notifications
            val context = getApplication<Application>().applicationContext
            if (dailyNotify || payNotify) {
                AlarmReceiver.scheduleReminders(context)
            } else {
                AlarmReceiver.cancelReminders(context)
            }
            _toastMessage.emit("Settings saved successfully!")
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllData()
            updateWidgets()
            _toastMessage.emit("All data cleared successfully.")
        }
    }

    // File actions
    fun exportCsvReport(context: Context): File? {
        val records = recordsFlow.value.filter { it.date.startsWith(_selectedMonth.value) }
        val file = ReportUtils.exportToCsv(context, _selectedMonth.value, records)
        if (file == null) {
            viewModelScope.launch { _toastMessage.emit("Failed to generate CSV report.") }
        }
        return file
    }

    fun exportPdfReport(context: Context, totalLitres: Double, totalExpense: Double, milkDays: Int, leaveDays: Int): File? {
        val records = recordsFlow.value.filter { it.date.startsWith(_selectedMonth.value) }
        val file = ReportUtils.exportToPdf(
            context = context,
            monthLabel = _selectedMonth.value,
            list = records,
            totalLitres = totalLitres,
            totalExpense = totalExpense,
            milkDaysCount = milkDays,
            leaveDaysCount = leaveDays
        )
        if (file == null) {
            viewModelScope.launch { _toastMessage.emit("Failed to generate PDF report.") }
        }
        return file
    }

    fun shareReport(context: Context, file: File, mimeType: String) {
        ReportUtils.triggerShare(context, file, mimeType)
    }

    fun getBackupString(): String {
        return ReportUtils.generateJsonBackup(recordsFlow.value, configFlow.value)
    }

    fun restoreBackup(backupStr: String) {
        viewModelScope.launch {
            val res = ReportUtils.parseJsonBackup(backupStr)
            if (res != null) {
                val (records, config) = res
                repository.clearAllData()
                repository.saveConfig(config)
                repository.importBackupRecords(records)
                updateWidgets()
                _toastMessage.emit("Backup restored standard successfully! Imported ${records.size} entries.")
            } else {
                _toastMessage.emit("Invalid backup data format.")
            }
        }
    }

    // Seed dummy data
    private suspend fun seedSampleRecords() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        
        // Let's seed past 3 months (90 days)
        val random = Random()
        val defaultRate = 45.0
        val defaultQty = 1.0

        for (i in 0..90) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)
            
            // Random taken, mostly YES (85% probability)
            val taken = random.nextDouble() < 0.85
            val quantity = if (taken) {
                // Alternates between 1.0, 1.5, and 0.5 occasionally
                val selection = random.nextInt(4)
                when (selection) {
                    0 -> 1.5
                    1 -> 0.5
                    else -> defaultQty
                }
            } else 0.0

            val notes = if (!taken) {
                val selection = random.nextInt(3)
                when (selection) {
                    0 -> "Out of town"
                    1 -> "Leftover milk inside fridge"
                    else -> "Vendor didn't deliver"
                }
            } else ""

            repository.insertRecord(
                MilkRecord(
                    date = dateStr,
                    taken = taken,
                    quantity = if (taken) quantity else 1.0, // Quantity tracked even on leave
                    rate = defaultRate,
                    notes = notes
                )
            )
        }
    }

    private fun updateWidgets() {
        val context = getApplication<Application>().applicationContext
        val widgetIntent = Intent(context, MilkWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, MilkWidgetProvider::class.java))
        widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(widgetIntent)
    }

    companion object {
        fun getCurrentYearMonth(): String {
            return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        }

        fun getCurrentYear(): String {
            return SimpleDateFormat("yyyy", Locale.US).format(Date())
        }
    }
}
