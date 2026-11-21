package com.ahmed.zad

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Parses spoken Egyptian-Arabic amounts into an arithmetic expression.
 * Understands digits (Arabic-Indic and Latin), number words, and the
 * operations زائد (+) ناقص (-) في (×) على (÷).
 */
object VoiceParser {

    private val ones = mapOf(
        "صفر" to 0L, "واحد" to 1L, "واحدة" to 1L, "اتنين" to 2L, "اثنين" to 2L,
        "تلاتة" to 3L, "ثلاثة" to 3L, "اربعة" to 4L, "أربعة" to 4L, "خمسة" to 5L,
        "ستة" to 6L, "سبعة" to 7L, "تمانية" to 8L, "ثمانية" to 8L, "تسعة" to 9L,
    )
    private val teens = mapOf(
        "عشرة" to 10L, "عشر" to 10L, "حداشر" to 11L, "احداشر" to 11L, "اتناشر" to 12L,
        "اثناشر" to 12L, "تلتاشر" to 13L, "اربعتاشر" to 14L, "خمستاشر" to 15L,
        "ستاشر" to 16L, "سبعتاشر" to 17L, "تمنتاشر" to 18L, "تسعتاشر" to 19L,
    )
    private val tens = mapOf(
        "عشرين" to 20L, "تلاتين" to 30L, "ثلاثين" to 30L, "اربعين" to 40L,
        "أربعين" to 40L, "خمسين" to 50L, "ستين" to 60L, "سبعين" to 70L,
        "تمانين" to 80L, "ثمانين" to 80L, "تسعين" to 90L,
    )

    private fun normalizeDigits(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                in '٠'..'٩' -> sb.append('0' + (c - '٠'))
                in '۰'..'۹' -> sb.append('0' + (c - '۰'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Returns an expression string like "100+50", or null if nothing usable was heard. */
    fun parse(spoken: String): String? {
        val tokens = normalizeDigits(spoken)
            .replace("جنيه", " ").replace("جنية", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it != "و" }
        val out = StringBuilder()
        var number = 0L
        var hasNumber = false
        fun flush() {
            if (hasNumber) {
                out.append(number)
                number = 0L
                hasNumber = false
            }
        }
        for (token in tokens) {
            when {
                token.matches(Regex("\\d+(\\.\\d{1,2})?")) -> {
                    flush()
                    out.append(token)
                }
                ones.containsKey(token) -> { number += ones[token]!!; hasNumber = true }
                teens.containsKey(token) -> { number += teens[token]!!; hasNumber = true }
                tens.containsKey(token) -> { number += tens[token]!!; hasNumber = true }
                token in listOf("مية", "مائة", "ميه") -> {
                    number = (if (number == 0L) 100L else number * 100L); hasNumber = true
                }
                token in listOf("الف", "ألف") -> {
                    number = (if (number == 0L) 1000L else number * 1000L); hasNumber = true
                }
                token in listOf("زائد", "زايد", "+", "بلس") -> { flush(); out.append("+") }
                token in listOf("ناقص", "-", "مايناس", "إلا") -> { flush(); out.append("-") }
                token in listOf("في", "×", "*", "ضرب") -> { flush(); out.append("×") }
                token in listOf("على", "÷", "/", "قسمة") -> { flush(); out.append("÷") }
                else -> return null // an unrecognized word: safer to let the user type
            }
        }
        flush()
        val expression = out.toString().trim('+', '-', '×', '÷')
        return expression.ifBlank { null }
    }
}

fun speechRecognizerAvailable(context: android.content.Context): Boolean =
    context.packageManager
        .queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
        .isNotEmpty()

/** Shown when voice entry is on but the device has no speech service: a button
 *  that opens the Google app page, plus plain instructions. */
@Composable
fun SpeechInstallPrompt() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    if (speechRecognizerAvailable(context)) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(onClick = {
            val opened = runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=com.google.android.googlequicksearchbox")))
            }.isSuccess || runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox")))
            }.isSuccess
            if (!opened) status = "مش قادر أفتح المتجر من هنا. افتح متجر جوجل بلاي وابحث عن: تطبيق Google"
        }) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("ثبّت تطبيق جوجل للإدخال الصوتي", fontSize = 13.sp)
        }
    }
    Text(
        "الجهاز ده مفيهوش خدمة التعرف الصوتي. ثبّت تطبيق جوجل (مجاني) والميزة هتشتغل لوحدها.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    status?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
}

/** Mic button that listens offline-first and writes the parsed expression into the amount field. */
@Composable
fun VoiceAmountButton(enabled: Boolean, onResult: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    // Some devices ship without a speech-recognition service (no Google app /
    // Speech Services). Detect it up front instead of failing on tap.
    val recognizerAvailable = remember { speechRecognizerAvailable(context) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val heard = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            val parsed = heard?.let { VoiceParser.parse(it) }
            when {
                parsed != null -> { onResult(parsed); status = null }
                heard != null -> status = "سمعت \"$heard\" بس مش قادر أفهم منها مبلغ. جرّب تاني أو اكتب."
                else -> status = "مسمعتش حاجة. جرّب تاني."
            }
        } else {
            status = "التسجيل اتلغى."
        }
    }
    if (enabled && !recognizerAvailable) {
        SpeechInstallPrompt()
    }
    if (enabled && recognizerAvailable) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ar", "EG").toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-EG")
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "قول المبلغ، مثال: مية وخمسين زائد عشرين")
                }
                try {
                    launcher.launch(intent)
                } catch (e: Exception) {
                    status = "التعرف الصوتي مش متاح على الجهاز ده. اكتب المبلغ عادي."
                }
            }) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("قول المبلغ", fontSize = 13.sp)
            }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}
