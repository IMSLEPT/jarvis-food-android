package com.jarvis.food.engine

import com.jarvis.food.data.*
import java.time.LocalDate
import kotlin.math.abs

/** Offline reviewed knowledge; thresholds are population guidance, never diagnostic cut-offs. */
object NutritionEvidence {
    const val VERSION = "2026.11.01-v1"
    const val REVIEWED = "01/11/2026"
    data class Source(val id: String, val title: String, val year: Int, val url: String)
    val sources = listOf(
        Source("sugars", "OMS · Sugars intake for adults and children", 2015, "https://www.who.int/publications/i/item/9789241549028"),
        Source("sodium", "OMS · Sodium intake for adults and children", 2012, "https://www.who.int/publications/i/item/9789241504836"),
        Source("fat", "OMS · Saturated fatty acid and trans-fatty acid intake", 2023, "https://www.who.int/publications/i/item/9789240073630"),
        Source("fiber", "OMS · Carbohydrate intake for adults and children", 2023, "https://www.who.int/publications/i/9789240073593"),
        Source("vitD", "EFSA · Dietary reference values for vitamin D", 2016, "https://doi.org/10.2903/j.efsa.2016.4547"),
        Source("vitC", "EFSA · Dietary reference values for vitamin C", 2013, "https://doi.org/10.2903/j.efsa.2013.3418"),
        Source("vitB12", "EFSA · Dietary reference values: vitamin E and cobalamin", 2015, "https://www.efsa.europa.eu/en/press/news/150709"),
        Source("portions", "SINU · LARN V, Standard quantitativi delle porzioni", 2024, "https://sinu.it/wp-content/uploads/2025/01/Standard-Quantitativi-delle-Porzioni.pdf"),
        Source("variety", "CREA · Linee guida per una sana alimentazione, revisione 2018", 2018, "https://www.crea.gov.it/web/alimenti-e-nutrizione/-/linee-guida-per-una-sana-alimentazione-2018"),
        Source("context", "ISS · Linee guida per una sana alimentazione", 2020, "https://www.epicentro.iss.it/alimentazione/linee-guida-sana-alimentazione-crea-2018")
    )
    fun source(id: String) = sources.first { it.id == id }
    const val FREE_SUGAR_ENERGY_FRACTION = 0.10
    const val SATURATED_ENERGY_FRACTION = 0.10
    const val SODIUM_MG = 2000.0
    const val FIBER_G = 25.0
    fun reference(key: String, p: Profile): Double? = when (key) {
        "vitD" -> 15.0
        "vitC" -> if (p.male) 110.0 else 95.0
        "vitB12" -> 4.0
        "fiber" -> FIBER_G
        "sodium" -> SODIUM_MG
        else -> null
    }
}

data class RecordedNutrient(val key: String, val name: String, val unit: String, val average: Double?,
    val knownEntries: Int, val totalEntries: Int, val reference: Double? = null) {
    val coverage: Double get() = if (totalEntries == 0) 0.0 else knownEntries.toDouble() / totalEntries
    val complete get() = totalEntries > 0 && knownEntries == totalEntries
    val percent: Double? get() = if (complete && reference != null && reference > 0) average?.div(reference)?.times(100) else null
}
data class EatingHabit(val food: Food, val days: Int, val occurrences: Int, val gramsPerOccurrence: Double, val standardGrams: Double?)
data class NutritionNotice(val id: String, val title: String, val explanation: String, val reference: String,
    val meaning: String, val action: String, val sourceId: String, val attention: Boolean = false, val entryId: String? = null)
data class NutritionReview(val window: Int, val recordedDays: Int, val usableDays: List<String>, val heuristicDays: Int,
    val nutrients: List<RecordedNutrient>, val notices: List<NutritionNotice>, val habits: List<EatingHabit>,
    val repeatedBreakfastDays: Int, val breakfastDays: Int, val uniqueFoods: Int, val groups: Int, val applicable: Boolean) {
    fun nutrient(key: String) = nutrients.first { it.key == key }
}

object NutritionScience {
    private fun valid(v: Double?) = v?.takeIf { it.isFinite() && it >= 0 }
    /** Explicit values or data fields only. Total sugars are NEVER substituted for free/added sugars. */
    fun per100(f: Food, key: String): Double? = if (key in f.unknownNutrients || (f.source == "recipe" && f.nutrientDataVersion == 0 && key !in setOf("kcal", "protein", "carbs", "fat"))) null else valid(when (key) {
        "kcal" -> f.kcal
        "protein" -> f.protein
        "carbs" -> f.carbs
        "fat" -> f.fat
        "fiber" -> f.fiber ?: f.micro[key]
        "sugar" -> f.sugar ?: f.micro[key]
        "addedSugar" -> f.addedSugar ?: f.micro[key]
        "freeSugar" -> f.freeSugar ?: f.micro[key]
        "satFat" -> f.satFat ?: f.micro[key]
        "sodium" -> f.micro[key] ?: f.salt?.times(400)
        "salt" -> f.salt ?: f.micro["sodium"]?.div(400)
        else -> f.micro[key]
    })

    fun standardPortion(f: Food): Double? {
        val n = FoodDb.normalize(f.name)
        if (f.source == "recipe") return null
        return when {
            n.contains("yogurt") || n.contains("kefir") -> 125.0
            n.contains("latte") && f.liquid -> 125.0
            n.contains("pane") -> 50.0
            n.contains("avena") || n.contains("cereali da colazione") || n.contains("fette biscottate") -> 30.0
            n.contains("patat") -> 200.0
            n.contains("uova") || n.contains("uovo") -> 50.0
            f.group == "frutta" -> 150.0
            f.group in setOf("frutta_secca", "semi") -> 30.0
            f.group == "legumi" -> if (f.kcal > 250) 50.0 else 150.0
            f.group.startsWith("verdura") -> if (n.contains("insalata") || n.contains("lattuga")) 80.0 else 200.0
            f.group.startsWith("pesce") -> if (n.contains("scatola") || n.contains("affumicat")) 50.0 else 150.0
            f.group.startsWith("carne") -> 100.0
            f.group in setOf("cereali", "cereali_integrali") -> 80.0
            f.group == "latticini" -> if (f.fat > 25) 50.0 else 100.0
            else -> null
        }
    }

    fun analyze(s: AppState, today: LocalDate = LocalDate.now(), days: Int = 7,
        targetForDate: (LocalDate) -> Double = { s.goals.kcal.toDouble() }): NutritionReview {
        require(days in setOf(7, 14, 30))
        val from = today.minusDays(days - 1L)
        val recent = s.diary.filter { e ->
            val date = runCatching { LocalDate.parse(e.date) }.getOrNull()
            date != null && date in from..today && e.grams.isFinite() && e.grams > 0 && e.food.kcal.isFinite() && e.food.kcal >= 0
        }
        val byDay = recent.groupBy { it.date }
        val usable = byDay.filter { (date, entries) ->
            val target = targetForDate(LocalDate.parse(date))
            date in s.nutritionCompleteDays || (date < today.toString() && target.isFinite() && target > 0 &&
                entries.map { it.meal }.distinct().size >= 3 && entries.sumOf { it.food.kcal * it.grams / 100 } >= target * 0.70)
        }
        val entries = usable.values.flatten()
        val n = usable.size
        val applicable = Nutrition.age(s.profile, today) >= 18 && s.profile.special == 0
        val basic = listOf("kcal" to ("Calorie" to "kcal"), "protein" to ("Proteine" to "g"), "carbs" to ("Carboidrati" to "g"),
            "fat" to ("Grassi" to "g"), "sugar" to ("Zuccheri totali" to "g"), "addedSugar" to ("Zuccheri aggiunti" to "g"),
            "freeSugar" to ("Zuccheri liberi" to "g"), "satFat" to ("Grassi saturi" to "g"), "salt" to ("Sale" to "g"))
        val definitions = (basic + Micros.defs.filter { it.key != "sugar" }.map { it.key to (it.name to it.unit) }).distinctBy { it.first }
        val nutrients = definitions.map { (key, label) ->
            val known = entries.mapNotNull { e -> per100(e.food, key)?.times(e.grams / 100) }
            RecordedNutrient(key, label.first, label.second, if (n == 0 || known.isEmpty()) null else known.sum() / n,
                known.size, entries.size, if (applicable) NutritionEvidence.reference(key, s.profile) else null)
        }
        fun m(key: String) = nutrients.first { it.key == key }
        fun culprit(key: String) = entries.maxByOrNull { (per100(it.food, key) ?: 0.0) * it.grams / 100 }
        val notices = mutableListOf<NutritionNotice>()
        val calories = m("kcal").average ?: 0.0
        if (applicable && n >= 3 && calories > 0) {
            val sugar = m("freeSugar")
            if (sugar.complete && sugar.average!! * 4 / calories >= NutritionEvidence.FREE_SUGAR_ENERGY_FRACTION) notices += NutritionNotice(
                "free-sugar", "Zuccheri liberi da riequilibrare", "Media ${fmt(sugar.average)} g/die, ${fmt(sugar.average * 400 / calories)}% dell’energia registrata.",
                "OMS: meno del 10% dell’energia; su ${fmt(calories)} kcal, meno di ${fmt(calories * 0.10 / 4)} g/die. Obiettivo nutrizionale, non limite di tossicità.",
                "Un eccesso abituale favorisce carie e aumento di peso. Non significa che un singolo alimento causi diabete.",
                "Riduci bevande zuccherate e porzioni dolci; considera yogurt bianco, frutta intera e avena.", "sugars", true, culprit("freeSugar")?.id)
            val sat = m("satFat")
            if (sat.complete && sat.average!! * 9 / calories > NutritionEvidence.SATURATED_ENERGY_FRACTION) notices += NutritionNotice(
                "saturated", "Grassi saturi sopra il riferimento", "Media ${fmt(sat.average)} g/die, ${fmt(sat.average * 900 / calories)}% dell’energia.",
                "OMS: non oltre il 10% dell’energia dai saturi; circa ${fmt(calories * 0.10 / 9)} g per l’energia media registrata.",
                "La sostituzione dei saturi con grassi insaturi può migliorare il profilo cardiovascolare; conta l’abitudine complessiva.",
                "Alterna burro e formaggi grassi con olio d’oliva, legumi o yogurt meno grasso.", "fat", true, culprit("satFat")?.id)
            val sodium = m("sodium")
            if (sodium.complete && sodium.average!! >= NutritionEvidence.SODIUM_MG) notices += NutritionNotice(
                "sodium", "Sale da ridurre gradualmente", "Media ${fmt(sodium.average / 400)} g di sale equivalenti/die (${fmt(sodium.average)} mg di sodio).",
                "OMS per adulti: meno di 2.000 mg di sodio/die, equivalenti a 5 g di sale. Obiettivo nutrizionale.",
                "Un consumo elevato e prolungato contribuisce all’aumento della pressione arteriosa.",
                "Confronta le etichette e riduci condimenti salati; alterna salumi e formaggi con alimenti meno salati.", "sodium", true, culprit("sodium")?.id)
            val fiber = m("fiber")
            if (fiber.complete && fiber.average!! < NutritionEvidence.FIBER_G) notices += NutritionNotice(
                "fiber", "Più spazio alle fibre", "Media registrata ${fmt(fiber.average)} g/die.", "OMS per adulti: almeno 25 g/die di fibre naturalmente presenti negli alimenti.",
                "È un riferimento dietetico, non una diagnosi di carenza.", "Aumenta gradualmente avena, legumi, cereali integrali, frutta e verdura, insieme ai liquidi.", "fiber")
        }
        val breakfasts = recent.filter { it.meal == 0 }.groupBy { it.date }
        val fingerprints = breakfasts.values.map { es -> es.map { it.food.id }.distinct().sorted().joinToString("|") }
        val repeat = fingerprints.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
        if (repeat >= 3) notices += NutritionNotice("repetition", "La tua colazione abituale", "La stessa combinazione compare $repeat volte su ${breakfasts.size} giorni con colazione registrata.",
            "Non esiste un limite universale alle ripetizioni di una colazione.", "Ripetere un alimento non è di per sé dannoso: contano quantità, composizione e varietà complessiva.",
            "Puoi alternare fonti di fibre e proteine senza cambiare necessariamente tutta la colazione.", "variety", entryId = breakfasts.values.lastOrNull()?.firstOrNull()?.id)
        if (n > 0 && (!m("addedSugar").complete || !m("freeSugar").complete)) notices += NutritionNotice("missing-sugars", "Zuccheri: mancano alcuni dati", "Totali, aggiunti e liberi sono voci diverse; l’etichetta spesso riporta soltanto i totali.",
            "Il riferimento OMS del 10% riguarda gli zuccheri liberi. Nessun confronto viene effettuato con gli zuccheri totali.",
            "Non è possibile quantificare gli aggiunti o i liberi dalle sole calorie o dai totali.", "Aggiungi i valori dichiarati dal produttore o dalla ricetta nella modifica dell’alimento; lascia vuoto ciò che non conosci.", "sugars")
        val habits = recent.groupBy { it.food.id }.values.map { es ->
            EatingHabit(es.first().food, es.map { it.date }.distinct().size, es.size, es.sumOf { it.grams } / es.size, standardPortion(es.first().food))
        }.sortedWith(compareByDescending<EatingHabit> { it.days }.thenByDescending { it.occurrences }).take(12)
        return NutritionReview(days, byDay.size, usable.keys.sorted(), usable.keys.count { it !in s.nutritionCompleteDays }, nutrients, notices, habits,
            repeat, breakfasts.size, recent.map { it.food.id }.distinct().size, recent.map { it.food.group }.filter { it.isNotBlank() }.distinct().size, applicable)
    }
    fun fmt(value: Double?): String = value?.let { String.format(java.util.Locale.ITALY, "%.1f", it) } ?: "non disponibile"
}
