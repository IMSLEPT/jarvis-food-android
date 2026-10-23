package com.jarvis.food.engine

import com.jarvis.food.data.PantryItem
import com.jarvis.food.data.ShoppingItem
import kotlin.math.ceil
import kotlin.math.max

object Shopping {
    val SHOP_ORDER = listOf("Frutta e verdura", "Pane, pasta e cereali", "Carne e pesce", "Latticini e uova",
        "Legumi e tofu", "Frutta secca e semi", "Dispensa", "Condimenti", "Bevande", "Altro")

    /** Alimenti che di solito sono già in casa: non finiscono in lista se non mancano davvero. */
    val STAPLES = setOf("sale", "pepe", "olio", "aceto_bals", "cannella")

    /**
     * Lista della spesa dai pasti pianificati: somma i grammi di ogni alimento, toglie quanto già c'è in dispensa
     * e arrotonda alle confezioni tipiche. Gli articoli aggiunti a mano e le spunte già fatte vengono mantenuti.
     */
    fun build(db: FoodDb, meals: List<SolvedRecipe>, pantry: List<PantryItem>, previous: List<ShoppingItem>, newId: () -> String): List<ShoppingItem> {
        val need = LinkedHashMap<String, Double>()
        for (m in meals) for (it in m.items) need[it.food.id] = (need[it.food.id] ?: 0.0) + it.grams
        val have = HashMap<String, Double>()
        val haveUnknown = HashSet<String>()
        for (p in pantry) {
            val id = p.foodId?.removePrefix("db:") ?: continue
            if (p.grams != null) have[id] = (have[id] ?: 0.0) + p.grams else haveUnknown += id
        }
        val out = mutableListOf<ShoppingItem>()
        for ((id, g) in need) {
            val f = db.food(id) ?: continue
            if (id in haveUnknown) continue
            val missing = g - (have[id] ?: 0.0)
            if (missing <= 0.5) continue
            if (id in STAPLES && (have.containsKey(id))) continue
            val pack = f.pack ?: 100.0
            val packs = max(1, ceil(missing / pack - 1e-9).toInt())
            val prevChecked = previous.firstOrNull { it.foodId == id }?.checked ?: false
            out += ShoppingItem(
                id = newId(), foodId = id, name = f.name, needGrams = Math.round(missing).toDouble(), packs = packs,
                packGrams = pack, shop = f.shop, checked = prevChecked,
                note = if (id in STAPLES) "di solito già in casa" else ""
            )
        }
        val manual = previous.filter { it.manual }
        return (out + manual).sortedWith(compareBy({ SHOP_ORDER.indexOf(it.shop).let { i -> if (i < 0) 99 else i } }, { it.name }))
    }

    fun describe(item: ShoppingItem, f: DbFood?): String {
        if (item.manual) return item.note
        val unit = if (f?.liquid == true) "ml" else "g"
        val need = if (f?.id == "uova") "${(item.needGrams / (f.pieceGrams ?: 50.0)).let { ceil(it).toInt() }} uova"
        else "${item.needGrams.toInt()} $unit"
        val pack = when {
            f?.id == "uova" -> "confezione da 6"
            item.packGrams >= 1000 && item.packGrams % 1000 == 0.0 -> "${(item.packGrams / 1000).toInt()} ${if (unit == "ml") "l" else "kg"}"
            else -> "${item.packGrams.toInt()} $unit"
        }
        return "Servono $need · ${item.packs} × $pack"
    }
}

data class PantrySuggestion(val solved: SolvedRecipe, val have: List<String>, val missing: List<DbFood>, val coverage: Double)

object Kitchen {
    /** Ingredienti da non considerare "mancanti" (aromi e condimenti di base). */
    private val BASIC = setOf("sale", "pepe", "olio", "aglio", "cipolla", "prezzemolo", "basilico", "cannella", "limone", "capperi", "salsa_soia", "aceto_bals")

    /**
     * Ricette realizzabili con ciò che c'è in dispensa, calcolate sull'obiettivo del pasto.
     * Ordinate per quota di ingredienti già disponibili e poi per aderenza a calorie e macro.
     */
    fun suggest(db: FoodDb, pantry: List<PantryItem>, meal: String?, target: (RecipeTemplate) -> Macro,
                allowed: (RecipeTemplate) -> Boolean, limit: Int = 12): List<PantrySuggestion> {
        val ids = pantry.mapNotNull { it.foodId?.removePrefix("db:") }.toSet()
        if (ids.isEmpty()) return emptyList()
        // Prima la copertura della dispensa (economica), poi si risolvono solo le migliori: con migliaia di ricette non le calcoliamo tutte.
        return db.recipes.asSequence()
            .filter { (meal == null || it.meal == meal) && allowed(it) }
            .mapNotNull { r ->
                val main = r.ings.filter { it.food !in BASIC }
                if (main.isEmpty()) return@mapNotNull null
                val have = main.filter { it.food in ids || equivalent(it.food, ids) }
                if (have.isEmpty()) return@mapNotNull null
                Triple(r, have, have.size.toDouble() / main.size)
            }
            .sortedByDescending { it.third }
            .take(limit * 6)
            .map { (r, have, cov) ->
                val main = r.ings.filter { it.food !in BASIC }
                val missing = main.filter { it !in have }.mapNotNull { db.food(it.food) }
                val solved = Solver.solve(r, db, target(r))
                PantrySuggestion(solved, have.mapNotNull { db.food(it.food)?.name }, missing, cov)
            }
            .sortedWith(compareByDescending<PantrySuggestion> { it.coverage }.thenBy { Solver.fitScore(it.solved) })
            .take(limit).toList()
    }

    /** Alimenti intercambiabili per la dispensa (es. riso e basmati, latte e latte senza lattosio). */
    private val EQUIV = listOf(
        setOf("riso", "basmati"), setOf("latte_ps", "latte_sl", "latte_intero", "latte_scremato"),
        setOf("greco0", "greco_intero"), setOf("ricotta", "ricotta_light"), setOf("mozzarella", "mozzarella_light"),
        setOf("pasta", "pasta_int"), setOf("pane", "pane_int"), setOf("spinaci", "spinaci_surg"),
        setOf("tonno_nat", "tonno_olio"), setOf("lenticchie", "lenticchie_secche"), setOf("ceci", "ceci_secchi"),
        setOf("parmigiano", "pecorino")
    )

    private fun equivalent(food: String, ids: Set<String>) = EQUIV.any { food in it && it.any { x -> x in ids } }
}
