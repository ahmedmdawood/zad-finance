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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    var name by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("coffee_dark") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("زاد", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("إدارة أموالك، بطريقتك.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        Spacer(Modifier.height(28.dp))
        Text("تحب أناديك بإيه؟", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Text("اختار الثيم", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        ThemePicker(selected = theme, onSelect = { theme = it })
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { if (name.isNotBlank()) vm.completeOnboarding(name.trim(), theme) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("يلا نبدأ", fontSize = 16.sp) }
    }
}

@Composable
fun HomeTab(vm: MainViewModel) {
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()
    val name by vm.userName.collectAsState()
    val special by vm.isSpecial.collectAsState()
    val salaryExpr by vm.expectedSalary.collectAsState()
    val confirmations by vm.salaryConfirmations.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("أهلاً يا", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (special) GoldColor else MaterialTheme.colorScheme.onBackground,
                        )
                        if (special) {
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            AdBadge()
                        }
                    }
                }
                IconButton(onClick = { vm.onEyeTapped() }) {
                    Icon(
                        if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "الخصوصية",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("صافي الوضع", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(
                        vm.totalAssets() - vm.totalLiabilities(), hidden,
                        fontSize = 28, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    TotalRow("إجمالي الأصول", vm.totalAssets(), hidden)
                    TotalRow("الالتزامات", vm.totalLiabilities(), hidden)
                    TotalRow("السيولة الحرة", vm.freeLiquidity(), hidden)
                }
            }
        }

        val overBudget = vm.overBudgetCategories()
        if (overBudget.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("تنبيه ميزانية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        overBudget.forEach { (category, spent) ->
                            Text(
                                "عدّيت ميزانية $category الشهر ده (${if (hidden) MASKED_MONEY else formatMoney(spent)})",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        if (salaryExpr.isNotBlank() && confirmations.none { it.monthKey == vm.currentMonthKey() }) {
            item { SalaryCard(vm, salaryExpr, hidden) }
        }

        item {
            Text("إجراءات سريعة", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction("دخل", Modifier.weight(1f)) { vm.openOverlay(Overlay.AddTransaction("INCOME")) }
                QuickAction("مصروف", Modifier.weight(1f)) { vm.openOverlay(Overlay.AddTransaction("EXPENSE")) }
                QuickAction("تحويل", Modifier.weight(1f)) { vm.openOverlay(Overlay.AddTransaction("TRANSFER")) }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("أحدث العمليات", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        if (transactions.isEmpty()) {
            item {
                Text(
                    "لسه مفيش عمليات. ابدأ بإضافة حساب من تبويب الحسابات.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )
            }
        } else {
            items(transactions.take(15), key = { it.id }) { tx ->
                TransactionRow(vm, tx, accounts, hidden)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun TotalRow(label: String, amount: Long, hidden: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(amount, hidden, fontSize = 14)
    }
}

@Composable
fun QuickAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier) { Text(label, fontSize = 14.sp) }
}

@Composable
fun SalaryCard(vm: MainViewModel, salaryExpr: String, hidden: Boolean) {
    val accounts by vm.accounts.collectAsState()
    var selectedAccount by remember { mutableStateOf<Long?>(null) }
    val preview = remember(salaryExpr) {
        try { evaluateToPiasters(salaryExpr).first } catch (e: EvalException) { null }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("تأكيد مرتب الشهر", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (preview != null) {
                MoneyText(preview, hidden, fontSize = 18, fontWeight = FontWeight.Bold)
            }
            AccountChips(accounts.filter { it.type != "CREDIT" }, selectedAccount) { selectedAccount = it }
            Button(
                onClick = { selectedAccount?.let { vm.confirmSalary(it, salaryExpr) } },
                enabled = selectedAccount != null && preview != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("تأكيد استلام المرتب") }
        }
    }
}

@Composable
fun TransactionRow(vm: MainViewModel, tx: TransactionEntity, accounts: List<AccountEntity>, hidden: Boolean) {
    var confirmDelete by remember { mutableStateOf(false) }
    val accountName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "؟"
    val toName = tx.toAccountId?.let { id -> accounts.firstOrNull { it.id == id }?.name }
    val (label, sign, color) = when (tx.type) {
        "INCOME" -> Triple("دخل", "+", IncomeGreen)
        "EXPENSE" -> Triple("مصروف", "-", ExpenseRed)
        "TRANSFER" -> Triple("تحويل", "", MaterialTheme.colorScheme.primary)
        "DEBT_GIVEN" -> Triple("إقراض", "-", ExpenseRed)
        "DEBT_COLLECTED" -> Triple("تحصيل دين", "+", IncomeGreen)
        "DEBT_BORROWED" -> Triple("استلاف", "+", IncomeGreen)
        "DEBT_REPAID" -> Triple("سداد دين", "-", ExpenseRed)
        "CARD_PURCHASE" -> Triple("شراء بالبطاقة", "+", ExpenseRed)
        "CARD_PAYMENT" -> Triple("سداد بطاقة", "", MaterialTheme.colorScheme.primary)
        else -> Triple(tx.type, "", MaterialTheme.colorScheme.onSurface)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (tx.note.isNotBlank()) tx.note else label,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                )
                Text(
                    buildString {
                        append(label)
                        append(" · ")
                        append(accountName)
                        if (toName != null) { append(" → "); append(toName) }
                    },
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!hidden && tx.amountExpression != null && tx.amountExpression != tx.amountPiasters.toString()) {
                    Text(
                        tx.amountExpression,
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (hidden) MASKED_MONEY else "$sign${formatMoney(tx.amountPiasters)}",
                    color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                )
                Text(
                    SimpleDateFormat("d/M/yyyy", Locale("ar")).format(Date(tx.createdAt)),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف العملية؟") },
            text = { Text("هيتم حذف العملية وتحديث الأرصدة المرتبطة بيها.") },
            confirmButton = {
                Button(onClick = { vm.deleteTransaction(tx.id); confirmDelete = false }) { Text("حذف") }
            },
            dismissButton = {
                Button(onClick = { confirmDelete = false }) { Text("إلغاء") }
            },
        )
    }
}
