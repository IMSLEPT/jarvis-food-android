package com.jarvis.food.ui

import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.food.engine.FoodArtCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object FoodArtCache {
    val cache = object : LruCache<Int, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap) = value.allocationByteCount
    }
    @Suppress("DEPRECATION")
    fun decode(context: android.content.Context, cell: Int): Bitmap? = synchronized(this) {
        cache.get(cell)?.let { return@synchronized it }
        runCatching {
            context.assets.open(FoodArtCatalog.ASSET).use { stream ->
                val decoder = BitmapRegionDecoder.newInstance(stream, false) ?: return@use null
                try {
                    val col = cell % 6; val row = cell / 6
                    decoder.decodeRegion(Rect(col * decoder.width / 6, row * decoder.height / 6,
                        (col + 1) * decoder.width / 6, (row + 1) * decoder.height / 6), null)?.also { cache.put(cell, it) }
                } finally { decoder.recycle() }
            }
        }.getOrNull()
    }
}

/** Offline asynchronous thumbnails with a bounded shared cache and a category fallback. */
@Composable
fun FoodIllustration(name: String, group: String = "", size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val art = remember(name, group) { FoodArtCatalog.resolve(name, group) }
    val context = LocalContext.current.applicationContext
    val bitmap by produceState(art.cell?.let { FoodArtCache.cache.get(it) }, art.cell) {
        val cell = art.cell
        value = if (cell == null) null else withContext(Dispatchers.IO) { FoodArtCache.decode(context, cell) }
    }
    val image = bitmap
    val m = modifier.size(size).clip(RoundedCornerShape(size * 0.25f))
    if (image != null) Image(image.asImageBitmap(), "Illustrazione indicativa: $name", modifier = m.testTag("food-art-${art.cell}"), contentScale = ContentScale.Crop)
    else Box(m.testTag("food-art-fallback").background(C.AccentSoft), contentAlignment = Alignment.Center) {
        Text(art.emoji, style = T.Title)
    }
}
