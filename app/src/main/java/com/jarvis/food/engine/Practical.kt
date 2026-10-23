package com.jarvis.food.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * "Riadatta ricetta" pratico: oltre alla matematica considera le unità che si usano davvero (una piadina intera, un uovo,
 * un vasetto di yogurt, mezza mozzarella), giudica se l'adattamento è sensato e, se non lo è, propone un contorno
 * o un'altra ricetta invece di forzare quantità assurde. Tutti i valori sono ricalcolati dal database sulle quantità finali.
 */
object Practical {
    /** Alimenti che si usano solo interi (non si taglia mezza piadina o mezza scatoletta di tonno). */
    private val WHOLE = setOf(
        "uova", "piadina", "pane_pita", "tortilla_mais", "panino_hamb", "cornetto", "tonno_nat", "tonno_olio", "yogurt", "greco0",
        "greco_intero", "yogurt_magro", "whey", "salsiccia", "falafel", "fette_bisc", "gallette", "biscotti", "prosciutto_cotto",
        "fesa_tacchino", "prosciutto_crudo", "bresaola", "salame", "salmone_aff", "kiwi", "mandarini", "albicocche", "prugne",
        "datteri", "fichi", "fichi_secchi", "prugne_secche", "lime"
    )
    /** Alimenti che si possono dividere a metà (mezza mela, mezza mozzarella, mezzo barattolo di ceci, mezza fetta di pane). */
    private val HALF = setOf("pane", "pane_int", "mela", "pera", "banana", "arancia", "pesca", "cachi", "mango", "avocado", "pompelmo",
        "mozzarella", "mozzarella_light", "ceci", "fagioli", "fagioli_rossi")

    /** Grammi di un'unità pratica (intera o mezza) oppure null se l'alimento si dosa liberamente in grammi. */
    fun unitStep(f: DbFood): Double? {
        val p = f.pieceGrams ?: return null
        return when (f.id) { in WHOLE -> p; in HALF -> p / 2; else -> null }
    }

    fun isWholeOnly(f: DbFood) = f.id in WHOLE
    fun isDiscrete(f: DbFood) = unitStep(f) != null

    /** Quantità a parole: "1 piadina (110 g)", "½ mozzarella (60 g)", "2 uova". */
    fun describe(f: DbFood, grams: Double): String {
        val p = f.pieceGrams ?: return formatAmount(f, grams)
        if (!isDiscrete(f)) return formatAmount(f, grams)
        val n = grams / p
        val name = f.pieceName ?: "unità"
        val q = when {
            abs(n - n.roundToInt()) < 0.05 -> n.roundToInt().toString()
            abs(n * 2 - (n * 2).roundToInt()) < 0.05 -> { val h = (n * 2).roundToInt(); if (h == 1) "½" else "${h / 2}${if (h % 2 == 1) "½" else ""}" }
            else -> return formatAmount(f, grams)
        }
        return if (f.id == "uova") formatAmount(f, grams) else "$q $name (${grams.roundToInt()} ${if (f.liquid) "ml" else "g"})"
    }

    enum class Level { GOOD, LIMITED, POOR }

    data class Side(val food: DbFood, val grams: Double, val label: String, val macro: Macro, val after: SolvedRecipe)

    data class Alternative(val solved: SolvedRecipe, val kcalErrPct: Double)

    data class Adaptation(
        val practical: SolvedRecipe,
        val exact: SolvedRecipe,
        val level: Level,
        val kcalErrPct: Double,
        val headline: String,
        val notes: List<String>,
        val side: Side?,
        val alternatives: List<Alternative>
    )

    fun kcalErrPct(r: SolvedRecipe) = if (r.target.kcal <= 0) 0.0 else (r.total.kcal - r.target.kcal) / r.target.kcal * 100

    private fun proteinGap(r: SolvedRecipe) = r.total.protein - r.target.protein

    fun level(r: SolvedRecipe): Level {
        val k = abs(kcalErrPct(r))
        val pTol = max(5.0, r.target.protein * 0.18)
        return when {
            k <= 7 && abs(proteinGap(r)) <= pTol -> Level.GOOD
            k <= 15 -> Level.LIMITED
            else -> Level.POOR
        }
    }

    /** Contorni semplici e realistici con porzioni "da persona". Ogni voce: id, mezzi pasti adatti. */
    private data class SideDef(val id: String, val meals: Set<String>, val maxUnits: Int)
    private val SIDES = listOf(
        SideDef("banana", setOf("colazione", "spuntino", "dessert", "pranzo"), 2), SideDef("mela", setOf("colazione", "spuntino", "dessert", "pranzo", "cena"), 2),
        SideDef("arancia", setOf("colazione", "spuntino", "dessert", "pranzo", "cena"), 2), SideDef("kiwi", setOf("colazione", "spuntino", "dessert", "pranzo", "cena"), 3),
        SideDef("pera", setOf("colazione", "spuntino", "dessert", "pranzo", "cena"), 2),
        SideDef("greco0", setOf("colazione", "spuntino", "dessert"), 2), SideDef("yogurt", setOf("colazione", "spuntino", "dessert"), 2),
        SideDef("pane", setOf("pranzo", "cena", "colazione"), 4), SideDef("pane_int", setOf("pranzo", "cena", "colazione"), 4),
        SideDef("noci", setOf("colazione", "spuntino", "pranzo", "cena"), 2), SideDef("mandorle", setOf("colazione", "spuntino", "pranzo", "cena"), 2),
        SideDef("parmigiano", setOf("pranzo", "cena"), 2), SideDef("bresaola", setOf("pranzo", "cena"), 4), SideDef("fesa_tacchino", setOf("pranzo", "cena"), 4),
        SideDef("mozzarella_light", setOf("pranzo", "cena"), 1), SideDef("fette_bisc", setOf("colazione", "spuntino"), 4),
        SideDef("gallette", setOf("colazione", "spuntino", "pranzo", "cena"), 3)
    )

    /** Dose di un contorno: porzioni intere per i frutti/fette, 15 g per la frutta secca, 20 g per il parmigiano. */
    private fun sideUnit(f: DbFood): Double = when (f.id) {
        "noci", "mandorle" -> 15.0
        "parmigiano" -> 20.0
        else -> f.pieceGrams ?: 50.0
    }

    /** Aggiunge un alimento come riga fissa (ruolo "x") per mostrare il piatto completo con il contorno. */
    fun withSide(r: SolvedRecipe, f: DbFood, grams: Double): SolvedRecipe =
        r.copy(items = r.items + SolvedIngredient(TemplateIngredient(f.id, grams, "x", null), f, grams))

    private fun bestSide(db: FoodDb, base: SolvedRecipe, meal: String, excludeTags: List<String>): Side? {
        val cur = Solver.fitScore(base)
        var best: Side? = null; var bestScore = cur
        val usedIds = base.items.map { it.food.id }.toSet()
        for (def in SIDES) {
            val f = db.food(def.id) ?: continue
            if (meal !in def.meals || f.id in usedIds || f.tags.any { it in excludeTags }) continue
            val unit = sideUnit(f)
            for (n in 1..def.maxUnits) {
                val g = unit * n
                val after = withSide(base, f, g)
                // Un contorno ha senso solo se non sfora il pasto: niente aggiunte che superano l'obiettivo oltre il 7%.
                if (kcalErrPct(after) > 7) break
                val sc = Solver.fitScore(after)
                if (sc < bestScore - 1e-9) { bestScore = sc; best = Side(f, g, describe(f, g), Macro.of(f, g), after) }
            }
        }
        // si accetta solo un miglioramento netto (almeno 30% sul punteggio di aderenza)
        return best?.takeIf { bestScore <= cur * 0.7 }
    }

    /** Analizza la ricetta per l'obiettivo dato: la versione con porzioni pratiche, il giudizio, un eventuale contorno e alternative migliori. */
    fun adapt(db: FoodDb, tpl: RecipeTemplate, target: Macro, excludeTags: List<String> = emptyList(),
              allowed: (RecipeTemplate) -> Boolean = { true }, withAlternatives: Boolean = true): Adaptation {
        val exact = Solver.solve(tpl, db, target)
        val prac = Solver.solve(tpl, db, target, practical = true)
        val lv = level(prac)
        val notes = ArrayList<String>()

        // dove le unità intere si discostano dal calcolo "puro": lo spiego, così si capisce cosa è stato fatto e perché
        for (p in prac.items) {
            if (p.ing.role == "x" || !isDiscrete(p.food)) continue
            val e = exact.items.first { it.ing === p.ing }.grams
            if (abs(e - p.grams) >= (unitStep(p.food) ?: 1.0) * 0.4 && p.food.id != "uova") {
                notes += "${p.displayName().replaceFirstChar { it.uppercase() }}: il calcolo darebbe ${formatAmount(p.food, e)}, " +
                    "ma uso ${describe(p.food, p.grams)} perché non ha senso dividere in frazioni scomode; ho riequilibrato gli altri ingredienti."
            }
        }
        // ingredienti spinti al limite di una proporzione sensata
        val capped = prac.items.filter { it.ing.role in setOf("c", "p", "f") && it.grams >= minOf(it.ing.g * 2.2, Solver.maxServing(it.food)) - 0.5 && it.ing.g > 0 && Solver.maxServing(it.food) > 0 }
        if (lv != Level.GOOD && capped.isNotEmpty())
            notes += "${capped.first().displayName().replaceFirstChar { it.uppercase() }} è già alla quantità massima sensata per questo piatto."

        var side: Side? = null
        var alts = emptyList<Alternative>()
        if (lv != Level.GOOD) {
            side = bestSide(db, prac, tpl.meal, excludeTags)
            if (side != null) notes += "Con ${side.label} ci si avvicina molto di più all'obiettivo senza stravolgere la ricetta."
        }
        val afterSide = side?.after ?: prac
        val finalLevel = level(afterSide)
        if (withAlternatives && finalLevel != Level.GOOD) {
            val cands = db.recipes.filter { it.meal == tpl.meal && it.id != tpl.id && allowed(it) }
            val here = Solver.fitScore(afterSide)
            alts = Planner.shortlist(db, cands, target, 70).map { Solver.solve(it, db, target, practical = true) }
                .filter { abs(kcalErrPct(it)) <= 7 && level(it) == Level.GOOD && Solver.fitScore(it) < here * 0.6 }
                .sortedBy { Solver.fitScore(it) }.take(3).map { Alternative(it, kcalErrPct(it)) }
        }
        val headline = when {
            lv == Level.GOOD -> "Adattamento riuscito: porzioni pratiche e obiettivo centrato."
            side != null && finalLevel == Level.GOOD -> "Con un piccolo contorno l'obiettivo è centrato."
            finalLevel == Level.LIMITED -> "Adattamento limitato: la ricetta non arriva esattamente all'obiettivo."
            else -> "Questa ricetta non si adatta bene all'obiettivo."
        }
        if (finalLevel == Level.POOR) notes += "Per arrivare ai numeri richiesti servirebbero quantità poco sensate (${if (kcalErrPct(afterSide) < 0) "troppo poche" else "troppe"} calorie: " +
            "${abs(afterSide.total.kcal - target.kcal).roundToInt()} kcal di scarto), quindi non le forzo."
        return Adaptation(prac, exact, lv, kcalErrPct(prac), headline, notes, side, alts)
    }
}
