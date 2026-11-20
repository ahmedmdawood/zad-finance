package com.ahmed.zad

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun accountTypeLabel(type: String): String = when (type) {
    "CASH" -> "نقدي"
    "BANK" -> "بنكي"
    "CREDIT" -> "بطاقة ائتمانية"
    else -> "أخرى"
}

@Composable
fun AccountsTab(vm: MainViewModel) {
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("الحسابات", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = { vm.openOverlay(Overlay.AddAccount) }) { Text("إضافة حساب") }
            }
        }
        if (accounts.isEmpty()) {
            item {
                Text(
                    "ضيف أول حساب: نقدي، بنكي، أو بطاقة ائتمانية.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )
            }
        }
        items(accounts, key = { it.id }) { account ->
            val balance = accountBalance(account, transactions)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(account.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                accountTypeLabel(account.type),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (account.type == "CREDIT") {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("المستحق", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(balance, hidden, fontSize = 16, fontWeight = FontWeight.Bold)
                                account.creditLimitPiasters?.let { limit ->
                                    Text(
                                        if (hidden) MASKED_MONEY
                                        else "متاح: ${formatMoney((limit - balance).coerceAtLeast(0))}",
                                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            MoneyText(balance, hidden, fontSize = 16, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (account.type == "CREDIT") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { vm.openOverlay(Overlay.CardPurchase(account.id)) },
                                modifier = Modifier.weight(1f),
                            ) { Text("شراء بالبطاقة", fontSize = 13.sp) }
                            OutlinedButton(
                                onClick = { vm.openOverlay(Overlay.CardPayment(account.id)) },
                                modifier = Modifier.weight(1f),
                            ) { Text("سداد البطاقة", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun DebtsTab(vm: MainViewModel) {
    val debts by vm.debts.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()
    var filter by remember { mutableStateOf("ALL") }

    val shown = debts.filter {
        when (filter) {
            "LENT" -> it.direction == "LENT"
            "BORROWED" -> it.direction == "BORROWED"
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("الديون", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = { vm.openOverlay(Overlay.AddDebt) }) { Text("إضافة دين") }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text("الكل") })
                FilterChip(selected = filter == "LENT", onClick = { filter = "LENT" }, label = { Text("لي") })
                FilterChip(selected = filter == "BORROWED", onClick = { filter = "BORROWED" }, label = { Text("عليا") })
            }
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    "مفيش ديون هنا. لما تسلّف حد أو تستلف، سجّلها عشان متتنسيش.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )
            }
        }
        items(shown, key = { it.id }) { debt ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(debt.person, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                if (debt.direction == "LENT") "لي عنده" else "عليا ليه",
                                fontSize = 12.sp,
                                color = if (debt.direction == "LENT") IncomeGreen else ExpenseRed,
                            )
                            if (!hidden && debt.expression != null) {
                                Text(
                                    debt.expression,
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (debt.settled) {
                                Text("اتسدد", color = IncomeGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                MoneyText(debt.remainingPiasters, hidden, fontSize = 16, fontWeight = FontWeight.Bold)
                                Text(
                                    if (hidden) "" else "من ${formatMoney(debt.principalPiasters)}",
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (!debt.settled) {
                        OutlinedButton(
                            onClick = { vm.openOverlay(Overlay.PayDebt(debt.id)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("تسجيل دفعة", fontSize = 13.sp) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
