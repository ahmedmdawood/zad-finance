package com.ahmed.zad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun dayLabel(epoch: Long): String =
    SimpleDateFormat("d/M/yyyy", Locale("ar")).format(Date(epoch))

private fun startOfToday(): Long {
    val now = System.currentTimeMillis()
    return now - (now % 86_400_000L)
}

@Composable
fun ObligationsTab(vm: MainViewModel) {
    val obligations by vm.obligations.collectAsState()
    val occurrences by vm.occurrences.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()

    LaunchedEffect(obligations.size) { vm.ensureOccurrences() }

    val upcoming = occurrences.filter { it.status == "PENDING" }.sortedBy { it.dueDate }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("الالتزامات الدورية", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = { vm.openOverlay(Overlay.AddObligation) }) { Text("إضافة") }
            }
        }
        if (obligations.isEmpty()) {
            item {
                Text(
                    "سجّل التزاماتك المتكررة: إيجار، اشتراكات، أقساط. هنفتكرك بكل استحقاق.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )
            }
        }
        items(upcoming.take(20), key = { it.id }) { occurrence ->
            val obligation = obligations.firstOrNull { it.id == occurrence.obligationId } ?: return@items
            val overdue = occurrence.dueDate < System.currentTimeMillis() - 86_400_000L
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(obligation.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            (if (overdue) "متأخر · " else "يستحق ") + dayLabel(occurrence.dueDate),
                            fontSize = 12.sp,
                            color = if (overdue) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        MoneyText(obligation.amountPiasters, hidden, fontSize = 15, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { vm.skipOccurrence(occurrence.id) }) {
                                Text("تخطي", fontSize = 12.sp)
                            }
                            Button(onClick = { vm.openOverlay(Overlay.PayOccurrence(occurrence.id)) }) {
                                Text("دفع", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun PlansTab(vm: MainViewModel) {
    val plans by vm.plans.collectAsState()
    val allocations by vm.allocations.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("الخطط المالية", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = { vm.openOverlay(Overlay.AddPlan) }) { Text("خطة جديدة") }
            }
            Text(
                "تخصيص مبلغ لخطة مش مصروف؛ ده حجز جزء من سيولتك لهدف معين.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (plans.isEmpty()) {
            item {
                Text(
                    "اعمل خطة لأي هدف: موبايل جديد، سفر، طوارئ.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )
            }
        }
        items(plans, key = { it.id }) { plan ->
            val allocated = planAllocated(plan.id, allocations)
            val spent = planSpent(plan.id, transactions)
            val progress = if (plan.targetPiasters > 0)
                (allocated.toFloat() / plan.targetPiasters.toFloat()).coerceIn(0f, 1f) else 0f
            val overBudget = allocated > 0 && spent > allocated
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().clickable { vm.openOverlay(Overlay.PlanDetail(plan.id)) },
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(plan.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                when (plan.status) {
                                    "PAUSED" -> "متوقفة"
                                    "COMPLETED" -> "مكتملة"
                                    else -> "نشطة"
                                },
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            MoneyText(allocated, hidden, fontSize = 16, fontWeight = FontWeight.Bold)
                            Text(
                                if (hidden) "" else "الهدف: ${formatMoney(plan.targetPiasters)}",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    if (overBudget) {
                        Text(
                            "تحذير: المصروف على الخطة عدّى المبلغ المخصص",
                            color = ExpenseRed, fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
