package com.jarvis.food.engine

/** Una categoria della scheda Ricette: filtro già impostato, emoji, etichetta ben leggibile e numero reale di ricette. */
data class RecipeCat(val emoji: String, val label: String, val filter: RecipeFilter, val count: Int, val listTitle: String)

data class RecipeCatGroup(val id: String, val title: String, val cats: List<RecipeCat>)

/**
 * Categorie in stile Yazio. I conteggi richiedono di esaminare tutte le ricette: si calcolano UNA volta, fuori dal thread
 * principale (all'avvio dell'app), e poi restano in memoria. Così aprire la scheda Ricette è immediato.
 */
object RecipeCats {
    private val MEAL_EMOJI = mapOf("colazione" to "🥐", "pranzo" to "🍝", "cena" to "🍽️", "spuntino" to "🍎", "dessert" to "🍰")
    private val KCAL_EMOJI = listOf("🥬", "🥗", "🍳", "🍲", "🍛", "🍝", "🥩", "🍔")
    /** Fasce di tempo totale (preparazione + cottura + attese): ogni categoria mostra TUTTE le ricette fino a quel tempo. */
    val TIME_STEPS = listOf(10 to "⚡", 15 to "🥪", 20 to "🍳", 30 to "🍝", 45 to "🥘", 60 to "🍲")
    private val GOAL_EMOJI = mapOf(
        "alto_proteico" to "💪", "poche_cal" to "🍃", "pochi_grassi" to "💧", "low_carb" to "🥦", "fibre" to "🌾",
        "bilanciato" to "⚖️", "ipertrofia" to "🏋️", "ricomposizione" to "🔄", "dimagrimento" to "🎯"
    )
    private val DIET_EMOJI = mapOf(
        "mediterranea" to "🍅", "vegetariana" to "🥕", "vegana" to "🌱", "keto" to "🥓", "senza_lattosio" to "🥛",
        "senza_glutine" to "🌾", "senza_zuccheri" to "🍬", "pescetariana" to "🐟"
    )
    private val METHOD_EMOJI = mapOf(
        "veloce" to "⚡", "pochi_ingredienti" to "✨", "economico" to "💶", "meal_prep" to "🍱", "pranzo_ufficio" to "💼",
        "freddo" to "❄️", "padella" to "🍳", "forno" to "🔥", "friggitrice" to "💨", "senza_cottura" to "🥗", "anticipo" to "⏰"
    )
    private val CUISINE_EMOJI = mapOf(
        "italiana" to "🇮🇹", "messicana" to "🌮", "asiatica" to "🥢", "americana" to "🍔", "giapponese" to "🍣",
        "indiana" to "🍛", "mediterranea" to "🌊", "altre" to "🌍"
    )

    /** Etichetta chiara per le fasce di calorie ("< 100" → "Sotto 100 kcal"). */
    fun kcalLabel(key: String): String = when {
        key.startsWith("<") -> "Sotto ${key.drop(1)} kcal"
        key.startsWith(">") -> "Oltre ${key.drop(1)} kcal"
        else -> key.replace("-", "–") + " kcal"
    }

    @Volatile private var cache: Pair<Int, List<RecipeCatGroup>>? = null

    /** Risultato già calcolato (null se non ancora pronto). */
    fun peek(db: FoodDb): List<RecipeCatGroup>? = cache?.takeIf { it.first == db.recipes.size }?.second

    fun get(db: FoodDb): List<RecipeCatGroup> {
        peek(db)?.let { return it }
        val built = build(db)
        cache = db.recipes.size to built
        return built
    }

    private fun n(db: FoodDb, f: RecipeFilter) = db.recipes.count { RecipeSearch.matches(db, it, f, emptySet(), emptySet()) }

    private fun build(db: FoodDb): List<RecipeCatGroup> {
        fun cat(emoji: String, label: String, f: RecipeFilter, title: String = label) = RecipeCat(emoji, label, f, n(db, f), title)
        return listOf(
            RecipeCatGroup("pasto", "Per pasto", RecipeMeta.MEALS.map { (k, l) -> cat(MEAL_EMOJI[k] ?: "🍽️", l, RecipeFilter(meals = setOf(k))) }),
            RecipeCatGroup("tempo", "Quanto tempo hai?", TIME_STEPS.map { (m, e) ->
                cat(e, "Entro $m min", RecipeFilter(maxMinutes = m, sort = "tempo"), "Pronte in $m minuti o meno")
            }),
            RecipeCatGroup("kcal", "Per calorie (a porzione)", RecipeMeta.KCAL_BUCKETS.mapIndexed { i, (k, r) ->
                val f = RecipeFilter(kcalMin = r.first.toInt().takeIf { it > 0 }, kcalMax = r.second.toInt().takeIf { it < 99999 })
                cat(KCAL_EMOJI.getOrElse(i) { "🍽️" }, kcalLabel(k), f, "Ricette ${kcalLabel(k).replaceFirstChar { it.lowercase() }}")
            }),
            RecipeCatGroup("obiettivo", "Obiettivo nutrizionale", RecipeMeta.GOALS.map { (k, l) -> cat(GOAL_EMOJI[k] ?: "🎯", l, RecipeFilter(goals = setOf(k))) }),
            RecipeCatGroup("dieta", "Tipo di dieta", RecipeMeta.DIETS.map { (k, l) -> cat(DIET_EMOJI[k] ?: "🥗", l, RecipeFilter(diets = setOf(k)), "Dieta: $l") }),
            RecipeCatGroup("metodo", "Metodo e tempo", RecipeMeta.METHODS.map { (k, l) -> cat(METHOD_EMOJI[k] ?: "🍳", l, RecipeFilter(methods = setOf(k))) }),
            RecipeCatGroup("cucina", "Cucine del mondo", RecipeMeta.CUISINES.map { (k, l) -> cat(CUISINE_EMOJI[k] ?: "🌍", l, RecipeFilter(cuisines = setOf(k)), "Cucina: $l") })
        ).map { g -> g.copy(cats = g.cats.filter { it.count > 0 }) }.filter { it.cats.isNotEmpty() }
    }
}
