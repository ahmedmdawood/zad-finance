package com.ahmed.zad

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(vm: MainViewModel) {
    val name by vm.userName.collectAsState()
    val themeId by vm.themeId.collectAsState()
    val hidden by vm.privacyHidden.collectAsState()
    val hasPassword by vm.hasPassword.collectAsState()
    val salaryExpr by vm.expectedSalary.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoSet by vm.userPhotoSet.collectAsState()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { runCatching { context.contentResolver.openInputStream(it) }.getOrNull()?.let(vm::saveUserPhoto) }
    }

    var nameDraft by remember(name) { mutableStateOf(name) }
    var salaryDraft by remember(salaryExpr) { mutableStateOf(salaryExpr) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val voiceEnabled by vm.voiceEnabled.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val appLockEnabled by vm.appLockEnabled.collectAsState()
    val quickAddEnabled by vm.quickAddEnabled.collectAsState()
    val weeklyDigestEnabled by vm.weeklyDigestEnabled.collectAsState()
    val dailyReminderEnabled by vm.dailyReminderEnabled.collectAsState()
    val dailyReminderMinutes by vm.dailyReminderMinutes.collectAsState()
    val accountsList by vm.accounts.collectAsState()
    val customCategories by vm.customCategories.collectAsState()
    val trashItems by vm.trash.collectAsState()

    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryEmoji by remember { mutableStateOf("📦") }
    var csvMonthOffset by remember { mutableStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                backupMessage = try {
                    val ym = java.time.YearMonth.now().minusMonths(csvMonthOffset.toLong())
                    exportMonthCsv(context, uri, transactions, accountsList, ym.year, ym.monthValue)
                    "اتعمل ملف الإكسل بنجاح"
                } catch (e: Exception) {
                    "فشل تصدير الملف"
                }
            }
        }
    }

    fun askNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            try {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } catch (e: Exception) { }
        }
    }
    var budgetCategory by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
    var budgetExpr by remember { mutableStateOf("") }

    val reportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                backupMessage = try {
                    generateMonthlyReport(context, uri, transactions, EXPENSE_CATEGORIES)
                    "اتعمل التقرير بنجاح"
                } catch (e: Exception) {
                    "فشل إنشاء التقرير"
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                backupMessage = try {
                    exportDatabase(context, vm.dbPath(), uri)
                    "اتعملت نسخة احتياطية بنجاح"
                } catch (e: Exception) {
                    "فشل النسخ الاحتياطي"
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                backupMessage = try {
                    importDatabase(context, vm.dbPath(), uri)
                    "اتستعدت النسخة. التطبيق هيقفل دلوقتي؛ افتحه من تاني."
                } catch (e: Exception) {
                    "فشلت الاستعادة: الملف مش نسخة زاد صالحة"
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Text("الإعدادات", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الاسم", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PressScaleButton(
                    onClick = { vm.changeName(nameDraft.trim()) },
                    enabled = nameDraft.isNotBlank() && nameDraft.trim() != name,
                ) { Text("حفظ الاسم") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الصورة الشخصية", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val photo = remember(photoSet) {
                        if (!photoSet) null else runCatching {
                            android.graphics.BitmapFactory.decodeFile(context.filesDir.resolve("user_photo.jpg").absolutePath)?.asImageBitmap()
                        }.getOrNull()
                    }
                    if (photo != null) {
                        Image(
                            bitmap = photo, contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(name.firstOrNull()?.toString() ?: "؟", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("بتظهر على بطاقتك في الرئيسية", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { photoPicker.launch("image/*") }) {
                                Text(if (photoSet) "تغيير" else "اختيار صورة", fontSize = 12.sp)
                            }
                            if (photoSet) {
                                OutlinedButton(onClick = { vm.clearUserPhoto() }) { Text("إزالة", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الملاحظات", fontWeight = FontWeight.SemiBold)
                Text("ملاحظاتك وتذكيراتك، والمميزة ⭐ تفضل فوق دايماً.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PressScaleButton(onClick = { vm.openOverlay(Overlay.Notes) }, modifier = Modifier.fillMaxWidth()) { Text("فتح الملاحظات") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المرتب الشهري المتوقع", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = salaryDraft,
                    onValueChange = { salaryDraft = it },
                    singleLine = true,
                    placeholder = { Text("مثال: 2500") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (salaryDraft.isNotBlank()) {
                    val salaryPreview = try {
                        evaluateToPiasters(salaryDraft).first to null
                    } catch (e: EvalException) {
                        null to (e.message ?: "خطأ")
                    }
                    if (salaryPreview.first != null) {
                        Text("= ${formatMoney(salaryPreview.first!!)}", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    } else {
                        Text(salaryPreview.second ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
                PressScaleButton(onClick = { vm.setExpectedSalary(salaryDraft.trim()) }) { Text("حفظ المرتب") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الثيم", fontWeight = FontWeight.SemiBold)
                val darkOverride by vm.darkOverride.collectAsState()
                val theme = themeById(themeId)
                val isDark = if (darkOverride == -1) theme.defaultDark else darkOverride == 1
                ThemePicker(selected = themeId, isDark = isDark, onSelect = { id, dark ->
                    vm.setTheme(id)
                    vm.setDarkOverride(if (dark) 1 else 0)
                })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الوضع الداكن", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(
                        checked = isDark,
                        onCheckedChange = { vm.setDarkOverride(if (it) 1 else 0) },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val pos = coords.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
                            ThemeReveal.origin = androidx.compose.ui.geometry.Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f,
                            )
                        },
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الخصوصية", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("إخفاء الأرقام", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = hidden, onCheckedChange = { vm.onEyeTapped() })
                }
                if (hasPassword) {
                    OutlinedButton(onClick = { vm.clearPassword() }, modifier = Modifier.fillMaxWidth()) {
                        Text("إزالة كلمة مرور العرض")
                    }
                } else {
                    OutlinedButton(onClick = { showPasswordDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("تعيين كلمة مرور لإظهار الأرقام")
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الإدخال الصوتي", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("قول المبلغ بدل ما تكتبه", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = voiceEnabled, onCheckedChange = { vm.setVoiceEnabled(it) })
                }
                Text(
                    "اختياري بالكامل والتطبيق شغال من غيره. التعرف الصوتي ممكن يغلط أحيانًا، فراجع المبلغ قبل الحفظ. بيشتغل أوفلاين بعد أول تحميل لحزمة اللغة.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ميزانيات الفئات", fontWeight = FontWeight.SemiBold)
                budgets.forEach { budget ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(categoryEmoji(budget.category) + " " + budget.category, fontSize = 14.sp, maxLines = 1)
                            Text(
                                "حد الشهر: " + formatMoney(budget.limitPiasters),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { vm.clearBudget(budget.category) }) {
                            Text("إزالة", fontSize = 12.sp)
                        }
                    }
                }
                CategoryChips(selected = budgetCategory, categories = vm.allCategories(), onSelect = { budgetCategory = it })
                CalculatorAmountInput(value = budgetExpr, onValueChange = { budgetExpr = it }, label = "حد الشهر للفئة المختارة")
                PressScaleButton(
                    onClick = {
                        try {
                            val (p, _) = evaluateToPiasters(budgetExpr)
                            if (p > 0) { vm.setBudget(budgetCategory, budgetExpr); budgetExpr = "" }
                        } catch (e: EvalException) { }
                    },
                    enabled = budgetExpr.isNotBlank(),
                ) { Text("حفظ ميزانية $budgetCategory") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("التقرير الشهري", fontWeight = FontWeight.SemiBold)
                Text(
                    "ملخص PDF لدخلك ومصروفاتك الشهر ده، مقسومة على الفئات.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { reportLauncher.launch("zad-report.pdf") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("إنشاء تقرير الشهر (PDF)") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("النسخ الاحتياطي", fontWeight = FontWeight.SemiBold)
                Text(
                    "بياناتك كلها محلية على جهازك. اعمل نسخة احتياطية واحتفظ بيها في مكان آمن.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("zad-backup.db") },
                        modifier = Modifier.weight(1f),
                    ) { Text("نسخ احتياطي", fontSize = 13.sp) }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("استعادة", fontSize = 13.sp) }
                }
                backupMessage?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الأمان", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("قفل التطبيق بالبصمة أو قفل الجهاز", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = appLockEnabled, onCheckedChange = { vm.setAppLockEnabled(it) })
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الإشعارات", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("إضافة مصروف سريعة من الإشعارات", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = quickAddEnabled, onCheckedChange = {
                        if (it) askNotificationPermission()
                        vm.setQuickAddEnabled(it)
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ملخص أسبوعي (الجمعة ٦ مساءً)", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = weeklyDigestEnabled, onCheckedChange = {
                        if (it) askNotificationPermission()
                        vm.setWeeklyDigestEnabled(it)
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تذكير يومي بتسجيل المصاريف", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = dailyReminderEnabled, onCheckedChange = {
                        if (it) askNotificationPermission()
                        vm.setDailyReminder(it, dailyReminderMinutes)
                    })
                }
                if (dailyReminderEnabled) {
                    Text("وقت التذكير:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(12 * 60 to "١٢ الظهر", 17 * 60 to "٥ مساءً", 20 * 60 to "٨ مساءً", 22 * 60 to "١٠ مساءً").forEach { (minutes, label) ->
                            FilterChip(
                                selected = dailyReminderMinutes == minutes,
                                onClick = { vm.setDailyReminder(true, minutes) },
                                label = { Text(label, fontSize = 12.sp) },
                            )
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("فئات مخصصة", fontWeight = FontWeight.SemiBold)
                if (customCategories.isNotEmpty()) {
                    customCategories.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.icon + " " + category.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                            TextButton(onClick = { vm.removeCategory(category.name) }) {
                                Text("إزالة", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategoryEmoji,
                        onValueChange = { newCategoryEmoji = it.take(2) },
                        modifier = Modifier.padding(end = 8.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اسم الفئة الجديدة", fontSize = 13.sp) },
                        singleLine = true,
                    )
                }
                PressScaleButton(
                    onClick = {
                        vm.addCategory(newCategoryName, newCategoryEmoji, "#5C6BC0")
                        newCategoryName = ""
                    },
                    enabled = newCategoryName.isNotBlank(),
                ) { Text("إضافة الفئة") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تصدير Excel / CSV", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = csvMonthOffset == 0,
                        onClick = { csvMonthOffset = 0 },
                        label = { Text("الشهر ده", fontSize = 12.sp) },
                    )
                    FilterChip(
                        selected = csvMonthOffset == 1,
                        onClick = { csvMonthOffset = 1 },
                        label = { Text("الشهر اللي فات", fontSize = 12.sp) },
                    )
                }
                PressScaleButton(
                    onClick = {
                        val ym = java.time.YearMonth.now().minusMonths(csvMonthOffset.toLong())
                        csvLauncher.launch("zad-${ym.year}-" + "%02d".format(ym.monthValue) + ".csv")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("⬇ تصدير الشهر المختار (Excel / CSV)") }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable { vm.openOverlay(Overlay.Trash) },
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("سلة المحذوفات", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    if (trashItems.isEmpty()) "" else trashItems.size.toString(),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("‹", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable { vm.openOverlay(Overlay.About) },
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("عن زاد", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("‹", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showPasswordDialog) {
        var pw by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("كلمة مرور العرض") },
            text = {
                Column {
                    Text("هتحتاجها كل مرة تظهر فيها الأرقام. من غيرها الأرقام تفضل مخفية.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                PressScaleButton(
                    onClick = { vm.setPassword(pw); showPasswordDialog = false },
                    enabled = pw.length >= 4,
                ) { Text("حفظ") }
            },
            dismissButton = { PressScaleButton(onClick = { showPasswordDialog = false }) { Text("إلغاء") } },
        )
    }
}

@Composable
fun AboutScreen(vm: MainViewModel) {
    val uriHandler = LocalUriHandler.current
    val special by vm.isSpecial.collectAsState()
    var eggTaps by remember { mutableStateOf(0) }
    var showEgg by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero: brand block with a two-tone wash.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.secondary,
                        )
                    )
                )
                .padding(horizontal = 22.dp, vertical = 26.dp)
                .clickable {
                    eggTaps++
                    if (eggTaps >= 5) {
                        showEgg = true
                        eggTaps = 0
                    }
                },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "زاد",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    "إدارة أموالك، بطريقتك.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    "💰  📊  🎯",
                    fontSize = 18.sp,
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ليه زاد؟", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "زاد اتعمل عشان حد زيك: عارف إن الفلوس بتيجي وبتروح، بس مش عارف بالظبط معاه كام، " +
                        "ولا فلوسه فين، ولا بيصرفها في إيه. بدل ما تقعد تجمع الحكاية من كذا مكان، " +
                        "زاد بيلمها لك في مكان واحد: حساباتك، دخلك، مصروفك، ديونك، بطاقاتك، " +
                        "والتزاماتك وخططك.",
                    fontSize = 14.sp, lineHeight = 22.sp,
                )
                Text(
                    "كله على جهازك، من غير إنترنت ولا حسابات.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            "«احلم وإحنا ننفذ.»",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        if (special) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AdBadge()
                    Spacer(Modifier.padding(horizontal = 5.dp))
                    Text("أحمد داود", color = GoldColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Created with care by ahmed dawood", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("EST. 2026", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { uriHandler.openUri("https://www.instagram.com/rzzx08/") },
                modifier = Modifier.weight(1f),
            ) { Text("Instagram", fontSize = 13.sp) }
            OutlinedButton(
                onClick = { uriHandler.openUri("https://www.linkedin.com/in/ahmedmohameddawood/") },
                modifier = Modifier.weight(1f),
            ) { Text("LinkedIn", fontSize = 13.sp) }
        }

        PressScaleButton(onClick = { vm.closeOverlay() }, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
        Spacer(Modifier.height(24.dp))
    }

    if (showEgg) {
        EasterEggDialog(onClose = { showEgg = false })
    }
}

/** The hidden treat: a little rain of gold coins for whoever finds it. */
@Composable
fun EasterEggDialog(onClose: () -> Unit) {
    val spin by rememberInfiniteTransition(label = "coins").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "coinfall",
    )
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { PressScaleButton(onClick = onClose) { Text("❤") } },
        title = { Text("🎉 انت لقيت السر!", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                    val gold = androidx.compose.ui.graphics.Color(0xFFC9A227)
                    val deep = androidx.compose.ui.graphics.Color(0xFF8C6D1F)
                    for (i in 0 until 18) {
                        val frac = (i * 0.618034f) % 1f
                        val speed = 0.55f + (i % 5) * 0.11f
                        val y = ((spin * speed + i * 0.13f) % 1f) * size.height
                        val x = frac * size.width
                        val r = 7f + (i % 3) * 3f
                        drawCircle(deep, radius = r + 2f, center = androidx.compose.ui.geometry.Offset(x, y))
                        drawCircle(gold, radius = r, center = androidx.compose.ui.geometry.Offset(x, y))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "ده سر صغير مخباه ليك. كل قرش دهب بينزل هنا هو قرش هتوفره بإذن الله.",
                    fontSize = 14.sp, lineHeight = 21.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Created with care by ahmed dawood",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
