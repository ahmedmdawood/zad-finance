package com.ahmed.zad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccountChips(
    accounts: List<AccountEntity>,
    selected: Long?,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        accounts.forEach { account ->
            FilterChip(
                selected = selected == account.id,
                onClick = { onSelect(account.id) },
                label = { Text("${account.name} (${accountTypeLabel(account.type)})") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FormScaffold(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onClose) { Text("إلغاء", fontSize = 13.sp) }
        }
        content()
    }
}

private fun validatePositive(expression: String): Pair<Long, String?> {
    return try {
        val (piasters, _) = evaluateToPiasters(expression)
        if (piasters <= 0) 0L to "المبلغ لازم يكون أكبر من صفر" else piasters to null
    } catch (e: EvalException) {
        0L to (e.message ?: "خطأ في التعبير")
    }
}

@Composable
fun AddTransactionScreen(vm: MainViewModel, type: String) {
    val accounts by vm.accounts.collectAsState()
    val plans by vm.plans.collectAsState()
    val liquid = accounts.filter { it.type != "CREDIT" }

    var expression by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf<Long?>(null) }
    var toAccount by remember { mutableStateOf<Long?>(null) }
    var planId by remember { mutableStateOf<Long?>(null) }
    var category by remember { mutableStateOf("أخرى") }
    var receiptPath by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val voiceEnabled by vm.voiceEnabled.collectAsState()
    val budgets by vm.budgets.collectAsState()

    val title = when (type) {
        "INCOME" -> "تسجيل دخل"
        "EXPENSE" -> "تسجيل مصروف"
        else -> "تحويل بين حساباتك"
    }

    FormScaffold(title, onClose = { vm.closeOverlay() }) {
        VoiceAmountButton(enabled = voiceEnabled, onResult = { expression = it })
        CalculatorAmountInput(value = expression, onValueChange = { expression = it }, label = "المبلغ")
        if (type == "TRANSFER") {
            Text("من حساب", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            AccountChips(liquid, fromAccount) { fromAccount = it }
            Text("إلى حساب", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            AccountChips(liquid.filter { it.id != fromAccount }, toAccount) { toAccount = it }
        } else {
            Text("الحساب", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            AccountChips(liquid, fromAccount) { fromAccount = it }
        }
        if (type == "EXPENSE") {
            Text("الفئة", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            CategoryChips(selected = category, categories = vm.allCategories(), onSelect = { category = it })
            budgets.firstOrNull { it.category == category }?.let { budget ->
                val spent = vm.categorySpentThisMonth(category)
                val candidate = try { evaluateToPiasters(expression).first } catch (e: EvalException) { 0L }
                if (candidate > 0 && spent + candidate > budget.limitPiasters) {
                    Text(
                        "تنبيه: المبلغ ده هيعدّي ميزانية $category (${formatMoney(spent)} من ${formatMoney(budget.limitPiasters)} اتصرفت الشهر ده)",
                        color = ExpenseRed, fontSize = 12.sp,
                    )
                }
            }
        }
        if (type == "EXPENSE" && plans.any { it.status == "ACTIVE" }) {
            Text("خطة مرتبطة (اختياري)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plans.filter { it.status == "ACTIVE" }.forEach { plan ->
                    FilterChip(
                        selected = planId == plan.id,
                        onClick = { planId = if (planId == plan.id) null else plan.id },
                        label = { Text(plan.name, fontSize = 13.sp) },
                    )
                }
            }
        }
        if (type == "EXPENSE") {
            ReceiptPicker(receiptPath = receiptPath, onPicked = { receiptPath = it })
        }
        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text("ملاحظة (اختياري)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                val (_, err) = validatePositive(expression)
                error = when {
                    err != null -> err
                    fromAccount == null -> "اختار الحساب"
                    type == "TRANSFER" && toAccount == null -> "اختار حساب التحويل"
                    else -> null
                }
                if (error == null) {
                    vm.addTransaction(
                        type, expression, fromAccount!!, toAccount, note.trim(), planId, category, receiptPath
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("حفظ") }
        if (liquid.isEmpty()) {
            Text("ضيف حساب الأول من تبويب الحسابات.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
}

@Composable
fun CardPurchaseScreen(vm: MainViewModel, accountId: Long) {
    var expression by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    FormScaffold("شراء بالبطاقة", onClose = { vm.closeOverlay() }) {
        Text("الشراء بيزوّد المستحق على البطاقة، مش بيخصم من حسابك النقدي.", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        CalculatorAmountInput(value = expression, onValueChange = { expression = it }, label = "المبلغ")
        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text("ملاحظة (اختياري)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                val (_, err) = validatePositive(expression)
                error = err
                if (err == null) {
                    vm.addTransaction("CARD_PURCHASE", expression, accountId, null, note.trim())
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("حفظ") }
    }
}

@Composable
fun CardPaymentScreen(vm: MainViewModel, accountId: Long) {
    val accounts by vm.accounts.collectAsState()
    val liquid = accounts.filter { it.type != "CREDIT" }
    var expression by remember { mutableStateOf("") }
    var fromAccount by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    FormScaffold("سداد البطاقة", onClose = { vm.closeOverlay() }) {
        Text("السداد بيقلل المستحق على البطاقة وبيخصم من الحساب اللي دفعت منه.", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        CalculatorAmountInput(value = expression, onValueChange = { expression = it }, label = "المبلغ")
        Text("الدفع من حساب", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        AccountChips(liquid, fromAccount) { fromAccount = it }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                val (_, err) = validatePositive(expression)
                error = err ?: if (fromAccount == null) "اختار حساب الدفع" else null
                if (error == null) {
                    vm.addTransaction("CARD_PAYMENT", expression, fromAccount!!, accountId, "")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("حفظ") }
    }
}

@Composable
fun AddAccountScreen(vm: MainViewModel) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CASH") }
    var opening by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    FormScaffold("إضافة حساب", onClose = { vm.closeOverlay() }) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("اسم الحساب") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CASH" to "نقدي", "BANK" to "بنكي", "EWALLET" to "محفظة إلكترونية").forEach { (id, label) ->
                FilterChip(selected = type == id, onClick = { type = id }, label = { Text(label, fontSize = 13.sp) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CREDIT" to "ائتمانية", "OTHER" to "أخرى").forEach { (id, label) ->
                FilterChip(selected = type == id, onClick = { type = id }, label = { Text(label, fontSize = 13.sp) })
            }
        }
        if (type == "CREDIT") {
            CalculatorAmountInput(value = limit, onValueChange = { limit = it }, label = "الحد الائتماني")
        } else {
            CalculatorAmountInput(value = opening, onValueChange = { opening = it }, label = "الرصيد الافتتاحي")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                error = when {
                    name.isBlank() -> "اكتب اسم الحساب"
                    type != "CREDIT" && opening.isBlank() -> "اكتب الرصيد الافتتاحي"
                    type != "CREDIT" -> try { evaluateToPiasters(opening); null } catch (e: EvalException) { e.message }
                    limit.isBlank() -> null
                    else -> try { evaluateToPiasters(limit); null } catch (e: EvalException) { e.message }
                }
                if (error == null) {
                    vm.addAccount(name.trim(), type, if (type == "CREDIT") "0" else opening, limit.ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("حفظ الحساب") }
    }
}

@Composable
fun AddDebtScreen(vm: MainViewModel) {
    val accounts by vm.accounts.collectAsState()
    val liquid = accounts.filter { it.type != "CREDIT" }
    var person by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("LENT") }
    var expression by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    FormScaffold("إضافة دين", onClose = { vm.closeOverlay() }) {
        OutlinedTextField(
            value = person, onValueChange = { person = it },
            label = { Text("اسم الشخص") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = direction == "LENT", onClick = { direction = "LENT" },
                label = { Text("لي عنده (أنا اللي سلّفت)") })
            FilterChip(selected = direction == "BORROWED", onClick = { direction = "BORROWED" },
                label = { Text("عليا ليه (أنا اللي استلفت)") })
        }
        CalculatorAmountInput(value = expression, onValueChange = { expression = it }, label = "المبلغ")
        if (liquid.isNotEmpty()) {
            Text("ربط بحساب (اختياري)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            AccountChips(liquid, accountId) { accountId = if (accountId == it) null else it }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                val (_, err) = validatePositive(expression)
                error = when {
                    person.isBlank() -> "اكتب اسم الشخص"
                    err != null -> err
                    else -> null
                }
                if (error == null) vm.addDebt(person.trim(), direction, expression, accountId)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("حفظ الدين") }
    }
}

@Composable
fun PayDebtScreen(vm: MainViewModel, debtId: Long) {
    val debts by vm.debts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val liquid = accounts.filter { it.type != "CREDIT" }
    val debt = debts.firstOrNull { it.id == debtId }
    var expression by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    if (debt == null) {
        FormScaffold("تسجيل دفعة", onClose = { vm.closeOverlay() }) { Text("الدين مش موجود") }
        return
    }
    FormScaffold("دفعة - ${debt.person}", onClose = { vm.closeOverlay() }) {
        Text("المتبقي: ${formatMoney(debt.remainingPiasters)}", fontWeight = FontWeight.SemiBold)
        CalculatorAmountInput(value = expression, onValueChange = { expression = it }, label = "مبلغ الدفعة")
        if (liquid.isNotEmpty()) {
            Text(if (debt.direction == "LENT") "التحصيل في حساب (اختياري)" else "السداد من حساب (اختياري)",
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            AccountChips(liquid, accountId) { accountId = if (accountId == it) null else it }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                val (_, err) = validatePositive(expression)
                error = err
                if (err == null) vm.payDebt(debtId, expression, accountId)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("تسجيل الدفعة") }
    }
}
