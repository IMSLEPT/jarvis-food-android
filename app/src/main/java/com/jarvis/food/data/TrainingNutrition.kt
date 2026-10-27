package com.jarvis.food.data

import com.jarvis.food.engine.DayEnergy
import java.time.LocalDate

/** Explicit daily targets in kcal and grams; independent of the legacy percentage goals. */
@kotlinx.serialization.Serializable
data class DailyTargets(val kcal: Int, val protein: Double, val carbs: Double, val fat: Double) {
    val valid: Boolean get() = kcal in 1..50000 && listOf(protein, carbs, fat).all { it.isFinite() && it in 0.0..10000.0 } && protein + carbs + fat > 0
    val macroKcal: Double get() = protein * 4 + carbs * 4 + fat * 9
}

@kotlinx.serialization.Serializable
data class TrainingNutrition(
    val enabled: Boolean = false,
    val on: DailyTargets? = null,
    val off: DailyTargets? = null,
    /** ISO weekdays: Monday = 1, Sunday = 7. */
    val trainingDays: List<Int> = emptyList(),
    /** Per-date exceptions; absence means follow the weekly calendar. */
    val overrides: Map<String, Boolean> = emptyMap()
) {
    val active: Boolean get() = enabled && on?.valid == true && off?.valid == true
    fun isTraining(date: LocalDate): Boolean = overrides[date.toString()] ?: (date.dayOfWeek.value in trainingDays)
    fun target(date: LocalDate): DailyTargets? = if (!active) null else if (isTraining(date)) on else off
    fun withOverride(date: LocalDate, training: Boolean?): TrainingNutrition = copy(overrides =
        if (training == null) overrides - date.toString() else overrides + (date.toString() to training))

    fun apply(base: DayEnergy, date: LocalDate): DayEnergy {
        val t = target(date) ?: return base
        val type = if (isTraining(date)) "ON · Allenamento" else "OFF · Riposo"
        return base.copy(baseKcal = t.kcal.toDouble(), targetKcal = t.kcal.toDouble(), extraKcal = 0.0,
            protein = t.protein, carbs = t.carbs, fat = t.fat, calibrated = false,
            explanation = listOf("$type: obiettivo impostato di ${t.kcal} kcal, P ${t.protein} g, C ${t.carbs} g, G ${t.fat} g.",
                "${if (date.toString() in overrides) "Eccezione per questa data" else "Tipo di giornata dal calendario settimanale"}.",
                "Le attività vengono registrate, senza aggiungere altre calorie agli obiettivi ON/OFF."))
    }
}

/** Replace atomically while retaining entry identity, position, meal, date and plan links. */
object DiaryEdits {
    fun replace(entries: List<DiaryEntry>, id: String, food: Food, grams: Double): List<DiaryEntry> {
        require(grams.isFinite() && grams > 0) { "Quantità non valida" }
        return entries.map { if (it.id == id) it.copy(food = food, grams = grams) else it }
    }
}
