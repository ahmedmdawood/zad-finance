package com.ahmed.zad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(vm: MainViewModel) {
    val series by vm.monthlyExpenseSeries.collectAsState()
    val topCategories by vm.topCategoriesThisMonth.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.closeOverlay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("مقارنة الشهور (مصاريف)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        val maxVal = series.maxOfOrNull { it.second } ?: 0L
                        if (maxVal <= 0) {
                            Text(
                                "لا توجد مصاريف كافية بعد لعرض المقارنة.",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            series.forEach { (label, value) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        label, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp).height(20.dp),
                                    )
                                    Box(
                                        Modifier.weight(1f).height(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val fraction = value.toFloat() / maxVal.toFloat()
                                        Box(
                                            Modifier.fillMaxWidth(fraction).height(14.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Text(
                                        if (hidden) MASKED_MONEY else formatMoney(value),
                                        fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("أعلى الفئات هذا الشهر", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (topCategories.isEmpty()) {
                            Text(
                                "لا توجد مصاريف هذا الشهر بعد.",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            val total = topCategories.sumOf { it.second }.coerceAtLeast(1)
                            topCategories.take(6).forEach { (category, value) ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row {
                                        Text(categoryEmoji(category) + " " + category, Modifier.weight(1f), fontSize = 13.sp)
                                        Text(
                                            if (hidden) MASKED_MONEY else formatMoney(value),
                                            fontSize = 13.sp,
                                        )
                                    }
                                    Box(
                                        Modifier.fillMaxWidth().height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth(value.toFloat() / total.toFloat())
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: MainViewModel) {
    val transactions by vm.transactions.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()

    val month = YearMonth.now()
    val daysInMonth = month.lengthOfMonth()
    val spendByDay: Map<Int, Long> = transactions
        .filter { it.type == "EXPENSE" }
        .groupBy { tx ->
            java.time.Instant.ofEpochMilli(tx.createdAt)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }
        .filterKeys { YearMonth.from(it) == month }
        .mapKeys { (date, _) -> date.dayOfMonth }
        .mapValues { (_, txs) -> txs.sumOf { it.amountPiasters } }

    val maxDay = spendByDay.values.maxOrNull() ?: 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تقويم المصاريف", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.closeOverlay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "${monthName(month.monthValue)} ${month.year}",
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
            )
            Row(Modifier.fillMaxWidth()) {
                listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت").forEach {
                    Text(
                        it, Modifier.weight(1f), fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Day-of-week index of the 1st, Sunday-first grid (Calendar.SUNDAY = 1).
            val firstDow = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, month.year)
                set(java.util.Calendar.MONTH, month.monthValue - 1)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
            }.get(java.util.Calendar.DAY_OF_WEEK) - 1
            val cells = List(firstDow) { 0 } + (1..daysInMonth).toList()
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        val spend = if (day > 0) spendByDay[day] else null
                        val intensity = if (spend != null && maxDay > 0)
                            0.15f + 0.65f * (spend.toFloat() / maxDay.toFloat()) else 0f
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        day == 0 -> androidx.compose.ui.graphics.Color.Transparent
                                        spend == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = intensity)
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day > 0) {
                                Text(day.toString(), fontSize = 12.sp)
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text(
                "الأيام الأغمق صرفت فيها أكثر.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun monthName(month: Int): String = when (month) {
    1 -> "يناير"; 2 -> "فبراير"; 3 -> "مارس"; 4 -> "أبريل"
    5 -> "مايو"; 6 -> "يونيو"; 7 -> "يوليو"; 8 -> "أغسطس"
    9 -> "سبتمبر"; 10 -> "أكتوبر"; 11 -> "نوفمبر"; else -> "ديسمبر"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(vm: MainViewModel) {
    val trash by vm.trash.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سلة المحذوفات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.closeOverlay() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "العمليات المحذوفة تبقى هنا ٣٠ يوم قبل الحذف النهائي.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trash.isEmpty()) {
                item {
                    Text(
                        "السلة فارغة.",
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                items(trash, key = { it.id }) { tx ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (hidden) MASKED_MONEY else formatMoney(tx.amountPiasters),
                                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                )
                                Text(
                                    tx.category + if (tx.note.isNotBlank()) " - ${tx.note}" else "",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            androidx.compose.material3.TextButton(onClick = { vm.restoreTransaction(tx.id) }) {
                                Text("استعادة", fontSize = 12.sp)
                            }
                            androidx.compose.material3.TextButton(onClick = { vm.deleteTransactionForever(tx.id) }) {
                                Text(
                                    "حذف نهائي", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
