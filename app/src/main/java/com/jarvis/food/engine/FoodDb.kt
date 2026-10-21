package com.jarvis.food.engine

import com.jarvis.food.data.Food
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Alimento del database (USDA FoodData Central, valori per 100 g). */
@Serializable
data class DbFood(
    val id: String,
    val name: String,
    val shop: String,
    val group: String,
    val tags: List<String> = emptyList(),
    val fdc: Int,
    val usda: String,
    val liquid: Boolean = false,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val micro: Map<String, Double> = emptyMap(),
    val pieceName: String? = null,
    val pieceGrams: Double? = null,
    val pack: Double? = null,
    val syn: List<String> = emptyList(),
    val note: String? = null
) {
    /** Converte nel modello usato dal diario, con fibre, zuccheri, saturi e sale espliciti. */
    fun toFood(): Food = Food(
        id = "db:$id", name = name, kcal = kcal, protein = protein, carbs = carbs, fat = fat,
        fiber = micro["fiber"], sugar = micro["sugar"], satFat = micro["satFat"],
        salt = micro["sodium"]?.let { it * 2.5 / 1000.0 },
        servingName = pieceName, servingGrams = pieceGrams, source = "db", liquid = liquid,
        category = group, micro = micro, fdc = fdc, group = group, tags = tags
    )
}

@Serializable
data class FoodsFile(val source: String, val foods: List<DbFood>)

@Serializable
data class TemplateIngredient(
    val food: String,
    val g: Double,
    /** c carboidrati, p proteine, f grassi, v verdura/frutta, x fisso */
    val role: String,
    val label: String? = null
)

@Serializable
data class ImageCredit(val author: String = "", val license: String = "", val source: String = "", val title: String = "")

@Serializable
data class RecipeTemplate(
    val id: String,
    val name: String,
    /** colazione | pranzo | cena | spuntino */
    val meal: String,
    val minutes: Int,
    val ings: List<TemplateIngredient>,
    val steps: List<String>,
    val tip: String = "",
    val image: String? = null,
    val credit: ImageCredit? = null,
    /** Foto illustrative dei passaggi (stesso ordine di [steps]); null se mancante. */
    val stepImages: List<String?> = emptyList()
)

@Serializable
data class RecipesFile(val recipes: List<RecipeTemplate>)

/** Database alimenti e ricette caricato dagli asset. Logica pura: testabile sulla JVM. */
class FoodDb(foods: List<DbFood>, recipes: List<RecipeTemplate>) {
    val foods: List<DbFood> = foods
    val recipes: List<RecipeTemplate> = recipes
    private val byId = foods.associateBy { it.id }
    private val recipeById = recipes.associateBy { it.id }

    fun food(id: String): DbFood? = byId[id.removePrefix("db:")]
    fun recipe(id: String): RecipeTemplate? = recipeById[id]

    /** Tag di una ricetta (lattosio, glutine, pesce...) derivati dagli ingredienti. */
    fun recipeTags(r: RecipeTemplate): Set<String> = r.ings.flatMap { food(it.food)?.tags ?: emptyList() }.toSet()

    /** Gruppi alimentari presenti nella ricetta (pesce, legumi, carne_rossa...). */
    fun recipeGroups(r: RecipeTemplate): Set<String> = r.ings.filter { it.role != "x" }.mapNotNull { food(it.food)?.group }.toSet()

    fun search(query: String, limit: Int = 40): List<DbFood> {
        val q = normalize(query)
        if (q.isBlank()) return emptyList()
        val terms = q.split(" ").filter { it.isNotBlank() }
        return foods.mapNotNull { f ->
            val hay = normalize(f.name + " " + f.syn.joinToString(" ") + " " + f.group)
            if (!terms.all { hay.contains(it) }) return@mapNotNull null
            val n = normalize(f.name)
            val score = when {
                n == q -> 0
                n.startsWith(q) -> 1
                f.syn.any { normalize(it) == q } -> 1
                n.contains(q) -> 2
                else -> 3
            }
            score to f
        }.sortedWith(compareBy({ it.first }, { it.second.name.length })).take(limit).map { it.second }
    }

    /**
     * Riconosce un alimento da un testo libero (riga di scontrino, nome dato dall'AI).
     * Restituisce null se nessun sinonimo è abbastanza specifico.
     */
    fun match(text: String): DbFood? {
        val t = " " + normalize(text) + " "
        if (t.isBlank()) return null
        var best: DbFood? = null
        var bestLen = 0
        for (f in foods) {
            for (s in f.syn + f.name) {
                val ns = normalize(s)
                if (ns.length >= 3 && t.contains(" $ns ") || (ns.length >= 5 && t.contains(ns))) {
                    if (ns.length > bestLen) { best = f; bestLen = ns.length }
                }
            }
        }
        return best
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(foodsJson: String, recipesJson: String): FoodDb =
            FoodDb(json.decodeFromString(FoodsFile.serializer(), foodsJson).foods,
                json.decodeFromString(RecipesFile.serializer(), recipesJson).recipes)

        fun normalize(s: String): String = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
    }
}
