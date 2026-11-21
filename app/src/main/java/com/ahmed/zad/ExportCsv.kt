package com.ahmed.zad

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes one month's transactions as CSV into a user-picked file. */
fun exportMonthCsv(
    context: Context,
    target: Uri,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    year: Int,
    month: Int,
) {
    val monthTx = transactions.filter { tx ->
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.createdAt }
        cal.get(java.util.Calendar.YEAR) == year && cal.get(java.util.Calendar.MONTH) + 1 == month
    }.sortedBy { it.createdAt }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    fun typeLabel(type: String): String = when (type) {
        "INCOME" -> "دخل"
        "EXPENSE" -> "مصروف"
        "TRANSFER" -> "تحويل"
        "DEBT_GIVEN" -> "إقراض"
        "DEBT_COLLECTED" -> "تحصيل دين"
        "DEBT_BORROWED" -> "استلاف"
        "DEBT_REPAID" -> "سداد دين"
        "CARD_PURCHASE" -> "شراء بالبطاقة"
        "CARD_PAYMENT" -> "سداد بطاقة"
        else -> type
    }

    val sb = StringBuilder()
    sb.append("﻿") // UTF-8 BOM so Excel opens Arabic correctly
    sb.append("التاريخ,النوع,الفئة,المبلغ (جنيه),الحساب,إلى حساب,ملاحظة,التعبير\n")
    for (tx in monthTx) {
        val account = accounts.firstOrNull { it.id == tx.accountId }?.name ?: ""
        val toAccount = tx.toAccountId?.let { id -> accounts.firstOrNull { it.id == id }?.name } ?: ""
        val pounds = (tx.amountPiasters / 100.0).toString()
        fun esc(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
        sb.append(
            listOf(
                dateFormat.format(Date(tx.createdAt)),
                typeLabel(tx.type),
                if (tx.type == "EXPENSE") tx.category else "",
                pounds,
                account,
                toAccount,
                tx.note,
                tx.amountExpression ?: "",
            ).joinToString(",") { esc(it) }
        ).append("\n")
    }

    context.contentResolver.openOutputStream(target)?.use { out ->
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
    } ?: throw IllegalStateException("cannot write csv")
}

/** A new file Uri in the public Downloads collection. Needs no permissions on API 29+
 *  and no DocumentsUI, so it works on stripped devices with no files app. */
fun downloadsUri(context: Context, name: String, mime: String): Uri? {
    if (android.os.Build.VERSION.SDK_INT < 29) return null
    return runCatching {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, mime)
        }
        context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
    }.getOrNull()
}
