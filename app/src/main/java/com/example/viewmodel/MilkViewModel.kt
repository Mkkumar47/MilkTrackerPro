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
    val sellersFlow: StateFlow<List<com.example.data.Seller>>
    val paymentsFlow: StateFlow<List<com.example.data.Payment>>

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

        sellersFlow = repository.allSellers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        paymentsFlow = repository.allPayments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed dummy records if database is empty
        viewModelScope.launch {
            repository.allRecords.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedSampleRecords()
                }
            }
            repository.allSellers.first().let { currentSellers ->
                if (currentSellers.isEmpty()) {
                    seedSellersAndPayments()
                }
            }
            // Sync Alarm system matching settings
            AlarmReceiver.scheduleReminders(application)
        }
    }

    // Records management
    fun saveRecord(
        date: String,
        taken: Boolean,
        quantity: Double,
        rate: Double,
        notes: String,
        session: String = "Morning",
        sellerName: String = "",
        milkType: String = "Cow Milk"
    ) {
        viewModelScope.launch {
            val rec = MilkRecord(
                date = date,
                taken = taken,
                quantity = quantity,
                rate = rate,
                notes = notes,
                session = session,
                sellerName = sellerName,
                milkType = milkType
            )
            repository.insertRecord(rec)
            updateWidgets()
            _toastMessage.emit("Record for $date saved!")
        }
    }

    fun saveRecordRange(
        startDate: String,
        endDate: String,
        taken: Boolean,
        quantity: Double,
        rate: Double,
        notes: String,
        session: String = "Morning",
        sellerName: String = "",
        milkType: String = "Cow Milk"
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            try {
                val start = sdf.parse(startDate) ?: Date()
                val end = sdf.parse(endDate) ?: Date()
                
                val cal = Calendar.getInstance()
                cal.time = start
                
                var count = 0
                while (!cal.time.after(end)) {
                    val dateStr = sdf.format(cal.time)
                    val rec = MilkRecord(
                        date = dateStr,
                        taken = taken,
                        quantity = quantity,
                        rate = rate,
                        notes = notes,
                        session = session,
                        sellerName = sellerName,
                        milkType = milkType
                    )
                    repository.insertRecord(rec)
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    count++
                }
                updateWidgets()
                _toastMessage.emit("Saved $count record(s) from $startDate to $endDate!")
            } catch (e: Exception) {
                _toastMessage.emit("Failed to save date range: ${e.message}")
            }
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
        payDaysBefore: Int = 1,
        currencyCode: String = "USD",
        currencySymbol: String = "$",
        dailyHour: Int? = null,
        dailyMinute: Int? = null
    ) {
        viewModelScope.launch {
            val current = repository.getConfig()
            val newCfg = current.copy(
                defaultQuantity = defaultQty,
                defaultRate = defaultRate,
                dailyReminderEnabled = dailyNotify,
                paymentReminderEnabled = payNotify,
                themePreference = themePref,
                paymentReminderDay = payDay,
                paymentReminderDaysBefore = payDaysBefore,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                dailyReminderHour = dailyHour ?: current.dailyReminderHour,
                dailyReminderMinute = dailyMinute ?: current.dailyReminderMinute
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

    fun updateGoogleSignIn(name: String?, email: String?, photoUrl: String?, signedIn: Boolean) {
        viewModelScope.launch {
            val current = repository.getConfig()
            val updated = current.copy(
                googleUserName = name,
                googleEmail = email,
                googlePhotoUrl = photoUrl,
                isGoogleSignedIn = signedIn
            )
            repository.saveConfig(updated)
            if (signedIn) {
                _toastMessage.emit("Welcome, $name!")
            } else {
                _toastMessage.emit("Signed out successfully.")
            }
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
    fun exportCsvReport(context: Context, sellerFilter: String = "ALL"): File? {
        val records = recordsFlow.value.filter { it.date.startsWith(_selectedMonth.value) }
            .filter { if (sellerFilter == "ALL") true else it.sellerName.equals(sellerFilter, ignoreCase = true) }
        val file = ReportUtils.exportToCsv(context, _selectedMonth.value, records, configFlow.value.currencySymbol)
        if (file == null) {
            viewModelScope.launch { _toastMessage.emit("Failed to generate CSV report.") }
        }
        return file
    }

    fun exportPdfReport(context: Context, totalLitres: Double, totalExpense: Double, milkDays: Int, leaveDays: Int, sellerFilter: String = "ALL"): File? {
        val records = recordsFlow.value.filter { it.date.startsWith(_selectedMonth.value) }
            .filter { if (sellerFilter == "ALL") true else it.sellerName.equals(sellerFilter, ignoreCase = true) }
        val file = ReportUtils.exportToPdf(
            context = context,
            monthLabel = _selectedMonth.value,
            list = records,
            totalLitres = totalLitres,
            totalExpense = totalExpense,
            milkDaysCount = milkDays,
            leaveDaysCount = leaveDays,
            currencySymbol = configFlow.value.currencySymbol
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
        
        // Period A: Jan 1, 2026 to Apr 30, 2026
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2026, Calendar.JANUARY, 1, 12, 0, 0)
        
        val endApril = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        endApril.set(2026, Calendar.APRIL, 30, 12, 0, 0)
        
        while (!cal.after(endApril)) {
            val dateStr = sdf.format(cal.time)
            repository.insertRecord(
                MilkRecord(
                    date = dateStr,
                    taken = true,
                    quantity = 1.0,
                    rate = 80.0,
                    notes = ""
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Period B: May 1, 2026 to May 31, 2026
        cal.set(2026, Calendar.MAY, 1, 12, 0, 0)
        val endMay = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        endMay.set(2026, Calendar.MAY, 31, 12, 0, 0)
        
        while (!cal.after(endMay)) {
            val dateStr = sdf.format(cal.time)
            repository.insertRecord(
                MilkRecord(
                    date = dateStr,
                    taken = true,
                    quantity = 1.0,
                    rate = 90.0,
                    notes = ""
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private suspend fun seedSellersAndPayments() {
        val s1Id = repository.insertSeller(com.example.data.Seller(name = "Mother Dairy", phone = "+1234567890"))
        val s2Id = repository.insertSeller(com.example.data.Seller(name = "Amul Dairy Services", phone = "+1987654321"))
        val s3Id = repository.insertSeller(com.example.data.Seller(name = "Heritage Milk Vendor", phone = "+1555123456"))

        // Add some sample payments
        repository.insertPayment(
            com.example.data.Payment(
                date = "2026-04-15",
                sellerId = s1Id.toInt(),
                sellerName = "Mother Dairy",
                amount = 1200.00,
                paymentMode = "UPI",
                notes = "Advance payment for April deliver",
                session = "Morning",
                milkType = "Cow Milk"
            )
        )
        repository.insertPayment(
            com.example.data.Payment(
                date = "2026-04-28",
                sellerId = s2Id.toInt(),
                sellerName = "Amul Dairy Services",
                amount = 800.00,
                paymentMode = "Cash",
                notes = "Paid to delivery boy",
                session = "Evening",
                milkType = "Buffalo Milk"
            )
        )
        repository.insertPayment(
            com.example.data.Payment(
                date = "2026-05-10",
                sellerId = s3Id.toInt(),
                sellerName = "Heritage Milk Vendor",
                amount = 1500.00,
                paymentMode = "Bank",
                notes = "Online Bank Transfer",
                session = "Morning",
                milkType = "Cow Milk"
            )
        )
        repository.insertPayment(
            com.example.data.Payment(
                date = "2026-05-25",
                sellerId = s1Id.toInt(),
                sellerName = "Mother Dairy",
                amount = 250.00,
                paymentMode = "Other",
                notes = "Settled pending balance",
                session = "All Sessions",
                milkType = "All Milk Types"
            )
        )
    }

    // Seller Actions
    fun saveSeller(
        id: Int = 0,
        name: String,
        phone: String = "",
        address: String = "",
        milkType: String = "Both",
        cowRate: Double = 0.0,
        buffaloRate: Double = 0.0,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _toastMessage.emit("Seller name cannot be empty.")
                return@launch
            }
            val seller = com.example.data.Seller(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                milkType = milkType,
                cowRate = cowRate,
                buffaloRate = buffaloRate
            )
            val newId = repository.insertSeller(seller)
            _toastMessage.emit("Seller '${seller.name}' saved!")
            onSuccess(newId)
        }
    }

    fun deleteSeller(seller: com.example.data.Seller) {
        viewModelScope.launch {
            repository.deleteSeller(seller)
            _toastMessage.emit("Seller '${seller.name}' deleted")
        }
    }

    // Payment Actions
    fun savePayment(
        date: String,
        sellerId: Int,
        sellerName: String,
        amount: Double,
        paymentMode: String,
        notes: String,
        session: String = "All Sessions",
        milkType: String = "All Milk Types"
    ) {
        viewModelScope.launch {
            val payment = com.example.data.Payment(
                date = date,
                sellerId = sellerId,
                sellerName = sellerName,
                amount = amount,
                paymentMode = paymentMode,
                notes = notes,
                session = session,
                milkType = milkType
            )
            repository.insertPayment(payment)
            _toastMessage.emit("Payment of ${configFlow.value.currencySymbol}$amount saved!")
        }
    }

    fun deletePayment(id: Int) {
        viewModelScope.launch {
            repository.deletePaymentById(id)
            _toastMessage.emit("Payment deleted")
        }
    }

    fun populateStandard2026Data() {
        viewModelScope.launch {
            repository.deleteAllRecords()
            repository.deleteAllSellers()
            repository.deleteAllPayments()
            seedSampleRecords()
            seedSellersAndPayments()
            updateWidgets()
            _toastMessage.emit("Database populated with standard 2026 records successfully!")
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
