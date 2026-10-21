package com.jarvis.food.engine

import com.jarvis.food.data.DiaryEntry
import com.jarvis.food.data.Profile
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Valori di riferimento giornalieri per adulti (EFSA, Dietary Reference Values, tabelle riassuntive 2017/2019).
 * PRI = assunzione raccomandata per la popolazione; AI = assunzione adeguata; LIM = limite da non superare.
 */
data class MicroDef(
    val key: String,
    val name: String,
    val unit: String,
    val men: Double,
    val women: Double,
    val kind: String,          // PRI | AI | LIM
    val role: String,          // a cosa serve, in breve
    val perMJ: Boolean = false // tiamina e niacina: riferimento per MJ di energia
)

object Micros {
    val defs = listOf(
        MicroDef("fiber", "Fibre", "g", 25.0, 25.0, "AI", "regolarità intestinale e sazietà"),
        MicroDef("vitC", "Vitamina C", "mg", 110.0, 95.0, "PRI", "difese immunitarie, assorbimento del ferro"),
        MicroDef("vitD", "Vitamina D", "µg", 15.0, 15.0, "AI", "ossa e muscoli; si forma soprattutto con il sole"),
        MicroDef("vitA", "Vitamina A", "µg", 750.0, 650.0, "PRI", "vista, pelle, difese immunitarie"),
        MicroDef("vitE", "Vitamina E", "mg", 13.0, 11.0, "AI", "antiossidante"),
        MicroDef("vitK", "Vitamina K", "µg", 70.0, 70.0, "AI", "coagulazione e ossa"),
        MicroDef("thiamin", "Vitamina B1", "mg", 0.1, 0.1, "PRI", "uso dei carboidrati per produrre energia", perMJ = true),
        MicroDef("riboflavin", "Vitamina B2", "mg", 1.6, 1.6, "PRI", "metabolismo energetico"),
        MicroDef("niacin", "Vitamina B3", "mg", 1.6, 1.6, "PRI", "metabolismo energetico", perMJ = true),
        MicroDef("vitB6", "Vitamina B6", "mg", 1.7, 1.6, "PRI", "metabolismo delle proteine"),
        MicroDef("folate", "Folati", "µg", 330.0, 330.0, "PRI", "formazione dei globuli rossi"),
        MicroDef("vitB12", "Vitamina B12", "µg", 4.0, 4.0, "AI", "sistema nervoso e globuli rossi"),
        MicroDef("calcium", "Calcio", "mg", 950.0, 950.0, "PRI", "ossa e denti, contrazione muscolare"),
        MicroDef("iron", "Ferro", "mg", 11.0, 16.0, "PRI", "trasporto dell'ossigeno"),
        MicroDef("magnesium", "Magnesio", "mg", 350.0, 300.0, "AI", "muscoli, nervi, energia"),
        MicroDef("potassium", "Potassio", "mg", 3500.0, 3500.0, "AI", "pressione arteriosa, muscoli"),
        MicroDef("zinc", "Zinco", "mg", 11.7, 9.3, "PRI", "difese immunitarie, pelle (riferimento con apporto medio di fitati)"),
        MicroDef("selenium", "Selenio", "µg", 70.0, 70.0, "AI", "tiroide, antiossidante"),
        MicroDef("phosphorus", "Fosforo", "mg", 550.0, 550.0, "AI", "ossa ed energia"),
        MicroDef("sodium", "Sodio", "mg", 2000.0, 2000.0, "LIM", "da limitare: 2 g di sodio = 5 g di sale"),
        MicroDef("sugar", "Zuccheri", "g", 0.0, 0.0, "LIM", "zuccheri totali: limite calcolato sul 10% delle calorie")
    )
    val byKey = defs.associateBy { it.key }

    fun reference(d: MicroDef, p: Profile, kcal: Double): Double {
        val age = com.jarvis.food.data.Nutrition.age(p)
        return when {
            d.key == "sugar" -> kcal * 0.10 / 4.0
            d.perMJ -> (if (p.male) d.men else d.women) * kcal * 0.004184
            d.key == "calcium" && age < 25 -> 1000.0
            else -> if (p.male) d.men else d.women
        }
    }

    fun fmt(v: Double, unit: String): String = when {
        v >= 100 -> "${v.roundToInt()} $unit"
        v >= 10 -> "${"%.0f".format(v)} $unit"
        else -> "${"%.1f".format(java.util.Locale.ITALY, v)} $unit"
    }
}

data class MicroStatus(
    val def: MicroDef,
    val avg: Double,
    val ref: Double,
    /** ok | vicino | basso | alto (per i limiti) */
    val level: String
) {
    val pct: Double get() = if (ref <= 0) 0.0 else avg / ref * 100
}

data class FoodTip(val food: DbFood, val grams: Double, val amount: Double, val pctOfRef: Double)

data class GroupGap(val group: String, val label: String, val daysSince: Int?, val tip: String)

data class MicroReport(
    val days: Int,
    val loggedDays: Int,
    /** Quota delle calorie registrate che proviene da alimenti con dati di micronutrienti completi. */
    val coverage: Double,
    val statuses: List<MicroStatus>,
    val groups: List<GroupGap>
) {
    val low: List<MicroStatus> get() = statuses.filter { it.level == "basso" && it.def.kind != "LIM" }.sortedBy { it.pct }
    val high: List<MicroStatus> get() = statuses.filter { it.level == "alto" }
}

object MicroAnalysis {
    /** Gruppi alimentari monitorati per "da quanto tempo non mangi...". */
    val GROUPS = listOf(
        Triple("pesce", "Pesce", "almeno 2 volte a settimana, una delle quali pesce grasso"),
        Triple("pesce_grasso", "Pesce grasso (salmone, sgombro, sardine, trota)", "fonte di omega-3 e vitamina D"),
        Triple("legumi", "Legumi", "almeno 3 porzioni a settimana"),
        Triple("verdura_foglia", "Verdura a foglia verde", "folati, vitamina K, magnesio"),
        Triple("frutta", "Frutta fresca", "vitamina C e fibre, ogni giorno"),
        Triple("frutta_secca", "Frutta secca e semi", "una manciata (30 g) più volte a settimana"),
        Triple("cereali_integrali", "Cereali integrali", "fibre, magnesio, vitamine del gruppo B"),
        Triple("latticini", "Latte e yogurt", "calcio e vitamina B12")
    )

    fun analyze(diary: List<DiaryEntry>, p: Profile, kcalTarget: Double, today: LocalDate = LocalDate.now(), days: Int = 7): MicroReport {
        val from = today.minusDays(days.toLong() - 1)
        val recent = diary.filter { val d = LocalDate.parse(it.date); !d.isBefore(from) && !d.isAfter(today) }
        val byDay = recent.groupBy { it.date }
        // Giorni "registrati": almeno metà dell'obiettivo calorico inserito, per non sottostimare con giorni incompleti.
        val logged = byDay.filter { (_, es) -> es.sumOf { it.food.kcal * it.grams / 100 } >= kcalTarget * 0.5 }
        val n = logged.size
        val entries = logged.values.flatten()
        val totKcal = entries.sumOf { it.food.kcal * it.grams / 100 }
        val withMicro = entries.filter { it.food.micro.size >= 10 }.sumOf { it.food.kcal * it.grams / 100 }
        val coverage = if (totKcal > 0) withMicro / totKcal else 0.0

        val statuses = Micros.defs.map { d ->
            val sum = entries.sumOf { e ->
                val v = e.food.micro[d.key] ?: when (d.key) {
                    "fiber" -> e.food.fiber; "sugar" -> e.food.sugar
                    "sodium" -> e.food.salt?.let { it * 400 }  // sale (g) -> sodio (mg)
                    else -> null
                } ?: 0.0
                v * e.grams / 100
            }
            val avg = if (n > 0) sum / n else 0.0
            val ref = Micros.reference(d, p, kcalTarget)
            val level = if (d.kind == "LIM") { if (avg > ref) "alto" else "ok" } else when {
                avg >= ref -> "ok"
                avg >= ref * 0.7 -> "vicino"
                else -> "basso"
            }
            MicroStatus(d, avg, ref, level)
        }

        val groups = GROUPS.map { (g, label, tip) ->
            val last = diary.filter { e ->
                val gs = setOf(e.food.group) + e.food.tags.filter { it.startsWith("grp:") }.map { it.removePrefix("grp:") }
                g in gs || (g == "pesce" && "pesce_grasso" in gs)
            }
                .maxOfOrNull { it.date }
            GroupGap(g, label, last?.let { ChronoUnit.DAYS.between(LocalDate.parse(it), today).toInt() }, tip)
        }
        return MicroReport(days, n, coverage, statuses, groups)
    }

    /**
     * Alimenti del database che aiutano a coprire la quota mancante di un nutriente, con la porzione suggerita.
     * Porzione = quanto serve per colmare la differenza, entro una porzione realistica dell'alimento.
     */
    fun tips(db: FoodDb, status: MicroStatus, exclude: List<String>, limit: Int = 4): List<FoodTip> {
        val key = status.def.key
        val gap = max(0.0, status.ref - status.avg)
        if (gap <= 0) return emptyList()
        return db.foods.asSequence()
            .filter { f -> f.tags.none { it in exclude } && f.group !in setOf("spezie", "erbe", "integratori", "alcol", "condimenti", "dolci") }
            .mapNotNull { f ->
                val per100 = f.micro[key] ?: return@mapNotNull null
                if (per100 <= 0) return@mapNotNull null
                val portion = typicalPortion(f)
                val grams = min(portion, gap / per100 * 100).coerceAtLeast(min(portion, 10.0))
                val amount = per100 * grams / 100
                // Densità: quanto nutriente per 100 kcal, per preferire alimenti che non sbilanciano la dieta.
                val density = per100 / max(f.kcal, 20.0)
                Triple(f, grams, amount) to density
            }
            .sortedByDescending { it.second }
            .map { (t, _) -> FoodTip(t.first, roundTo5(t.second), t.third, t.third / status.ref * 100) }
            .distinctBy { it.food.group }
            .take(limit).toList()
    }

    fun typicalPortion(f: DbFood): Double = when {
        f.pieceGrams != null && f.id != "aglio" -> max(f.pieceGrams, if (f.group == "frutta") f.pieceGrams else 30.0)
        f.group in setOf("frutta_secca", "semi") -> 30.0
        f.group == "olio" -> 10.0
        f.group.startsWith("verdura") -> 200.0
        f.group == "frutta" -> 150.0
        f.group == "legumi" -> if (f.kcal > 250) 60.0 else 150.0
        f.group.startsWith("pesce") || f.group.startsWith("carne") -> 150.0
        f.group == "latticini" -> if (f.liquid) 200.0 else if (f.kcal > 300) 30.0 else 125.0
        f.group in setOf("cereali", "cereali_integrali") -> 80.0
        else -> 100.0
    }

    private fun roundTo5(v: Double) = max(5.0, Math.round(v / 5.0) * 5.0)
}
