package com.ahmed.zad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun validatePositive2(expression: String): String? {
    return try {
        val (piasters, _) = evaluateToPiasters(expression)
        if (piasters <= 0) "المبلغ لازم يكون أكبر من صفر" else null
    } catch (e: EvalException) {
        e.message ?: "خطأ في التعبير"
    }
}

@Composable
fun AddObligationScreen(vm: MainViewModel) {
    var title by remember { mutableStateOf("") }
    var expression by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("MONTHLY") }
    var customDays by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row {
            Text("التزام دوري جديد", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { vm.closeOverlay() }) { Text("إلغاء", fontSize = 13.sp) }
        }
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("اسم الالتزام (إيجار، اشتراك...)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CalculatorAmountInput(value = expression, onValueChange = { expression = it }, label = "المبلغ المتوقع")
        Text("الدورة", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = cycle == "MONTHLY", onClick = { cycle = "MONTHLY" }, label = { Text("شهري") })
            FilterChip(selected = cycle == "WEEKLY", onClick = { cycle = "WEEKLY" }, label = { Text("أسبوعي") })
            FilterChip(selected = cycle == "CUSTOM_DAYS", onClick = { cycle = "CUSTOM_DAYS" },
                label = { Text("كل عدد أيام") })
        }
        if (cycle == "CUSTOM_DAYS") {
            OutlinedTextField(
                value = customDays, onValueChange = { customDays = it.filter { c -> c.isDigit() } },
                label = { Text("عدد الأيام") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                val days = customDays.toIntOrNull()
                error = when {
                    title.isBlank() -> "اكتب اسم الالتزام"
                    validatePositive2(expression) != null -> validatePositive2(expression)
                    cycle == "CUSTOM_DAYS" && (days == null || days <= 0) -> "اكتب عدد أيام صحيح"
                    else -> null
                }
                if (error == null) {
                    val today = System.currentTimeMillis() - (System.currentTimeMillis() % 86_400_000L)
                    vm.addObligation(title.trim(), expression, cycle, days, today)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("حفظ الالتزام") }
    }
}

@Composable
fun PayOccurrenceScreen(vm: MainViewModel, occurrenceId: Long) {
    val occurrences by vm.occurrences.collectAsState()
    val obligations by vm.obligations.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val liquid = accounts.filter { it.type != "CREDIT" }
    val occurrence = occurrences.firstOrNull { it.id == occurrenceId }
    val obligation = occurrence?.let { o -> obligations.firstOrNull { it.id == o.obligationId } }
    var expression by remember(occurrenceId) { mutableStateOf(obligation?.expression ?: "") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row {
            Text("دفع: ${obligation?.title ?: ""}", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { vm.closeOverlay() }) { Text("إلغاء", fontSize = 13.sp) }
        }
        CalculatorAmountInput(value = expression, onValueChange = { expression = it }, label = "المبلغ المدفوع")
        Text("الدفع من حساب", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        AccountChips(liquid, accountId) { accountId = it }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                error = validatePositive2(expression)
                    ?: if (accountId == null) "اختار حساب الدفع" else null
                if (error == null) vm.payOccurrence(occurrenceId, expression, accountId!!)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("تسجيل الدفع") }
    }
}

@Composable
fun AddPlanScreen(vm: MainViewModel) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var initial by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row {
            Text("خطة مالية جديدة", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { vm.closeOverlay() }) { Text("إلغاء", fontSize = 13.sp) }
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("اسم الخطة") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CalculatorAmountInput(value = target, onValueChange = { target = it }, label = "المبلغ المستهدف")
        CalculatorAmountInput(value = initial, onValueChange = { initial = it }, label = "تخصيص مبدئي (اختياري)")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        PressScaleButton(
            onClick = {
                error = when {
                    name.isBlank() -> "اكتب اسم الخطة"
                    validatePositive2(target) != null -> validatePositive2(target)
                    initial.isNotBlank() -> try { evaluateToPiasters(initial); null }
                        catch (e: EvalException) { e.message }
                    else -> null
                }
                if (error == null) vm.addPlan(name.trim(), target, initial.ifBlank { null })
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("حفظ الخطة") }
    }
}

@Composable
fun PlanDetailScreen(vm: MainViewModel, planId: Long) {
    val plans by vm.plans.collectAsState()
    val allocations by vm.allocations.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()
    val plan = plans.firstOrNull { it.id == planId }
    var amountExpr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    if (plan == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            OutlinedButton(onClick = { vm.closeOverlay() }) { Text("رجوع") }
            Text("الخطة مش موجودة")
        }
        return
    }
    val allocated = planAllocated(planId, allocations)
    val spent = planSpent(planId, transactions)
    val history = allocations.filter { it.planId == planId }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row {
            Text(plan.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { vm.closeOverlay() }) { Text("رجوع", fontSize = 13.sp) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TotalRow("المخصص", allocated, hidden)
                TotalRow("المصروف من الخطة", spent, hidden)
                TotalRow("المتبقي", allocated - spent, hidden)
                TotalRow("الهدف", plan.targetPiasters, hidden)
                if (!hidden && plan.targetExpression != null) {
                    Text("تعبير الهدف: ${plan.targetExpression}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (spent > allocated && allocated > 0) {
            Text("تحذير: المصروف عدّى المبلغ المخصص", color = ExpenseRed, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.setPlanStatus(plan, if (plan.status == "PAUSED") "ACTIVE" else "PAUSED") },
                modifier = Modifier.weight(1f),
            ) { Text(if (plan.status == "PAUSED") "استئناف" else "إيقاف مؤقت", fontSize = 13.sp) }
            OutlinedButton(
                onClick = { vm.setPlanStatus(plan, if (plan.status == "COMPLETED") "ACTIVE" else "COMPLETED") },
                modifier = Modifier.weight(1f),
            ) { Text(if (plan.status == "COMPLETED") "إعادة تفعيل" else "إنهاء الخطة", fontSize = 13.sp) }
        }
        CalculatorAmountInput(value = amountExpr, onValueChange = { amountExpr = it }, label = "مبلغ")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PressScaleButton(
                onClick = {
                    error = validatePositive2(amountExpr)
                    if (error == null) { vm.allocateToPlan(planId, amountExpr, "ALLOCATE"); amountExpr = "" }
                },
                modifier = Modifier.weight(1f),
                enabled = plan.status == "ACTIVE",
            ) { Text("تخصيص", fontSize = 13.sp) }
            PressScaleButton(
                onClick = {
                    error = validatePositive2(amountExpr)
                    if (error == null) { vm.allocateToPlan(planId, amountExpr, "WITHDRAW"); amountExpr = "" }
                },
                modifier = Modifier.weight(1f),
            ) { Text("سحب", fontSize = 13.sp) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        Text("سجل التخصيصات", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        history.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (a.kind == "ALLOCATE") "تخصيص" else "سحب", fontSize = 13.sp)
                    if (!hidden && a.expression != null) {
                        Text(a.expression, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    MoneyText(a.amountPiasters, hidden, fontSize = 13)
                    Text(
                        SimpleDateFormat("d/M/yyyy", Locale("ar")).format(Date(a.createdAt)),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PasswordPromptDialog(vm: MainViewModel) {
    var pw by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { vm.dismissPasswordPrompt() },
        title = { Text("أدخل كلمة المرور") },
        text = {
            Column {
                Text("الأرقام مخفية. أدخل كلمة المرور عشان تظهرها.", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pw, onValueChange = { pw = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                message?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            PressScaleButton(onClick = {
                when (val result = vm.tryUnlock(pw)) {
                    is MainViewModel.UnlockResult.Success -> { pw = ""; message = null }
                    is MainViewModel.UnlockResult.Wrong ->
                        message = "كلمة مرور غلط. متبقي ${result.attemptsLeft} محاولات."
                    is MainViewModel.UnlockResult.Locked ->
                        message = "استنى ${result.secondsLeft} ثانية قبل المحاولة من تاني."
                }
            }) { Text("فتح") }
        },
        dismissButton = { PressScaleButton(onClick = { vm.dismissPasswordPrompt() }) { Text("إلغاء") } },
    )
}


@Composable
fun NotesScreen(vm: MainViewModel) {
    val notes by vm.notes.collectAsState()
    var editing by remember { mutableStateOf<ZadNote?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val sorted = notes.sortedWith(compareByDescending<ZadNote> { it.pinned }.thenByDescending { it.createdAt })
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ملاحظاتي", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { vm.closeOverlay() }) { Text("رجوع", fontSize = 13.sp) }
        }
        if (sorted.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("مفيش ملاحظات لسه 📝", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("اكتب أول ملاحظة: فكرة، تذكير، أو حاجة عايز ترجعلها.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        sorted.forEach { note ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (note.pinned) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (note.pinned) "⭐" else "☆",
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { vm.toggleNotePin(note.id) },
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).clickable { editing = note }) {
                        Text(note.text, fontSize = 14.sp)
                        if (note.pinned) Text("مميزة", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("🗑", fontSize = 16.sp, modifier = Modifier.clickable { vm.deleteNote(note.id) })
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        PressScaleButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("ملاحظة جديدة") }
        Spacer(Modifier.height(20.dp))
    }
    if (showAdd || editing != null) {
        val isEdit = editing != null
        var text by remember(showAdd, editing) { mutableStateOf(editing?.text ?: "") }
        AlertDialog(
            onDismissRequest = { showAdd = false; editing = null },
            title = { Text(if (isEdit) "تعديل الملاحظة" else "ملاحظة جديدة") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (text.isNotBlank()) {
                        if (isEdit) vm.updateNote(editing!!.id, text) else vm.addNote(text)
                        showAdd = false
                        editing = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAdd = false; editing = null }) { Text("إلغاء") }
            },
        )
    }
}
