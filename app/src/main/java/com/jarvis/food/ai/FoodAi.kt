package com.jarvis.food.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.jarvis.food.data.AiConfig
import com.jarvis.food.data.Food
import com.jarvis.food.engine.FoodDb
import com.jarvis.food.engine.ImageCredit
import com.jarvis.food.engine.Macro
import com.jarvis.food.engine.RecipeTemplate
import com.jarvis.food.engine.TemplateIngredient
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

/** Alimento riconosciuto dall'AI. Se [foodId] è valorizzato, i valori nutrizionali vengono dal database (non dall'AI). */
data class AiItem(
    val name: String,
    val grams: Double,
    val foodId: String?,
    /** Solo per alimenti fuori database: stima AI della porzione intera. */
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
) {
    /** Alimento non presente nel database: valori per 100 g ricavati dalla stima AI, segnalati come tali. */
    fun toEstimatedFood(): Food {
        val g = grams.coerceAtLeast(1.0); val k = 100.0 / g
        return Food(
            id = "ai:${name.lowercase().hashCode()}:${System.currentTimeMillis()}",
            name = name, kcal = kcal * k, protein = protein * k, carbs = carbs * k, fat = fat * k,
            servingName = "porzione stimata", servingGrams = g, source = "ai", category = "Stima AI"
        )
    }
}

data class PantryGuess(val name: String, val foodId: String?, val grams: Double?)

data class AiLabel(val label: String, val score: Float)

object FoodAi {

    // ---------------------------------------------------------------- client OpenAI-compatibile

    fun toJpegBase64(bitmap: Bitmap, maxSide: Int = 1280): String {
        val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
        val b = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
        val out = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** Invia la richiesta provando prima il modello scelto e poi i modelli gratuiti di riserva. */
    private suspend fun chat(cfg: AiConfig, prompt: String, image: Bitmap?): String = withContext(Dispatchers.IO) {
        require(cfg.apiKey.isNotBlank()) { "Serve la chiave gratuita: Profilo → Intelligenza artificiale." }
        val models = (listOf(cfg.model) + if (cfg.baseUrl.contains("openrouter")) AiConfig.FALLBACKS else emptyList()).distinct()
        val img = image?.let { toJpegBase64(it) }
        var lastError: Exception? = null
        for (m in models) {
            try { return@withContext call(cfg, m, prompt, img) } catch (e: Exception) { lastError = e }
        }
        throw lastError ?: RuntimeException("Nessun modello disponibile")
    }

    private fun call(cfg: AiConfig, model: String, prompt: String, imgB64: String?): String {
        val content: Any = if (imgB64 == null) prompt else JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$imgB64")))
        val body = JSONObject().put("model", model).put("temperature", 0.1)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
        val c = URL(cfg.baseUrl.trimEnd('/') + "/chat/completions").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 20000; c.readTimeout = 120000
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("Authorization", "Bearer ${cfg.apiKey.trim()}")
        c.setRequestProperty("X-Title", "Jarvis Food")
        try {
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            val txt = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(txt).getJSONObject("error").getString("message") }.getOrNull()
                throw RuntimeException("Errore AI $code ($model): ${msg ?: txt.take(160)}")
            }
            return JSONObject(txt).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "")
        } finally { c.disconnect() }
    }

    private fun jsonOf(answer: String): JSONObject {
        val s = answer.indexOf('{'); val e = answer.lastIndexOf('}')
        if (s < 0 || e <= s) throw RuntimeException("Risposta AI non leggibile: ${answer.take(140)}")
        return JSONObject(answer.substring(s, e + 1))
    }

    private fun catalog(db: FoodDb) = db.foods.joinToString("\n") { "${it.id}: ${it.name}" }

    /** Se l'AI propone un id non valido, provo a riconoscere l'alimento dal nome. */
    private fun resolveId(db: FoodDb, id: String?, name: String): String? =
        id?.takeIf { db.food(it) != null } ?: db.match(name)?.id

    private fun Double.nz() = if (isNaN()) 0.0 else this

    // ---------------------------------------------------------------- funzioni

    /** Foto di un piatto: alimenti e grammi. I valori nutrizionali li calcola l'app dal database. */
    suspend fun analyzeMeal(cfg: AiConfig, db: FoodDb, bitmap: Bitmap, hint: String): Pair<List<AiItem>, String> {
        val prompt = """Sei un nutrizionista. Guarda la foto del pasto, riconosci ogni alimento e stima i grammi presenti nel piatto
(peso dell'alimento così come appare, cotto se è cotto; per pasta e riso cotti indica il peso CRUDO equivalente dividendo per 2,2 la pasta e per 2,5 il riso).
Per ogni alimento scegli l'id più adatto da questo catalogo; se non c'è, usa id null e stima kcal e macronutrienti della porzione.
CATALOGO (id: nome):
${catalog(db)}
${if (hint.isNotBlank()) "Informazioni dall'utente: $hint\n" else ""}Rispondi SOLO con JSON:
{"items":[{"id":"pasta","name":"pasta al pomodoro","grams":90,"kcal":0,"protein":0,"carbs":0,"fat":0}],"note":"..."}"""
        val o = jsonOf(chat(cfg, prompt, bitmap))
        val arr = o.optJSONArray("items") ?: JSONArray()
        val items = (0 until arr.length()).map { i ->
            val it = arr.getJSONObject(i)
            val name = it.optString("name", "Alimento")
            AiItem(
                name = name, grams = it.optDouble("grams", 100.0).nz().coerceIn(1.0, 1500.0),
                foodId = resolveId(db, it.optString("id").takeIf { s -> s.isNotBlank() && s != "null" }, name),
                kcal = it.optDouble("kcal", 0.0).nz(), protein = it.optDouble("protein", 0.0).nz(),
                carbs = it.optDouble("carbs", 0.0).nz(), fat = it.optDouble("fat", 0.0).nz()
            )
        }
        return items to o.optString("note", "")
    }

    /** Foto di frigorifero o dispensa: prodotti visibili con quantità stimata. */
    suspend fun pantryFromPhoto(cfg: AiConfig, db: FoodDb, bitmap: Bitmap): List<PantryGuess> {
        val prompt = """Guarda la foto di un frigorifero, una dispensa o una spesa. Elenca gli alimenti riconoscibili
(leggi anche le etichette). Per ciascuno scegli l'id dal catalogo (o null) e stima i grammi disponibili (null se non stimabile).
CATALOGO (id: nome):
${catalog(db)}
Rispondi SOLO con JSON: {"items":[{"id":"uova","name":"uova","grams":300}]}"""
        return guesses(db, jsonOf(chat(cfg, prompt, bitmap)))
    }

    /** Testo di uno scontrino (letto sul telefono con OCR): prodotti alimentari e quantità. */
    suspend fun pantryFromReceipt(cfg: AiConfig, db: FoodDb, receiptText: String): List<PantryGuess> {
        val prompt = """Questo è il testo di uno scontrino italiano letto con OCR (può contenere errori).
Estrai SOLO i prodotti alimentari. Per ciascuno scegli l'id dal catalogo (o null) e indica i grammi o ml acquistati
se ricavabili dalla descrizione (es. "PASTA 500G" -> 500), altrimenti null. Ignora prezzi, sconti, borse, detersivi.
CATALOGO (id: nome):
${catalog(db)}
SCONTRINO:
${receiptText.take(4000)}
Rispondi SOLO con JSON: {"items":[{"id":"pasta","name":"spaghetti","grams":500}]}"""
        return guesses(db, jsonOf(chat(cfg, prompt, null)))
    }

    private fun guesses(db: FoodDb, o: JSONObject): List<PantryGuess> {
        val arr = o.optJSONArray("items") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            val it = arr.getJSONObject(i)
            val name = it.optString("name", "").ifBlank { return@mapNotNull null }
            val g = if (it.isNull("grams")) null else it.optDouble("grams", Double.NaN).takeIf { v -> !v.isNaN() && v > 0 }
            PantryGuess(name, resolveId(db, it.optString("id").takeIf { s -> s.isNotBlank() && s != "null" }, name), g)
        }
    }

    /**
     * Ricetta nuova con ciò che c'è in casa. L'AI sceglie SOLO ingredienti del catalogo e scrive i passaggi;
     * grammature e valori nutrizionali li calcola poi l'app con il database e il calcolatore.
     */
    suspend fun inventRecipe(cfg: AiConfig, db: FoodDb, pantryIds: List<String>, meal: String, target: Macro, exclude: List<String>): RecipeTemplate {
        val avail = pantryIds.mapNotNull { db.food(it) }.joinToString("\n") { "${it.id}: ${it.name}" }
        val prompt = """Sei uno chef e nutrizionista italiano. Crea UNA ricetta realistica e semplice (massimo 30 minuti) per "$meal",
usando principalmente questi ingredienti disponibili in casa:
$avail
Puoi aggiungere solo sale, pepe, olio, aglio, cipolla, erbe aromatiche, limone (id: sale, pepe, olio, aglio, cipolla, prezzemolo, basilico, limone).
${if (exclude.isNotEmpty()) "Evita: ${exclude.joinToString()}.\n" else ""}Obiettivo indicativo: ${target.kcal.toInt()} kcal, ${target.protein.toInt()} g proteine, ${target.carbs.toInt()} g carboidrati, ${target.fat.toInt()} g grassi.
Per ogni ingrediente indica id, grammi a crudo e ruolo: c (fonte di carboidrati), p (proteine), f (grassi/condimento), v (verdura o frutta), x (aromi fissi).
Nei passaggi scrivi {id} dove va la quantità di un ingrediente (es. "Cuoci {pasta} in acqua salata").
Rispondi SOLO con JSON:
{"name":"...","minutes":20,"ingredients":[{"id":"pasta","g":80,"role":"c"}],"steps":["..."],"tip":"..."}"""
        val o = jsonOf(chat(cfg, prompt, null))
        val arr = o.optJSONArray("ingredients") ?: JSONArray()
        val ings = (0 until arr.length()).mapNotNull { i ->
            val it = arr.getJSONObject(i)
            val id = it.optString("id")
            val f = db.food(id) ?: return@mapNotNull null
            val role = it.optString("role", "v").take(1).let { r -> if (r in setOf("c", "p", "f", "v", "x")) r else "v" }
            TemplateIngredient(f.id, it.optDouble("g", 50.0).nz().coerceIn(1.0, 400.0), role)
        }.distinctBy { it.food }
        if (ings.size < 2) throw RuntimeException("La ricetta proposta non usa ingredienti riconosciuti. Riprova.")
        val steps = o.optJSONArray("steps")?.let { s -> (0 until s.length()).map { s.getString(it) } } ?: emptyList()
        return RecipeTemplate(
            id = "ai_" + System.currentTimeMillis(), name = o.optString("name", "Ricetta dalla dispensa"),
            meal = meal, minutes = o.optInt("minutes", 20), ings = ings, steps = steps, tip = o.optString("tip", ""),
            image = null, credit = ImageCredit(title = "Ricetta creata con AI")
        )
    }

    // ---------------------------------------------------------------- offline (TFLite sul telefono)

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
                .setBaseOptions(BaseOptions.builder().setNumThreads(4).build()).setMaxResults(6).build()
            ImageClassifier.createFromFileAndOptions(context, "food.tflite", opts).also { classifier = it }
        } catch (e: Throwable) {
            offlineError = "Modello offline non disponibile: ${e.message}"; throw RuntimeException(offlineError)
        }
    }

    suspend fun analyzeOffline(context: Context, bitmap: Bitmap): List<AiLabel> = withContext(Dispatchers.Default) {
        val cls = getClassifier(context)
        val argb = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        cls.classify(TensorImage.fromBitmap(argb)).flatMap { it.categories }
            .filter { it.label.isNotBlank() && !it.label.contains("background", true) && !it.displayName.contains("background", true) }
            .sortedByDescending { it.score }
            .map { AiLabel(it.displayName.ifBlank { it.label }, it.score) }
            .distinctBy { it.label.lowercase() }.take(5)
    }
}
