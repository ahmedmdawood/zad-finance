package com.ahmed.zad

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Exports the local database file to a user-picked destination. */
suspend fun exportDatabase(context: Context, dbPath: String, target: Uri) = withContext(Dispatchers.IO) {
    val db = ZadDatabase.get(context)
    db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    val source = java.io.File(dbPath)
    if (!source.exists()) throw IllegalStateException("no database")
    context.contentResolver.openOutputStream(target)?.use { out ->
        source.inputStream().use { it.copyTo(out) }
    } ?: throw IllegalStateException("cannot write")
}

/**
 * Replaces the local database with a backup file, then stops the process.
 * The app must be reopened after a successful restore.
 */
suspend fun importDatabase(context: Context, dbPath: String, source: Uri) = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
        ?: throw IllegalStateException("cannot read")
    val header = "SQLite format 3".toByteArray()
    if (bytes.size < 100 || !bytes.copyOfRange(0, header.size).contentEquals(header)) {
        throw IllegalArgumentException("not a sqlite database")
    }
    val temp = java.io.File(dbPath + ".restore")
    temp.writeBytes(bytes)
    ZadDatabase.closeInstance()
    val dbFile = java.io.File(dbPath)
    java.io.File(dbPath + "-wal").delete()
    java.io.File(dbPath + "-shm").delete()
    if (!temp.renameTo(dbFile)) {
        temp.copyTo(dbFile, overwrite = true)
        temp.delete()
    }
    android.os.Process.killProcess(android.os.Process.myPid())
}
