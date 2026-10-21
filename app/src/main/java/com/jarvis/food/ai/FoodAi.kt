package com.jarvis.food.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.jarvis.food.data.AiConfig
import com.jarvis.food.data.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Un alimento stimato dall'AI: valori riferiti alla porzione vista in foto. */
data class AiItem(
    val name: String,
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double
) {
    /** Converte in Food con valori per 100 g, così si può modificare la quantità. */
    fun toFood(): Food {
        val g = grams.coerceAtLeast(1.0)
        val k = 100.0 / g
        return Food(
            id = "ai:${name.lowercase().hashCode()}:${System.currentTimeMillis()}",
            name = name, kcal = kcal * k, protein = protein * k, carbs = carbs * k, fat = fat * k,
            fiber = fiber * k, servingName = "porzione stimata", servingGrams = g, source = "ai", category = "Stima AI"
        )
    }
}

data class AiLabel(val label: String, val score: Float)

object FoodAi {

    // ---------------- ONLINE (API compatibile OpenAI, es. OpenRouter modelli gratuiti) ----------------

    private const val PROMPT = """Sei un nutrizionista esperto. Analizza la foto del pasto.
Identifica ogni alimento visibile, stima i grammi della porzione presente nel piatto e i valori nutrizionali
per QUELLA porzione (non per 100 g). Usa nomi in italiano.
Rispondi SOLO con JSON valido, senza testo prima o dopo, in questo formato:
{"items":[{"name":"...","grams":0,"kcal":0,"protein":0,"carbs":0,"fat":0,"fiber":0}],"note":"..."}
Se nella foto non c'è cibo rispondi {"items":[],"note":"nessun alimento riconosciuto"}."""

    fun toJpegBase64(bitmap: Bitmap, maxSide: Int = 1024): String {
        val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
        val b = if (scale < 1f) Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
        ) else bitmap
        val out = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeOnline(cfg: AiConfig, bitmap: Bitmap, hint: String = ""): Pair<List<AiItem>, String> =
        withContext(Dispatchers.IO) {
            require(cfg.apiKey.isNotBlank()) { "Inserisci la chiave API nelle impostazioni AI (Profilo → AI foto)." }
            val img = toJpegBase64(bitmap)
            val content = JSONArray()
                .put(JSONObject().put("type", "text").put("text", PROMPT + if (hint.isNotBlank()) "\nNota dell'utente: $hint" else ""))
                .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$img")))
            val body = JSONObject()
                .put("model", cfg.model)
                .put("temperature", 0.2)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))

            val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"
            val c = URL(url).openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.doOutput = true
            c.connectTimeout = 20000
            c.readTimeout = 90000
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("Authorization", "Bearer ${cfg.apiKey.trim()}")
            c.setRequestProperty("X-Title", "Jarvis Food")
            try {
                c.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = c.responseCode
                val txt = (if (code in 200..299) c.inputStream else c.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) {
                    val msg = runCatching { JSONObject(txt).getJSONObject("error").getString("message") }.getOrNull()
                    throw RuntimeException("Errore AI ($code): ${msg ?: txt.take(200)}")
                }
                val answer = JSONObject(txt).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").optString("content", "")
                parseItems(answer)
            } finally {
                c.disconnect()
            }
        }

    private fun parseItems(answer: String): Pair<List<AiItem>, String> {
        val start = answer.indexOf('{')
        val end = answer.lastIndexOf('}')
        if (start < 0 || end <= start) throw RuntimeException("Risposta AI non leggibile: ${answer.take(160)}")
        val obj = JSONObject(answer.substring(start, end + 1))
        val arr = obj.optJSONArray("items") ?: JSONArray()
        val items = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AiItem(
                name = o.optString("name", "Alimento"),
                grams = o.optDouble("grams", 100.0).takeIf { !it.isNaN() } ?: 100.0,
                kcal = o.optDouble("kcal", 0.0).nz(),
                protein = o.optDouble("protein", 0.0).nz(),
                carbs = o.optDouble("carbs", 0.0).nz(),
                fat = o.optDouble("fat", 0.0).nz(),
                fiber = o.optDouble("fiber", 0.0).nz()
            )
        }
        return items to obj.optString("note", "")
    }

    private fun Double.nz() = if (isNaN()) 0.0 else this

    // ---------------- OFFLINE (classificatore TFLite on-device) ----------------

    private var classifier: ImageClassifier? = null
    private var offlineError: String? = null

    fun offlineAvailable(context: Context): Boolean =
        runCatching { context.assets.list("")?.contains("food.tflite") == true }.getOrDefault(false)

    @Synchronized
    private fun getClassifier(context: Context): ImageClassifier {
        classifier?.let { return it }
        offlineError?.let { throw RuntimeException(it) }
        return try {
            val opts = ImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(BaseOptions.builder().setNumThreads(4).build())
                .setMaxResults(5)
                .build()
            ImageClassifier.createFromFileAndOptions(context, "food.tflite", opts).also { classifier = it }
        } catch (e: Throwable) {
            offlineError = "Modello offline non disponibile: ${e.message}"
            throw RuntimeException(offlineError)
        }
    }

    suspend fun analyzeOffline(context: Context, bitmap: Bitmap): List<AiLabel> = withContext(Dispatchers.Default) {
        val cls = getClassifier(context)
        val argb = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val results = cls.classify(TensorImage.fromBitmap(argb))
        results.flatMap { it.categories }
            .filter { it.label.isNotBlank() && !it.label.equals("__background__", true) }
            .sortedByDescending { it.score }
            .map { AiLabel(it.displayName.ifBlank { it.label }, it.score) }
            .distinctBy { it.label.lowercase() }
            .take(5)
    }
}
