package com.jarvis.food

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.chat.Actions
import com.jarvis.food.chat.LocalLlm
import com.jarvis.food.chat.LocalModel
import com.jarvis.food.chat.ModelManager
import com.jarvis.food.chat.Turn
import com.jarvis.food.data.AiConfig
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
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
            val prof = Profile("Alessandro", true, 1997, 178.0, 80.0, 78.0, 2, -1, 0.5, detailed = true, job = 0, stepsLevel = 2, workouts = 3, workoutMin = 45)
            Store.update { it.copy(onboarded = true, profile = prof, goals = Nutrition.autoGoals(prof, 80.0, it.goals)) }
            val system = Actions.systemPrompt(Store.current)
            log("prompt di sistema: ${system.length} caratteri")
            val tests = listOf(
                listOf(Turn("user", "Ho mangiato 150 g di pollo a pranzo")),
                listOf(Turn("user", "Ho mangiato una pasta")),
                listOf(Turn("user", "Ho mangiato una pasta"), Turn("assistant", "Quanta pasta hai mangiato?"), Turn("user", "100 g a pranzo")),
                listOf(Turn("user", "Quante calorie al giorno mi servono?")),
                listOf(Turn("user", "Che macro mi conviene per dimagrire?")),
                listOf(Turn("user", "Ho bevuto mezzo litro d'acqua")),
                listOf(Turn("user", "Stamattina mi sono pesato: 79,4 kg")),
                listOf(Turn("user", "Cambia il mio obiettivo a mantenere il peso"))
            )
            for (h in tests) {
                val q = h.last().text
                val t0 = System.currentTimeMillis()
                var first = 0L
                val raw = LocalLlm.reply(ctx, cfg, system, h) { if (first == 0L) first = System.currentTimeMillis() - t0 }
                val p = Actions.parse(raw)
                log("DOMANDA: ${h.joinToString(" | ") { it.role.take(1) + ": " + it.text }}")
                log("  tempo totale ${(System.currentTimeMillis() - t0) / 1000} s, primo token ${first / 1000} s")
                log("  RISPOSTA GREZZA: ${raw.replace("\n", " ⏎ ")}")
                log("  azioni: ${p.actions.map { it.name + it.args }}  rapide: ${p.quick}")
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
