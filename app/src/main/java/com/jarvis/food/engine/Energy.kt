package com.jarvis.food.engine

import com.jarvis.food.data.ActivityEntry
import com.jarvis.food.data.EnergySettings
import com.jarvis.food.data.Goals
import com.jarvis.food.data.HealthDay
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
import kotlin.math.max
import kotlin.math.roundToInt

/** Fabbisogno di una giornata, con l'eventuale adattamento all'attività fisica. */
data class DayEnergy(
    val bmr: Double,
    /** Obiettivo base (senza attività extra). */
    val baseKcal: Double,
    /** Obiettivo del giorno (base + extra attività). */
    val targetKcal: Double,
    val extraKcal: Double,
    /** Calorie attive considerate (già ridotte del fattore prudenziale per lo smartwatch). */
    val activeKcal: Double,
    val activeRaw: Double,
    /** watch | stima | manuale | fisso */
    val source: String,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val explanation: List<String>
) {
    val macro: Macro get() = Macro(targetKcal, protein, carbs, fat)
}

object Energy {
    const val SEDENTARY = 1.2
    /** Quota di energia spesa per digerire il cibo (effetto termico): circa 10% del totale. */
    const val TEF = 0.10

    fun baseKcal(p: Profile, g: Goals, s: EnergySettings, weight: Double): Double {
        if (!g.auto) return g.kcal.toDouble()
        val bmr = Nutrition.bmr(p, weight)
        val factor = if (s.mode == 0) Nutrition.ACTIVITY_FACTORS[p.activity.coerceIn(0, 4)] else SEDENTARY
        val raw = bmr * factor + Nutrition.dailyDelta(p)
        return max(Nutrition.minKcal(p).toDouble(), raw)
    }

    /**
     * Calorie attive del giorno secondo la fonte disponibile, senza doppi conteggi:
     * - smartwatch: calorie attive del dispositivo; se mancano, calorie totali meno metabolismo basale;
     *   se mancano anche quelle, stima da passi + allenamenti registrati dal dispositivo.
     * - manuale: attività inserite a mano + passi.
     */
    fun activeFor(
        s: EnergySettings, bmr: Double, weight: Double, health: HealthDay?, activities: List<ActivityEntry>,
        manualSteps: Int, dayFraction: Double
    ): Pair<Double, String> = when (s.mode) {
        2 -> {
            val h = health
            when {
                h == null -> 0.0 to "watch"
                h.activeKcal != null && h.activeKcal > 0 -> h.activeKcal to "watch"
                h.totalKcal != null && h.totalKcal > 0 -> max(0.0, h.totalKcal - bmr * dayFraction) to "watch"
                else -> {
                    val sessions = h.sessions.sumOf { ws -> ws.kcal ?: 0.0 }
                    (Nutrition.stepsKcal(h.steps.toInt(), weight) + sessions) to "stima"
                }
            }
        }
        1 -> (activities.sumOf { it.kcal }.toDouble() + Nutrition.stepsKcal(manualSteps, weight)) to "manuale"
        else -> 0.0 to "fisso"
    }

    fun forDay(
        p: Profile, g: Goals, s: EnergySettings, weight: Double, health: HealthDay?,
        activities: List<ActivityEntry>, manualSteps: Int, dayFraction: Double = 1.0
    ): DayEnergy {
        val bmr = Nutrition.bmr(p, weight)
        val base = baseKcal(p, g, s, weight)
        val (activeRaw, source) = activeFor(s, bmr, weight, health, activities, manualSteps, dayFraction)
        val k = if (s.mode == 0) 0.0 else s.eatBackPct.coerceIn(0, 100) / 100.0
        val active = activeRaw * k
        // Spesa misurata = (basale + attività) / (1 - effetto termico). Il livello "sedentario" (1,2 × basale)
        // include già circa 0,09 × basale di movimento quotidiano: si aggiunge solo quanto lo supera.
        val measured = (bmr + active) / (1 - TEF)
        val extra = if (s.mode == 0) 0.0 else max(0.0, measured - bmr * SEDENTARY)
        val target = base + extra
        // Proteine e grassi restano quelli del piano; le calorie extra vanno ai carboidrati (carburante dell'attività).
        val prot: Double; val fat: Double; val carbs: Double
        if (g.auto) {
            prot = weight * Nutrition.proteinPerKg(p)
            fat = base * g.fatPct / 100.0 / 9.0
            carbs = max(0.0, (base - prot * 4 - fat * 9) / 4.0) + extra / 4.0
        } else {
            prot = base * g.proteinPct / 100.0 / 4.0
            fat = base * g.fatPct / 100.0 / 9.0
            carbs = base * g.carbsPct / 100.0 / 4.0 + extra / 4.0
        }

        val ex = mutableListOf<String>()
        ex += "Metabolismo basale ${bmr.roundToInt()} kcal (Mifflin-St Jeor, peso ${"%.1f".format(weight)} kg)."
        when (s.mode) {
            0 -> ex += "Livello di attività fisso del profilo: le attività registrate non cambiano l'obiettivo, così non vengono contate due volte."
            1 -> ex += "Base sedentaria (1,2 × basale) + attività registrate e passi: ${activeRaw.roundToInt()} kcal, conteggiate al ${s.eatBackPct}%."
            else -> ex += "Base sedentaria (1,2 × basale) + calorie attive dello smartwatch: ${activeRaw.roundToInt()} kcal" +
                    (if (source == "stima") " (stimate da passi e allenamenti)" else "") + ", conteggiate al ${s.eatBackPct}%."
        }
        if (extra > 0) ex += "Oggi hai ${extra.roundToInt()} kcal in più, tutte assegnate ai carboidrati; proteine e grassi restano invariati."
        val delta = Nutrition.dailyDelta(p)
        if (g.auto && delta != 0.0) ex += "Obiettivo peso: ${if (delta > 0) "+" else ""}${delta.roundToInt()} kcal al giorno rispetto al fabbisogno."
        return DayEnergy(bmr, base, target, extra, active, activeRaw, source, prot, carbs, fat, ex)
    }
}
