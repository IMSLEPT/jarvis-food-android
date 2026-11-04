package com.jarvis.food.engine

import com.jarvis.food.data.AppState
import com.jarvis.food.data.DiaryEntry
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * "Per te oggi" (video 4 novembre 2026): analisi della dieta registrata sull'ultimo giorno, sugli ultimi
 * 7 o 30 giorni. Cerca abitudini ripetute, gruppi alimentari mancanti, eccessi e scostamenti dai tuoi
 * obiettivi e produce consigli personali con i numeri che li giustificano. Funziona sempre offline;
 * l'eventuale commento dell'AI di Jarvis parte da [summary].
 */
object DietCoach {
    data class Context(val target: (LocalDate) -> Double, val protein: (LocalDate) -> Double, val waterGoal: Int)

    private val vegetables = setOf("verdura", "verdura_foglia", "tuberi")
    private val sweets = setOf("dolci")
    private val processedMeat = setOf("salumi")
    private val fish = setOf("pesce", "pesce_grasso")
    private val proteinGroups = setOf("carne_bianca", "carne_rossa", "pesce", "pesce_grasso", "uova", "legumi", "latticini", "salumi")

    private fun group(e: DiaryEntry): String {
        if (e.food.group.isNotBlank()) return e.food.group
        val n = FoodDb.normalize(e.food.name)
        return when {
            listOf("biscott", "cioccolat", "merendin", "torta", "gelato", "brioche", "cornetto", "crostat", "nutella", "caramell").any { it in n } -> "dolci"
            listOf("prosciutto", "salame", "mortadella", "wurstel", "speck", "bresaola", "pancetta").any { it in n } -> "salumi"
            listOf("insalata", "zucchin", "broccol", "spinac", "pomodor", "carot", "melanzan", "peperon", "verdur", "lattuga", "cavol").any { it in n } -> "verdura"
            listOf("mela", "banana", "arancia", "pera", "fragol", "kiwi", "frutta", "uva", "pesca", "albicocc", "mirtill").any { it in n } -> "frutta"
            listOf("ceci", "lenticch", "fagiol", "piselli", "legum", "hummus").any { it in n } -> "legumi"
            listOf("salmone", "tonno", "merluzzo", "orata", "branzino", "pesce", "gamber", "sgombro", "trota").any { it in n } -> "pesce"
            else -> ""
        }
    }

    private fun kcal(e: DiaryEntry) = e.food.kcal * e.grams / 100
    private fun protein(e: DiaryEntry) = e.food.protein * e.grams / 100

    fun windowLabel(days: Int) = when (days) { 1 -> "oggi"; 7 -> "negli ultimi 7 giorni"; else -> "negli ultimi 30 giorni" }

    /** Consigli ordinati per importanza; [days] = 1, 7 oppure 30. */
    fun analyze(s: AppState, date: LocalDate, days: Int, ctx: Context, review: NutritionReview? = null): List<DailyFoodTips.Tip> {
        val from = date.minusDays(days - 1L)
        val rows = s.diary.filter { e -> runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { it in from..date } == true && e.grams > 0 }
        val byDay = rows.groupBy { it.date }
        val logged = byDay.keys.size
        val tips = mutableListOf<Pair<Double, DailyFoodTips.Tip>>()
        val where = windowLabel(days)
        if (logged == 0) return emptyList()

        // 1) Alimenti ripetuti troppo spesso (abitudini, non singoli pasti).
        if (days > 1 && logged >= 3) {
            rows.groupBy { FoodDb.normalize(it.food.name) }.values
                .map { list -> list to list.map { it.date }.distinct().size }
                .filter { (list, d) -> d >= maxOf(3, (logged * 0.6).roundToInt()) && group(list.first()) !in setOf("bevande", "condimenti", "olio", "spezie", "erbe", "grassi") }
                .sortedByDescending { it.second }.take(1).forEach { (list, d) ->
                    val name = list.first().food.name
                    tips += (2.0 + d.toDouble() / logged) to DailyFoodTips.Tip("coach-repeat", "Varia: $name ricorre spesso",
                        "$name compare in $d giorni su $logged registrati $where. Alternalo con un alimento simile dello stesso gruppo per avere più nutrienti diversi.",
                        name, group(list.first()), "variety", true, true)
                }
        }
        // 2) Dolci e salumi frequenti.
        val sweetDays = byDay.count { (_, l) -> l.any { group(it) in sweets } }
        if ((days == 1 && byDay.values.flatten().count { group(it) in sweets } >= 2) || (days > 1 && sweetDays >= maxOf(3, (logged * 0.5).roundToInt())))
            tips += 2.6 to DailyFoodTips.Tip("coach-sweets", "Dolci frequenti",
                if (days == 1) "Oggi hai registrato più di un dolce. Per lo spuntino prova frutta intera o yogurt bianco."
                else "Hai registrato dolci in $sweetDays giorni su $logged $where. Riservali a qualche occasione e prova frutta o yogurt bianco.",
                "biscotti", "dolci", "sugars", true, true)
        val meatDays = byDay.count { (_, l) -> l.any { group(it) in processedMeat } }
        if (days > 1 && meatDays >= 3)
            tips += 2.4 to DailyFoodTips.Tip("coach-salumi", "Meno salumi", "Salumi in $meatDays giorni su $logged $where: alternali con legumi, pesce o carne bianca.",
                "prosciutto", "salumi", "sodium", true, true)
        // 3) Verdura e frutta.
        val vegDays = byDay.count { (_, l) -> l.any { group(it) in vegetables } }
        if (days == 1) {
            if (vegDays == 0) tips += 2.2 to DailyFoodTips.Tip("coach-veg", "Manca la verdura", "Oggi non hai ancora registrato verdura: aggiungine una porzione a pranzo o a cena.", "broccoli", "verdura", "variety", true)
        } else if (vegDays < logged * 0.6) tips += 2.3 to DailyFoodTips.Tip("coach-veg", "Più verdura nella settimana",
            "Verdura solo in $vegDays giorni su $logged $where. Inseriscila in almeno un pasto al giorno.", "broccoli", "verdura", "variety", true)
        val fruitDays = byDay.count { (_, l) -> l.any { group(it) == "frutta" } }
        if (days > 1 && fruitDays < logged * 0.5) tips += 1.8 to DailyFoodTips.Tip("coach-fruit", "Frutta intera", "Frutta registrata in $fruitDays giorni su $logged $where: prova un frutto come spuntino.", "mela", "frutta", "variety", true)
        // 4) Legumi e pesce nel periodo (rispettando esclusioni e stile).
        val excluded = s.dietPrefs.exclude.toSet()
        val style = s.dietPrefs.styleId
        if (days >= 7 && logged >= 4 && rows.none { group(it) == "legumi" } && "legumi" !in excluded && style != "paleo" && style != "carnivore")
            tips += 1.9 to DailyFoodTips.Tip("coach-legumes", "Nessun legume", "Nessun legume registrato $where: ceci, lenticchie o fagioli 2-3 volte a settimana aiutano fibre e varietà.", "ceci", "legumi", "variety", true)
        if (days >= 7 && logged >= 4 && rows.none { group(it) in fish } && "pesce" !in excluded && style !in setOf("vegetarian", "vegan"))
            tips += 1.7 to DailyFoodTips.Tip("coach-fish", "Aggiungi il pesce", "Nessun pesce registrato $where: considera 1-2 porzioni a settimana, se compatibili con le tue preferenze.", "salmone", "pesce", "variety", true)
        // 5) Energia e proteine rispetto ai tuoi obiettivi.
        val complete = byDay.filter { (d, l) -> l.map { it.meal }.distinct().size >= 3 && d < date.toString() || (days == 1 && d == date.toString()) }
        if (complete.isNotEmpty()) {
            val ratios = complete.map { (d, l) -> l.sumOf(::kcal) / ctx.target(LocalDate.parse(d)).coerceAtLeast(1.0) }
            val avg = ratios.average()
            val proteinRatio = complete.map { (d, l) -> l.sumOf(::protein) / ctx.protein(LocalDate.parse(d)).coerceAtLeast(1.0) }.average()
            if (avg > 1.12) tips += 2.8 to DailyFoodTips.Tip("coach-over", "Calorie sopra l’obiettivo",
                "${if (days == 1) "Oggi sei" else "In media sei"} al ${(avg * 100).roundToInt()}% del tuo obiettivo $where. Riduci le porzioni dei condimenti e degli spuntini.", "piatto", "", "context", true, true)
            else if (days > 1 && avg < 0.75) tips += 2.1 to DailyFoodTips.Tip("coach-under", "Registri meno del previsto",
                "Le giornate complete sono in media al ${(avg * 100).roundToInt()}% dell’obiettivo $where: controlla di registrare tutto o aggiungi uno spuntino.", "piatto", "", "context", true)
            if (days > 1 && proteinRatio < 0.8) {
                val source = rows.groupBy { group(it) }.filterKeys { it in proteinGroups }.maxByOrNull { it.value.size }?.key
                tips += 2.5 to DailyFoodTips.Tip("coach-protein", "Proteine sotto l’obiettivo",
                    "In media raggiungi il ${(proteinRatio * 100).roundToInt()}% delle proteine impostate $where. Aggiungi una fonte proteica a colazione o allo spuntino" +
                        (if (source != null) " e varia rispetto a quella più usata." else "."), "uova", "uova", "portions", true)
            }
        }
        // 6) Colazione saltata.
        if (days > 1 && logged >= 4) {
            val skipped = byDay.count { (_, l) -> l.none { it.meal == 0 } }
            if (skipped >= logged / 2) tips += 1.6 to DailyFoodTips.Tip("coach-breakfast", "Colazione spesso assente",
                "Colazione registrata in ${logged - skipped} giorni su $logged $where. Se ti aiuta, prova una colazione semplice del tuo piano.", "avena", "cereali_integrali", "variety", true)
        }
        // 7) Acqua.
        val waterDays = (0 until days).map { date.minusDays(it.toLong()).toString() }.filter { it in byDay }
        if (waterDays.isNotEmpty() && ctx.waterGoal > 0) {
            val water = waterDays.map { (s.water[it] ?: 0).toDouble() / ctx.waterGoal }.average()
            if (water < 0.6 && waterDays.any { (s.water[it] ?: 0) > 0 }) tips += 1.5 to DailyFoodTips.Tip("coach-water", "Bevi un po’ di più",
                "In media arrivi al ${(water * 100).roundToInt()}% dell’acqua obiettivo $where. Tieni una bottiglia a portata di mano.", "acqua", "bevande", "context", true)
        }
        // 8) Segnalazioni nutrizionali già validate (zuccheri liberi, sale, saturi, fibre).
        review?.let { NutritionScience.actionable(it) }.orEmpty().take(2).forEach { n ->
            tips += 2.7 to DailyFoodTips.Tip("personal:${n.id}", n.title, n.explanation + " " + n.action, "piatto", "", n.sourceId, true, n.attention)
        }
        return tips.sortedByDescending { it.first }.map { it.second }.distinctBy { it.id }
    }

    /** Dati compatti, in italiano, per il commento dell'AI. Nessun dato personale oltre ai pasti registrati. */
    fun summary(s: AppState, date: LocalDate, days: Int, ctx: Context, tips: List<DailyFoodTips.Tip>): String {
        val from = date.minusDays(days - 1L)
        val rows = s.diary.filter { e -> runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { it in from..date } == true && e.grams > 0 }
        val byDay = rows.groupBy { it.date }.toSortedMap()
        val foods = rows.groupBy { it.food.name }.mapValues { it.value.map { e -> e.date }.distinct().size }.entries.sortedByDescending { it.value }.take(15)
        return buildString {
            appendLine("Periodo: ${windowLabel(days)} (${byDay.size} giorni registrati).")
            byDay.entries.toList().takeLast(10).forEach { (d, l) ->
                appendLine("$d: ${l.sumOf(::kcal).roundToInt()} kcal su ${ctx.target(LocalDate.parse(d)).roundToInt()}, proteine ${l.sumOf(::protein).roundToInt()} g su ${ctx.protein(LocalDate.parse(d)).roundToInt()} g, pasti ${l.map { it.meal }.distinct().size}.")
            }
            appendLine("Alimenti più frequenti (giorni): " + foods.joinToString { "${it.key} ${it.value}" })
            appendLine("Segnali rilevati: " + tips.joinToString("; ") { it.title })
        }
    }
}
