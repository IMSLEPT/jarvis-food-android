package com.jarvis.food.chat

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.jarvis.food.data.AiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Un turno della conversazione passato al modello. [role] è "user" o "assistant". */
data class Turn(val role: String, val text: String)

/**
 * Modello open source (Gemma 4) eseguito sul telefono con LiteRT-LM: nessun limite di domande, nessuna attesa di rete,
 * i testi non lasciano il dispositivo.
 */
object LocalLlm {
    private var engine: Engine? = null
    private var loadedKey: String? = null
    private val lock = Mutex()

    /** Libera la memoria del modello (cambio modello, eliminazione file). */
    fun unload() {
        runCatching { engine?.close() }
        engine = null; loadedKey = null
    }

    val loaded: Boolean get() = engine != null

    private fun ensure(ctx: Context, path: String, gpu: Boolean): Engine {
        val key = "$path|$gpu"
        engine?.let { if (loadedKey == key) return it }
        unload()
        val cfg = EngineConfig(
            modelPath = path,
            backend = if (gpu) Backend.GPU() else Backend.CPU(),
            cacheDir = ctx.cacheDir.path
        )
        val e = Engine(cfg)
        try { e.initialize() } catch (t: Throwable) { runCatching { e.close() }; throw t }
        engine = e; loadedKey = key
        return e
    }

    suspend fun reply(ctx: Context, cfg: AiConfig, system: String, history: List<Turn>, onText: (String) -> Unit): String =
        lock.withLock {
            withContext(Dispatchers.Default) {
                val model = LocalModel.of(cfg.localModel)
                val file = ModelManager.file(ctx, model)
                require(ModelManager.isReady(ctx, model)) { "Il modello non è ancora scaricato: apri le impostazioni della chat." }
                require(history.isNotEmpty() && history.last().role == "user") { "Nessuna domanda da elaborare." }
                val eng = ensure(ctx.applicationContext, file.path, cfg.chatGpu)
                val earlier = history.dropLast(1).map { if (it.role == "user") Message.user(it.text) else Message.model(it.text) }
                val conv = eng.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(system),
                        initialMessages = earlier,
                        samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.4)
                    )
                )
                conv.use { c ->
                    val sb = StringBuilder()
                    c.sendMessageAsync(history.last().text).collect { piece ->
                        sb.append(piece.toString())
                        onText(sb.toString())
                    }
                    sb.toString()
                }
            }
        }
}

/** Modelli gratuiti sul cloud (API compatibile OpenAI, es. OpenRouter): più potenti ma con limiti giornalieri. */
object CloudLlm {
    suspend fun reply(cfg: AiConfig, system: String, history: List<Turn>): String = withContext(Dispatchers.IO) {
        require(cfg.apiKey.isNotBlank()) { "Serve la chiave gratuita: Profilo → Intelligenza artificiale." }
        val models = (listOf(cfg.model) + if (cfg.baseUrl.contains("openrouter")) AiConfig.FALLBACKS else emptyList()).distinct()
        var last: Exception? = null
        for (m in models) {
            try { return@withContext call(cfg, m, system, history) } catch (e: Exception) { last = e }
        }
        throw last ?: RuntimeException("Nessun modello disponibile")
    }

    private fun call(cfg: AiConfig, model: String, system: String, history: List<Turn>): String {
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        history.forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.text)) }
        val body = JSONObject().put("model", model).put("temperature", 0.4).put("messages", msgs)
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
                throw RuntimeException(if (code == 429) "Limite giornaliero del cloud raggiunto ($model)." else "Errore AI $code ($model): ${msg ?: txt.take(160)}")
            }
            return JSONObject(txt).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "")
        } finally { c.disconnect() }
    }
}
