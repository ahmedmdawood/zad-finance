package com.ahmed.zad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.time.YearMonth
import java.time.ZoneId

/** Builds a one-page monthly financial report PDF into a user-picked file. */
fun generateMonthlyReport(
    context: Context,
    target: Uri,
    transactions: List<TransactionEntity>,
    categories: List<String>,
) {
    val month = YearMonth.now()
    val monthStart = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val monthTx = transactions.filter { it.createdAt >= monthStart }

    val income = monthTx.filter { it.type == "INCOME" }.sumOf { it.amountPiasters }
    val expenses = monthTx.filter { it.type == "EXPENSE" }
    val expenseTotal = expenses.sumOf { it.amountPiasters }

    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val titlePaint = Paint().apply {
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val headerPaint = Paint().apply {
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val textPaint = Paint().apply { textSize = 13f; isAntiAlias = true }

    var y = 70f
    canvas.drawText("زاد — التقرير الشهري", 470f, y, titlePaint); y += 20f
    canvas.drawText("${month.monthValue}/${month.year}", 470f, y, textPaint); y += 40f

    canvas.drawText("الملخص", 500f, y, headerPaint); y += 24f
    canvas.drawText("الدخل: ${formatMoney(income)}", 470f, y, textPaint); y += 20f
    canvas.drawText("المصروفات: ${formatMoney(expenseTotal)}", 470f, y, textPaint); y += 20f
    canvas.drawText("الفرق: ${formatMoney(income - expenseTotal)}", 470f, y, textPaint); y += 36f

    canvas.drawText("المصروفات حسب الفئة", 450f, y, headerPaint); y += 24f
    for (category in categories) {
        val spent = expenses.filter { it.category == category }.sumOf { it.amountPiasters }
        if (spent > 0) {
            canvas.drawText("$category: ${formatMoney(spent)}", 470f, y, textPaint); y += 20f
        }
    }
    y += 20f

    canvas.drawText("أكبر العمليات", 460f, y, headerPaint); y += 24f
    monthTx.sortedByDescending { it.amountPiasters }.take(10).forEach { tx ->
        val label = when (tx.type) {
            "INCOME" -> "دخل"
            "EXPENSE" -> "مصروف (${tx.category})"
            "TRANSFER" -> "تحويل"
            else -> tx.type
        }
        canvas.drawText("$label — ${formatMoney(tx.amountPiasters)}", 470f, y, textPaint)
        y += 18f
        if (y > 800f) return@forEach
    }

    document.finishPage(page)
    context.contentResolver.openOutputStream(target)?.use { out ->
        document.writeTo(out)
    } ?: throw IllegalStateException("cannot write report")
    document.close()
}
