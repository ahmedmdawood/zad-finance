package com.ahmed.zad

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.YearMonth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class V16Test {
    @get:Rule val compose = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication()
    private fun expense(amount: Long = 12345L) = TransactionEntity(type = "EXPENSE", amountPiasters = amount, accountId = 1L, note = "اختبار التصدير")
    private fun evidence(name: String) = File("build/test-evidence/$name").also { it.parentFile.mkdirs() }
    @After fun closeDb() { ZadDatabase.closeInstance() }

    @Test fun overspendDecisionAndNegativeBalance() {
        assertTrue(shouldWarnOverspend("EXPENSE", 600001, 600000))
        assertFalse(shouldWarnOverspend("EXPENSE", 600000, 600000))
        assertFalse(shouldWarnOverspend("INCOME", 900000, 0))
        assertTrue(shouldWarnOverspend("EXPENSE", 1, -100))
        val a = AccountEntity(id = 1, name = "محفظة", type = "CASH", openingPiasters = 600000)
        assertEquals(-1L, accountBalance(a, listOf(expense(600001))))
    }
    @Test fun csvActuallyWritesUtf8File() {
        val file = evidence("sample.csv")
        val now = YearMonth.now()
        exportMonthCsv(app, Uri.fromFile(file), listOf(expense()), emptyList(), now.year, now.monthValue)
        assertTrue(file.length() > 50)
        val text = file.readText(Charsets.UTF_8)
        assertTrue(text.startsWith("\uFEFF"))
        assertTrue(text.contains("123.45"))
        assertTrue(text.contains("اختبار التصدير"))
    }
    @Test fun changingPhotoUpdatesRevisionAndPixels() {
        val vm = MainViewModel(app)
        fun save(color: Int, expected: Long) {
            val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            image.eraseColor(color)
            val bytes = ByteArrayOutputStream().also { image.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            vm.saveUserPhoto(ByteArrayInputStream(bytes))
            val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (vm.userPhotoRevision.value < expected && System.nanoTime() < until) Thread.sleep(20)
            assertEquals(expected, vm.userPhotoRevision.value)
            assertTrue(vm.userPhotoSet.value)
        }
        save(Color.RED, 1)
        save(Color.BLUE, 2)
        val current = android.graphics.BitmapFactory.decodeFile(File(app.filesDir, "user_photo.jpg").absolutePath)
        assertTrue(Color.blue(current.getPixel(32, 32)) > 200)
    }
    @Test fun pinnedNoteSortsFirstAndSurvivesReload() {
        File(app.filesDir, "notes.json").delete()
        val vm = MainViewModel(app)
        vm.addNote("ملاحظة عادية")
        Thread.sleep(5)
        vm.addNote("ملاحظة مهمة")
        val important = vm.notes.value.first { it.text == "ملاحظة مهمة" }
        vm.toggleNotePin(important.id)
        val pinnedFirst = vm.notes.value.sortedWith(compareByDescending<ZadNote> { it.pinned }.thenByDescending { it.createdAt })
        assertEquals("ملاحظة مهمة", pinnedFirst.first().text)
        val reloaded = MainViewModel(app)
        val restored = reloaded.notes.value.first { it.id == important.id }
        assertTrue(restored.pinned)
        vm.toggleNotePin(important.id)
        assertFalse(vm.notes.value.first { it.id == important.id }.pinned)
        File(app.filesDir, "notes.json").delete()
    }
    @Test fun accountDeleteRetainsHistoryAndOtherTransferBalance() {
        val dao = ZadDatabase.get(app).dao()
        runBlocking {
            dao.insertAccount(AccountEntity(id = 21, name = "حساب قديم", type = "CASH", openingPiasters = 600000))
            val bank = AccountEntity(id = 22, name = "البنك", type = "BANK", openingPiasters = 0)
            dao.insertAccount(bank)
            val txId = dao.insertTransaction(TransactionEntity(type = "TRANSFER", amountPiasters = 100000, accountId = 21, toAccountId = 22))
            dao.deleteAccount(21)
            val saved = dao.transactionById(txId)
            assertNotNull(saved)
            assertEquals(100000L, accountBalance(bank, listOf(saved!!)))
        }
    }
}
