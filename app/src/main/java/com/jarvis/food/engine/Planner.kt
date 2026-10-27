package com.jarvis.food.engine

import com.jarvis.food.data.DayPlan
import com.jarvis.food.data.DietPrefs
import com.jarvis.food.data.PlanSlot
import java.time.LocalDate
import kotlin.math.max

/** Un pasto del piano con le grammature calcolate. */
data class PlannedMeal(val slotIndex: Int, val slotName: String, val diaryMeal: Int, val solved: SolvedRecipe)

object Planner {

    /** Obiettivo di ciascun pasto: quota della giornata secondo la ripartizione scelta. */
    fun slotTargets(day: Macro, prefs: DietPrefs): List<Macro> {
        val tot = prefs.slots.sumOf { it.share }.coerceAtLeast(1)
        return prefs.slots.map { day * (it.share.toDouble() / tot) }
    }

    fun allowed(db: FoodDb, r: RecipeTemplate, prefs: DietPrefs): Boolean =
        r.id !in prefs.disliked && db.recipeTags(r).none { it in prefs.exclude } && r.minutes <= max(prefs.maxMinutes, 5)

    fun candidates(db: FoodDb, meal: String, prefs: DietPrefs): List<RecipeTemplate> =
        db.recipes.filter { it.meal == meal && allowed(db, it, prefs) }.ifEmpty { db.recipes.filter { it.meal == meal } }

    /**
     * Con migliaia di ricette non si risolvono tutte: si tengono le più vicine all'obiettivo del pasto per calorie e macro
     * (il solver scala poi le quantità). L'ordine è deterministico, quindi stesso input = stessa selezione.
     */
    fun shortlist(db: FoodDb, cands: List<RecipeTemplate>, target: Macro, n: Int = 120): List<RecipeTemplate> {
        if (cands.size <= n) return cands
        fun dist(r: RecipeTemplate): Double {
            val i = RecipeMeta.info(db, r)
            val k = Math.abs(Math.log(i.kcal.coerceAtLeast(1.0) / target.kcal.coerceAtLeast(1.0)))
            val p = Math.abs(i.protein - target.protein) / max(target.protein, 5.0)
            val c = Math.abs(i.carbs - target.carbs) / max(target.carbs, 5.0)
            val f = Math.abs(i.fat - target.fat) / max(target.fat, 3.0)
            return 3.0 * k + 0.8 * p + 0.5 * c + 0.4 * f
        }
        return cands.sortedBy { dist(it) }.take(n)
    }

    /**
     * Calcola tutti i pasti di una giornata. I pasti vengono risolti in ordine e lo scarto di ciascuno
     * (es. una colazione con poche proteine) viene ridistribuito sui pasti successivi, così il totale
     * della giornata resta vicino all'obiettivo.
     */
    fun solveDay(db: FoodDb, plan: DayPlan, day: Macro, prefs: DietPrefs): List<PlannedMeal> {
        val targets = slotTargets(day, prefs)
        val shares = prefs.slots.map { it.share.toDouble() }
        var residual = Macro.ZERO
        val out = mutableListOf<PlannedMeal>()
        val ordered = plan.slots.filter { it.slot in prefs.slots.indices }.sortedBy { it.slot }
        for ((idx, ps) in ordered.withIndex()) {
            val rec = db.recipe(ps.recipeId) ?: continue
            val remainingShare = ordered.drop(idx).sumOf { shares[it.slot] }.coerceAtLeast(1.0)
            val base = targets[ps.slot]
            val adj = base + residual * (shares[ps.slot] / remainingShare)
            val t = Macro(
                max(adj.kcal, base.kcal * 0.6), max(adj.protein, base.protein * 0.5),
                max(adj.carbs, base.carbs * 0.4), max(adj.fat, base.fat * 0.4)
            )
            val solved = Solver.solve(rec, db, t)
            residual = residual + (t - solved.total)
            val def = prefs.slots[ps.slot]
            out += PlannedMeal(ps.slot, def.name, def.diaryMeal, solved)
        }
        return out
    }

    /** Alternative per un pasto, ordinate per quanto riescono a rispettare calorie e macro. */
    fun alternatives(db: FoodDb, meal: String, target: Macro, prefs: DietPrefs, exclude: Set<String> = emptySet()): List<SolvedRecipe> =
        shortlist(db, candidates(db, meal, prefs).filter { it.id !in exclude }, target, 90)
            .map { Solver.solve(it, db, target) }
            .sortedBy { Solver.fitScore(it) }

    /**
     * Genera il piano per [days] giorni a partire da [start]. Scelte deterministiche (stesso input = stesso piano)
     * con regole di varietà: nessuna ricetta ripetuta nello stesso pasto entro 3 giorni, rotazione delle fonti
     * proteiche (pesce, legumi, carne bianca, carne rossa, uova, latticini) e preferenza per i piatti che
     * rispettano meglio calorie e macro.
     */
    fun generate(
        db: FoodDb, prefs: DietPrefs, start: LocalDate, days: Int, dayTarget: Macro,
        existing: List<DayPlan> = emptyList(), seed: Long = start.toEpochDay(),
        dayTargetForDate: ((LocalDate) -> Macro)? = null
    ): List<DayPlan> {
        var targets = slotTargets(dayTarget, prefs)
        val history = existing.filter { LocalDate.parse(it.date) < start }.sortedBy { it.date }.takeLast(7).toMutableList()
        val fitCache = HashMap<String, Double>()
        fun fit(r: RecipeTemplate, slot: Int): Double = fitCache.getOrPut("${r.id}#$slot#${targets[slot]}") {
            Solver.fitScore(Solver.solve(r, db, targets[slot]))
        }
        var pools = prefs.slots.mapIndexed { si, def -> shortlist(db, candidates(db, def.meal, prefs), targets[si], 140) }
        val weekCount = HashMap<String, Int>()
        val out = mutableListOf<DayPlan>()
        for (d in 0 until days) {
            val date = start.plusDays(d.toLong())
            if (dayTargetForDate != null) {
                targets = slotTargets(dayTargetForDate(date), prefs)
                pools = prefs.slots.mapIndexed { si, def -> shortlist(db, candidates(db, def.meal, prefs), targets[si], 140) }
            }
            val dayGroups = mutableSetOf<String>()
            val slots = mutableListOf<PlanSlot>()
            for ((si, def) in prefs.slots.withIndex()) {
                val recent = (history + out).takeLast(3).flatMap { dp -> dp.slots.filter { it.slot == si }.map { it.recipeId } }.toSet()
                val cands = pools[si]
                val best = cands.minByOrNull { r ->
                    val groups = db.recipeGroups(r)
                    var score = fit(r, si) * 10
                    if (r.id in recent) score += 5.0
                    if ((history + out).flatMap { dp -> dp.slots.map { it.recipeId } }.contains(r.id)) score += 0.6
                    // Varietà proteica: favorisci pesce e legumi, limita la carne rossa, evita doppioni nello stesso giorno.
                    if (def.meal == "pranzo" || def.meal == "cena") {
                        if ("pesce" in groups || "pesce_grasso" in groups) score -= if ((weekCount["pesce"] ?: 0) < 3) 0.8 else 0.0
                        if ("legumi" in groups) score -= if ((weekCount["legumi"] ?: 0) < 3) 0.8 else 0.0
                        if ("carne_rossa" in groups) score += if ((weekCount["carne_rossa"] ?: 0) >= 2) 3.0 else 0.2
                        if (groups.any { it in dayGroups && it in MAIN_PROTEIN_GROUPS }) score += 2.0
                    }
                    score + jitter(seed, d, si, r.id)
                } ?: continue
                slots += PlanSlot(si, best.id)
                val g = db.recipeGroups(best)
                dayGroups += g
                for (k in listOf("pesce", "legumi", "carne_rossa")) {
                    if (g.contains(k) || (k == "pesce" && g.contains("pesce_grasso"))) weekCount[k] = (weekCount[k] ?: 0) + 1
                }
            }
            out += DayPlan(date.toString(), slots)
        }
        return out
    }

    val MAIN_PROTEIN_GROUPS = setOf("pesce", "pesce_grasso", "legumi", "carne_bianca", "carne_rossa", "uova", "salumi")

    /** Piccola variazione pseudo-casuale ma riproducibile, per non avere sempre lo stesso ordine a parità di punteggio. */
    private fun jitter(seed: Long, d: Int, s: Int, id: String): Double {
        var h = seed * 31 + d * 131 + s * 17 + id.hashCode()
        h = h xor (h ushr 33); h *= -0xae502812aa7333L; h = h xor (h ushr 33)
        return ((h ushr 11).toDouble() / (1L shl 53).toDouble()) * 0.9
    }
}
