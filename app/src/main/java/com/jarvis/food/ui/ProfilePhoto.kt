package com.jarvis.food.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Foto profilo: ritagliata quadrata, ridotta a 512 px e salvata nella memoria privata dell'app. */
object ProfilePhoto {
    private const val SIZE = 512

    fun save(ctx: Context, uri: Uri): String? = runCatching {
        // Prima lettura: solo le dimensioni, per non caricare in memoria un'immagine enorme.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
        val src = ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val side = minOf(src.width, src.height)
        val m = Matrix().apply { postRotate(rotation); postScale(SIZE.toFloat() / side, SIZE.toFloat() / side) }
        val out = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side, m, true)
        ctx.filesDir.listFiles { f -> f.name.startsWith("profile_") }?.forEach { it.delete() }
        val file = File(ctx.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        file.absolutePath
    }.getOrNull()

    fun delete(path: String?) { if (path != null) runCatching { File(path).delete() } }
}

/** Cerchio con la foto profilo o, in mancanza, l'iniziale del nome su sfondo arancione tenue. */
@Composable
fun Avatar(name: String, photo: String?, size: Dp, modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = CircleShape) {
    val bmp by produceState<Bitmap?>(null, photo) {
        value = if (photo == null) null else withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(photo) }.getOrNull() }
    }
    val b = bmp
    if (b != null) {
        Image(b.asImageBitmap(), contentDescription = "Foto profilo", contentScale = ContentScale.Crop, modifier = modifier.size(size).clip(shape))
    } else {
        Box(modifier.size(size).clip(shape).background(Brush.linearGradient(listOf(A.Tonal, A.Tonal))),
            contentAlignment = Alignment.Center) {
            Text(name.trim().take(1).uppercase().ifBlank { "J" }, style = T.Title.copy(color = A.TonalInk, fontSize = (size.value * 0.42f).sp))
        }
    }
}
