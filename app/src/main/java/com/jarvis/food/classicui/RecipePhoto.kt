package com.jarvis.food.classicui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File

/** Foto delle ricette create dall'utente: ridotta (max 1280 px), ruotata correttamente e salvata nella memoria privata dell'app. */
object RecipePhoto {
    private const val MAX = 1280

    fun save(ctx: Context, uri: Uri, recipeId: String): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX) sample *= 2
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
        val k = minOf(1f, MAX.toFloat() / maxOf(src.width, src.height))
        val m = Matrix().apply { postRotate(rotation); postScale(k, k) }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        val file = File(ctx.filesDir, "recipe_${recipeId}_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        // una sola foto per ricetta: elimino le precedenti
        ctx.filesDir.listFiles { f -> f.name.startsWith("recipe_${recipeId}_") && f != file }?.forEach { it.delete() }
        file.absolutePath
    }.getOrNull()

    /** Copia la foto di una ricetta per una ricetta duplicata (così eliminare una non toglie la foto all'altra). */
    fun copy(ctx: Context, path: String?, newRecipeId: String): String? {
        if (path == null) return null
        return runCatching {
            val dst = File(ctx.filesDir, "recipe_${newRecipeId}_${System.currentTimeMillis()}.jpg")
            File(path).copyTo(dst, overwrite = true)
            dst.absolutePath
        }.getOrNull()
    }

    fun delete(path: String?) { if (path != null) runCatching { File(path).delete() } }
}
