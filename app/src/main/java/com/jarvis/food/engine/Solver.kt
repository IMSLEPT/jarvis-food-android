package com.jarvis.food.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Calorie e macronutrienti (grammi). */
data class Macro(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double) {
    operator fun plus(o: Macro) = Macro(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat)
    operator fun minus(o: Macro) = Macro(kcal - o.kcal, protein - o.protein, carbs - o.carbs, fat - o.fat)
    operator fun times(k: Double) = Macro(kcal * k, protein * k, carbs * k, fat * k)

    companion object {
        val ZERO = Macro(0.0, 0.0, 0.0, 0.0)
        fun of(f: DbFood, grams: Double): Macro {
            val k = grams / 100.0
            return Macro(f.kcal * k, f.protein * k, f.carbs * k, f.fat * k)
        }
    }
}

data class SolvedIngredient(val ing: TemplateIngredient, val food: DbFood, val grams: Double) {
    val macro: Macro get() = Macro.of(food, grams)
    /** Etichetta per testi e liste: "95 g di pasta", "2 uova", "250 ml di latte". */
    fun amountLabel(): String = formatAmount(food, grams)
    fun displayName(): String = ing.label ?: food.name.lowercase()
}

data class SolvedRecipe(
    val template: RecipeTemplate,
    val items: List<SolvedIngredient>,
    val target: Macro
) {
    val total: Macro get() = items.fold(Macro.ZERO) { a, i -> a + i.macro }
    val grams: Double get() = items.sumOf { it.grams }

    /** Somma di un micronutriente (chiave di [Micros]) sulla porzione calcolata. */
    fun micro(key: String): Double = items.sumOf { (it.food.micro[key] ?: 0.0) * it.grams / 100.0 }

    /** Scostamento percentuale delle calorie dall'obiettivo del pasto. */
    val kcalErrorPct: Double get() = if (target.kcal <= 0) 0.0 else (total.kcal - target.kcal) / target.kcal * 100

    /** Passaggi con le grammature inserite al posto dei segnaposto {chiave}. */
    fun steps(): List<String> = template.steps.map { s ->
        var out = s
        for (it in items) {
            val text = if (it.food.id == "uova") it.amountLabel() else it.amountLabel() + " di " + it.displayName()
            out = out.replace("{${it.ing.food}}", text)
        }
        out.replace(Regex("\\{[a-z0-9_]+\\}"), "")
    }

    /** Elenco breve degli ingredienti: "Pasta 95 g · petto di pollo 140 g · ..." */
    fun shortList(): String = items.filter { it.ing.role != "x" }.joinToString(" · ") {
        it.displayName().replaceFirstChar { c -> c.uppercase() } + " " + formatAmount(it.food, it.grams, short = true)
    }.let { s -> s.replaceFirstChar { it.uppercase() } }
}

fun formatAmount(f: DbFood, grams: Double, short: Boolean = false): String {
    if (f.id == "uova") {
        val n = (grams / (f.pieceGrams ?: 50.0)).roundToInt().coerceAtLeast(1)
        return if (n == 1) "1 uovo" else "$n uova"
    }
    val unit = if (f.liquid) "ml" else "g"
    val v = if (grams < 10 && grams % 1.0 != 0.0) String.format(java.util.Locale.ITALY, "%.1f", grams) else grams.roundToInt().toString()
    return "$v $unit"
}

/** Il piatto calcolato come unico alimento (valori per 100 g, micronutrienti inclusi) da salvare nel diario. */
fun SolvedRecipe.toFood(): com.jarvis.food.data.Food {
    val g = grams.coerceAtLeast(1.0)
    val k = 100.0 / g
    val t = total
    val keys = items.flatMap { it.food.micro.keys }.toSet()
    val micro = keys.associateWith { key -> micro(key) * k }
    val groups = items.filter { it.ing.role != "x" }.map { it.food.group }.toSet()
    val main = items.filter { it.ing.role == "p" }.maxByOrNull { it.macro.protein }?.food?.group ?: groups.firstOrNull() ?: ""
    return com.jarvis.food.data.Food(
        id = "recipe:${template.id}", name = template.name, kcal = t.kcal * k, protein = t.protein * k, carbs = t.carbs * k, fat = t.fat * k,
        fiber = micro["fiber"], sugar = micro["sugar"], satFat = micro["satFat"], salt = micro["sodium"]?.let { it * 2.5 / 1000 },
        servingName = "porzione calcolata", servingGrams = g, source = "recipe", category = "Ricetta",
        micro = micro, group = main,
        tags = items.flatMap { it.food.tags }.distinct() + groups.map { "grp:$it" }
    )
}

/**
 * Calcola le grammature degli ingredienti di una ricetta per avvicinarsi il più possibile
 * a calorie e macronutrienti obiettivo del pasto.
 *
 * Metodo: minimi quadrati pesati con vincoli (limiti per ruolo e porzioni massime realistiche),
 * risolti con discesa coordinata su una griglia di passi decrescenti (20, 10, 5 g; uova intere).
 * Un piccolo termine di regolarizzazione mantiene le proporzioni originali del piatto, così il risultato
 * resta un piatto vero e non una combinazione matematica innaturale. Deterministico e verificabile.
 */
object Solver {
    private const val W_KCAL = 3.0
    private const val W_PROT = 2.0
    private const val W_CARB = 1.0
    private const val W_FAT = 1.2
    private const val LAMBDA = 0.15

    fun maxServing(f: DbFood): Double = when {
        f.id == "uova" -> 200.0
        f.id == "albume" -> 250.0
        f.id in setOf("fette_bisc", "crackers", "gallette") -> 60.0
        f.id == "piadina" -> 220.0
        f.id == "whey" -> 40.0
        f.id == "brodo_veg" -> 600.0
        f.id == "latte_cocco" -> 150.0
        f.group == "olio" -> 20.0
        f.group in setOf("frutta_secca", "semi") -> 40.0
        f.group in setOf("dolci") -> 40.0
        f.group in setOf("condimenti", "grassi") -> if (f.kcal > 300) 30.0 else 40.0
        f.group == "erbe" || f.group == "spezie" -> 10.0
        f.group == "integratori" -> 40.0
        f.liquid -> 300.0
        f.group == "latticini" -> if (f.kcal > 300) 40.0 else 250.0
        f.group in setOf("cereali", "cereali_integrali") -> if (f.kcal > 300) 150.0 else 200.0
        f.group == "tuberi" -> 400.0
        f.group == "legumi" -> if (f.kcal > 250) 100.0 else 300.0
        f.group == "salumi" -> 80.0
        f.group.startsWith("carne") || f.group.startsWith("pesce") -> 250.0
        f.group == "frutta" -> if (f.kcal > 200) 50.0 else 300.0
        f.group.startsWith("verdura") -> 400.0
        else -> 300.0
    }

    private fun step(f: DbFood, role: String): Double = when {
        role == "x" -> 0.0
        f.id == "uova" -> f.pieceGrams ?: 50.0
        f.liquid -> 10.0
        else -> 5.0
    }

    fun solve(t: RecipeTemplate, db: FoodDb, target: Macro, practical: Boolean = false): SolvedRecipe {
        val ings = t.ings.mapNotNull { ing -> db.food(ing.food)?.let { ing to it } }
        val n = ings.size
        val base = DoubleArray(n) { ings[it].first.g }
        val k0 = ings.sumOf { Macro.of(it.second, it.first.g).kcal }
        val s = if (k0 > 0) (target.kcal / k0).coerceIn(0.4, 2.5) else 1.0

        val lo = DoubleArray(n); val hi = DoubleArray(n); val st = DoubleArray(n)
        for (i in 0 until n) {
            val (ing, f) = ings[i]
            val d = ing.g
            st[i] = if (practical && ing.role != "x") Practical.unitStep(f) ?: step(f, ing.role) else step(f, ing.role)
            when (ing.role) {
                "x" -> { lo[i] = d; hi[i] = d }
                "v" -> { lo[i] = d * 0.7; hi[i] = min(d * 1.6, maxServing(f)) }
                "f" -> { lo[i] = 0.0; hi[i] = min(d * 2.5, maxServing(f)) }
                else -> { lo[i] = d * 0.4; hi[i] = min(d * 2.2, maxServing(f)) }
            }
            if (f.id == "uova" && ing.role != "x") lo[i] = max(lo[i], f.pieceGrams ?: 50.0)
            if (practical && ing.role != "x" && Practical.isDiscrete(f)) lo[i] = max(lo[i], st[i])
            if (hi[i] < lo[i]) hi[i] = lo[i]
        }

        fun snap(i: Int, v: Double): Double {
            val c = v.coerceIn(lo[i], hi[i])
            return if (st[i] <= 0) c else (Math.round(c / st[i]) * st[i]).coerceIn(
                Math.ceil(lo[i] / st[i]) * st[i], max(Math.floor(hi[i] / st[i]) * st[i], Math.ceil(lo[i] / st[i]) * st[i]))
        }

        val g = DoubleArray(n) { i ->
            val d = base[i]
            snap(i, if (ings[i].first.role in setOf("c", "p", "f")) d * s else d)
        }

        val sc = doubleArrayOf(max(target.kcal, 1.0), max(target.protein, 5.0), max(target.carbs, 5.0), max(target.fat, 3.0))
        fun cost(arr: DoubleArray): Double {
            var kc = 0.0; var p = 0.0; var c = 0.0; var fa = 0.0; var reg = 0.0
            for (i in 0 until n) {
                val f = ings[i].second; val q = arr[i] / 100.0
                kc += f.kcal * q; p += f.protein * q; c += f.carbs * q; fa += f.fat * q
                val role = ings[i].first.role
                if (role != "x" && base[i] > 0) {
                    val ref = if (role in setOf("c", "p", "f")) base[i] * s else base[i]
                    val r = (arr[i] - ref) / ref
                    reg += r * r
                }
            }
            val e = W_KCAL * sq((kc - target.kcal) / sc[0]) + W_PROT * sq((p - target.protein) / sc[1]) +
                    W_CARB * sq((c - target.carbs) / sc[2]) + W_FAT * sq((fa - target.fat) / sc[3])
            return e + LAMBDA * reg
        }

        for (mul in intArrayOf(4, 2, 1)) {
            var improved = true; var iter = 0
            while (improved && iter < 300) {
                improved = false; iter++
                for (i in 0 until n) {
                    if (st[i] <= 0) continue
                    val delta = if (ings[i].second.id == "uova" || (practical && Practical.isDiscrete(ings[i].second))) st[i] else st[i] * mul
                    val cur = cost(g)
                    var bestV = g[i]; var best = cur
                    for (dir in intArrayOf(1, -1)) {
                        val old = g[i]
                        g[i] = snap(i, old + dir * delta)
                        val c = cost(g)
                        if (c < best - 1e-12) { best = c; bestV = g[i] }
                        g[i] = old
                    }
                    if (bestV != g[i]) { g[i] = bestV; improved = true }
                }
            }
        }
        return SolvedRecipe(t, ings.mapIndexed { i, (ing, f) -> SolvedIngredient(ing, f, g[i]) }, target)
    }

    /** Punteggio di aderenza (più basso = migliore), usato per ordinare le alternative. */
    fun fitScore(r: SolvedRecipe): Double {
        val t = r.target; val a = r.total
        return 3 * sq((a.kcal - t.kcal) / max(t.kcal, 1.0)) + 2 * sq((a.protein - t.protein) / max(t.protein, 5.0)) +
                sq((a.carbs - t.carbs) / max(t.carbs, 5.0)) + 1.2 * sq((a.fat - t.fat) / max(t.fat, 3.0))
    }

    private fun sq(x: Double) = x * x
    @Suppress("unused") private fun pct(a: Double, b: Double) = if (b == 0.0) 0.0 else abs(a - b) / b
}
