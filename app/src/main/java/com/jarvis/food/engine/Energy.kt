package com.jarvis.food.engine

import com.jarvis.food.data.ActivityEntry
import com.jarvis.food.data.EnergySettings
import com.jarvis.food.data.Goals
import com.jarvis.food.data.HealthDay
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/** Fabbisogno di una giornata, con l'eventuale adattamento all'attività fisica. */
data class DayEnergy(
    val bmr: Double,
    /** Obiettivo base (fabbisogno atteso + obiettivo di peso, senza attività extra). */
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
    val explanation: List<String>,
    /** Fabbisogno di mantenimento atteso (dal profilo o calibrato sullo smartwatch). */
    val expectedTdee: Double = 0.0,
    /** true se il fabbisogno atteso è stato corretto con i giorni passati registrati dal watch. */
    val calibrated: Boolean = false
) {
    val macro: Macro get() = Macro(targetKcal, protein, carbs, fat)
}

object Energy {
    const val SEDENTARY = 1.2
    /** Quota di energia spesa per digerire il cibo (effetto termico): circa 10% del totale. */
    const val TEF = 0.10

    /** Giorni passati completi necessari per calibrare il fabbisogno sullo smartwatch. */
    const val CALIBRATION_MIN_DAYS = 5
    const val CALIBRATION_WINDOW = 14

    /**
     * Fabbisogno di mantenimento atteso per un giorno qualsiasi. Parte dal profilo (livello di attività o questionario);
     * con lo smartwatch, dopo alcuni giorni di dati, si avvicina a ciò che il watch misura davvero (entro -15% / +20%).
     * Così l'obiettivo non crolla la mattina presto, quando i passi della giornata sono ancora pochi.
     */
    fun expectedTdee(p: Profile, weight: Double, calibration: Double?): Double {
        val profile = Nutrition.tdee(p, weight)
        return if (calibration == null) profile else calibration.coerceIn(profile * 0.85, profile * 1.20)
    }

    /** Spesa misurata dal watch in una giornata completa: (basale + calorie attive conteggiate) / (1 - effetto termico). */
    fun measuredTdee(bmr: Double, activeKcal: Double, eatBackPct: Int): Double =
        (bmr + activeKcal * eatBackPct.coerceIn(0, 100) / 100.0) / (1 - TEF)

    /**
     * Media della spesa misurata nei giorni passati completi (watch indossato). Restituisce null se i giorni utili
     * sono meno di [CALIBRATION_MIN_DAYS].
     */
    fun calibration(p: Profile, weight: Double, s: EnergySettings, health: Map<String, HealthDay>, today: LocalDate): Double? {
        if (s.mode != 2) return null
        val bmr = Nutrition.bmr(p, weight)
        val vals = (1..CALIBRATION_WINDOW).mapNotNull { i ->
            val h = health[today.minusDays(i.toLong()).toString()] ?: return@mapNotNull null
            val active = h.activeKcal
            // Giorno utile: il watch era indossato (passi e calorie attive presenti).
            if (h.steps < 1500 || active == null || active <= 0) null else measuredTdee(bmr, active, s.eatBackPct)
        }
        return if (vals.size >= CALIBRATION_MIN_DAYS) vals.average() else null
    }

    fun baseKcal(p: Profile, g: Goals, s: EnergySettings, weight: Double, calibration: Double? = null): Double {
        if (!g.auto) return g.kcal.toDouble()
        val tdee = expectedTdee(p, weight, if (s.mode == 0) null else calibration)
        val raw = tdee + Nutrition.safeDelta(p, tdee)
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
        activities: List<ActivityEntry>, manualSteps: Int, dayFraction: Double = 1.0, calibration: Double? = null
    ): DayEnergy {
        val bmr = Nutrition.bmr(p, weight)
        val cal = if (s.mode == 0) null else calibration
        val expected = expectedTdee(p, weight, cal)
        val base = baseKcal(p, g, s, weight, cal)
        val (activeRaw, source) = activeFor(s, bmr, weight, health, activities, manualSteps, dayFraction)
        val k = if (s.mode == 0) 0.0 else s.eatBackPct.coerceIn(0, 100) / 100.0
        val active = activeRaw * k
        // Spesa misurata oggi = (basale + attività) / (1 - effetto termico). Il fabbisogno atteso include già l'attività
        // abituale del profilo: si aggiunge al budget solo quanto la spesa misurata lo SUPERA, mai di meno. Così al mattino,
        // con pochi passi, l'obiettivo resta quello del profilo invece di scendere.
        val measured = (bmr + active) / (1 - TEF)
        val extra = if (s.mode == 0) 0.0 else max(0.0, measured - expected)
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
        val bmrWhat = if (p.bodyFatPct != null && p.bodyFatPct in 5.0..55.0) "Katch-McArdle, massa grassa ${p.bodyFatPct.roundToInt()}%" else "Mifflin-St Jeor"
        ex += "Metabolismo basale ${bmr.roundToInt()} kcal ($bmrWhat, peso ${"%.1f".format(java.util.Locale.ITALY, weight)} kg)."
        val profileTdee = Nutrition.tdee(p, weight)
        ex += if (p.detailed) "Fabbisogno di mantenimento dal tuo questionario: ${profileTdee.roundToInt()} kcal (movimento quotidiano più allenamenti)."
        else "Fabbisogno di mantenimento dal livello di attività: ${profileTdee.roundToInt()} kcal."
        if (cal != null) ex += "Calibrato sui tuoi ultimi giorni con lo smartwatch: ${expected.roundToInt()} kcal."
        when (s.mode) {
            0 -> ex += "Livello di attività fisso del profilo: le attività registrate non cambiano l'obiettivo, così non vengono contate due volte."
            1 -> ex += "Attività inserite a mano e passi: ${activeRaw.roundToInt()} kcal, conteggiate al ${s.eatBackPct}%. Si aggiunge solo ciò che supera la tua attività abituale."
            else -> ex += "Calorie attive dello smartwatch: ${activeRaw.roundToInt()} kcal" +
                    (if (source == "stima") " (stimate da passi e allenamenti)" else "") + ", conteggiate al ${s.eatBackPct}%. " +
                    "Si aggiunge solo ciò che supera la tua attività abituale: al mattino l'obiettivo non scende."
        }
        if (extra > 0) ex += "Oggi hai ${extra.roundToInt()} kcal in più, tutte assegnate ai carboidrati; proteine e grassi restano invariati."
        val delta = Nutrition.safeDelta(p, expected)
        if (g.auto && delta != 0.0) ex += "Obiettivo peso: ${if (delta > 0) "+" else ""}${delta.roundToInt()} kcal al giorno rispetto al fabbisogno."
        return DayEnergy(bmr, base, target, extra, active, activeRaw, source, prot, carbs, fat, ex, expected, cal != null)
    }
}
