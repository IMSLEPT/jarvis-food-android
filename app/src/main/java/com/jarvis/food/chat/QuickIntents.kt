package com.jarvis.food.chat

import com.jarvis.food.data.AppState
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Store
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

/**
 * Risposte immediate, senza modello: comandi chiari ("ho bevuto 500 ml", "ho mangiato 150 g di pollo a pranzo",
 * "peso 79,4") e domande sui dati già presenti ("quante calorie mi restano?"). Il modello sul telefono resta per
 * tutto il resto. Se la frase è ambigua o l'azione non riesce, [parse]/[answer] danno null e decide il modello.
 */
object QuickIntents {
    private val num = "(\\d+(?:[.,]\\d+)?)"
    private fun d(s: String) = s.replace(',', '.').toDouble()
    private fun f0(v: Double) = Math.round(v).toString()

    private val waterRe = Regex("""\b(?:bevo|bevuto|bevi|aggiungi|segna|registra)\b.*?$num\s*(ml|cl|l|lt|litri?|litro|bicchier[ei]|bottigli[ae])""")
    private val waterWords = Regex("""\b(?:bevo|bevuto|bevi)\b.*\b(?:un|una|mezzo)\s+(litro|bicchiere|bottiglia)""")
    private val weightRe = Regex("""\b(?:peso|pesato|pesata|bilancia)\b\D{0,20}$num\s*(?:kg|chili|chilogrammi)?""")
    private val foodRe = Regex("""\b(?:ho\s+mangiato|mangio|ho\s+preso|ho\s+assunto|aggiungi|segna|registra)\b.*?$num\s*(?:g|gr|grammi)\b\s*(?:di|d')?\s*([^,.]+)""")
    private val mealRe = Regex("""\b(colazione|pranzo|cena|merenda|spuntino)\b""")
    private val startFastRe = Regex("""\b(?:avvia|inizia|comincia|parti|attiva|start)\b.*\bdigiuno\b(?:.*?(\d{2}:\d{1,2}))?""")
    private val stopFastRe = Regex("""\b(?:termina|finisci|fine|chiudi|stop|interrompi|ferma)\b.*\bdigiuno\b""")

    data class Plan(val calls: List<ActionCall>, val reply: (AppState) -> String)

    private fun call(name: String, vararg kv: Pair<String, Any>) =
        ActionCall(name, JSONObject().also { o -> kv.forEach { (k, v) -> o.put(k, v) } })

    private fun mealWord(t: String): String? = mealRe.find(t)?.groupValues?.get(1)

    /** Comando da eseguire subito, oppure null se serve il modello. */
    fun parse(raw: String): Plan? {
        val t = raw.lowercase(Locale.ITALIAN).trim().replace("’", "'")
        if (t.length > 140 || t.endsWith("?")) return null
        if (Regex("""\b(?:ieri|domani|l'altro ieri|scorso|ogni)\b""").containsMatchIn(t)) return null

        waterRe.find(t)?.let { m ->
            val n = d(m.groupValues[1]); val u = m.groupValues[2]
            val ml = when {
                u == "ml" -> n; u == "cl" -> n * 10
                u.startsWith("l") && u != "lt" || u == "lt" -> n * 1000
                u.startsWith("bicchier") -> n * 250
                else -> n * 500
            }.toInt()
            if (ml in 50..3000 && (t.contains("acqua") || u == "ml" || u == "cl" || u.startsWith("l") || u.startsWith("bicch")))
                return Plan(listOf(call("add_water", "ml" to ml))) { s -> "Aggiunti $ml ml. Oggi sei a ${s.water[LocalDate.now().toString()] ?: 0} ml su ${s.goals.waterMl}." }
        }
        waterWords.find(t)?.let { m ->
            val ml = when (m.groupValues[1]) { "litro" -> if (t.contains("mezzo")) 500 else 1000; "bicchiere" -> 250; else -> 500 }
            return Plan(listOf(call("add_water", "ml" to ml))) { s -> "Aggiunti $ml ml. Oggi sei a ${s.water[LocalDate.now().toString()] ?: 0} ml su ${s.goals.waterMl}." }
        }
        if (!t.contains("pesavo") && !t.contains("era")) weightRe.find(t)?.let { m ->
            val kg = d(m.groupValues[1])
            if (kg in 30.0..300.0) return Plan(listOf(call("log_weight", "kg" to kg))) { s ->
                val prev = s.weights.filter { it.date != LocalDate.now().toString() }.lastOrNull()?.kg
                "Peso registrato: ${"%.1f".format(Locale.ITALY, kg)} kg." + (prev?.let { p -> val diff = kg - p; if (kotlin.math.abs(diff) >= 0.05) " Rispetto all'ultima volta: ${"%+.1f".format(Locale.ITALY, diff)} kg." else " Invariato rispetto all'ultima volta." } ?: "")
            }
        }
        if (stopFastRe.containsMatchIn(t)) return Plan(listOf(call("stop_fast"))) { "Digiuno terminato e salvato nello storico." }
        startFastRe.find(t)?.let { m ->
            val plan = m.groupValues[1].ifBlank { "16:8" }
            return Plan(listOf(call("start_fast", "plan" to plan))) { "Digiuno $plan avviato. Ti avviso quando è il momento di mangiare." }
        }
        foodRe.find(t)?.let { m ->
            val g = d(m.groupValues[1])
            var food = m.groupValues[2].trim()
            val meal = mealWord(t)
            food = food.replace(Regex("""\s+(?:a|per|al|alla|di)\s+(?:colazione|pranzo|cena|merenda|spuntino)\b.*$"""), "").replace(mealRe, "").trim()
            // Più alimenti nella stessa frase: lo gestisce il modello.
            if (food.isBlank() || Regex("""\s(?:e|con|più|piu)\s|\+""").containsMatchIn(food)) return null
            val args = mutableListOf<Pair<String, Any>>("food" to food, "grams" to g)
            if (meal != null) args += "meal" to meal
            return Plan(listOf(call("log_food", *args.toTypedArray()))) { s ->
                val e = Store.energyFor(s, LocalDate.now()); val tot = Nutrition.sum(Store.entriesFor(s, LocalDate.now()))
                "Oggi sei a ${f0(tot.kcal)} kcal su ${f0(e.targetKcal)}."
            }
        }
        return null
    }

    /** Domande a cui l'app sa già rispondere dai propri dati. Null = lo decide il modello. */
    fun answer(raw: String, s: AppState): String? {
        val t = raw.lowercase(Locale.ITALIAN).trim()
        if (t.length > 90) return null
        val today = LocalDate.now()
        val e = Store.energyFor(s, today)
        val entries = Store.entriesFor(s, today)
        val tot = Nutrition.sum(entries)
        fun left(target: Double, eaten: Double, unit: String) = if (target - eaten >= 0) "${f0(target - eaten)} $unit" else "0 $unit (${f0(eaten - target)} oltre)"
        return when {
            Regex("""quante\s+(?:calorie|kcal)\b.*\b(?:restano|rimangono|mancano|posso|rimaste|avanzano)""").containsMatchIn(t) ->
                "Ti restano ${left(e.targetKcal, tot.kcal, "kcal")}: hai mangiato ${f0(tot.kcal)} su ${f0(e.targetKcal)} di oggi."
            Regex("""quant[ea]\s+(?:proteine|carboidrati|grassi)\b.*\b(?:mancano|restano|rimangono|devo)""").containsMatchIn(t) -> when {
                t.contains("protein") -> "Proteine: te ne mancano ${left(e.protein, tot.protein, "g")} (obiettivo ${f0(e.protein)} g)."
                t.contains("carbo") -> "Carboidrati: te ne mancano ${left(e.carbs, tot.carbs, "g")} (obiettivo ${f0(e.carbs)} g)."
                else -> "Grassi: te ne mancano ${left(e.fat, tot.fat, "g")} (obiettivo ${f0(e.fat)} g)."
            }
            Regex("""quant[ae]\s+acqua\b.*\b(?:bevuto|bevuta|ho)""").containsMatchIn(t) ->
                "Hai bevuto ${s.water[today.toString()] ?: 0} ml su ${s.goals.waterMl} ml."
            Regex("""\b(?:cosa|che\s+cosa|quello\s+che)\s+ho\s+mangiato\b""").containsMatchIn(t) ->
                if (entries.isEmpty()) "Oggi non hai ancora registrato nulla."
                else "Oggi hai registrato: " + entries.joinToString("; ") { "${com.jarvis.food.data.MEAL_NAMES[it.meal].lowercase()}: ${it.food.name} ${f0(it.grams)} g" } + ". Totale ${f0(tot.kcal)} kcal."
            Regex("""\b(?:qual\s+è|quanto)\b.*\b(?:obiettivo|target)\b.*\b(?:calori|kcal)""").containsMatchIn(t) ->
                "Il tuo obiettivo di oggi è ${f0(e.targetKcal)} kcal: proteine ${f0(e.protein)} g, carboidrati ${f0(e.carbs)} g, grassi ${f0(e.fat)} g."
            else -> null
        }
    }
}
