package com.ahmed.zad

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// ---------- Transaction types ----------
// INCOME / EXPENSE: real money in or out.
// TRANSFER: moves money between the user's own accounts (not income or expense).
// DEBT_GIVEN / DEBT_COLLECTED: lending moves money to a receivable, not an expense.
// DEBT_BORROWED / DEBT_REPAID: borrowing is a liability, not income.
// CARD_PURCHASE: spending on a credit card raises the liability, not an expense out of cash.
// CARD_PAYMENT: paying a card lowers cash and the liability, without re-counting the expense.

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // CASH | BANK | CREDIT | OTHER
    val openingPiasters: Long,
    val openingExpression: String? = null,
    val creditLimitPiasters: Long? = null,
    val creditLimitExpression: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountPiasters: Long,
    val amountExpression: String? = null,
    val accountId: Long,
    val toAccountId: Long? = null,
    val planId: Long? = null,
    val category: String = "أخرى",
    val receiptPath: String? = null,
    val deletedAt: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: String, // LENT (لي) | BORROWED (عليا)
    val principalPiasters: Long,
    val remainingPiasters: Long,
    val expression: String? = null,
    val settled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amountPiasters: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "salary_confirmations")
data class SalaryConfirmationEntity(
    @PrimaryKey val monthKey: String, // yyyy-MM
    val transactionId: Long,
    val amountPiasters: Long,
    val expression: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recurring_obligations")
data class ObligationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountPiasters: Long,
    val expression: String? = null,
    val cycle: String, // MONTHLY | WEEKLY | CUSTOM_DAYS
    val customIntervalDays: Int? = null,
    val startDate: Long, // day-normalized epoch millis
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "obligation_occurrences")
data class OccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obligationId: Long,
    val dueDate: Long, // day-normalized epoch millis
    val status: String, // PENDING | PAID | SKIPPED
    val paidTransactionId: Long? = null,
    val paidAmountPiasters: Long? = null,
)

@Entity(tableName = "financial_plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetPiasters: Long,
    val targetExpression: String? = null,
    val status: String = "ACTIVE", // ACTIVE | PAUSED | COMPLETED
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "custom_categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val icon: String, // an emoji chosen by the user
    val colorHex: String, // e.g. "#6F4E37"
)

@Entity(tableName = "category_budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val limitPiasters: Long,
    val expression: String? = null,
)

@Entity(tableName = "plan_allocation_history")
data class AllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val amountPiasters: Long,
    val expression: String? = null,
    val kind: String, // ALLOCATE | WITHDRAW
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ZadDao {
    @Insert
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun accountsFlow(): Flow<List<AccountEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun transactionById(id: Long): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("UPDATE transactions SET deletedAt = :whenMs WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, whenMs: Long)

    @Query("UPDATE transactions SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreTransaction(id: Long)

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun transactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun trashFlow(): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeOldTrash(before: Long)

    @Insert
    suspend fun insertCategory(category: CategoryEntity)

    @Query("DELETE FROM custom_categories WHERE name = :name")
    suspend fun deleteCategory(name: String)

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun categoriesFlow(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun debtsFlow(): Flow<List<DebtEntity>>

    @Insert
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long

    @Query("SELECT * FROM debt_payments ORDER BY createdAt ASC")
    fun debtPaymentsFlow(): Flow<List<DebtPaymentEntity>>

    @Insert
    suspend fun insertSalaryConfirmation(confirmation: SalaryConfirmationEntity)

    @Query("SELECT * FROM salary_confirmations ORDER BY monthKey DESC")
    fun salaryConfirmationsFlow(): Flow<List<SalaryConfirmationEntity>>

    @Query("DELETE FROM salary_confirmations WHERE transactionId = :transactionId")
    suspend fun deleteSalaryConfirmationByTransaction(transactionId: Long)

    @Insert
    suspend fun insertObligation(obligation: ObligationEntity): Long

    @Update
    suspend fun updateObligation(obligation: ObligationEntity)

    @Query("SELECT * FROM recurring_obligations ORDER BY createdAt DESC")
    fun obligationsFlow(): Flow<List<ObligationEntity>>

    @Insert
    suspend fun insertOccurrence(occurrence: OccurrenceEntity): Long

    @Update
    suspend fun updateOccurrence(occurrence: OccurrenceEntity)

    @Query("SELECT * FROM obligation_occurrences ORDER BY dueDate ASC")
    fun occurrencesFlow(): Flow<List<OccurrenceEntity>>

    @Query("SELECT MAX(dueDate) FROM obligation_occurrences WHERE obligationId = :obligationId")
    suspend fun maxOccurrenceDueDate(obligationId: Long): Long?

    @Query("SELECT * FROM obligation_occurrences WHERE paidTransactionId = :transactionId LIMIT 1")
    suspend fun occurrenceByTransaction(transactionId: Long): OccurrenceEntity?

    @Insert
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("SELECT * FROM category_budgets")
    fun budgetsFlow(): Flow<List<BudgetEntity>>

    @Query("DELETE FROM category_budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("SELECT * FROM financial_plans ORDER BY createdAt DESC")
    fun plansFlow(): Flow<List<PlanEntity>>

    @Insert
    suspend fun insertAllocation(allocation: AllocationEntity): Long

    @Query("SELECT * FROM plan_allocation_history ORDER BY createdAt DESC")
    fun allocationsFlow(): Flow<List<AllocationEntity>>
}

@Database(
    entities = [
        AccountEntity::class, TransactionEntity::class, DebtEntity::class,
        DebtPaymentEntity::class, SalaryConfirmationEntity::class,
        ObligationEntity::class, OccurrenceEntity::class,
        PlanEntity::class, AllocationEntity::class, BudgetEntity::class, CategoryEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class ZadDatabase : RoomDatabase() {

    abstract fun dao(): ZadDao

    companion object {
        @Volatile
        private var instance: ZadDatabase? = null

        fun get(context: Context): ZadDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZadDatabase::class.java,
                    "zad.db"
                ).build().also { instance = it }
            }

        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}

/** Balance of one account from its opening balance plus all related transactions. */
fun todayStartMillis(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun daysInCurrentMonth(): Int = java.util.Calendar.getInstance().getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

fun dayOfMonth(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)

/** Inclusive start and exclusive end of the month at the given offset from the current one. */
fun monthBounds(offset: Int): Pair<Long, Long> {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    cal.add(java.util.Calendar.MONTH, offset)
    val start = cal.timeInMillis
    cal.add(java.util.Calendar.MONTH, 1)
    return start to cal.timeInMillis
}

fun accountBalance(account: AccountEntity, transactions: List<TransactionEntity>): Long {
    var bal = account.openingPiasters
    if (account.type == "CREDIT") {
        // A credit account's balance is the current liability (amount owed).
        for (t in transactions) {
            when {
                t.type == "CARD_PURCHASE" && t.accountId == account.id ->
                    bal = Math.addExact(bal, t.amountPiasters)
                t.type == "CARD_PAYMENT" && t.toAccountId == account.id ->
                    bal = Math.subtractExact(bal, t.amountPiasters)
            }
        }
        return bal
    }
    for (t in transactions) {
        when (t.type) {
            "INCOME", "DEBT_COLLECTED", "DEBT_BORROWED" ->
                if (t.accountId == account.id) bal = Math.addExact(bal, t.amountPiasters)
            "EXPENSE", "DEBT_GIVEN", "DEBT_REPAID" ->
                if (t.accountId == account.id) bal = Math.subtractExact(bal, t.amountPiasters)
            "CARD_PAYMENT" ->
                if (t.accountId == account.id) bal = Math.subtractExact(bal, t.amountPiasters)
            "TRANSFER" -> {
                if (t.accountId == account.id) bal = Math.subtractExact(bal, t.amountPiasters)
                if (t.toAccountId == account.id) bal = Math.addExact(bal, t.amountPiasters)
            }
        }
    }
    return bal
}

fun planAllocated(planId: Long, allocations: List<AllocationEntity>): Long {
    var total = 0L
    for (a in allocations) {
        if (a.planId != planId) continue
        total = if (a.kind == "ALLOCATE") Math.addExact(total, a.amountPiasters)
        else Math.subtractExact(total, a.amountPiasters)
    }
    return total
}

fun planSpent(planId: Long, transactions: List<TransactionEntity>): Long {
    var total = 0L
    for (t in transactions) {
        if (t.planId == planId && t.type == "EXPENSE") total = Math.addExact(total, t.amountPiasters)
    }
    return total
}

// ---------- Local preferences ----------

class ZadPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("zad_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(v) = prefs.edit().putString("user_name", v).apply()

    var isSpecialIdentity: Boolean
        get() = prefs.getBoolean("special_identity", false)
        set(v) = prefs.edit().putBoolean("special_identity", v).apply()

    var themeId: String
        get() = prefs.getString("theme_id", "gold") ?: "gold"
        set(v) = prefs.edit().putString("theme_id", v).apply()

    var privacyHidden: Boolean
        get() = prefs.getBoolean("privacy_hidden", false)
        set(v) = prefs.edit().putBoolean("privacy_hidden", v).apply()

    /** -1 = theme default, 0 = light, 1 = dark. */
    var darkOverride: Int
        get() = prefs.getInt("dark_override", -1)
        set(v) = prefs.edit().putInt("dark_override", v).apply()

    var voiceEntryEnabled: Boolean
        get() = prefs.getBoolean("voice_entry", false)
        set(v) = prefs.edit().putBoolean("voice_entry", v).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean("app_lock", false)
        set(v) = prefs.edit().putBoolean("app_lock", v).apply()

    var quickAddEnabled: Boolean
        get() = prefs.getBoolean("quick_add", false)
        set(v) = prefs.edit().putBoolean("quick_add", v).apply()

    var weeklyDigestEnabled: Boolean
        get() = prefs.getBoolean("weekly_digest", false)
        set(v) = prefs.edit().putBoolean("weekly_digest", v).apply()

    var dailyReminderEnabled: Boolean
        get() = prefs.getBoolean("daily_reminder", false)
        set(v) = prefs.edit().putBoolean("daily_reminder", v).apply()

    var dailyReminderMinutes: Int
        get() = prefs.getInt("daily_reminder_minutes", 20 * 60)
        set(v) = prefs.edit().putInt("daily_reminder_minutes", v).apply()

    var smsParsingEnabled: Boolean
        get() = prefs.getBoolean("sms_parsing", false)
        set(v) = prefs.edit().putBoolean("sms_parsing", v).apply()

    var smsDefaultAccountId: Long
        get() = prefs.getLong("sms_default_account", 0L)
        set(v) = prefs.edit().putLong("sms_default_account", v).apply()

    var projectionDismissed: Boolean
        get() = prefs.getBoolean("projection_dismissed", false)
        set(v) = prefs.edit().putBoolean("projection_dismissed", v).apply()

    var userPhotoSet: Boolean
        get() = prefs.getBoolean("user_photo_set", false)
        set(v) = prefs.edit().putBoolean("user_photo_set", v).apply()

    var expectedSalaryExpression: String
        get() = prefs.getString("expected_salary_expression", "") ?: ""
        set(v) = prefs.edit().putString("expected_salary_expression", v).apply()

    var passwordSalt: String?
        get() = prefs.getString("pw_salt", null)
        set(v) = prefs.edit().putString("pw_salt", v).apply()

    var passwordVerifier: String?
        get() = prefs.getString("pw_verifier", null)
        set(v) = prefs.edit().putString("pw_verifier", v).apply()

    val hasPassword: Boolean
        get() = passwordSalt != null && passwordVerifier != null

    var failedAttempts: Int
        get() = prefs.getInt("pw_fails", 0)
        set(v) = prefs.edit().putInt("pw_fails", v).apply()

    var lockoutUntil: Long
        get() = prefs.getLong("lockout_until", 0L)
        set(v) = prefs.edit().putLong("lockout_until", v).apply()

    fun clearPassword() {
        prefs.edit().remove("pw_salt").remove("pw_verifier")
            .remove("pw_fails").remove("lockout_until").apply()
    }
}

// ---------- Privacy password (PBKDF2, never stored in plain text) ----------

object PrivacySecurity {

    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun verify(password: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(password, salt), expected)

    fun encode(data: ByteArray): String =
        android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)

    fun decode(data: String): ByteArray =
        android.util.Base64.decode(data, android.util.Base64.NO_WRAP)
}
