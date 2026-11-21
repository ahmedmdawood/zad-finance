package com.ahmed.zad

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var showBrowser by remember { mutableStateOf(false) }
    val startPicker = rememberZadImagePicker(
        onUnavailable = { showBrowser = true },
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
    if (showBrowser) {
        ZadPhotoBrowserDialog(
            onPick = { uri ->
                showBrowser = false
                try {
                    onPicked(saveReceipt(context, uri))
                    error = false
                } catch (e: Exception) {
                    error = true
                }
            },
            onDismiss = { showBrowser = false },
        )
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


/** Reads device photos straight from MediaStore: works with no gallery app installed. */
private fun queryDeviceImages(context: Context, limit: Int = 240): List<Uri> {
    val out = mutableListOf<Uri>()
    runCatching {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (c.moveToNext() && out.size < limit) {
                out.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(idCol)))
            }
        }
    }
    return out
}

private fun loadDeviceThumb(context: Context, uri: Uri): android.graphics.Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 29) {
        context.contentResolver.loadThumbnail(uri, Size(320, 320), null)
    } else {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 320 && bounds.outHeight / (sample * 2) >= 320) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}.getOrNull()

@Composable
private fun DevicePhotoThumb(uri: Uri, onClick: () -> Unit) {
    val context = LocalContext.current
    var bmp by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bmp = withContext(Dispatchers.IO) { loadDeviceThumb(context, uri)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(10.dp)
    val base = Modifier
        .padding(3.dp)
        .aspectRatio(1f)
        .clip(shape)
    val current = bmp
    if (current != null) {
        Image(bitmap = current, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = base.clickable(onClick = onClick))
    } else {
        Box(modifier = base.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

/**
 * In-app photo picker for devices with no gallery/picker apps at all.
 * Reads photos via MediaStore (framework API, always available) and offers
 * direct camera capture when a camera app exists.
 */
@Composable
fun ZadPhotoBrowserDialog(onPick: (Uri) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(Unit) {
        if (!granted) runCatching { permLauncher.launch(perm) }.onFailure { denied = true }
    }
    LaunchedEffect(granted) {
        if (granted) images = withContext(Dispatchers.IO) { queryDeviceImages(context) }
    }
    val hasCamera = remember {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val f = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
            runCatching {
                f.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
            }.onSuccess { onPick(Uri.fromFile(f)) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختار صورة") },
        text = {
            Column {
                if (hasCamera) {
                    OutlinedButton(onClick = { runCatching { cameraLauncher.launch(null) } }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Text(" التقط صورة بالكاميرا", fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                when {
                    denied -> Column {
                        Text("محتاجين إذن الوصول للصور. فعّله من إعدادات التطبيق، أو نزّل أي تطبيق جاليري.", fontSize = 13.sp)
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
                            }
                        }) { Text("افتح الإعدادات") }
                    }
                    !granted -> Text("لحظة واحدة…", fontSize = 13.sp)
                    images.isEmpty() -> Text("مفيش صور محفوظة على الجهاز.", fontSize = 13.sp)
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 420.dp),
                    ) {
                        items(images) { uri ->
                            DevicePhotoThumb(uri) { onPick(uri) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
