package com.jarvis.food.data

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

data class Totals(
    val kcal: Double = 0.0, val protein: Double = 0.0, val carbs: Double = 0.0, val fat: Double = 0.0,
    val fiber: Double = 0.0, val sugar: Double = 0.0, val salt: Double = 0.0, val satFat: Double = 0.0
) {
    operator fun plus(o: Totals) = Totals(
        kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat,
        fiber + o.fiber, sugar + o.sugar, salt + o.salt, satFat + o.satFat
    )
    fun scaled(k: Double) = Totals(kcal * k, protein * k, carbs * k, fat * k, fiber * k, sugar * k, salt * k, satFat * k)
}

object Nutrition {
    val ACTIVITY_LABELS = listOf(
        "Sedentario (lavoro seduto, poco movimento)",
        "Leggermente attivo (1-3 allenamenti/settimana)",
        "Attivo (3-5 allenamenti/settimana)",
        "Molto attivo (6-7 allenamenti/settimana)",
        "Estremamente attivo (lavoro fisico + sport)"
    )
    val ACTIVITY_FACTORS = listOf(1.2, 1.375, 1.55, 1.725, 1.9)

    fun of(food: Food, grams: Double): Totals {
        val k = grams / 100.0
        return Totals(
            food.kcal * k, food.protein * k, food.carbs * k, food.fat * k,
            (food.fiber ?: 0.0) * k, (food.sugar ?: 0.0) * k, (food.salt ?: 0.0) * k, (food.satFat ?: 0.0) * k
        )
    }

    fun sum(entries: List<DiaryEntry>) = entries.fold(Totals()) { acc, e -> acc + of(e.food, e.grams) }

    fun age(p: Profile) = max(14, LocalDate.now().year - p.birthYear)

    /** Metabolismo basale, formula di Mifflin-St Jeor. */
    fun bmr(p: Profile, weight: Double): Double =
        10 * weight + 6.25 * p.heightCm - 5 * age(p) + if (p.male) 5 else -161

    fun tdee(p: Profile, weight: Double) = bmr(p, weight) * ACTIVITY_FACTORS[p.activity.coerceIn(0, 4)]

    /** 1 kg di massa corporea ≈ 7700 kcal (approssimazione standard). */
    fun dailyDelta(p: Profile): Double = when (p.goal) {
        -1 -> -p.weeklyRate * 7700 / 7
        1 -> p.weeklyRate * 7700 / 7
        else -> 0.0
    }

    fun minKcal(p: Profile) = if (p.male) 1500 else 1200

    fun recommendedKcal(p: Profile, weight: Double): Int {
        val raw = tdee(p, weight) + dailyDelta(p)
        return max(minKcal(p).toDouble(), raw).roundToInt() / 10 * 10
    }

    /** Proteine: 1,6 g/kg per mantenere, 1,8 g/kg in definizione o in aumento di massa muscolare. */
    fun proteinPerKg(p: Profile) = when (p.goal) { -1 -> 1.8; 1 -> 1.8; else -> 1.6 }

    fun autoGoals(p: Profile, weight: Double, g: Goals, kcalOverride: Int? = null): Goals {
        val kcal = kcalOverride ?: recommendedKcal(p, weight)
        val protG = weight * proteinPerKg(p)
        val protPct = ((protG * 4 / kcal) * 100).roundToInt().coerceIn(15, 40)
        val fatPct = 27
        val carbPct = 100 - protPct - fatPct
        val water = ((weight * 33) / 250).roundToInt() * 250
        return g.copy(kcal = kcal, proteinPct = protPct, fatPct = fatPct, carbsPct = carbPct, waterMl = water.coerceIn(1500, 4000))
    }

    fun goalGrams(g: Goals) = Triple(
        g.kcal * g.carbsPct / 100.0 / 4.0,
        g.kcal * g.proteinPct / 100.0 / 4.0,
        g.kcal * g.fatPct / 100.0 / 9.0
    )

    /** Ripartizione indicativa delle calorie tra i pasti. */
    val MEAL_SPLIT = listOf(0.25, 0.35, 0.30, 0.10)

    fun activityKcal(met: Double, weight: Double, minutes: Int) = (met * weight * minutes / 60.0).roundToInt()

    /** Stima: circa 0,0005 kcal per passo per kg di peso corporeo. */
    fun stepsKcal(steps: Int, weight: Double) = (steps * 0.0005 * weight).roundToInt()

    fun bmi(weight: Double, heightCm: Double) = weight / ((heightCm / 100) * (heightCm / 100))

    fun bmiLabel(bmi: Double) = when {
        bmi < 18.5 -> "Sottopeso"
        bmi < 25 -> "Normopeso"
        bmi < 30 -> "Sovrappeso"
        else -> "Obesità"
    }

    fun recipeTotals(r: Recipe): Totals = r.ingredients.fold(Totals()) { a, i -> a + of(i.food, i.grams) }
    fun recipeGrams(r: Recipe) = r.ingredients.sumOf { it.grams }

    /** Trasforma una ricetta in un "alimento" (valori per 100 g) da aggiungere al diario. */
    fun recipeAsFood(r: Recipe): Food {
        val t = recipeTotals(r)
        val g = recipeGrams(r).coerceAtLeast(1.0)
        val k = 100.0 / g
        return Food(
            id = "recipe:${r.id}", name = r.name, kcal = t.kcal * k, protein = t.protein * k,
            carbs = t.carbs * k, fat = t.fat * k, fiber = t.fiber * k, sugar = t.sugar * k, salt = t.salt * k,
            servingName = "porzione", servingGrams = g / r.servings, source = "recipe", category = "Ricetta"
        )
    }
}

object FastingPlans {
    data class Plan(val id: String, val fastHours: Int, val title: String, val desc: String, val level: String)

    val plans = listOf(
        Plan("12:12", 12, "12:12", "12 ore di digiuno, 12 per mangiare. Ideale per iniziare.", "Principiante"),
        Plan("14:10", 14, "14:10", "Un passo in più: salta lo spuntino serale.", "Principiante"),
        Plan("16:8", 16, "16:8", "Il più diffuso: finestra di 8 ore per i pasti.", "Intermedio"),
        Plan("18:6", 18, "18:6", "Due pasti in 6 ore.", "Avanzato"),
        Plan("20:4", 20, "20:4", "Detto anche \"dieta del guerriero\".", "Avanzato"),
        Plan("23:1", 23, "OMAD 23:1", "Un solo pasto al giorno. Solo per esperti.", "Esperto"),
        Plan("36", 36, "36 ore", "Digiuno prolungato: valutalo con il medico.", "Esperto")
    )

    data class Phase(val fromH: Double, val title: String, val desc: String, val emoji: String)

    /** Fasi indicative: i tempi reali variano da persona a persona e in base all'ultimo pasto. */
    val phases = listOf(
        Phase(0.0, "Digestione", "Il corpo usa l'energia del pasto appena consumato; la glicemia sale e poi scende.", "🍽️"),
        Phase(4.0, "Calo della glicemia", "L'insulina diminuisce e il corpo inizia ad attingere alle riserve.", "📉"),
        Phase(8.0, "Uso del glicogeno", "Il fegato rilascia glucosio dalle riserve di glicogeno.", "⚡"),
        Phase(12.0, "Più grassi come energia", "Con il glicogeno in calo aumenta l'uso dei grassi come combustibile.", "🔥"),
        Phase(18.0, "Chetosi lieve", "Possibile aumento dei corpi chetonici. Bevi molta acqua.", "💧")
    )

    fun phaseAt(hours: Double) = phases.last { hours >= it.fromH }
}
