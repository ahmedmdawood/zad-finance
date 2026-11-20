package com.ahmed.zad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.CompositionLocalProvider

val GoldColor = Color(0xFFC9A227)

/** Preset expense categories — chosen, never typed. */
val EXPENSE_CATEGORIES = listOf(
    "أكل", "مواصلات", "تسوق", "فواتير", "صحة", "تعليم", "ترفيه", "أخرى",
)

/** Friendly icon per preset category; custom categories carry their own emoji. */
fun categoryEmoji(category: String): String = when (category) {
    "أكل" -> "🍔"
    "مواصلات" -> "🚌"
    "تسوق" -> "🛒"
    "فواتير" -> "💡"
    "صحة" -> "💊"
    "تعليم" -> "📚"
    "ترفيه" -> "🎮"
    else -> "📦"
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(selected: String, categories: List<String>, onSelect: (String) -> Unit) {
    // Content-sized chips in a flow: Arabic labels never get squeezed into
    // letter-by-letter wrapping.
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        categories.forEach { category ->
            androidx.compose.material3.FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(categoryEmoji(category) + " " + category, fontSize = 13.sp, maxLines = 1) },
            )
        }
    }
}
val IncomeGreen = Color(0xFF66BB6A)
val ExpenseRed = Color(0xFFE57373)

data class ZadTheme(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val defaultDark: Boolean,
    val preview: List<Color>,
)

val ZadThemes = listOf(
    ZadTheme(
        "mono", "أحادي وحبر", "Monochrome & Ink",
        lightColorScheme(
            primary = Color(0xFF111111),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF666666),
            background = Color(0xFFFAFAFA),
            onBackground = Color(0xFF111111),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF111111),
            surfaceVariant = Color(0xFFEDEDED),
            onSurfaceVariant = Color(0xFF555555),
        ),
        darkColorScheme(
            primary = Color(0xFFE8E8E8),
            onPrimary = Color(0xFF111111),
            secondary = Color(0xFF9A9A9A),
            background = Color(0xFF121212),
            onBackground = Color(0xFFEDEDED),
            surface = Color(0xFF1E1E1E),
            onSurface = Color(0xFFEDEDED),
            surfaceVariant = Color(0xFF2A2A2A),
            onSurfaceVariant = Color(0xFFAAAAAA),
        ),
        defaultDark = false,
        listOf(Color(0xFF111111), Color(0xFFFAFAFA), Color(0xFF888888)),
    ),
    ZadTheme(
        "cinematic", "سينمائي هادئ", "Cinematic Muted",
        lightColorScheme(
            primary = Color(0xFF4A6A8C),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF6E7F94),
            background = Color(0xFFF2F4F7),
            onBackground = Color(0xFF1D2632),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1D2632),
            surfaceVariant = Color(0xFFE2E7EE),
            onSurfaceVariant = Color(0xFF5A6B80),
        ),
        darkColorScheme(
            primary = Color(0xFF8FA3BF),
            onPrimary = Color(0xFF10151B),
            secondary = Color(0xFF7E8CA0),
            background = Color(0xFF1A2028),
            onBackground = Color(0xFFE6EAF0),
            surface = Color(0xFF232B36),
            onSurface = Color(0xFFE6EAF0),
            surfaceVariant = Color(0xFF2C3542),
            onSurfaceVariant = Color(0xFF9AA7B8),
        ),
        defaultDark = true,
        listOf(Color(0xFF1A2028), Color(0xFF8FA3BF), Color(0xFF2C3542)),
    ),
    ZadTheme(
        "metallic", "معدني كلاسيكي", "Classic Metallic",
        lightColorScheme(
            primary = Color(0xFF4A4E57),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF8A6D1F),
            background = Color(0xFFF4F5F7),
            onBackground = Color(0xFF1C1E22),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1C1E22),
            surfaceVariant = Color(0xFFE6E8EB),
            onSurfaceVariant = Color(0xFF5A5E66),
        ),
        darkColorScheme(
            primary = Color(0xFFB8BCC4),
            onPrimary = Color(0xFF17181C),
            secondary = GoldColor,
            background = Color(0xFF17181C),
            onBackground = Color(0xFFEDEFF2),
            surface = Color(0xFF212329),
            onSurface = Color(0xFFEDEFF2),
            surfaceVariant = Color(0xFF2A2C33),
            onSurfaceVariant = Color(0xFFA0A4AD),
        ),
        defaultDark = true,
        listOf(Color(0xFF17181C), Color(0xFFB8BCC4), GoldColor),
    ),
    ZadTheme(
        "coffee_light", "قهوة فاتح", "Coffee Mix Light",
        lightColorScheme(  // light variant
            primary = Color(0xFF6F4E37),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFA67B4F),
            background = Color(0xFFF6F0E8),
            onBackground = Color(0xFF2E211A),
            surface = Color(0xFFFFFDF9),
            onSurface = Color(0xFF2E211A),
            surfaceVariant = Color(0xFFEDE3D6),
            onSurfaceVariant = Color(0xFF7A6A5C),
        ),
        darkColorScheme(
            primary = Color(0xFFC08A5A),
            onPrimary = Color(0xFF1C1410),
            secondary = GoldColor,
            background = Color(0xFF1C1410),
            onBackground = Color(0xFFF2E8DD),
            surface = Color(0xFF281D16),
            onSurface = Color(0xFFF2E8DD),
            surfaceVariant = Color(0xFF33261D),
            onSurfaceVariant = Color(0xFFBFAC9C),
        ),
        defaultDark = false,
        listOf(Color(0xFFF6F0E8), Color(0xFF6F4E37), Color(0xFFA67B4F)),
    ),
    ZadTheme(
        "coffee_dark", "قهوة غامق", "Coffee Mix Dark",
        lightColorScheme(
            primary = Color(0xFF6F4E37),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFA67B4F),
            background = Color(0xFFF6F0E8),
            onBackground = Color(0xFF2E211A),
            surface = Color(0xFFFFFDF9),
            onSurface = Color(0xFF2E211A),
            surfaceVariant = Color(0xFFEDE3D6),
            onSurfaceVariant = Color(0xFF7A6A5C),
        ),
        darkColorScheme(
            primary = Color(0xFFC08A5A),
            onPrimary = Color(0xFF1C1410),
            secondary = GoldColor,
            background = Color(0xFF1C1410),
            onBackground = Color(0xFFF2E8DD),
            surface = Color(0xFF281D16),
            onSurface = Color(0xFFF2E8DD),
            surfaceVariant = Color(0xFF33261D),
            onSurfaceVariant = Color(0xFFBFAC9C),
        ),
        defaultDark = true,
        listOf(Color(0xFF1C1410), Color(0xFFC08A5A), GoldColor),
    ),
)

fun themeById(id: String): ZadTheme = ZadThemes.firstOrNull { it.id == id } ?: ZadThemes.last()

fun themeScheme(themeId: String, darkOverride: Int): ColorScheme {
    val theme = themeById(themeId)
    val dark = when (darkOverride) {
        0 -> false
        1 -> true
        else -> theme.defaultDark
    }
    return if (dark) theme.darkScheme else theme.lightScheme
}

@Composable
fun ZadAppTheme(themeId: String, darkOverride: Int = -1, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = themeScheme(themeId, darkOverride), content = content)
    }
}

/** Golden founder badge: gradient disc, thin light ring. */
@Composable
fun AdBadge(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFFF6D878), Color(0xFFC9A227), Color(0xFF8C6D1F))
                )
            )
            .border(1.dp, Color(0xFFFFF3C4), CircleShape)
            .padding(2.dp),
    ) {
        Text(
            "AD",
            color = Color(0xFF3B2E05),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun MoneyText(
    amountPiasters: Long,
    hidden: Boolean,
    fontSize: Int = 15,
    fontWeight: FontWeight = FontWeight.SemiBold,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        if (hidden) MASKED_MONEY else formatMoney(amountPiasters),
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
    )
}

@Composable
fun ThemePicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ZadThemes.forEach { theme ->
            val isSelected = theme.id == selected
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(theme.id) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        theme.preview.forEach { c ->
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(theme.nameAr, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            theme.nameEn,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/** Amount field with a live result preview, error display and a mini keypad. */
@Composable
fun CalculatorAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (value.isNotBlank()) {
            val preview = try {
                val (piasters, wasRounded) = evaluateToPiasters(value)
                val suffix = if (wasRounded) " (مقرب)" else ""
                Preview("= ${formatMoney(piasters)}$suffix", null)
            } catch (e: EvalException) {
                Preview(null, e.message ?: "خطأ في التعبير")
            }
            Spacer(Modifier.height(4.dp))
            if (preview.result != null) {
                Text(preview.result, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            } else {
                Text(preview.error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        MiniKeypad(
            onKey = { onValueChange(value + it) },
            onBackspace = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) },
            onClear = { onValueChange("") }
        )
    }
}

private data class Preview(val result: String?, val error: String?)

@Composable
private fun MiniKeypad(onKey: (String) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit) {
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "(", "+"),
        listOf(")", "C", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                when (key) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    else -> onKey(key)
                                }
                            }
                    ) {
                        Text(key, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (row.size < 4) Spacer(Modifier.weight(1f))
            }
        }
    }
}
