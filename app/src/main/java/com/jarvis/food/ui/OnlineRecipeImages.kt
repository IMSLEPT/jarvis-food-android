package com.jarvis.food.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection

/** Test only: no bundled image or recipe data is changed. Files live only in cacheDir. */
object OnlineRecipeImages {
    const val ORIGIN = "https://jarvis-food-images.justale-c.chatgpt.site"
    const val CATALOG_URL = "$ORIGIN/catalog-v1.json"
    const val DISK_LIMIT = 64L * 1024 * 1024
    private const val MAX_IMAGE = 1024 * 1024
    private const val MAX_CATALOG = 2 * 1024 * 1024
    private const val REFRESH_MS = 6 * 60 * 60 * 1000L
    private const val RETRY_MS = 60 * 1000L
    data class Entry(val id: String, val url: String, val sha256: String)
    private val catalogLock = Mutex()
    private val imageLocks = Array(32) { Mutex() }
    private val networkSlots = Semaphore(3)
    private val memory = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    @Volatile private var entries: Map<String, Entry>? = null
    private var lastAttempt = 0L
    private var lastSuccess = 0L
    private val failures = java.util.concurrent.ConcurrentHashMap<String, Long>()
    internal val downloads = AtomicInteger()
    internal var offlineForTest = false

    /** Only HTTPS files on this Site, with bounded IDs and strong content hashes. */
    internal fun parseCatalog(text: String): Map<String, Entry> {
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") == 1)
        val array = root.getJSONArray("images")
        require(array.length() <= 10000)
        return buildMap {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.getString("recipeId")
                val url = item.getString("url")
                val sha = item.getString("sha256")
                require(id.matches(Regex("[a-z0-9_]{1,160}")))
                require(sha.matches(Regex("[a-f0-9]{64}")))
                val parsed = URL(url)
                require(parsed.protocol == "https" && parsed.host == URL(ORIGIN).host && parsed.port == -1)
                require(parsed.userInfo == null && parsed.query == null && parsed.ref == null)
                require(parsed.path.matches(Regex("/images/[a-z0-9_.-]+\\.webp")))
                require(id !in this)
                put(id, Entry(id, url, sha))
            }
        }
    }
    private fun dir(ctx: Context) = File(ctx.cacheDir, "online-recipe-images-v1").apply { mkdirs() }
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun fetch(url: String, limit: Int, type: String): ByteArray {
        check(!offlineForTest) { "Offline test" }
        val c = URL(url).openConnection() as HttpsURLConnection
        c.connectTimeout = 5000; c.readTimeout = 8000; c.instanceFollowRedirects = false
        c.setRequestProperty("Accept", type); c.setRequestProperty("User-Agent", "JarvisFood-ImageTest/1")
        try {
            require(c.responseCode == 200) { "HTTP ${c.responseCode}" }
            require(c.contentType?.substringBefore(';')?.trim() == type) { "Unexpected content type" }
            require(c.contentLengthLong <= limit)
            return c.inputStream.use { input ->
                val out = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    require(out.size() + n <= limit); out.write(buffer, 0, n)
                }; out.toByteArray()
            }.also { downloads.incrementAndGet() }
        } finally { c.disconnect() }
    }
    suspend fun catalog(ctx: Context): Map<String, Entry> = withContext(Dispatchers.IO) {
        catalogLock.withLock {
            val file = AtomicFile(File(dir(ctx), "catalog.json"))
            if (entries == null) {
                entries = runCatching { parseCatalog(file.openRead().use { it.readBytes().toString(Charsets.UTF_8) }) }.getOrDefault(emptyMap())
                lastSuccess = file.baseFile.lastModified()
            }
            val now = System.currentTimeMillis()
            if (now - lastSuccess > REFRESH_MS && now - lastAttempt > RETRY_MS) {
                lastAttempt = now
                try {
                    val bytes = networkSlots.withPermit { fetch(CATALOG_URL, MAX_CATALOG, "application/json") }
                    val parsed = parseCatalog(bytes.toString(Charsets.UTF_8))
                    val out = file.startWrite()
                    try { out.write(bytes); file.finishWrite(out) } catch (e: Exception) { file.failWrite(out); throw e }
                    entries = parsed; lastSuccess = now
                } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Last valid catalog stays usable offline. */ }
            }
            entries.orEmpty()
        }
    }
    suspend fun load(ctx: Context, recipeId: String): Bitmap? {
        val entry = catalog(ctx)[recipeId] ?: return null
        return withContext(Dispatchers.IO) {
            imageLocks[(entry.sha256.hashCode() and Int.MAX_VALUE) % imageLocks.size].withLock {
                memory.get(entry.sha256)?.let { return@withLock it }
                val folder = dir(ctx); val path = File(folder, "${entry.sha256}.webp")
                try {
                    var bytes = path.takeIf { it.exists() }?.readBytes()
                    if (bytes != null && sha(bytes) != entry.sha256) { path.delete(); bytes = null }
                    if (bytes == null) {
                        if (System.currentTimeMillis() - (failures[entry.sha256] ?: 0L) < RETRY_MS) return@withLock null
                        bytes = networkSlots.withPermit { fetch(entry.url, MAX_IMAGE, "image/webp") }
                        require(sha(bytes) == entry.sha256)
                    }
                    val data = bytes ?: return@withLock null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                    require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048)
                    val bmp = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return@withLock null
                    if (!path.exists()) {
                        synchronized(this@OnlineRecipeImages) {
                            val atomic = AtomicFile(path); val out = atomic.startWrite()
                            try { out.write(data); atomic.finishWrite(out) } catch(e: Exception) { atomic.failWrite(out); throw e }
                            trim(folder)
                        }
                    }
                    path.setLastModified(System.currentTimeMillis())
                    failures.remove(entry.sha256); memory.put(entry.sha256, bmp); bmp
                } catch (e: CancellationException) { throw e } catch (_: Exception) {
                    failures[entry.sha256] = System.currentTimeMillis(); null
                }
            }
        }
    }
    internal fun trim(folder: File, limit: Long = DISK_LIMIT) {
        val files = folder.listFiles { f -> f.extension == "webp" }?.sortedBy { it.lastModified() }.orEmpty()
        var total = files.sumOf { it.length() }
        for (file in files) { if (total <= limit) break; val size = file.length(); if (file.delete()) total -= size }
    }
    suspend fun prefetch(ctx: Context, ids: List<String>) = coroutineScope {
        ids.distinct().take(4).forEach { id -> launch { load(ctx, id) } }
    }
    internal fun resetForTest() { entries = null; lastAttempt = 0; lastSuccess = 0; failures.clear(); memory.evictAll(); downloads.set(0); offlineForTest = false }
    internal fun hasMemoryForTest() = memory.size() > 0
    internal fun clearMemoryForTest() { memory.evictAll() }
}
