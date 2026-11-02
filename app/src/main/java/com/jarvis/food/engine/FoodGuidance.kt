package com.jarvis.food.engine

import com.jarvis.food.data.DiaryEntry
import com.jarvis.food.data.Food
import java.time.LocalDate

/** Label screening, not an overall health score or a NOVA inference. */
object FoodGuidance {
    const val LABEL_SOURCE = "https://www.nhs.uk/live-well/eat-well/food-guidelines-and-food-labels/how-to-read-food-labels/"
    const val FREQUENCY_SOURCE = "https://sapermangiare.an.crea.gov.it/498/che-cosa-sono-e-a-cosa-servono-le-proteine.html"
    const val DRINK_SOURCE = "https://www.gov.scot/publications/supporting-healthy-choices-framework-voluntary-action/pages/13/"
    const val VARIETY_SOURCE = "https://www.crea.gov.it/web/alimenti-e-nutrizione/-/linee-guida-per-una-sana-alimentazione-2018"
    enum class Level { LOW, MEDIUM, HIGH, UNKNOWN }
    data class Signal(val key: String, val label: String, val per100: Double?, val level: Level)
    data class Advice(val title: String, val explanation: String, val portion: String, val frequency: String, val signals: List<Signal>, val caution: Boolean, val alternativeQuery: String)
    private fun unclassifiedLiquid(f: Food): Boolean {
        val n = FoodDb.normalize(f.name)
        return f.liquid && (f.group in setOf("olio", "grassi", "condimenti") || listOf("olio", "aceto", "salsa", "sciroppo").any { n.startsWith(it) })
    }
    fun signals(f: Food): List<Signal> = listOf(
        Triple("sugar", "Zuccheri", if (f.liquid) 2.5 to 11.25 else 5.0 to 22.5),
        Triple("satFat", "Saturi", if (f.liquid) 0.75 to 2.5 else 1.5 to 5.0),
        Triple("salt", "Sale", if (f.liquid) 0.3 to 0.75 else 0.3 to 1.5)
    ).map { (key, label, limits) ->
        val v = NutritionScience.per100(f, key)
        Signal(key, label, v, when { v == null || unclassifiedLiquid(f) -> Level.UNKNOWN; v > limits.second -> Level.HIGH; v <= limits.first -> Level.LOW; else -> Level.MEDIUM })
    }
    fun advice(f: Food, grams: Double, diary: List<DiaryEntry>, date: LocalDate): Advice {
        val n = FoodDb.normalize(f.name)
        val signals = signals(f)
        val high = signals.filter { it.level == Level.HIGH }
        val nuts = listOf("mandorl", "arachid", "nocciol", "pistacch", "anacard", "noci").any { n.contains(it) }
        val pureNuts = nuts && f.name.contains("100%")
        val processed = f.group == "salumi" || (f.source == "db" && f.group.startsWith("carne") &&
            listOf("salame", "salsiccia", "wurstel", "prosciutto", "speck", "bresaola", "pancetta", "affettat").any { n.contains(it) })
        val occurrences = diary.filter { it.food.id == f.id && runCatching { LocalDate.parse(it.date) in date.minusDays(6)..date }.getOrDefault(false) }.map { it.date }.distinct().size
        val title = when { high.isNotEmpty() -> "Da dosare: ${high.joinToString { it.label.lowercase() }}"; processed -> "Carne lavorata · da limitare"; pureNuts -> "100% frutta a guscio · conta la porzione"; signals.any { it.level == Level.UNKNOWN } -> "Valutazione parziale"; else -> "Nessun valore elevato nei parametri valutati" }
        val explanation = when {
            pureNuts -> "Se gli ingredienti confermano solo frutta a guscio, tostatura e macinazione non bastano per definirla poco salutare. È concentrata in energia: conta la quantità."
            processed -> "Limita le carni lavorate e alterna con legumi, pesce, uova o carne fresca secondo le tue preferenze."
            high.isNotEmpty() -> "L'etichetta indica valori elevati per ${high.joinToString { it.label.lowercase() }}. Considera porzione e totale giornaliero; gli zuccheri totali non coincidono con quelli aggiunti."
            else -> "Il giudizio riguarda i dati disponibili, non tutta la dieta. Ingredienti e grado di lavorazione non si deducono dalla marca o dai soli macro."
        }
        val standard = NutritionScience.standardPortion(f)
        val portion = "Quantità scelta: ${grams.toInt()} ${if (f.liquid) "ml" else "g"}. " + if (pureNuts) "Per la frutta a guscio intera il riferimento è 30 g; per la crema considera etichetta ed energia della porzione."
            else standard?.let { "Porzione di riferimento del gruppo: ${it.toInt()} ${if (f.liquid) "ml" else "g"}, da adattare al piano." } ?: "Non ho una porzione standard affidabile per questo prodotto."
        val frequency = "Registrato in $occurrences giorni negli ultimi 7 (diario anche incompleto). " + when {
            pureNuts -> "Non c'è un limite generale di 2–3 volte a settimana per una crema 100%: varia le fonti e rispetta le quantità."
            processed || high.isNotEmpty() -> "Preferisci porzioni contenute e alterna con opzioni meno ricche nei nutrienti evidenziati. Non assegno frequenze precise senza una guida specifica."
            f.group == "carne_rossa" -> "Riferimento CREA: 1 porzione di carne rossa da 100 g alla settimana. È una guida generale per adulti, da integrare con le altre fonti proteiche."
            f.group == "carne_bianca" -> "Riferimento CREA: 1–3 porzioni di carne bianca da 100 g alla settimana, secondo il fabbisogno; alterna con legumi, pesce e uova."
            f.group.startsWith("pesce") -> "Per il pesce fresco, CREA indica 2–4 porzioni da 150 g a settimana secondo il fabbisogno. Varia le specie; per prodotti conservati considera sale e porzioni specifiche."
            f.group == "legumi" -> "Esempio CREA per 2.000 kcal: almeno 3 porzioni settimanali di legumi (150 g cotti). Non è un obiettivo personale automatico; adatta il resto del piano."
            else -> "Alterna gli alimenti del gruppo: un indicatore verde non significa consumo illimitato."
        }
        val query = when { nuts -> "mandorle"; processed -> "ceci"; "cioccol" in n -> "cioccolato fondente"; "muesli" in n || "cereali" in n -> "avena"; "yogurt" in n -> "yogurt"; "latte" in n -> "latte"; "pane" in n || "biscott" in n -> "pane integrale"; f.liquid && high.any { it.key == "sugar" } -> "acqua"; else -> f.name }
        return Advice(title, explanation, portion, frequency, signals, high.isNotEmpty() || processed, query)
    }
}
