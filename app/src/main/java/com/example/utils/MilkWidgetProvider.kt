package com.example.utils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.MilkConfig
import com.example.data.MilkRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MilkWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == ACTION_WIDGET_ADD_MILK) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val config = db.milkConfigDao.getConfig() ?: MilkConfig()

                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val currentRecord = db.milkRecordDao.getRecordForDate(todayStr)

                    if (currentRecord == null || !currentRecord.taken) {
                        // Insert or toggle today's record as taken
                        val newRecord = MilkRecord(
                            date = todayStr,
                            taken = true,
                            quantity = config.defaultQuantity,
                            rate = config.defaultRate,
                            notes = "Logged via Widget Shortcut"
                        )
                        db.milkRecordDao.insertRecord(newRecord)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Logged today's milk: ${config.defaultQuantity}L", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Already logged as TAKEN!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Schedule widget update
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisWidget = ComponentName(context, MilkWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_ADD_MILK = "com.example.action.WIDGET_ADD_MILK"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.milk_widget_layout)

            // Setup MainActivity launching Intent on entire card click
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Setup Instant Add Broadcast Intent on Log Today click
            val logIntent = Intent(context, MilkWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_ADD_MILK
            }
            val logPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                logIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_log_button, logPendingIntent)

            // Query database and populate RemoteViews
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val prettyDate = SimpleDateFormat("MMM d, EEEE", Locale.US).format(Date())
                    val todayRecord = db.milkRecordDao.getRecordForDate(todayStr)

                    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                    val monthRecords = db.milkRecordDao.getRecordsForMonth(currentMonth).first()
                    val totalExpense = monthRecords.filter { it.taken }.sumOf { it.quantity * it.rate }
                    val config = db.milkConfigDao.getConfig() ?: MilkConfig()

                    views.setTextViewText(R.id.widget_date, prettyDate)
                    views.setTextViewText(R.id.widget_month_expense, String.format(Locale.US, "%s%.2f", config.currencySymbol, totalExpense))

                    if (todayRecord == null) {
                        views.setTextViewText(R.id.widget_today_status, "Today: PENDING (Not Logged)")
                    } else if (todayRecord.taken) {
                        views.setTextViewText(R.id.widget_today_status, String.format(Locale.US, "Today: TAKEN (%.1f L)", todayRecord.quantity))
                    } else {
                        views.setTextViewText(R.id.widget_today_status, "Today: ON LEAVE (Absent)")
                    }

                    withContext(Dispatchers.Main) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
