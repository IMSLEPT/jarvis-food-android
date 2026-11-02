package com.jarvis.food.engine

import com.jarvis.food.data.Ingredient
import com.jarvis.food.data.Recipe
import kotlin.math.abs

/** Amounts stay fixed until the user requests and accepts adjustment. */
object ManualMeals {
    fun complete(items: List<Ingredient>) = items.isNotEmpty() && items.all { i -> listOf("kcal", "protein", "carbs", "fat").all { NutritionScience.per100(i.food, it) != null } }
    fun total(items: List<Ingredient>) = items.fold(Macro.ZERO) { a, i -> a + Macro(i.food.kcal, i.food.protein, i.food.carbs, i.food.fat) * (i.grams / 100) }
    fun asSolved(recipe: Recipe, target: Macro, meal: String): SolvedRecipe {
        val items = recipe.ingredients.map { i ->
            val f = i.food
            val micros = (Micros.defs.map { it.key } + listOf("sugar", "satFat", "fiber", "sodium", "addedSugar", "freeSugar")).distinct().mapNotNull { key -> NutritionScience.per100(f, key)?.let { key to it } }.toMap()
            val db = DbFood(f.id.removePrefix("db:"), f.name, "Altro", f.group, f.tags, f.fdc ?: 0, "", f.liquid, f.kcal, f.protein, f.carbs, f.fat, micros, f.servingName, f.servingGrams)
            SolvedIngredient(TemplateIngredient(db.id, i.grams, "v", f.name), db, i.grams)
        }
        val template = RecipeTemplate(recipe.id, recipe.name, meal, recipe.minutes, items.map { it.ing }, recipe.steps.ifEmpty { listOf("Usa le quantità che hai scelto per ogni alimento.") })
        return SolvedRecipe(template, items, target)
    }
    fun error(a: Macro, t: Macro): Double = abs(a.kcal-t.kcal)/t.kcal.coerceAtLeast(100.0)*3 + abs(a.protein-t.protein)/t.protein.coerceAtLeast(10.0)*2 + abs(a.carbs-t.carbs)/t.carbs.coerceAtLeast(15.0) + abs(a.fat-t.fat)/t.fat.coerceAtLeast(8.0)
    fun adjust(items: List<Ingredient>, target: Macro): List<Ingredient> {
        if (!complete(items) || items.any { !it.grams.isFinite() || it.grams <= 0 }) return items
        var current = items
        var best = error(total(current), target)
        for (step in listOf(20.0, 5.0, 1.0)) repeat(8) {
            for (index in items.indices) for (direction in listOf(-1, 1)) {
                val base = items[index].grams
                val g = (current[index].grams + step * direction).coerceIn(base*.5, base*2)
                val candidate = current.toMutableList().also { it[index] = it[index].copy(grams=g) }
                val score = error(total(candidate), target)
                if (score < best-1e-9) { current=candidate; best=score }
            }
        }
        return current
    }
}
