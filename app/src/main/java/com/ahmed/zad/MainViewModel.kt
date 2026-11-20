package com.ahmed.zad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
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
}

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

    private val _userName = MutableStateFlow(prefs.userName)
    val userName: StateFlow<String> = _userName

    private val _isSpecial = MutableStateFlow(prefs.isSpecialIdentity)
    val isSpecial: StateFlow<Boolean> = _isSpecial

    private val _themeId = MutableStateFlow(prefs.themeId)
    val themeId: StateFlow<String> = _themeId

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

    /** Deleting a transaction reverts linked state: salary confirmations and paid occurrences. */
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            dao.deleteSalaryConfirmationByTransaction(id)
            val occurrence = dao.occurrenceByTransaction(id)
            if (occurrence != null) {
                dao.updateOccurrence(
                    occurrence.copy(status = "PENDING", paidTransactionId = null, paidAmountPiasters = null)
                )
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
