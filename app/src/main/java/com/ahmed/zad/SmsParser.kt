package com.ahmed.zad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Optional offline bank-SMS parsing. When the user enables it and grants the
 * SMS permission, a debit message (خصم / سحب / شراء / دفع + amount) records an
 * expense automatically to the account the user picked in Settings.
 * Nothing leaves the device.
 */
class SmsReceiver : BroadcastReceiver() {

    private val debitKeywords = listOf("خصم", "سحب", "شراء", "دفع", "تم خصم", "debit", "purchase", "withdrawn")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = ZadPrefs(context)
        if (!prefs.smsParsingEnabled) return
        val accountId = prefs.smsDefaultAccountId
        if (accountId <= 0) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val lower = body.lowercase()
        if (debitKeywords.none { lower.contains(it) }) return

        val amount = extractAmount(body) ?: return
        if (amount <= 0) return

        val piasters = (amount * 100).toLong()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = ZadDatabase.get(context).dao()
                val accounts = dao.accountsFlow().first()
                if (accounts.none { it.id == accountId }) return@launch
                dao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amountPiasters = piasters,
                        accountId = accountId,
                        category = "أخرى",
                        note = "من رسالة بنك SMS",
                    )
                )
            } finally {
                pending.finish()
            }
        }
    }

    /** First plain number (Arabic-Indic or Latin digits, optional decimals) in the text. */
    fun extractAmount(text: String): Double? {
        val normalized = buildString {
            for (c in text) {
                when (c) {
                    in '٠'..'٩' -> append('0' + (c - '٠'))
                    in '۰'..'۹' -> append('0' + (c - '۰'))
                    '٫' -> append('.')
                    else -> append(c)
                }
            }
        }
        return Regex("(\\d{1,3}(?:,\\d{3})+|\\d+)(\\.\\d{1,2})?")
            .find(normalized)
            ?.value
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }
}
