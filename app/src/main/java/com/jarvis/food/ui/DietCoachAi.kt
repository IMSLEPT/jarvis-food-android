package com.jarvis.food.ui

import android.content.Context
import com.jarvis.food.chat.CloudLlm
import com.jarvis.food.chat.Jarvis
import com.jarvis.food.chat.LocalLlm
import com.jarvis.food.chat.Turn
import com.jarvis.food.data.AppState
import java.time.LocalDate

/**
 * Commento scritto dall'AI di Jarvis (modello sul telefono o cloud, come la chat) sui dati della dieta
 * già calcolati da [com.jarvis.food.engine.DietCoach]. Risultato tenuto in memoria per giorno e periodo.
 */
object DietCoachAi {
    private val cache = HashMap<String, String>()
    private fun key(date: LocalDate, days: Int) = "$date:$days"

    fun cached(date: LocalDate, days: Int): String? = cache[key(date, days)]

    fun ready(ctx: Context, s: AppState): Boolean = runCatching { Jarvis.ready(ctx, s.ai) }.getOrDefault(false)

    suspend fun ask(ctx: Context, s: AppState, date: LocalDate, days: Int, summary: String): String {
        val system = "Sei Jarvis, assistente alimentare dell'app Jarvis Food. Rispondi in italiano, in modo pratico e gentile. " +
            "Analizza i dati del diario forniti e dai al massimo 4 consigli concreti e personalizzati (abitudini ripetute, varietà, porzioni, " +
            "proteine, verdura, dolci). Usa solo i dati forniti, non inventare numeri, non fare diagnosi mediche e ricorda che sono stime."
        val question = "Ecco il riepilogo della mia alimentazione:\n$summary\nQuali consigli mi dai per migliorare?"
        val result = runCatching {
            if (s.ai.chatEngine == 0) LocalLlm.reply(ctx, s.ai, system, listOf(Turn("user", question))) {}
            else CloudLlm.reply(s.ai, system, listOf(Turn("user", question)))
        }.getOrElse { "Non sono riuscito a completare l’analisi con l’AI (${it.message ?: "errore"}). I consigli qui sopra restano validi." }
        val clean = result.trim().ifEmpty { "Nessuna risposta dall’AI. I consigli qui sopra restano validi." }
        cache[key(date, days)] = clean
        return clean
    }
}
