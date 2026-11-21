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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
/** Button with a satisfying press bounce on top of the standard ripple. */
@Composable
fun PressScaleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )
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
        "nile", "أزرق", "Blue",
lightColorScheme(
            primary = Color(0xFF1C4E80),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF52779E),
            tertiary = Color(0xFF2E86AB),
            background = Color(0xFFF3F7FC),
            onBackground = Color(0xFF0F2233),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F2233),
            surfaceVariant = Color(0xFFE0EAF5),
            onSurfaceVariant = Color(0xFF47617E),
        ),
darkColorScheme(
            primary = Color(0xFF8FC1EE),
            onPrimary = Color(0xFF0A1D31),
            secondary = Color(0xFF92ABC4),
            tertiary = Color(0xFF63A8D8),
            background = Color(0xFF0B141F),
            onBackground = Color(0xFFE4EEF8),
            surface = Color(0xFF13202F),
            onSurface = Color(0xFFE4EEF8),
            surfaceVariant = Color(0xFF1E2E40),
            onSurfaceVariant = Color(0xFFAEC3D6),
        ),
        defaultDark = false,
        listOf(Color(0xFF1C4E80), Color(0xFF8FC1EE), Color(0xFF0B141F)),
    ),
    ZadTheme(
        "oasis", "أخضر", "Green",
lightColorScheme(
            primary = Color(0xFF116641),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF55826A),
            tertiary = Color(0xFF2E9E6B),
            background = Color(0xFFF2F8F3),
            onBackground = Color(0xFF0E231A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0E231A),
            surfaceVariant = Color(0xFFDDEDE1),
            onSurfaceVariant = Color(0xFF41604D),
        ),
darkColorScheme(
            primary = Color(0xFF82CBA2),
            onPrimary = Color(0xFF0A2416),
            secondary = Color(0xFF96B5A1),
            tertiary = Color(0xFF55B787),
            background = Color(0xFF0C1711),
            onBackground = Color(0xFFE3F1E7),
            surface = Color(0xFF14231A),
            onSurface = Color(0xFFE3F1E7),
            surfaceVariant = Color(0xFF1D3024),
            onSurfaceVariant = Color(0xFFACCDB8),
        ),
        defaultDark = false,
        listOf(Color(0xFF116641), Color(0xFF82CBA2), Color(0xFF0C1711)),
    ),
    ZadTheme(
        "amethyst", "بنفسجي", "Purple",
lightColorScheme(
            primary = Color(0xFF5C3A92),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF7C67A0),
            tertiary = Color(0xFF9357B8),
            background = Color(0xFFF8F5FC),
            onBackground = Color(0xFF1D132B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1D132B),
            surfaceVariant = Color(0xFFEBE2F5),
            onSurfaceVariant = Color(0xFF5B4A76),
        ),
darkColorScheme(
            primary = Color(0xFFBFA0E8),
            onPrimary = Color(0xFF26103C),
            secondary = Color(0xFFA18DBC),
            tertiary = Color(0xFFD0A8F0),
            background = Color(0xFF130E1E),
            onBackground = Color(0xFFEDE6F8),
            surface = Color(0xFF1D1629),
            onSurface = Color(0xFFEDE6F8),
            surfaceVariant = Color(0xFF2B2139),
            onSurfaceVariant = Color(0xFFBFAFD4),
        ),
        defaultDark = false,
        listOf(Color(0xFF5C3A92), Color(0xFFBFA0E8), Color(0xFF130E1E)),
    ),
    ZadTheme(
        "rose", "وردي", "Rose",
lightColorScheme(
            primary = Color(0xFFA1465C),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFA77A80),
            tertiary = Color(0xFFC4667C),
            background = Color(0xFFFBF4F3),
            onBackground = Color(0xFF2A161A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF2A161A),
            surfaceVariant = Color(0xFFF4E2E0),
            onSurfaceVariant = Color(0xFF77575C),
        ),
darkColorScheme(
            primary = Color(0xFFE8A3B0),
            onPrimary = Color(0xFF38121F),
            secondary = Color(0xFFBC9398),
            tertiary = Color(0xFFD67F93),
            background = Color(0xFF1B1013),
            onBackground = Color(0xFFF6E4E5),
            surface = Color(0xFF281A1E),
            onSurface = Color(0xFFF6E4E5),
            surfaceVariant = Color(0xFF352327),
            onSurfaceVariant = Color(0xFFCDADB1),
        ),
        defaultDark = false,
        listOf(Color(0xFFA1465C), Color(0xFFE8A3B0), Color(0xFF1B1013)),
    ),
    ZadTheme(
        "gold", "ذهبي", "Gold",
lightColorScheme(
            primary = Color(0xFF77590E),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFA08744),
            tertiary = Color(0xFFB98A2F),
            background = Color(0xFFFAF6EC),
            onBackground = Color(0xFF231B0E),
            surface = Color(0xFFFFFDF7),
            onSurface = Color(0xFF231B0E),
            surfaceVariant = Color(0xFFF0E7D2),
            onSurfaceVariant = Color(0xFF6E6142),
        ),
darkColorScheme(
            primary = Color(0xFFE3B94E),
            onPrimary = Color(0xFF241A05),
            secondary = Color(0xFFC7A96B),
            tertiary = Color(0xFF9C7B2E),
            background = Color(0xFF140F08),
            onBackground = Color(0xFFF4EBD6),
            surface = Color(0xFF20180E),
            onSurface = Color(0xFFF4EBD6),
            surfaceVariant = Color(0xFF2E2415),
            onSurfaceVariant = Color(0xFFC8B898),
        ),
        defaultDark = false,
        listOf(Color(0xFF140F08), Color(0xFFE3B94E), Color(0xFF77590E)),
    )
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
fun ThemePicker(selected: String, isDark: Boolean, onSelect: (String, Boolean) -> Unit) {
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
                    .clickable { onSelect(theme.id, isDark) }
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = isSelected && !isDark,
                        onClick = { onSelect(theme.id, false) },
                        label = { Text("☀ فاتح", fontSize = 12.sp) },
                    )
                    androidx.compose.material3.FilterChip(
                        selected = isSelected && isDark,
                        onClick = { onSelect(theme.id, true) },
                        label = { Text("🌙 داكن", fontSize = 12.sp) },
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
