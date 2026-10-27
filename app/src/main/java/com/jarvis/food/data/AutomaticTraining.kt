package com.jarvis.food.data

import kotlin.math.max
import kotlin.math.roundToInt

@kotlinx.serialization.Serializable
data class TrainingLoad(val minutes: Int = 45, val type: Int = 1, val intensity: Int = 1) {
    val valid: Boolean get() = minutes in 5..300 && type in 0..3 && intensity in 0..2
}

data class TrainingEstimate(
    val on: DailyTargets, val off: DailyTargets, val bmr: Double, val movementFactor: Double,
    val sessionKcal: Double, val met: Double, val dailyAdjustment: Double, val sessions: Int,
    val weeklyAverage: Double, val proteinPerKg: Double, val load: TrainingLoad
)

/** Transparent estimate, calculated locally. Exercise is added only on ON days, never twice.
 * MET categories: 2024 Adult Compendium (conditioning, running, sports).
 * Protein: ISSN 2017 protein and exercise, doi:10.1186/s12970-017-0177-8.
 * Movement multipliers and weight-goal adjustment retain the app's existing estimation model.
 */
object AutomaticTraining {
    fun eligible(p: Profile, weight: Double): Boolean = weight.isFinite() && weight in 30.0..300.0 &&
        p.heightCm.isFinite() && p.heightCm in 100.0..230.0 && Nutrition.age(p) >= 18 && p.special == 0 &&
        p.weeklyRate.isFinite() && p.weeklyRate in 0.0..2.0

    fun load(p: Profile, n: TrainingNutrition) = n.workout ?: TrainingLoad(p.workoutMin, p.trainingType, p.workoutIntensity)

    fun calculate(p: Profile, weight: Double, n: TrainingNutrition): TrainingEstimate? {
        val load = load(p, n)
        if (!eligible(p, weight) || !load.valid) return null
        val sessions = n.trainingDays.filter { it in 1..7 }.distinct().size
        // Representative session-average MET, including normal rests. Not a measured expenditure.
        val met = listOf(listOf(3.5, 5.0, 7.5), listOf(3.5, 5.0, 6.0),
            listOf(5.0, 7.5, 10.5), listOf(4.0, 6.0, 8.0))[load.type][load.intensity]
        val session = max(0.0, met - 1.0) * weight * load.minutes / 60.0
        val bmr = Nutrition.bmr(p, weight)
        val factor = Nutrition.neatFactor(p)
        val rest = bmr * factor
        val mean = rest + session * sessions / 7.0
        val delta = Nutrition.safeDelta(p, mean).coerceAtMost(mean * 0.15)
        val sportProfile = p.copy(detailed = true, workouts = sessions)
        val proteinKg = Nutrition.proteinPerKg(sportProfile)
        val protein = weight * proteinKg
        fun calories(exercise: Double) = (max(Nutrition.minKcal(p).toDouble(), rest + exercise + delta) / 10).roundToInt() * 10
        // Same protein target on both day types, including when the energy safety limit applies.
        val pGrams = minOf(protein, calories(0.0) * 0.4 / 4)
        fun targets(exercise: Double): DailyTargets {
            val kcal = calories(exercise)
            val desiredFat = if (p.dietStyle == 2) max(weight * 0.8, (kcal * 0.75 - pGrams * 4) / 9)
                else max(weight * 0.8, kcal * 0.27 / 9)
            val fat = desiredFat.coerceAtMost((kcal - pGrams * 4) * 0.9 / 9)
            val carbs = max(0.0, (kcal - pGrams * 4 - fat * 9) / 4)
            return DailyTargets(kcal, pGrams, carbs, fat)
        }
        val off = targets(0.0)
        val on = targets(session)
        return TrainingEstimate(on, off, bmr, factor, session, met, delta, sessions,
            (on.kcal * sessions + off.kcal * (7 - sessions)) / 7.0, pGrams / weight, load)
    }
}
