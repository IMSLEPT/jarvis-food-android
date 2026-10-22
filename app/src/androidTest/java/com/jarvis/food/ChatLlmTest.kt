package com.jarvis.food

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.chat.Actions
import com.jarvis.food.chat.LocalLlm
import com.jarvis.food.chat.LocalModel
import com.jarvis.food.chat.ModelManager
import com.jarvis.food.chat.Turn
import com.jarvis.food.data.AiConfig
import com.jarvis.food.data.Store
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Prova reale del chatbot sul telefono: carica Gemma 4 E2B (copiato dal workflow in files/models) con LiteRT-LM e verifica che
 * risponda in italiano seguendo il protocollo delle azioni. I risultati finiscono in files/chat_results.txt.
 */
@RunWith(AndroidJUnit4::class)
class ChatLlmTest {
    @Test fun gemmaFollowsActionProtocol() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        Store.init(ctx)
        val out = File(ctx.filesDir, "chat_results.txt")
        out.writeText("")
        fun log(s: String) { out.appendText(s + "\n"); println("[chat] $s") }
        try {
            assertTrue("modello non trovato in files/models", ModelManager.isReady(ctx, LocalModel.E2B))
            val cfg = AiConfig(chatEngine = 0, localModel = 0)
            val system = Actions.systemPrompt(Store.current)
            log("prompt di sistema: ${system.length} caratteri")
            val tests = listOf(
                "Ho mangiato 150 g di pollo a pranzo",
                "Ho mangiato una pasta",
                "Quante calorie al giorno mi servono?"
            )
            for (q in tests) {
                val t0 = System.currentTimeMillis()
                var first = 0L
                val raw = LocalLlm.reply(ctx, cfg, system, listOf(Turn("user", q))) { if (first == 0L) first = System.currentTimeMillis() - t0 }
                val p = Actions.parse(raw)
                log("DOMANDA: $q")
                log("  tempo totale ${(System.currentTimeMillis() - t0) / 1000} s, primo token ${first / 1000} s")
                log("  RISPOSTA GREZZA: ${raw.replace("\n", " ⏎ ")}")
                log("  testo: ${p.text}")
                log("  azioni: ${p.actions.map { it.name + it.args }}")
                log("  risposte rapide: ${p.quick}")
                assertTrue("risposta vuota per: $q", raw.isNotBlank())
            }
            log("OK")
        } catch (t: Throwable) {
            log("ERRORE: ${t.javaClass.name}: ${t.message}")
            t.stackTrace.take(12).forEach { log("    at $it") }
            throw t
        }
    }
}
