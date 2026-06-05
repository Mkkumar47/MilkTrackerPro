package com.example.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.MilkConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleReminders(context)
            return
        }

        val type = intent?.getStringExtra("type") ?: ""
        
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processAlarm(context, type)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processAlarm(context: Context, type: String) {
        val db = AppDatabase.getDatabase(context)
        val config = db.milkConfigDao.getConfig() ?: MilkConfig()

        if (type == "monthly") {
            if (config.paymentReminderEnabled) {
                if (isApproachingPaymentDate(Calendar.getInstance(), config)) {
                    val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                    val records = db.milkRecordDao.getAllRecords().first()
                    val payments = db.paymentDao.getAllPayments().first()

                    val monthlyRecords = records.filter { it.date.startsWith(currentMonthStr) }
                    val takenOnly = monthlyRecords.filter { it.taken }
                    val totalExpense = takenOnly.sumOf { it.quantity * it.rate }

                    val monthlyPayments = payments.filter { it.date.startsWith(currentMonthStr) }
                    val totalPaid = monthlyPayments.sumOf { it.amount }

                    val amountDue = totalExpense - totalPaid
                    showNotification(context, "monthly", config, amountDue)
                }
            }
        } else if (type == "daily") {
            if (config.dailyReminderEnabled) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val record = db.milkRecordDao.getRecordForDate(todayStr)
                if (record == null) {
                    showNotification(context, "daily", config)
                }
            }
        }
    }

    private fun isApproachingPaymentDate(current: Calendar, config: MilkConfig): Boolean {
        // Safe check for day fields
        val prefDay = config.paymentReminderDay.coerceIn(1, 28)
        val leadDays = config.paymentReminderDaysBefore.coerceAtLeast(0)

        // Clear time of day for exact date comparison
        val today = (current.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 1. Calculate due date this month
        val dueThisMonth = (today.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, prefDay)
        }
        val triggerThisMonth = (dueThisMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadDays)
        }

        if (!today.before(triggerThisMonth) && !today.after(dueThisMonth)) {
            return true
        }

        // 2. What if the range overlaps into the previous month?
        val duePrevMonth = (dueThisMonth.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            if (prefDay > maxDay) set(Calendar.DAY_OF_MONTH, maxDay)
        }
        val triggerPrevMonth = (duePrevMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadDays)
        }

        if (!today.before(triggerPrevMonth) && !today.after(duePrevMonth)) {
            return true
        }

        // 3. What if the range overlaps into the next month?
        val dueNextMonth = (dueThisMonth.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            if (prefDay > maxDay) set(Calendar.DAY_OF_MONTH, maxDay)
        }
        val triggerNextMonth = (dueNextMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadDays)
        }

        if (!today.before(triggerNextMonth) && !today.after(dueNextMonth)) {
            return true
        }

        return false
    }

    private fun showNotification(context: Context, type: String, config: MilkConfig, amountDue: Double = 0.0) {
        val channelId = "MILK_TRACK_REMINDERS"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily entry and monthly payment reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title: String
        val text: String
        val notificationId: Int

        if (type == "monthly") {
            title = "MilkTrack Pro: Settle Account"
            val numStr = String.format(Locale.US, "%s%.2f", config.currencySymbol, amountDue)
            text = "Your payment date of Day ${config.paymentReminderDay} is approaching! Outstanding Due: $numStr. Settle your milk invoice soon."
            notificationId = 1001
        } else {
            title = "MilkTrack Pro: Daily Check"
            text = "Did you purchase milk today? Save your entry in Dairy Ledger!"
            notificationId = 1002
        }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        fun scheduleReminders(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val config = db.milkConfigDao.getConfig() ?: MilkConfig()

                // Evening Alarm: Daily Log Check
                val dailyIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("type", "daily")
                }
                val dailyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    2001,
                    dailyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (config.dailyReminderEnabled) {
                    val dailyCalendar = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, config.dailyReminderHour)
                        set(Calendar.MINUTE, config.dailyReminderMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        if (before(Calendar.getInstance())) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }

                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        dailyCalendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        dailyPendingIntent
                    )
                } else {
                    alarmManager.cancel(dailyPendingIntent)
                }

                // Morning Alarm: Payment approaches check
                val monthlyIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("type", "monthly")
                }
                val monthlyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    2002,
                    monthlyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (config.paymentReminderEnabled) {
                    val monthlyCalendar = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        if (before(Calendar.getInstance())) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }

                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        monthlyCalendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        monthlyPendingIntent
                    )
                } else {
                    alarmManager.cancel(monthlyPendingIntent)
                }
            }
        }

        fun cancelReminders(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val dailyPending = PendingIntent.getBroadcast(
                context,
                2001,
                Intent(context, AlarmReceiver::class.java).apply { putExtra("type", "daily") },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(dailyPending)

            val monthlyPending = PendingIntent.getBroadcast(
                context,
                2002,
                Intent(context, AlarmReceiver::class.java).apply { putExtra("type", "monthly") },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(monthlyPending)
        }
    }
}
