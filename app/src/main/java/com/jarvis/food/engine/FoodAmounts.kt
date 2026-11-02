package com.jarvis.food.engine

import com.jarvis.food.data.Food
import com.jarvis.food.data.AppState
import java.util.Locale

/** Quantity metadata only: never changes a logged weight or the nutrition label. */
object FoodAmounts {
    fun mapFoods(s: AppState, transform: (Food) -> Food): AppState = s.copy(
        diary = s.diary.map { it.copy(food = transform(it.food)) },
        favorites = s.favorites.map(transform), customFoods = s.customFoods.map(transform),
        recipes = s.recipes.map { r -> r.copy(ingredients = r.ingredients.map { it.copy(food = transform(it.food)) }) }
    )
    fun migrate(s: AppState): AppState = mapFoods(s, ::normalize)
    private fun text(s: String) = s.lowercase(Locale.ROOT)
    private val solids = Regex("\\b(fette? biscottat[ae]|gallette?|pane|pancarre|biscotti?|cracker|pasta|riso|farina|cereali|cioccolato|formaggio|yogurt|burro|marmellata|confettura|crema|pollo|carne|pesce|uova?|frutta|verdura)\\b")
    private val drinks = Regex("\\b(acqua|bevanda|bevande|succo|succhi|latte|birra|vino|caffe|caffè|tè|the|te|brodo|aceto|olio)\\b")
    fun liquid(name: String, categories: List<String> = emptyList(), quantity: String = "", fallback: Boolean = false): Boolean {
        val n = text(name)
        val t = categories.map(::text)
        // OFF's broad 'plant-based-foods-and-beverages' includes bread and rice.
        // It must never identify every plant food as a beverage.
        val explicitDrink = t.any { tag ->
            tag.substringAfter(':') in setOf("beverages", "waters", "milks", "juices", "fruit-juices", "plant-milks", "vegetable-milks", "soft-drinks", "alcoholic-beverages") ||
                tag.endsWith("-waters") || tag.endsWith("-drinks") || tag.endsWith("-juices")
        }
        if (Regex("\\b(latte|milk) (in polvere|powder)\\b").containsMatchIn(n)) return false
        if (solids.containsMatchIn(n) && !Regex("^(latte|bevanda|succo|brodo|olio)\\b").containsMatchIn(n)) return false
        if (explicitDrink || drinks.containsMatchIn(n)) return true
        val q = text(quantity).replace(" ", "")
        if (Regex("\\d(?:ml|cl|dl|l)$").containsMatchIn(q)) return true
        if (Regex("\\d(?:kg|g)$").containsMatchIn(q)) return false
        return fallback
    }

    fun pieceName(food: Food): String = when {
        food.source == "recipe" -> "porzione"
        text(food.name).contains("fett") || text(food.name).contains("pane") || text(food.name).contains("pancarr") -> "fetta"
        text(food.name).contains("gallett") -> "galletta"
        text(food.name).contains("biscott") -> "biscotto"
        else -> food.servingName?.takeUnless { it.startsWith("porzione") } ?: "pezzo"
    }
    fun pieces(food: Food, count: Int): String {
        val one = pieceName(food)
        val plural = when (one) { "fetta" -> "fette"; "galletta" -> "gallette"; "biscotto" -> "biscotti"; "pezzo" -> "pezzi"; "porzione" -> "porzioni"; "uovo" -> "uova"; else -> one }
        return "$count ${if (count == 1) one else plural}"
    }

    fun normalize(food: Food): Food {
        var f = food.copy(liquid = liquid(food.name, fallback = food.liquid))
        if (f.portionSource == "Inserito da te") return f
        val n = text(f.name)
        // Exact brand/product match only. Different variants retain their own label.
        if (text(f.brand).contains("mulino bianco") && n.contains("fette biscottate integrali") &&
            listOf("mini", "senza glutine", "cioccol", "rigat").none { n.contains(it) }) {
            return f.copy(servingName = "fetta", servingGrams = 8.8,
                portionSource = "Mulino Bianco · etichetta per fetta, verificata il 02/11/2026",
                portionUrl = "https://www.mulinobianco.it/le-integrali")
        }
        val grams = f.servingGrams?.takeIf { it.isFinite() && it > 0 } ?: return f
        val label = f.servingName ?: return f
        val match = Regex("(\\d+(?:[.,]\\d+)?)\\s*(fette?|gallette?|biscotti?|pezzi?|slices?|pieces?|crackers?)\\b").find(text(label))
        if (match != null && label.startsWith("porzione")) {
            val count = match.groupValues[1].replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: return f
            f = f.copy(servingName = pieceName(f), servingGrams = grams / count, portionSource = "Open Food Facts · porzione dichiarata in etichetta")
        }
        return f
    }
}
