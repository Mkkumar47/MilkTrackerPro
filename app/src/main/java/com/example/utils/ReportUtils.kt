package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.MilkConfig
import com.example.data.MilkRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReportUtils {

    fun exportToCsv(context: Context, monthLabel: String, list: List<MilkRecord>): File? {
        try {
            val fileName = "MilkTrack_Pro_$monthLabel.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                val header = "Date,Milk Taken,Quantity (Litres),Cost Per Litre,Total Cost ($),Notes\n"
                out.write(header.toByteArray())
                for (rec in list) {
                    val status = if (rec.taken) "YES" else "NO"
                    val cost = if (rec.taken) rec.quantity * rec.rate else 0.0
                    val line = "${rec.date},$status,${rec.quantity},${rec.rate},$cost,${rec.notes.replace(",", " ")}\n"
                    out.write(line.toByteArray())
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportToPdf(
        context: Context,
        monthLabel: String,
        list: List<MilkRecord>,
        totalLitres: Double,
        totalExpense: Double,
        milkDaysCount: Int,
        leaveDaysCount: Int
    ): File? {
        try {
            val pdfDoc = PdfDocument()
            // A4 page size is 595 x 842 pixels in 72 dpi
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 24f
                color = Color.rgb(33, 150, 243) // Primary blue
            }

            val subtitlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textSize = 12f
                color = Color.DKGRAY
            }

            val boldTextPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 12f
                color = Color.BLACK
            }

            val textPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 11f
                color = Color.BLACK
            }

            val redTextPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 10f
                color = Color.rgb(211, 47, 47) // Red
            }

            val greenTextPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 10f
                color = Color.rgb(56, 142, 60) // Green
            }

            val linePaint = Paint().apply {
                strokeWidth = 1f
                color = Color.LTGRAY
            }

            // Outer margin / border
            val cardPaint = Paint().apply {
                color = Color.rgb(245, 245, 245)
                style = Paint.Style.FILL
            }

            // Draw Header Card
            canvas.drawRect(25f, 25f, 570f, 180f, cardPaint)

            canvas.drawText("MilkTrack Pro - Dairy Ledger", 40f, 60f, titlePaint)
            canvas.drawText("Monthly Milk Statement & Expense Report", 40f, 80f, subtitlePaint)
            canvas.drawText("Month / Period: $monthLabel", 40f, 105f, boldTextPaint)

            // Draw quick metrics in header container
            canvas.drawText("Total Consumption: ${String.format(Locale.US, "%.2f", totalLitres)} Litres", 40f, 130f, textPaint)
            canvas.drawText("Total Expense: $${String.format(Locale.US, "%.2f", totalExpense)}", 40f, 150f, textPaint)
            canvas.drawText("Delivery Days: $milkDaysCount | Leave Days: $leaveDaysCount", 40f, 170f, textPaint)

            // Draw Table Headers
            var currentY = 220f
            canvas.drawText("Date", 40f, currentY, boldTextPaint)
            canvas.drawText("Status", 140f, currentY, boldTextPaint)
            canvas.drawText("Quantity (L)", 220f, currentY, boldTextPaint)
            canvas.drawText("Price/Unit ($)", 320f, currentY, boldTextPaint)
            canvas.drawText("Total ($)", 420f, currentY, boldTextPaint)
            canvas.drawText("Notes", 500f, currentY, boldTextPaint)

            canvas.drawLine(25f, currentY + 10f, 570f, currentY + 10f, linePaint)
            currentY += 25f

            // Populate Table rows
            for (rec in list) {
                if (currentY > 800) {
                    // Start next page if layout runs out of bounds
                    break
                }
                
                canvas.drawText(rec.date, 40f, currentY, textPaint)
                if (rec.taken) {
                    canvas.drawText("TAKEN", 140f, currentY, greenTextPaint)
                } else {
                    canvas.drawText("LEAVE", 140f, currentY, redTextPaint)
                }
                canvas.drawText(String.format(Locale.US, "%.1f L", rec.quantity), 220f, currentY, textPaint)
                canvas.drawText(String.format(Locale.US, "$%.2f", rec.rate), 320f, currentY, textPaint)
                canvas.drawText(String.format(Locale.US, "$%.2f", rec.totalExpense), 420f, currentY, textPaint)
                
                val shortNote = if (rec.notes.length > 12) rec.notes.take(9) + "..." else rec.notes
                canvas.drawText(shortNote, 500f, currentY, textPaint)

                canvas.drawLine(25f, currentY + 4f, 570f, currentY + 4f, linePaint)
                currentY += 18f
            }

            pdfDoc.finishPage(page)

            val fileName = "MilkTrack_Pro_$monthLabel.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun triggerShare(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "com.aistudio.milktrack.puzqwl.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "MilkTrack Pro Report")
                putExtra(Intent.EXTRA_TEXT, "Exported Report from MilkTrack Pro")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Report via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateJsonBackup(list: List<MilkRecord>, config: MilkConfig): String {
        return try {
            val root = JSONObject()
            
            // Serialize settings block
            val cfgJson = JSONObject().apply {
                put("defaultQuantity", config.defaultQuantity)
                put("defaultRate", config.defaultRate)
                put("paymentReminderEnabled", config.paymentReminderEnabled)
                put("dailyReminderEnabled", config.dailyReminderEnabled)
            }
            root.put("config", cfgJson)

            // Serialize record values
            val recArray = JSONArray()
            for (rec in list) {
                val recObj = JSONObject().apply {
                    put("date", rec.date)
                    put("taken", rec.taken)
                    put("quantity", rec.quantity)
                    put("rate", rec.rate)
                    put("notes", rec.notes)
                }
                recArray.put(recObj)
            }
            root.put("records", recArray)
            root.toString(2)
        } catch (e: Exception) {
            ""
        }
    }

    fun parseJsonBackup(jsonStr: String): Pair<List<MilkRecord>, MilkConfig>? {
        return try {
            val root = JSONObject(jsonStr)
            
            // Extract settings model
            val config = if (root.has("config")) {
                val cfgObj = root.getJSONObject("config")
                MilkConfig(
                    id = 1,
                    defaultQuantity = cfgObj.optDouble("defaultQuantity", 1.0),
                    defaultRate = cfgObj.optDouble("defaultRate", 40.0),
                    paymentReminderEnabled = cfgObj.optBoolean("paymentReminderEnabled", true),
                    dailyReminderEnabled = cfgObj.optBoolean("dailyReminderEnabled", true)
                )
            } else {
                MilkConfig()
            }

            // Extract items lists
            val records = mutableListOf<MilkRecord>()
            if (root.has("records")) {
                val arr = root.getJSONArray("records")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    records.add(
                        MilkRecord(
                            date = obj.getString("date"),
                            taken = obj.getBoolean("taken"),
                            quantity = obj.getDouble("quantity"),
                            rate = obj.getDouble("rate"),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }
            Pair(records, config)
        } catch (e: Exception) {
            null
        }
    }
}
