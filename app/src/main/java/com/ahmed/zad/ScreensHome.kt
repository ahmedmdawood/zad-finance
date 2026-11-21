package com.ahmed.zad

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
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
    val theme by vm.themeId.collectAsState()
    val darkOverride by vm.darkOverride.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 96.dp),
        ) {
            Spacer(Modifier.height(40.dp))
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
            ThemePicker(selected = theme, isDark = darkOverride == 1, onSelect = { id, d -> vm.setTheme(id); vm.setDarkOverride(if (d) 1 else 0) })
            Spacer(Modifier.height(24.dp))
        }
        PressScaleButton(
            onClick = { if (name.isNotBlank()) vm.completeOnboarding(name.trim(), theme) },
            enabled = name.isNotBlank(),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
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

        item { BankHeroCard(vm, name, special, hidden) }

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
                QuickAction("ملاحظات", Modifier.weight(1f)) { vm.openOverlay(Overlay.Notes) }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("أحدث العمليات", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        if (transactions.isEmpty()) {
            item { Text("لسه مفيش عمليات. ابدأ بإضافة حساب من تبويب الحسابات.", fontSize = 14.sp) }
        } else {
            items(transactions.take(3), key = { it.id }) { tx ->
                if (tx.hidden) HiddenTransactionCard()
                else TransactionRow(vm, tx, accounts, hidden, readOnly = true)
            }
            item {
                TextButton(onClick = { vm.openOverlay(Overlay.FullLog) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (transactions.size > 3) "عرض السجل بالكامل" else "إدارة السجل")
                }
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

/** Drawn quick-action icons (no emojis): simple stroke glyphs in brand colors. */
@Composable
fun QuickActionGlyph(label: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        when (label) {
            "دخل" -> { // arrow down into a tray
                drawLine(color, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.5f, h * 0.62f), stroke.width, cap = StrokeCap.Round)
                val head = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.24f, h * 0.44f); lineTo(w * 0.5f, h * 0.68f); lineTo(w * 0.76f, h * 0.44f)
                }
                drawPath(head, color, style = stroke)
                drawLine(color, Offset(w * 0.14f, h * 0.9f), Offset(w * 0.86f, h * 0.9f), stroke.width, cap = StrokeCap.Round)
            }
            "مصروف" -> { // arrow up out of a tray
                drawLine(color, Offset(w * 0.5f, h * 0.68f), Offset(w * 0.5f, h * 0.16f), stroke.width, cap = StrokeCap.Round)
                val head = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.24f, h * 0.34f); lineTo(w * 0.5f, h * 0.1f); lineTo(w * 0.76f, h * 0.34f)
                }
                drawPath(head, color, style = stroke)
                drawLine(color, Offset(w * 0.14f, h * 0.9f), Offset(w * 0.86f, h * 0.9f), stroke.width, cap = StrokeCap.Round)
            }
            "تحويل" -> { // two opposing horizontal arrows
                drawLine(color, Offset(w * 0.16f, h * 0.32f), Offset(w * 0.84f, h * 0.32f), stroke.width, cap = StrokeCap.Round)
                val head1 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.62f, h * 0.16f); lineTo(w * 0.86f, h * 0.32f); lineTo(w * 0.62f, h * 0.48f)
                }
                drawPath(head1, color, style = stroke)
                drawLine(color, Offset(w * 0.84f, h * 0.68f), Offset(w * 0.16f, h * 0.68f), stroke.width, cap = StrokeCap.Round)
                val head2 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.38f, h * 0.52f); lineTo(w * 0.14f, h * 0.68f); lineTo(w * 0.38f, h * 0.84f)
                }
                drawPath(head2, color, style = stroke)
            }
            "التقارير" -> { // bar chart
                drawLine(color, Offset(w * 0.2f, h * 0.55f), Offset(w * 0.2f, h * 0.84f), stroke.width * 1.5f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.5f, h * 0.84f), stroke.width * 1.5f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.8f, h * 0.16f), Offset(w * 0.8f, h * 0.84f), stroke.width * 1.5f, cap = StrokeCap.Round)
            }
            "التقويم" -> { // calendar page
                drawRoundRect(
                    color,
                    topLeft = Offset(w * 0.14f, h * 0.2f),
                    size = Size(w * 0.72f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.14f, h * 0.4f), Offset(w * 0.86f, h * 0.4f), stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.32f, h * 0.1f), Offset(w * 0.32f, h * 0.26f), stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.68f, h * 0.1f), Offset(w * 0.68f, h * 0.26f), stroke.width, cap = StrokeCap.Round)
            }
            else -> { // ملاحظات: note sheet with a line
                drawRoundRect(
                    color,
                    topLeft = Offset(w * 0.2f, h * 0.1f),
                    size = Size(w * 0.6f, h * 0.8f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.32f, h * 0.36f), Offset(w * 0.68f, h * 0.36f), stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.32f, h * 0.54f), Offset(w * 0.68f, h * 0.54f), stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun QuickAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tint = when (label) {
        "دخل" -> IncomeGreen
        "مصروف" -> ExpenseRed
        else -> MaterialTheme.colorScheme.onPrimary
    }
    PressScaleButton(onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            QuickActionGlyph(label, tint)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, maxLines = 1)
        }
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
fun TransactionRow(vm: MainViewModel, tx: TransactionEntity, accounts: List<AccountEntity>, hidden: Boolean, readOnly: Boolean = false) {
    var confirmDelete by remember { mutableStateOf(false) }
    val accountName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "حساب محذوف"
    val toName = tx.toAccountId?.let { id -> accounts.firstOrNull { it.id == id }?.name ?: "حساب محذوف" }
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
        modifier = if (readOnly) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().clickable { confirmDelete = true },
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


/** Shared hero-card chrome: theme gradient + decorative circles. */
@Composable
fun HeroCardFrame(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.secondary,
                    )
                )
            ),
    ) {
        Box(Modifier.size(180.dp).align(Alignment.TopStart).offset((-60).dp, (-60).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)))
        Box(Modifier.size(120.dp).align(Alignment.TopStart).offset((-15).dp, (-90).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
        Box(Modifier.size(150.dp).align(Alignment.BottomEnd).offset(50.dp, 50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))
        content()
    }
}

/** Small round flip button shown on both faces of the hero card (visual top-left). */
@Composable
fun HeroFlipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Autorenew,
            contentDescription = "اقلب البطاقة",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun BankHeroCard(vm: MainViewModel, name: String, special: Boolean, hidden: Boolean) {
    val context = LocalContext.current
    val photoSet by vm.userPhotoSet.collectAsState()
    val photoRevision by vm.userPhotoRevision.collectAsState()
    val monthSpend by vm.monthSpendPiasters.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val remainingBudget by vm.remainingMonthBudgetPiasters.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.transactions.collectAsState()

    val photo = remember(photoSet, photoRevision) {
        if (!photoSet) null else runCatching {
            android.graphics.BitmapFactory.decodeFile(context.filesDir.resolve("user_photo.jpg").absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    val message = remember(monthSpend, budgets, remainingBudget) {
        when {
            budgets.isNotEmpty() && remainingBudget != null && remainingBudget!! <= 0L ->
                "الميزانية قربت تخلص بدري شوية.. خد نفس، بكرة يوم جديد 🌱"
            budgets.isNotEmpty() && remainingBudget != null ->
                "ميزانيتك في الأمان لحد دلوقتي، كمّل على البركة 💪"
            monthSpend == 0L ->
                "صفحة جديدة وشهر جديد.. يلا نكتبه صح من أوله ✨"
            else ->
                "كل قرش بيتسجل هنا، ودي أول خطوة تتحكم في فلوسك 👌"
        }
    }

    var flipped by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(600),
        label = "heroFlip",
    )
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val collapsedHeight = (screenWidth - 32.dp) / 1.6f
    val cardHeight by animateDpAsState(
        targetValue = if (expanded) collapsedHeight + 340.dp else collapsedHeight,
        animationSpec = tween(500),
        label = "heroGrow",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
    ) {
        if (rotation <= 90f) {
            // FRONT: total picture, unchanged design.
            HeroCardFrame(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth().padding(end = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("زاد", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.size(width = 34.dp, height = 24.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f))
                        )
                    }
                    Column {
                        Text("صافي الوضع", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        MoneyText(
                            vm.totalAssets() - vm.totalLiabilities(), hidden,
                            fontSize = 28, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (photo != null) {
                            Image(
                                bitmap = photo, contentDescription = "صورتك",
                                modifier = Modifier.size(46.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                Modifier.size(46.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                                    .border(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(name.firstOrNull()?.toString() ?: "ز", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                if (special) {
                                    Spacer(Modifier.width(4.dp))
                                    AdBadge(Modifier.size(18.dp))
                                }
                            }
                            Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), maxLines = 2)
                        }
                    }
                }
                HeroFlipButton(onClick = { flipped = true }, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
        } else {
            // BACK: every account with its own balance.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                HeroCardFrame(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(18.dp)) {
                        Row(Modifier.fillMaxWidth().padding(end = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("حساباتك", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.height(10.dp))
                        val shown = if (expanded) accounts else accounts.take(3)
                        val backScroll = rememberScrollState()
                        Column(
                            modifier = Modifier.weight(1f).let { m -> if (expanded) m.verticalScroll(backScroll) else m },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (accounts.isEmpty()) {
                                Text("لسه مفيش حسابات. ضيف أول حساب من تبويب الحسابات.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                            }
                            shown.forEach { account ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                                        Text(
                                            accountTypeLabel(account.type),
                                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                        )
                                    }
                                    MoneyText(
                                        accountBalance(account, transactions), hidden,
                                        fontSize = 15, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                        if (accounts.size > 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { expanded = !expanded }.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (expanded) "عرض أقل" else "عرض المزيد",
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                    HeroFlipButton(onClick = { flipped = false; expanded = false }, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
                }
            }
        }
    }
}


@Composable
fun HiddenTransactionCard(onReveal: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VisibilityOff, contentDescription = null)
            Text("عملية مخفية بكلمة مرور", modifier = Modifier.weight(1f).padding(horizontal = 10.dp), fontSize = 14.sp)
            if (onReveal != null) TextButton(onClick = onReveal) { Text("إظهار") }
        }
    }
}

@Composable
fun FullLogScreen(vm: MainViewModel) {
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()
    val hasPassword by vm.hasPassword.collectAsState()
    val logUnlocked by vm.logUnlocked.collectAsState()
    var query by remember { mutableStateOf("") }
    var passwordAction by remember { mutableStateOf<String?>(null) }
    var pendingHide by remember { mutableStateOf<Long?>(null) }
    var password by remember { mutableStateOf("") }
    var passwordAgain by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<TransactionEntity?>(null) }
    val shown = transactions.filter { tx ->
        if (tx.hidden && !logUnlocked) query.isBlank()
        else query.isBlank() || tx.note.contains(query, true) || tx.category.contains(query, true) ||
            formatMoney(tx.amountPiasters).contains(query) || accounts.any { it.id == tx.accountId && it.name.contains(query, true) }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("السجل بالكامل", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.closeOverlay() }) { Text("رجوع") }
            }
            Text("التعديل والحذف والإخفاء من هنا. العمليات المخفية بتفضل محسوبة في الأرصدة.", fontSize = 12.sp)
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("بحث في السجل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (shown.isEmpty()) item { Text("لا توجد عمليات مطابقة.") }
        items(shown, key = { it.id }) { tx ->
            if (tx.hidden && !logUnlocked) {
                HiddenTransactionCard(onReveal = { password = ""; message = null; passwordAction = "unlock" })
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TransactionRow(vm, tx, accounts, hidden, readOnly = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { vm.openOverlay(Overlay.AddTransaction(tx.type, tx.id)) }) { Text("تعديل") }
                        TextButton(onClick = { deleting = tx }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = {
                            if (tx.hidden) vm.setTransactionHidden(tx.id, false)
                            else if (hasPassword) vm.setTransactionHidden(tx.id, true)
                            else {
                                pendingHide = tx.id; password = ""; passwordAgain = ""; message = null; passwordAction = "create"
                            }
                        }) { Text(if (tx.hidden) "إلغاء الإخفاء" else "إخفاء بكلمة مرور") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    deleting?.let { tx ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف العملية؟") },
            text = { Text("هتتنقل لسلة المحذوفات، والأرصدة المرتبطة بيها هتتحدث. تقدر ترجعها من السلة.") },
            confirmButton = { TextButton(onClick = { vm.deleteTransaction(tx.id); deleting = null }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } },
        )
    }
    if (passwordAction != null) {
        val creating = passwordAction == "create"
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { passwordAction = null; pendingHide = null },
            title = { Text(if (creating) "حماية العملية بكلمة مرور" else "إظهار العمليات المخفية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (creating) "كلمة المرور دي هتحمي العمليات المخفية والأرقام الخاصة. احفظها كويس." else "اكتب كلمة المرور عشان تظهر العمليات المخفية لحد ما تخرج من صفحة السجل.", fontSize = 13.sp)
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                    if (creating) OutlinedTextField(value = passwordAgain, onValueChange = { passwordAgain = it }, label = { Text("تأكيد كلمة المرور") }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            },
            confirmButton = { TextButton(onClick = {
                if (creating) {
                    if (password.length < 4) message = "اكتب 4 حروف أو أرقام على الأقل."
                    else if (password != passwordAgain) message = "كلمتا المرور مش متطابقتين."
                    else {
                        vm.setPassword(password)
                        pendingHide?.let { vm.setTransactionHidden(it, true) }
                        pendingHide = null; passwordAction = null; password = ""; passwordAgain = ""
                    }
                } else {
                    when (val result = vm.tryUnlockLog(password)) {
                        is MainViewModel.UnlockResult.Success -> { passwordAction = null; password = "" }
                        is MainViewModel.UnlockResult.Wrong -> message = "كلمة مرور غلط. متبقي ${result.attemptsLeft} محاولات."
                        is MainViewModel.UnlockResult.Locked -> message = "استنى ${result.secondsLeft} ثانية قبل المحاولة من تاني."
                    }
                }
            }) { Text(if (creating) "حفظ وإخفاء" else "فتح") } },
            dismissButton = { TextButton(onClick = { passwordAction = null; pendingHide = null }) { Text("إلغاء") } },
        )
    }
}
