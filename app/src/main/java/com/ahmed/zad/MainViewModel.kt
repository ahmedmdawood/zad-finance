package com.ahmed.zad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class ZadTab { HOME, ACCOUNTS, DEBTS, OBLIGATIONS, PLANS, SETTINGS }

sealed class Overlay {
    object None : Overlay()
    data class AddTransaction(val type: String) : Overlay() // INCOME | EXPENSE | TRANSFER
    data class CardPurchase(val accountId: Long) : Overlay()
    data class CardPayment(val accountId: Long) : Overlay()
    object AddAccount : Overlay()
    object AddDebt : Overlay()
    data class PayDebt(val debtId: Long) : Overlay()
    object AddObligation : Overlay()
    data class ObligationDetail(val obligationId: Long) : Overlay()
    data class PayOccurrence(val occurrenceId: Long) : Overlay()
    object AddPlan : Overlay()
    data class PlanDetail(val planId: Long) : Overlay()
    object About : Overlay()
    object Charts : Overlay()
    object Calendar : Overlay()
    object Trash : Overlay()
    object Notes : Overlay()
}

data class ZadNote(val id: Long, val text: String, val pinned: Boolean, val createdAt: Long)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = ZadPrefs(app)
    private val dao = ZadDatabase.get(app).dao()

    val accounts: StateFlow<List<AccountEntity>> =
        dao.accountsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions: StateFlow<List<TransactionEntity>> =
        dao.transactionsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debts: StateFlow<List<DebtEntity>> =
        dao.debtsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debtPayments: StateFlow<List<DebtPaymentEntity>> =
        dao.debtPaymentsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val salaryConfirmations: StateFlow<List<SalaryConfirmationEntity>> =
        dao.salaryConfirmationsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val obligations: StateFlow<List<ObligationEntity>> =
        dao.obligationsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val occurrences: StateFlow<List<OccurrenceEntity>> =
        dao.occurrencesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val plans: StateFlow<List<PlanEntity>> =
        dao.plansFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allocations: StateFlow<List<AllocationEntity>> =
        dao.allocationsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets: StateFlow<List<BudgetEntity>> =
        dao.budgetsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customCategories: StateFlow<List<CategoryEntity>> =
        dao.categoriesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredTransactions: StateFlow<List<TransactionEntity>> =
        combine(transactions, _searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter { t ->
                t.note.contains(query, ignoreCase = true) ||
                    t.category.contains(query, ignoreCase = true) ||
                    formatMoney(t.amountPiasters).contains(query.trim())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Expenses recorded since the start of today. */
    val todaySpendPiasters: StateFlow<Long> =
        transactions.map { list ->
            val start = todayStartMillis()
            list.filter { it.type == "EXPENSE" && it.createdAt >= start }.sumOf { it.amountPiasters }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /** Expenses recorded since the start of the current month. */
    val monthSpendPiasters: StateFlow<Long> =
        transactions.map { list ->
            val start = monthBounds(0).first
            list.filter { it.type == "EXPENSE" && it.createdAt >= start }.sumOf { it.amountPiasters }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /** Sum of all set budgets minus this month's spend; null when no budgets are set. */
    val remainingMonthBudgetPiasters: StateFlow<Long?> =
        combine(budgets, monthSpendPiasters) { budgetsList, spent ->
            val total = budgetsList.sumOf { it.limitPiasters }
            if (total <= 0) null else total - spent
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Subtle projection: month spend scaled to the whole month; shown after the 5th. */
    val projectedMonthSpendPiasters: StateFlow<Long?> =
        monthSpendPiasters.map { spent ->
            val day = dayOfMonth()
            if (spent <= 0 || day < 5) null
            else spent * daysInCurrentMonth() / day
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Days this week with expenses at or under the implied daily budget; null when no budgets. */
    val underBudgetDaysThisWeek: StateFlow<Pair<Int, Int>?> =
        combine(budgets, transactions) { budgetsList, tx ->
            val total = budgetsList.sumOf { it.limitPiasters }
            if (total <= 0) return@combine null
            val dailyBudget = total / daysInCurrentMonth()
            if (dailyBudget <= 0) return@combine null
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayDow = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val daysBack = (todayDow - cal.firstDayOfWeek + 7) % 7
            var under = 0
            var elapsed = 0
            for (i in 0..daysBack) {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.add(java.util.Calendar.DAY_OF_MONTH, 0) // reset after reuse
                val c2 = java.util.Calendar.getInstance()
                c2.set(java.util.Calendar.HOUR_OF_DAY, 0)
                c2.set(java.util.Calendar.MINUTE, 0)
                c2.set(java.util.Calendar.SECOND, 0)
                c2.set(java.util.Calendar.MILLISECOND, 0)
                c2.add(java.util.Calendar.DAY_OF_MONTH, -i)
                val dayStart = c2.timeInMillis
                c2.add(java.util.Calendar.DAY_OF_MONTH, 1)
                val dayEnd = c2.timeInMillis
                val spend = tx.filter { it.type == "EXPENSE" && it.createdAt in dayStart until dayEnd }
                    .sumOf { it.amountPiasters }
                if (spend <= dailyBudget) under++
                elapsed++
            }
            under to elapsed
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Top expense categories this month, for charts. */
    val topCategoriesThisMonth: StateFlow<List<Pair<String, Long>>> =
        transactions.map { list ->
            val start = monthBounds(0).first
            list.filter { it.type == "EXPENSE" && it.createdAt >= start }
                .groupBy { it.category }
                .mapValues { (_, ts) -> ts.sumOf { it.amountPiasters } }
                .entries.sortedByDescending { it.value }
                .map { it.key to it.value }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Expense totals per month for the last 6 months, oldest first. */
    val monthlyExpenseSeries: StateFlow<List<Pair<String, Long>>> =
        transactions.map { list ->
            (5 downTo 0).map { offset ->
                val (start, end) = monthBounds(-offset)
                val label = java.time.YearMonth.now().minusMonths(offset.toLong()).month.let { m ->
                    when (m) {
                        java.time.Month.JANUARY -> "يناير"; java.time.Month.FEBRUARY -> "فبراير"
                        java.time.Month.MARCH -> "مارس"; java.time.Month.APRIL -> "أبريل"
                        java.time.Month.MAY -> "مايو"; java.time.Month.JUNE -> "يونيو"
                        java.time.Month.JULY -> "يوليو"; java.time.Month.AUGUST -> "أغسطس"
                        java.time.Month.SEPTEMBER -> "سبتمبر"; java.time.Month.OCTOBER -> "أكتوبر"
                        java.time.Month.NOVEMBER -> "نوفمبر"; else -> "ديسمبر"
                    }
                }
                val total = list.filter { it.type == "EXPENSE" && it.createdAt >= start && it.createdAt < end }
                    .sumOf { it.amountPiasters }
                label to total
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trash: StateFlow<List<TransactionEntity>> =
        dao.trashFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Preset categories plus the user's own. */
    fun allCategories(): List<String> =
        (EXPENSE_CATEGORIES + customCategories.value.map { it.name }).distinct()

    fun addCategory(name: String, icon: String, colorHex: String) {
        viewModelScope.launch {
            if (name.isNotBlank() && name !in allCategories()) {
                dao.insertCategory(CategoryEntity(name = name.trim(), icon = icon, colorHex = colorHex))
            }
        }
    }

    fun removeCategory(name: String) {
        viewModelScope.launch { dao.deleteCategory(name) }
    }

    fun restoreTransaction(id: Long) {
        viewModelScope.launch { dao.restoreTransaction(id) }
    }

    fun purgeOldTrash() {
        viewModelScope.launch {
            dao.purgeOldTrash(System.currentTimeMillis() - 30L * 86_400_000L)
        }
    }

    private val _userName = MutableStateFlow(prefs.userName)
    val userName: StateFlow<String> = _userName

    private val _isSpecial = MutableStateFlow(prefs.isSpecialIdentity)
    val isSpecial: StateFlow<Boolean> = _isSpecial

    private val _userPhotoSet = MutableStateFlow(prefs.userPhotoSet)
    val userPhotoSet: StateFlow<Boolean> = _userPhotoSet

    fun saveUserPhoto(stream: java.io.InputStream?) {
        if (stream == null) return
        // Decode and shrink off the main thread: full-size camera photos are big
        // enough to freeze or crash the app when decoded at full resolution.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bytes = stream.readBytes()
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return@launch
                getApplication<Application>().filesDir.resolve("user_photo.jpg").outputStream().use { out ->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                prefs.userPhotoSet = true
                _userPhotoSet.value = true
            } catch (e: Exception) { }
        }
    }

    fun clearUserPhoto() {
        getApplication<Application>().filesDir.resolve("user_photo.jpg").delete()
        prefs.userPhotoSet = false
        _userPhotoSet.value = false
    }

    private val _notes = MutableStateFlow(loadNotes())
    val notes: StateFlow<List<ZadNote>> = _notes

    private fun notesFile() = getApplication<Application>().filesDir.resolve("notes.json")

    private fun loadNotes(): List<ZadNote> {
        return try {
            val file = notesFile()
            if (!file.exists()) return emptyList()
            val arr = org.json.JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ZadNote(o.getLong("id"), o.getString("text"), o.optBoolean("pinned"), o.optLong("createdAt"))
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun persistNotes(list: List<ZadNote>) {
        try {
            val arr = org.json.JSONArray()
            list.forEach { n ->
                arr.put(
                    org.json.JSONObject()
                        .put("id", n.id)
                        .put("text", n.text)
                        .put("pinned", n.pinned)
                        .put("createdAt", n.createdAt)
                )
            }
            notesFile().writeText(arr.toString())
        } catch (e: Exception) { }
    }

    fun addNote(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val now = System.currentTimeMillis()
        val list = _notes.value + ZadNote(now, t, false, now)
        _notes.value = list
        persistNotes(list)
    }

    fun updateNote(id: Long, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val list = _notes.value.map { if (it.id == id) it.copy(text = t) else it }
        _notes.value = list
        persistNotes(list)
    }

    fun deleteNote(id: Long) {
        val list = _notes.value.filterNot { it.id == id }
        _notes.value = list
        persistNotes(list)
    }

    fun toggleNotePin(id: Long) {
        val list = _notes.value.map { if (it.id == id) it.copy(pinned = !it.pinned) else it }
        _notes.value = list
        persistNotes(list)
    }

    private val _themeId = MutableStateFlow(prefs.themeId)
    val themeId: StateFlow<String> = _themeId

    private val _darkOverride = MutableStateFlow(prefs.darkOverride)
    val darkOverride: StateFlow<Int> = _darkOverride

    fun setDarkOverride(value: Int) {
        prefs.darkOverride = value
        _darkOverride.value = value
    }

    private val _privacyHidden = MutableStateFlow(prefs.privacyHidden)
    val privacyHidden: StateFlow<Boolean> = _privacyHidden

    private val _hasPassword = MutableStateFlow(prefs.hasPassword)
    val hasPassword: StateFlow<Boolean> = _hasPassword

    private val _expectedSalary = MutableStateFlow(prefs.expectedSalaryExpression)
    val expectedSalary: StateFlow<String> = _expectedSalary

    private val _voiceEnabled = MutableStateFlow(prefs.voiceEntryEnabled)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled

    fun setVoiceEnabled(enabled: Boolean) {
        prefs.voiceEntryEnabled = enabled
        _voiceEnabled.value = enabled
    }

    private val _appLockEnabled = MutableStateFlow(prefs.appLockEnabled)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled

    private val _appUnlocked = MutableStateFlow(!prefs.appLockEnabled)
    val appUnlocked: StateFlow<Boolean> = _appUnlocked

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.appLockEnabled = enabled
        _appLockEnabled.value = enabled
        if (enabled) _appUnlocked.value = true // enabling from inside counts as unlocked
    }

    fun markAppUnlocked() {
        _appUnlocked.value = true
    }

    private val _quickAddEnabled = MutableStateFlow(prefs.quickAddEnabled)
    val quickAddEnabled: StateFlow<Boolean> = _quickAddEnabled

    fun setQuickAddEnabled(enabled: Boolean) {
        prefs.quickAddEnabled = enabled
        _quickAddEnabled.value = enabled
        ZadNotifications.showQuickAdd(getApplication(), enabled)
    }

    private val _weeklyDigestEnabled = MutableStateFlow(prefs.weeklyDigestEnabled)
    val weeklyDigestEnabled: StateFlow<Boolean> = _weeklyDigestEnabled

    fun setWeeklyDigestEnabled(enabled: Boolean) {
        prefs.weeklyDigestEnabled = enabled
        _weeklyDigestEnabled.value = enabled
        if (enabled) ZadNotifications.scheduleWeeklyDigest(getApplication())
        else ZadNotifications.cancelWeeklyDigest(getApplication())
    }

    private val _dailyReminderEnabled = MutableStateFlow(prefs.dailyReminderEnabled)
    val dailyReminderEnabled: StateFlow<Boolean> = _dailyReminderEnabled

    private val _dailyReminderMinutes = MutableStateFlow(prefs.dailyReminderMinutes)
    val dailyReminderMinutes: StateFlow<Int> = _dailyReminderMinutes

    fun setDailyReminder(enabled: Boolean, minutes: Int) {
        prefs.dailyReminderEnabled = enabled
        prefs.dailyReminderMinutes = minutes
        _dailyReminderEnabled.value = enabled
        _dailyReminderMinutes.value = minutes
        if (enabled) ZadNotifications.scheduleDailyReminder(getApplication(), minutes / 60, minutes % 60)
        else ZadNotifications.cancelDailyReminder(getApplication())
    }

    private val _smsParsingEnabled = MutableStateFlow(prefs.smsParsingEnabled)
    val smsParsingEnabled: StateFlow<Boolean> = _smsParsingEnabled

    fun setSmsParsing(enabled: Boolean, accountId: Long) {
        prefs.smsParsingEnabled = enabled
        prefs.smsDefaultAccountId = accountId
        _smsParsingEnabled.value = enabled
    }

    fun smsDefaultAccountId(): Long = prefs.smsDefaultAccountId

    private val _projectionDismissed = MutableStateFlow(prefs.projectionDismissed)
    val projectionDismissed: StateFlow<Boolean> = _projectionDismissed

    fun dismissProjection() {
        prefs.projectionDismissed = true
        _projectionDismissed.value = true
    }

    private val _unlocked = MutableStateFlow(false)

    private val _tab = MutableStateFlow(ZadTab.HOME)
    val tab: StateFlow<ZadTab> = _tab

    private val _overlay = MutableStateFlow<Overlay>(Overlay.None)
    val overlay: StateFlow<Overlay> = _overlay

    private val _needsPasswordPrompt = MutableStateFlow(false)
    val needsPasswordPrompt: StateFlow<Boolean> = _needsPasswordPrompt

    fun onboardingDone(): Boolean = prefs.userName.isNotEmpty()

    fun setTab(tab: ZadTab) {
        _tab.value = tab
    }

    fun openOverlay(overlay: Overlay) {
        _overlay.value = overlay
    }

    fun closeOverlay() {
        _overlay.value = Overlay.None
    }

    /** The special code ".....", only honored here on the welcome screen. */
    fun completeOnboarding(name: String, theme: String) {
        val special = name == "....."
        prefs.isSpecialIdentity = special
        _isSpecial.value = special
        prefs.userName = if (special) "أحمد داود" else name
        _userName.value = prefs.userName
        setTheme(theme)
    }

    /** Renaming from settings never grants the special identity. */
    fun changeName(name: String) {
        if (name.isBlank()) return
        if (name == ".....") return // the code only works on the welcome screen
        prefs.userName = name
        _userName.value = name
    }

    fun setTheme(theme: String) {
        prefs.themeId = theme
        _themeId.value = theme
        _darkOverride.value = -1
        prefs.darkOverride = -1
    }

    fun setExpectedSalary(expression: String) {
        prefs.expectedSalaryExpression = expression
        _expectedSalary.value = expression
    }

    // ---------- privacy ----------

    fun onEyeTapped() {
        if (_privacyHidden.value) {
            if (prefs.hasPassword && !_unlocked.value) {
                _needsPasswordPrompt.value = true
            } else {
                setHidden(false)
            }
        } else {
            setHidden(true)
        }
    }

    private fun setHidden(hidden: Boolean) {
        prefs.privacyHidden = hidden
        _privacyHidden.value = hidden
        if (hidden) _unlocked.value = false
    }

    sealed class UnlockResult {
        object Success : UnlockResult()
        data class Wrong(val attemptsLeft: Int) : UnlockResult()
        data class Locked(val secondsLeft: Long) : UnlockResult()
    }

    fun tryUnlock(password: String): UnlockResult {
        val now = System.currentTimeMillis()
        val lockout = prefs.lockoutUntil
        if (now < lockout) return UnlockResult.Locked((lockout - now) / 1000 + 1)

        val salt = prefs.passwordSalt ?: return UnlockResult.Wrong(0)
        val verifier = prefs.passwordVerifier ?: return UnlockResult.Wrong(0)
        val ok = PrivacySecurity.verify(password, PrivacySecurity.decode(salt), PrivacySecurity.decode(verifier))
        return if (ok) {
            prefs.failedAttempts = 0
            prefs.lockoutUntil = 0
            _unlocked.value = true
            _needsPasswordPrompt.value = false
            setHidden(false)
            UnlockResult.Success
        } else {
            val fails = prefs.failedAttempts + 1
            prefs.failedAttempts = fails
            if (fails >= 5) {
                val delaySeconds = when (fails - 4) {
                    1 -> 15L
                    2 -> 30L
                    else -> 60L
                }
                prefs.lockoutUntil = now + delaySeconds * 1000
                UnlockResult.Locked(delaySeconds)
            } else {
                UnlockResult.Wrong(5 - fails)
            }
        }
    }

    fun dismissPasswordPrompt() {
        _needsPasswordPrompt.value = false
    }

    fun setPassword(password: String) {
        if (password.length < 4) return
        val salt = PrivacySecurity.newSalt()
        prefs.passwordSalt = PrivacySecurity.encode(salt)
        prefs.passwordVerifier = PrivacySecurity.encode(PrivacySecurity.hash(password, salt))
        prefs.failedAttempts = 0
        prefs.lockoutUntil = 0
        _hasPassword.value = true
    }

    fun clearPassword() {
        prefs.clearPassword()
        _hasPassword.value = false
    }

    // ---------- finance actions ----------

    fun addAccount(name: String, type: String, openingExpression: String, creditLimitExpression: String?) {
        viewModelScope.launch {
            val (piasters, _) = evaluateToPiasters(openingExpression)
            var limit: Long? = null
            if (type == "CREDIT" && !creditLimitExpression.isNullOrBlank()) {
                val (limitPiasters, _) = evaluateToPiasters(creditLimitExpression)
                require(limitPiasters >= 0) { "limit" }
                limit = limitPiasters
            }
            dao.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    openingPiasters = if (type == "CREDIT") 0L else piasters,
                    openingExpression = if (type == "CREDIT") null else openingExpression,
                    creditLimitPiasters = limit,
                    creditLimitExpression = if (type == "CREDIT") creditLimitExpression else null,
                )
            )
            closeOverlay()
        }
    }

    fun addTransaction(
        type: String,
        expression: String,
        accountId: Long,
        toAccountId: Long?,
        note: String,
        planId: Long? = null,
        category: String = "أخرى",
        receiptPath: String? = null,
    ) {
        viewModelScope.launch {
            val (piasters, _) = evaluateToPiasters(expression)
            dao.insertTransaction(
                TransactionEntity(
                    type = type,
                    amountPiasters = piasters,
                    amountExpression = expression,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    planId = if (type == "EXPENSE") planId else null,
                    category = if (type == "EXPENSE") category else "أخرى",
                    receiptPath = if (type == "EXPENSE") receiptPath else null,
                    note = note,
                )
            )
            closeOverlay()
        }
    }

    fun setBudget(category: String, expression: String) {
        viewModelScope.launch {
            val (piasters, _) = evaluateToPiasters(expression)
            dao.insertBudget(BudgetEntity(category = category, limitPiasters = piasters, expression = expression))
        }
    }

    fun clearBudget(category: String) {
        viewModelScope.launch { dao.deleteBudget(category) }
    }

    /** This calendar month's spend in one category. */
    fun categorySpentThisMonth(category: String): Long {
        val monthStart = java.time.YearMonth.now().atDay(1)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        var total = 0L
        for (t in transactions.value) {
            if (t.type == "EXPENSE" && t.category == category && t.createdAt >= monthStart) {
                total = Math.addExact(total, t.amountPiasters)
            }
        }
        return total
    }

    /** Categories whose monthly spend reached or passed the budget. */
    fun overBudgetCategories(): List<Pair<String, Long>> =
        budgets.value.mapNotNull { budget ->
            val spent = categorySpentThisMonth(budget.category)
            if (spent >= budget.limitPiasters && budget.limitPiasters > 0) budget.category to spent else null
        }

    /**
     * Soft delete: the transaction moves to the 30-day trash and can be restored.
     * Linked state (salary confirmations, paid occurrences) reverts immediately.
     */
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            dao.deleteSalaryConfirmationByTransaction(id)
            val occurrence = dao.occurrenceByTransaction(id)
            if (occurrence != null) {
                dao.updateOccurrence(
                    occurrence.copy(status = "PENDING", paidTransactionId = null, paidAmountPiasters = null)
                )
            }
            dao.softDeleteTransaction(id, System.currentTimeMillis())
        }
    }

    /** Hard delete from the trash, also removing any stored receipt photo. */
    fun deleteTransactionForever(id: Long) {
        viewModelScope.launch {
            dao.transactionById(id)?.receiptPath?.let { path ->
                try { java.io.File(path).delete() } catch (e: Exception) { }
            }
            dao.deleteTransaction(id)
        }
    }

    fun addDebt(person: String, direction: String, expression: String, accountId: Long?) {
        viewModelScope.launch {
            val (piasters, _) = evaluateToPiasters(expression)
            dao.insertDebt(
                DebtEntity(
                    person = person,
                    direction = direction,
                    principalPiasters = piasters,
                    remainingPiasters = piasters,
                    expression = expression,
                )
            )
            if (accountId != null) {
                dao.insertTransaction(
                    TransactionEntity(
                        type = if (direction == "LENT") "DEBT_GIVEN" else "DEBT_BORROWED",
                        amountPiasters = piasters,
                        amountExpression = expression,
                        accountId = accountId,
                        note = person,
                    )
                )
            }
            closeOverlay()
        }
    }

    fun payDebt(debtId: Long, expression: String, accountId: Long?) {
        viewModelScope.launch {
            val (piasters, _) = evaluateToPiasters(expression)
            val debt = debts.value.firstOrNull { it.id == debtId } ?: return@launch
            val newRemaining = Math.subtractExact(debt.remainingPiasters, piasters).coerceAtLeast(0)
            dao.updateDebt(
                debt.copy(
                    remainingPiasters = newRemaining,
                    settled = newRemaining == 0L,
                )
            )
            dao.insertDebtPayment(DebtPaymentEntity(debtId = debtId, amountPiasters = piasters))
            if (accountId != null) {
                dao.insertTransaction(
                    TransactionEntity(
                        type = if (debt.direction == "LENT") "DEBT_COLLECTED" else "DEBT_REPAID",
                        amountPiasters = piasters,
                        amountExpression = expression,
                        accountId = accountId,
                        note = debt.person,
                    )
                )
            }
            closeOverlay()
        }
    }

    fun currentMonthKey(): String = YearMonth.now().toString()

    fun salaryConfirmedThisMonth(): Boolean =
        salaryConfirmations.value.any { it.monthKey == currentMonthKey() }

    /** Idempotent per month: a confirmation record with the month key prevents duplicates. */
    fun confirmSalary(accountId: Long, expression: String) {
        viewModelScope.launch {
            if (salaryConfirmedThisMonth()) return@launch
            val (piasters, _) = evaluateToPiasters(expression)
            val txId = dao.insertTransaction(
                TransactionEntity(
                    type = "INCOME",
                    amountPiasters = piasters,
                    amountExpression = expression,
                    accountId = accountId,
                    note = "مرتب",
                )
            )
            dao.insertSalaryConfirmation(
                SalaryConfirmationEntity(
                    monthKey = currentMonthKey(),
                    transactionId = txId,
                    amountPiasters = piasters,
                    expression = expression,
                )
            )
        }
    }

    // ---------- recurring obligations ----------

    fun addObligation(title: String, expression: String, cycle: String, customDays: Int?, startDate: Long) {
        viewModelScope.launch {
            val (piasters, _) = evaluateToPiasters(expression)
            val id = dao.insertObligation(
                ObligationEntity(
                    title = title,
                    amountPiasters = piasters,
                    expression = expression,
                    cycle = cycle,
                    customIntervalDays = customDays,
                    startDate = startDate,
                )
            )
            dao.insertOccurrence(OccurrenceEntity(obligationId = id, dueDate = startDate, status = "PENDING"))
            closeOverlay()
        }
    }

    fun setObligationActive(obligation: ObligationEntity, active: Boolean) {
        viewModelScope.launch { dao.updateObligation(obligation.copy(active = active)) }
    }

    private fun nextDueDate(obligation: ObligationEntity, after: Long): Long {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(after).atZone(zone).toLocalDate()
        val next = when (obligation.cycle) {
            "WEEKLY" -> date.plusWeeks(1)
            "CUSTOM_DAYS" -> date.plusDays((obligation.customIntervalDays ?: 30).toLong())
            else -> date.plusMonths(1)
        }
        return next.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /** Generates PENDING occurrences up to 90 days ahead for every active obligation. */
    fun ensureOccurrences() {
        viewModelScope.launch {
            val horizon = LocalDate.now().plusDays(90)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            for (obligation in obligations.value) {
                if (!obligation.active) continue
                var last = dao.maxOccurrenceDueDate(obligation.id) ?: obligation.startDate
                if (dao.maxOccurrenceDueDate(obligation.id) == null) {
                    dao.insertOccurrence(
                        OccurrenceEntity(obligationId = obligation.id, dueDate = obligation.startDate, status = "PENDING")
                    )
                }
                var next = nextDueDate(obligation, last)
                while (next <= horizon) {
                    dao.insertOccurrence(
                        OccurrenceEntity(obligationId = obligation.id, dueDate = next, status = "PENDING")
                    )
                    last = next
                    next = nextDueDate(obligation, last)
                }
            }
        }
    }

    fun payOccurrence(occurrenceId: Long, expression: String, accountId: Long) {
        viewModelScope.launch {
            val occurrence = occurrences.value.firstOrNull { it.id == occurrenceId } ?: return@launch
            if (occurrence.status == "PAID") return@launch
            val obligation = obligations.value.firstOrNull { it.id == occurrence.obligationId } ?: return@launch
            val (piasters, _) = evaluateToPiasters(expression)
            val txId = dao.insertTransaction(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPiasters = piasters,
                    amountExpression = expression,
                    accountId = accountId,
                    note = obligation.title,
                )
            )
            dao.updateOccurrence(
                occurrence.copy(status = "PAID", paidTransactionId = txId, paidAmountPiasters = piasters)
            )
            closeOverlay()
        }
    }

    fun skipOccurrence(occurrenceId: Long) {
        viewModelScope.launch {
            val occurrence = occurrences.value.firstOrNull { it.id == occurrenceId } ?: return@launch
            if (occurrence.status == "PENDING") {
                dao.updateOccurrence(occurrence.copy(status = "SKIPPED"))
            }
        }
    }

    // ---------- financial plans ----------

    fun addPlan(name: String, targetExpression: String, initialAllocationExpression: String?) {
        viewModelScope.launch {
            val (target, _) = evaluateToPiasters(targetExpression)
            val planId = dao.insertPlan(
                PlanEntity(name = name, targetPiasters = target, targetExpression = targetExpression)
            )
            if (!initialAllocationExpression.isNullOrBlank()) {
                val (amount, _) = evaluateToPiasters(initialAllocationExpression)
                if (amount > 0) {
                    dao.insertAllocation(
                        AllocationEntity(
                            planId = planId,
                            amountPiasters = amount,
                            expression = initialAllocationExpression,
                            kind = "ALLOCATE",
                        )
                    )
                }
            }
            closeOverlay()
        }
    }

    fun setPlanStatus(plan: PlanEntity, status: String) {
        viewModelScope.launch { dao.updatePlan(plan.copy(status = status)) }
    }

    /** Allocating money to a plan earmarks liquidity; it is not an expense. */
    fun allocateToPlan(planId: Long, expression: String, kind: String) {
        viewModelScope.launch {
            val (piasters, _) = evaluateToPiasters(expression)
            dao.insertAllocation(
                AllocationEntity(planId = planId, amountPiasters = piasters, expression = expression, kind = kind)
            )
        }
    }

    // ---------- totals ----------

    private fun liquidAccounts(): List<AccountEntity> = accounts.value.filter { it.type != "CREDIT" }

    fun totalAssets(): Long {
        var total = 0L
        for (a in liquidAccounts()) total = Math.addExact(total, accountBalance(a, transactions.value))
        for (d in debts.value) {
            if (d.direction == "LENT" && !d.settled) total = Math.addExact(total, d.remainingPiasters)
        }
        return total
    }

    fun totalLiabilities(): Long {
        var total = 0L
        for (a in accounts.value) {
            if (a.type == "CREDIT") {
                val owed = accountBalance(a, transactions.value)
                if (owed > 0) total = Math.addExact(total, owed)
            }
        }
        for (d in debts.value) {
            if (d.direction == "BORROWED" && !d.settled) total = Math.addExact(total, d.remainingPiasters)
        }
        return total
    }

    fun totalAllocatedToActivePlans(): Long {
        var total = 0L
        for (p in plans.value) {
            if (p.status != "ACTIVE") continue
            val allocated = planAllocated(p.id, allocations.value)
            if (allocated > 0) total = Math.addExact(total, allocated)
        }
        return total
    }

    /** Allocation earmarks liquidity: it lowers free cash, never total assets. */
    fun freeLiquidity(): Long {
        var total = 0L
        for (a in liquidAccounts()) total = Math.addExact(total, accountBalance(a, transactions.value))
        return Math.subtractExact(total, totalAllocatedToActivePlans())
    }

    fun dbPath(): String = getApplication<Application>().getDatabasePath("zad.db").absolutePath
}
