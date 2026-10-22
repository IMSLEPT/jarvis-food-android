package com.jarvis.food.chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/** Modelli open source (Gemma 4, licenza Apache-2.0) scaricabili da Hugging Face senza account. */
enum class LocalModel(val title: String, val file: String, val url: String, val bytes: Long, val note: String) {
    E2B(
        "Gemma 4 E2B", "gemma-4-E2B-it.litertlm",
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        2_588_147_712L, "Più veloce e leggero. Ottimo per registrare cibo e rispondere in fretta."
    ),
    E4B(
        "Gemma 4 E4B", "gemma-4-E4B-it.litertlm",
        "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        3_659_530_240L, "Più intelligente e preciso nei ragionamenti, un po' più lento."
    );

    companion object { fun of(index: Int) = entries[index.coerceIn(0, entries.lastIndex)] }
}

/**
 * Scarica e conserva il modello nella memoria privata dell'app. Il download riprende da dove si era fermato
 * se l'app viene chiusa o la rete cade; il file è usato solo quando è completo (dimensione verificata).
 */
object ModelManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var cancelled = false

    var downloading by mutableStateOf<LocalModel?>(null)
        private set
    var downloaded by mutableLongStateOf(0L)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private fun dir(ctx: Context) = File(ctx.filesDir, "models").apply { mkdirs() }
    fun file(ctx: Context, m: LocalModel) = File(dir(ctx), m.file)
    private fun part(ctx: Context, m: LocalModel) = File(dir(ctx), m.file + ".part")

    fun isReady(ctx: Context, m: LocalModel): Boolean = file(ctx, m).let { it.exists() && it.length() == m.bytes }

    /** Byte già scaricati di un download interrotto. */
    fun partial(ctx: Context, m: LocalModel): Long = part(ctx, m).let { if (it.exists()) it.length() else 0L }

    fun start(ctx: Context, m: LocalModel) {
        if (downloading != null || isReady(ctx, m)) return
        val app = ctx.applicationContext
        error = null
        val free = dir(app).usableSpace
        val need = m.bytes - partial(app, m)
        if (free < need + 200_000_000L) {
            error = "Spazio insufficiente: servono circa ${(need / 1_000_000_000.0).fmt1()} GB liberi."
            return
        }
        cancelled = false
        downloading = m
        downloaded = partial(app, m)
        job = scope.launch {
            try {
                download(app, m)
                if (isReady(app, m)) error = null
            } catch (e: Exception) {
                if (isActive) error = "Download interrotto: ${e.message ?: "errore di rete"}. Riprova: riparte da dove era arrivato."
            } finally {
                downloading = null
            }
        }
    }

    fun cancel() { cancelled = true; job?.cancel(); downloading = null }

    fun delete(ctx: Context, m: LocalModel) {
        if (downloading == m) cancel()
        file(ctx, m).delete(); part(ctx, m).delete()
        LocalLlm.unload()
    }

    private fun download(ctx: Context, m: LocalModel) {
        val part = part(ctx, m)
        var offset = if (part.exists()) part.length() else 0L
        if (offset > m.bytes) { part.delete(); offset = 0L }
        var url = m.url
        var redirects = 0
        while (true) {
            val c = URL(url).openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 20_000; c.readTimeout = 30_000
            if (offset > 0) c.setRequestProperty("Range", "bytes=$offset-")
            c.setRequestProperty("User-Agent", "JarvisFood")
            try {
                val code = c.responseCode
                if (code in 301..308) {
                    url = c.getHeaderField("Location") ?: throw RuntimeException("reindirizzamento non valido")
                    if (++redirects > 6) throw RuntimeException("troppi reindirizzamenti")
                    continue
                }
                if (code == 416) { // il file parziale è già completo o non valido
                    if (offset == m.bytes) { part.renameTo(file(ctx, m)); return }
                    part.delete(); offset = 0L; continue
                }
                if (code != 200 && code != 206) throw RuntimeException("server: HTTP $code")
                if (code == 200 && offset > 0) { part.delete(); offset = 0L } // il server non supporta la ripresa
                RandomAccessFile(part, "rw").use { raf ->
                    raf.seek(offset)
                    c.inputStream.use { input ->
                        val buf = ByteArray(256 * 1024)
                        var total = offset
                        while (true) {
                            if (cancelled) return
                            val n = input.read(buf)
                            if (n < 0) break
                            raf.write(buf, 0, n)
                            total += n
                            downloaded = total
                        }
                    }
                }
                if (part.length() != m.bytes) throw RuntimeException("file incompleto (${part.length()} di ${m.bytes} byte)")
                file(ctx, m).delete()
                if (!part.renameTo(file(ctx, m))) throw RuntimeException("impossibile salvare il modello")
                return
            } finally { c.disconnect() }
        }
    }

    fun Double.fmt1(): String = String.format(java.util.Locale.ITALY, "%.1f", this)
}

