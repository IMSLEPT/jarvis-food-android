package com.jarvis.food.engine

import java.time.LocalDate

/** Filtri combinabili della sezione Ricette. Le categorie della home sono semplicemente filtri già impostati. */
data class RecipeFilter(
    val query: String = "",
    val ingredients: String = "",
    val meals: Set<String> = emptySet(),
    val kcalMin: Int? = null, val kcalMax: Int? = null,
    val proteinMin: Int? = null, val carbsMax: Int? = null, val fatMax: Int? = null,
    val maxMinutes: Int? = null, val maxDifficulty: Int? = null,
    val diets: Set<String> = emptySet(), val goals: Set<String> = emptySet(),
    val cuisines: Set<String> = emptySet(), val methods: Set<String> = emptySet(),
    val excludeAllergens: Set<String> = emptySet(),
    val fridge: Boolean = false,
    val onlyFavs: Boolean = false,
    val sort: String = "rilevanza"
) {
    /** Numero di filtri attivi (per il badge del pulsante Filtri). */
    val activeCount: Int get() = listOf(
        ingredients.isNotBlank(), meals.isNotEmpty(), kcalMin != null || kcalMax != null, proteinMin != null, carbsMax != null,
        fatMax != null, maxMinutes != null, maxDifficulty != null, diets.isNotEmpty(), goals.isNotEmpty(), cuisines.isNotEmpty(),
        methods.isNotEmpty(), excludeAllergens.isNotEmpty(), fridge, onlyFavs
    ).count { it }
}

data class RecipeHit(val tpl: RecipeTemplate, val info: RecipeInfo, val fridge: Double)

object RecipeSearch {
    /** Alimenti sempre presenti in casa: non contano per "ho gli ingredienti". */
    private val STAPLES = setOf("sale", "pepe", "olio", "aceto_bals", "cannella", "origano", "paprika", "cumino", "curry", "curcuma")

    /** Quota degli ingredienti principali già in dispensa (0..1). */
    fun fridgeCoverage(db: FoodDb, t: RecipeTemplate, pantryFoodIds: Set<String>): Double {
        val need = t.ings.filter { it.role != "x" && it.food !in STAPLES }
        if (need.isEmpty()) return 1.0
        return need.count { it.food in pantryFoodIds }.toDouble() / need.size
    }

    fun missing(db: FoodDb, t: RecipeTemplate, pantryFoodIds: Set<String>): List<TemplateIngredient> =
        t.ings.filter { it.role != "x" && it.food !in STAPLES && it.food !in pantryFoodIds }

    private fun haystack(db: FoodDb, t: RecipeTemplate): String =
        FoodDb.normalize(t.name + " " + t.ings.joinToString(" ") { (it.label ?: "") + " " + (db.food(it.food)?.name ?: "") + " " + (db.food(it.food)?.syn?.joinToString(" ") ?: "") } + " " + t.cuisine)

    fun matches(db: FoodDb, t: RecipeTemplate, f: RecipeFilter, pantry: Set<String>, favs: Set<String>): Boolean {
        val i = RecipeMeta.info(db, t)
        if (f.meals.isNotEmpty() && t.meal !in f.meals) return false
        if (f.kcalMin != null && i.kcal < f.kcalMin) return false
        if (f.kcalMax != null && i.kcal >= f.kcalMax) return false
        if (f.proteinMin != null && i.protein < f.proteinMin) return false
        if (f.carbsMax != null && i.carbs > f.carbsMax) return false
        if (f.fatMax != null && i.fat > f.fatMax) return false
        if (f.maxMinutes != null && t.minutes > f.maxMinutes) return false
        if (f.maxDifficulty != null && t.difficulty > f.maxDifficulty) return false
        if (!i.diets.containsAll(f.diets)) return false
        if (!i.goals.containsAll(f.goals)) return false
        if (!i.methods.containsAll(f.methods)) return false
        if (f.cuisines.isNotEmpty() && t.cuisine !in f.cuisines) return false
        if (f.excludeAllergens.any { it in i.allergens || (it == "pesce" && db.recipeGroups(t).any { g -> g.startsWith("pesce") }) }) return false
        if (f.onlyFavs && t.id !in favs) return false
        if (f.fridge && fridgeCoverage(db, t, pantry) < 0.6) return false
        val hay by lazy { haystack(db, t) }
        val q = FoodDb.normalize(f.query)
        if (q.isNotBlank() && !q.split(" ").all { hay.contains(it) }) return false
        val ing = FoodDb.normalize(f.ingredients.replace(',', ' '))
        if (ing.isNotBlank() && !ing.split(" ").filter { it.length >= 3 }.all { hay.contains(it) }) return false
        return true
    }

    fun search(db: FoodDb, f: RecipeFilter, pantry: Set<String>, favs: Set<String>, disliked: Set<String>): List<RecipeHit> {
        val hits = db.recipes.filter { it.id !in disliked && matches(db, it, f, pantry, favs) }
            .map { RecipeHit(it, RecipeMeta.info(db, it), fridgeCoverage(db, it, pantry)) }
        return when (f.sort) {
            "kcal" -> hits.sortedBy { it.info.kcal }
            "proteine" -> hits.sortedByDescending { it.info.protein }
            "tempo" -> hits.sortedBy { it.tpl.minutes }
            else -> if (f.fridge) hits.sortedByDescending { it.fridge } else hits
        }
    }

    /** Ricetta del giorno: scelta stabile in base alla data, tra colazione, pranzo e cena compatibili con le preferenze. */
    fun recipeOfTheDay(db: FoodDb, date: LocalDate, excludeTags: List<String>, disliked: Set<String>): RecipeTemplate? {
        val pool = db.recipes.filter { it.meal in setOf("pranzo", "cena") && it.id !in disliked && db.recipeTags(it).none { t -> t in excludeTags } && it.image != null }
            .ifEmpty { db.recipes.filter { it.image != null } }
        if (pool.isEmpty()) return null
        return pool[Math.floorMod(date.toEpochDay().toInt() * 31 + 7, pool.size)]
    }
}

/** Ricetta alla grammatura originale dell'autore (senza adattamento a obiettivi). */
fun Solver.original(t: RecipeTemplate, db: FoodDb, target: Macro): SolvedRecipe =
    SolvedRecipe(t, t.ings.mapNotNull { ing -> db.food(ing.food)?.let { SolvedIngredient(ing, it, ing.g) } }, target)

/** Stessa ricetta con tutte le quantità moltiplicate per [k] (porzioni). */
fun SolvedRecipe.scaled(k: Double): SolvedRecipe = if (k == 1.0) this else copy(items = items.map { it.copy(grams = it.grams * k) })
