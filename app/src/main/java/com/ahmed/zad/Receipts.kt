package com.ahmed.zad

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Image picking that survives stripped devices without Google services.
 * Tries several picker intents in order (system photo picker, GET_CONTENT,
 * classic gallery PICK, OPEN_DOCUMENT) and reports when the device has no
 * image handler at all, instead of crashing on an unresolvable intent.
 */
@Composable
fun rememberZadImagePicker(
    onUnavailable: () -> Unit,
    onPicked: (Uri) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) onPicked(uri)
    }
    return {
        val pm = context.packageManager
        val candidates = listOf(
            Intent("android.provider.action.PICK_IMAGES"),
            Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
            Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*"),
        )
        val chosen = candidates.firstOrNull { it.resolveActivity(pm) != null }
        if (chosen == null) {
            onUnavailable()
        } else {
            try {
                launcher.launch(chosen)
            } catch (e: Exception) {
                onUnavailable()
            }
        }
    }
}

/** Copies a picked photo into private app storage (downscaled) and returns its path. */
fun saveReceipt(context: Context, source: Uri): String {
    val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
        ?: throw IllegalStateException("cannot read photo")
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 1280 && bounds.outHeight / (sample * 2) >= 1280) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val target = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    if (bmp != null) {
        target.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
    } else {
        target.writeBytes(bytes)
    }
    return target.absolutePath
}

/** Photo picker row for the expense form. */
@Composable
fun ReceiptPicker(receiptPath: String?, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf(false) }
    var noPicker by remember { mutableStateOf(false) }
    val startPicker = rememberZadImagePicker(
        onUnavailable = { noPicker = true },
        onPicked = { uri ->
            try {
                onPicked(saveReceipt(context, uri))
                error = false
            } catch (e: Exception) {
                error = true
            }
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { startPicker() }) {
            Icon(Icons.Default.Receipt, contentDescription = null)
            Text(if (receiptPath == null) " صورة فاتورة (اختياري)" else " الفاتورة اتحفظت", fontSize = 13.sp)
        }
    }
    if (noPicker) {
        Text("الجهاز ده مفيهوش تطبيق يفتح الصور. ثبّت أي تطبيق جاليري أو ملفات وجرب تاني.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    } else if (error) {
        Text("مش قادر أحفظ الصورة", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    }
}

/** Tappable receipt indicator + full view dialog. */
@Composable
fun ReceiptView(receiptPath: String) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) {
        Icon(Icons.Default.Receipt, contentDescription = null)
        Text(" الفاتورة", fontSize = 12.sp)
    }
    if (open) {
        val bitmap = remember(receiptPath) {
            BitmapFactory.decodeFile(receiptPath)?.asImageBitmap()
        }
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { Button(onClick = { open = false }) { Text("إغلاق") } },
            text = {
                Column {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "الفاتورة",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        )
                    } else {
                        Text("الصورة مش متوفرة")
                    }
                }
            },
        )
    }
}
