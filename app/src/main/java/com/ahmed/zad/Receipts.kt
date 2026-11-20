package com.ahmed.zad

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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

/** Copies a picked photo into private app storage and returns its path. */
fun saveReceipt(context: Context, source: Uri): String {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val target = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        target.outputStream().use { input.copyTo(it) }
    } ?: throw IllegalStateException("cannot read photo")
    return target.absolutePath
}

/** Photo picker row for the expense form. */
@Composable
fun ReceiptPicker(receiptPath: String?, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                onPicked(saveReceipt(context, uri))
            } catch (e: Exception) {
                error = true
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            try {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (e: Exception) {
                error = true
            }
        }) {
            Icon(Icons.Default.Receipt, contentDescription = null)
            Text(if (receiptPath == null) " صورة فاتورة (اختياري)" else " الفاتورة اتحفظت", fontSize = 13.sp)
        }
    }
    if (error) {
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
