package com.jarvis.food.engine

import com.jarvis.food.data.DietPrefs
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Una proposta per "Cosa mangio adesso?": ricetta già ricalcolata sui macro rimasti. */
data class NowSuggestion(
    val solved: SolvedRecipe,
    val info: RecipeInfo,
    /** 0..100: quanto la ricetta adattata si avvicina all'obiettivo (calorie e macro) senza stravolgere le quantità originali. */
    val compat: Int,
    val dKcal: Double, val dProtein: Double, val dCarbs: Double, val dFat: Double,
    /** Rapporto tra calorie adattate e calorie della ricetta base (1.0 = invariata). */
    val scale: Double,
    val fridge: Double
)

object WhatNow {
    /** Pasto della ricetta (colazione, pranzo, cena, spuntino) corrispondente a un pasto del diario. */
    fun keyForDiaryMeal(m: Int) = when (m) { 0 -> "colazione"; 1 -> "pranzo"; 2 -> "cena"; else -> "spuntino" }

    fun diaryMealForKey(k: String) = when (k) { "colazione" -> 0; "pranzo" -> 1; "cena" -> 2; else -> 3 }

    fun mealsFor(key: String) = when (key) {
        "colazione" -> setOf("colazione")
        "pranzo", "cena" -> setOf("pranzo", "cena")
        else -> setOf("spuntino", "dessert")
    }

    fun compat(r: SolvedRecipe, original: Macro): Int {
        val t = r.target; val a = r.total
        val dev = 0.5 * abs(a.kcal - t.kcal) / max(t.kcal, 50.0) + 0.2 * abs(a.protein - t.protein) / max(t.protein, 10.0) +
                0.15 * abs(a.carbs - t.carbs) / max(t.carbs, 15.0) + 0.15 * abs(a.fat - t.fat) / max(t.fat, 8.0)
        val scale = a.kcal / max(original.kcal, 1.0)
        val pen = if (scale > 1.6) (scale - 1.6) * 0.3 else if (scale < 0.6) (0.6 - scale) * 0.3 else 0.0
        return (100 * (1 - (dev + pen)).coerceIn(0.0, 1.0)).roundToInt()
    }

    /**
     * Proposte per il prossimo pasto: ricette del tipo giusto, compatibili con esclusioni e preferenze,
     * ricalcolate sul [target] e ordinate per compatibilità (a parità, con più ingredienti già in dispensa).
     */
    fun suggest(
        db: FoodDb, key: String, target: Macro, maxMinutes: Int, prefs: DietPrefs,
        pantryFoodIds: Set<String>, onlyFridge: Boolean, limit: Int = 6
    ): List<NowSuggestion> {
        val meals = mealsFor(key)
        val pool = db.recipes.filter {
            it.meal in meals && it.id !in prefs.disliked && db.recipeTags(it).none { t -> t in prefs.exclude } && it.minutes <= maxMinutes
        }
        // Pre-selezione economica: con migliaia di ricette si risolvono solo le 40 più vicine all'obiettivo.
        val near = pool.sortedBy { abs(RecipeMeta.info(db, it).kcal - target.kcal) / max(target.kcal, 50.0) }.take(40)
        return near.mapNotNull { t ->
            val fridge = RecipeSearch.fridgeCoverage(db, t, pantryFoodIds)
            if (onlyFridge && fridge < 0.6) return@mapNotNull null
            val info = RecipeMeta.info(db, t)
            val sr = Solver.solve(t, db, target)
            val a = sr.total
            NowSuggestion(sr, info, compat(sr, Macro(info.kcal, info.protein, info.carbs, info.fat)),
                a.kcal - target.kcal, a.protein - target.protein, a.carbs - target.carbs, a.fat - target.fat,
                a.kcal / max(info.kcal, 1.0), fridge)
        }.sortedWith(compareByDescending<NowSuggestion> { it.compat }.thenByDescending { it.fridge }).take(limit)
    }
}
