package com.ahmed.zad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        ThemePicker(selected = theme, isDark = false, onSelect = { id, _ -> theme = id })
        Spacer(Modifier.height(24.dp))
        PressScaleButton(
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
    val todaySpend by vm.todaySpendPiasters.collectAsState()
    val remainingBudget by vm.remainingMonthBudgetPiasters.collectAsState()
    val projection by vm.projectedMonthSpendPiasters.collectAsState()
    val projectionDismissed by vm.projectionDismissed.collectAsState()
    val challenge by vm.underBudgetDaysThisWeek.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val filteredTransactions by vm.filteredTransactions.collectAsState()

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.secondary,
                            )
                        )
                    )
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "💼 صافي الوضع",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                    MoneyText(
                        vm.totalAssets() - vm.totalLiabilities(), hidden,
                        fontSize = 30, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    TotalRow("إجمالي الأصول", vm.totalAssets(), hidden, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), MaterialTheme.colorScheme.onPrimary)
                    TotalRow("الالتزامات", vm.totalLiabilities(), hidden, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), MaterialTheme.colorScheme.onPrimary)
                    TotalRow("السيولة الحرة", vm.freeLiquidity(), hidden, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("النهارده", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(todaySpend, hidden, fontSize = 22, fontWeight = FontWeight.Bold)
                    remainingBudget?.let { remaining ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "المتبقي من ميزانيتك الشهرية",
                                Modifier.weight(1f), fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            MoneyText(
                                remaining, hidden, fontSize = 13,
                                color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        val projected = projection
        if (projected != null && !projectionDismissed) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "على المنوال ده، الشهر هيقفل على حوالي ${if (hidden) MASKED_MONEY else formatMoney(projected)}",
                            Modifier.weight(1f), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { vm.dismissProjection() }) {
                            Text("إخفاء", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        val challengeState = challenge
        if (challengeState != null && challengeState.second >= 2) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "التحدي: ${challengeState.first} من ${challengeState.second} أيام الأسبوع ده جوه ميزانيتك اليومية",
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction("التقارير", Modifier.weight(1f)) { vm.openOverlay(Overlay.Charts) }
                QuickAction("التقويم", Modifier.weight(1f)) { vm.openOverlay(Overlay.Calendar) }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("أحدث العمليات", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث بالمبلغ أو الفئة أو الملاحظة", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        }
        if (filteredTransactions.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Text(
                    "لا توجد نتائج مطابقة للبحث.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )
            }
        } else if (searchQuery.isNotBlank()) {
            items(filteredTransactions.take(30), key = { it.id }) { tx ->
                TransactionRow(vm, tx, accounts, hidden)
            }
        } else if (transactions.isEmpty()) {
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
fun TotalRow(
    label: String,
    amount: Long,
    hidden: Boolean,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    amountColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp, color = labelColor)
        MoneyText(amount, hidden, fontSize = 14, color = amountColor)
    }
}

@Composable
fun QuickAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val emoji = when (label) {
        "دخل" -> "💵"
        "مصروف" -> "💸"
        "تحويل" -> "🔁"
        "التقارير" -> "📊"
        "التقويم" -> "📅"
        else -> "✨"
    }
    PressScaleButton(onClick = onClick, modifier = modifier) {
        Text("$emoji $label", fontSize = 14.sp)
    }
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
            PressScaleButton(
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
            val bubbleEmoji = when (tx.type) {
                "EXPENSE" -> categoryEmoji(tx.category)
                "INCOME" -> "💵"
                "TRANSFER" -> "🔁"
                "DEBT_GIVEN", "DEBT_COLLECTED", "DEBT_BORROWED", "DEBT_REPAID" -> "🤝"
                "CARD_PURCHASE", "CARD_PAYMENT" -> "💳"
                else -> "✨"
            }
            val bubbleTint = when (tx.type) {
                "EXPENSE" -> ExpenseRed
                "INCOME", "DEBT_COLLECTED", "DEBT_BORROWED" -> IncomeGreen
                else -> MaterialTheme.colorScheme.primary
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bubbleTint.copy(alpha = 0.14f)),
            ) {
                Text(bubbleEmoji, fontSize = 17.sp)
            }
            Spacer(Modifier.width(10.dp))
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
                tx.receiptPath?.let { ReceiptView(it) }
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
                PressScaleButton(onClick = { vm.deleteTransaction(tx.id); confirmDelete = false }) { Text("حذف") }
            },
            dismissButton = {
                PressScaleButton(onClick = { confirmDelete = false }) { Text("إلغاء") }
            },
        )
    }
}
