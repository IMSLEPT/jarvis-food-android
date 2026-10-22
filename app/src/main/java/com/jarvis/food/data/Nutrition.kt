package com.jarvis.food.data

import java.time.LocalDate
import java.util.Locale
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

    /** Età in anni: dalla data di nascita completa se disponibile, altrimenti dall'anno. */
    fun age(p: Profile, today: LocalDate = LocalDate.now()): Int {
        val bd = p.birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val years = if (bd != null) java.time.Period.between(bd, today).years else today.year - p.birthYear
        return max(14, years)
    }

    /** Metabolismo basale: Katch-McArdle se è nota la massa grassa, altrimenti Mifflin-St Jeor. */
    fun bmr(p: Profile, weight: Double): Double {
        val bf = p.bodyFatPct
        if (bf != null && bf in 5.0..55.0) return 370 + 21.6 * weight * (1 - bf / 100.0)
        return 10 * weight + 6.25 * p.heightCm - 5 * age(p) + if (p.male) 5 else -161
    }

    val JOB_LABELS = listOf("Seduto (ufficio, studio, guida)", "In piedi o in movimento (negozio, scuola, sanità)", "Fisico (cantiere, magazzino, agricoltura)")
    val STEPS_LABELS = listOf("Meno di 4.000", "4.000 – 7.000", "7.000 – 10.000", "10.000 – 14.000", "Oltre 14.000")
    val INTENSITY_LABELS = listOf("Leggero (riesco a parlare)", "Moderato (fiato corto)", "Intenso (al limite)")
    val TRAINING_LABELS = listOf("Misto", "Pesi / calisthenics", "Cardio (corsa, bici, nuoto)", "Sport di squadra")

    private val JOB_BONUS = listOf(0.0, 0.05, 0.12)
    private val STEPS_BONUS = listOf(0.0, 0.03, 0.06, 0.10, 0.14)

    /** Moltiplicatore del movimento quotidiano (lavoro e passi), escluso l'allenamento: da 1,2 (sedentario) in su. */
    fun neatFactor(p: Profile): Double {
        val j = JOB_BONUS[p.job.coerceIn(0, 2)]
        val st = STEPS_BONUS[p.stepsLevel.coerceIn(0, 4)]
        // Lavoro e passi si sovrappongono: conto il maggiore e metà del minore.
        return SEDENTARY_FACTOR + max(j, st) + 0.5 * minOf(j, st)
    }

    /** MET dell'allenamento per tipo (righe) e intensità (colonne). */
    private val MET = listOf(
        listOf(4.0, 6.0, 8.5),
        listOf(3.5, 5.0, 6.5),
        listOf(5.0, 7.5, 10.0),
        listOf(5.0, 7.0, 9.0)
    )

    /** Calorie medie al giorno degli allenamenti settimanali, al netto del metabolismo a riposo (MET − 1). */
    fun exerciseKcalPerDay(p: Profile, weight: Double): Double {
        if (p.workouts <= 0 || p.workoutMin <= 0) return 0.0
        val met = MET[p.trainingType.coerceIn(0, 3)][p.workoutIntensity.coerceIn(0, 2)]
        return p.workouts.coerceAtMost(14) * p.workoutMin * (met - 1) * weight / 60.0 / 7.0
    }

    const val SEDENTARY_FACTOR = 1.2

    /** Fabbisogno giornaliero di mantenimento: modello esteso se il questionario è compilato, altrimenti il vecchio livello. */
    fun tdee(p: Profile, weight: Double): Double =
        if (p.detailed) bmr(p, weight) * neatFactor(p) + exerciseKcalPerDay(p, weight)
        else bmr(p, weight) * ACTIVITY_FACTORS[p.activity.coerceIn(0, 4)]

    /** 1 kg di massa corporea ≈ 7700 kcal (approssimazione standard). */
    fun dailyDelta(p: Profile): Double = when {
        p.special == 1 -> 0.0
        p.goal == -1 -> -p.weeklyRate * 7700 / 7
        p.goal == 1 -> p.weeklyRate * 7700 / 7
        else -> 0.0
    }

    /** Variazione giornaliera con un tetto prudente: il deficit non supera il 25% del fabbisogno. */
    fun safeDelta(p: Profile, tdee: Double): Double {
        val d = dailyDelta(p)
        return if (d < 0) max(d, -0.25 * tdee) else d
    }

    fun minKcal(p: Profile) = if (p.male) 1500 else 1200

    fun recommendedKcal(p: Profile, weight: Double): Int {
        val t = tdee(p, weight)
        val raw = t + safeDelta(p, t)
        return max(minKcal(p).toDouble(), raw).roundToInt() / 10 * 10
    }

    /** Proteine consigliate (g per kg): in deficit si proteggono i muscoli, in aumento si sostiene la crescita. */
    fun baseProteinPerKg(p: Profile): Double {
        val trains = (p.detailed && p.workouts >= 2) || (!p.detailed && p.activity >= 1)
        return when (p.goal) { -1 -> 2.0; 1 -> 1.8; else -> if (trains) 1.6 else 1.2 }
    }

    fun proteinPerKg(p: Profile): Double = (baseProteinPerKg(p) + if (p.dietStyle == 1) 0.2 else 0.0).coerceAtMost(2.4)

    fun autoGoals(p: Profile, weight: Double, g: Goals, kcalOverride: Int? = null): Goals {
        val kcal = kcalOverride ?: recommendedKcal(p, weight)
        val protG = weight * proteinPerKg(p)
        val protPct = ((protG * 4 / kcal) * 100).roundToInt().coerceIn(15, 40)
        val carbPct: Int; val fatPct: Int
        if (p.dietStyle == 2) { carbPct = 25; fatPct = 100 - protPct - carbPct }
        else { fatPct = 27; carbPct = 100 - protPct - fatPct }
        val water = ((weight * 33) / 250).roundToInt() * 250
        return g.copy(kcal = kcal, proteinPct = protPct, fatPct = fatPct, carbsPct = carbPct, waterMl = water.coerceIn(1500, 4000))
    }

    // ---------------------------------------------------------------- Consiglio sui macronutrienti

    data class MacroPreset(val name: String, val carbs: Int, val protein: Int, val fat: Int)

    val PRESETS = listOf(
        MacroPreset("Bilanciato", 50, 20, 30),
        MacroPreset("Proteico", 40, 30, 30),
        MacroPreset("Low carb", 25, 30, 45)
    )

    data class MacroAdvice(
        /** Indice in [PRESETS] del preset consigliato. */
        val best: Int,
        /** Proteine consigliate (g/kg) e relativi grammi. */
        val targetPerKg: Double,
        /** Proteine (g/kg) che darebbe ciascun preset con le calorie indicate. */
        val perKg: List<Double>,
        val reason: String,
        /** Una frase per ogni preset: quando ha senso sceglierlo. */
        val notes: List<String>
    )

    /**
     * Quale ripartizione conviene con [kcal] calorie. Il criterio principale è l'apporto di proteine per kg
     * (dimagrire ≈ 2,0 g/kg, aumentare ≈ 1,8, mantenere ≈ 1,6 se ci si allena): a parità di calorie e proteine,
     * carboidrati e grassi cambiano poco il risultato sul peso, quindi il low carb si consiglia solo se lo preferisci
     * o se non ti alleni con intensità.
     */
    fun macroAdvice(p: Profile, weight: Double, kcal: Double): MacroAdvice {
        val target = baseProteinPerKg(p)
        val per = PRESETS.map { kcal * it.protein / 100.0 / 4.0 / weight }
        val heavyTraining = p.detailed && p.workouts >= 4 && p.workoutIntensity >= 1
        val best = when {
            p.dietStyle == 2 && !heavyTraining -> 2
            per[0] >= target * 0.95 -> 0
            else -> 1
        }
        val goalTxt = when (p.goal) { -1 -> "per dimagrire senza perdere muscolo"; 1 -> "per costruire massa"; else -> "per mantenere il peso" }
        val reason = StringBuilder("Servono circa ${"%.1f".format(Locale.ITALY, target)} g/kg di proteine $goalTxt (≈ ${(target * weight).roundToInt()} g). ")
        when (best) {
            0 -> reason.append("Con ${kcal.roundToInt()} kcal il Bilanciato ne dà ${"%.1f".format(Locale.ITALY, per[0])} g/kg: basta, ed è il più facile da seguire.")
            1 -> reason.append("Il Bilanciato ne darebbe solo ${"%.1f".format(Locale.ITALY, per[0])} g/kg, il Proteico ${"%.1f".format(Locale.ITALY, per[1])}: è la scelta che copre il fabbisogno.")
            else -> reason.append("Il Low carb dà ${"%.1f".format(Locale.ITALY, per[2])} g/kg di proteine e hai scelto di ridurre i carboidrati: va bene finché ti senti in energia.")
        }
        val notes = listOf(
            "Adatto se ti alleni 2-4 volte a settimana e vuoi la scelta più sostenibile.",
            "Utile con le calorie ridotte o se vuoi più sazietà e muscoli protetti.",
            if (heavyTraining) "Con allenamenti frequenti e intensi può ridurre le prestazioni: meglio meno estremo."
            else "Efficace quanto gli altri a parità di calorie e proteine: sceglilo solo se ti piace e riesci a seguirlo."
        )
        return MacroAdvice(best, target, per, reason.toString(), notes)
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
