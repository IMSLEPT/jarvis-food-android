package com.jarvis.food

import com.jarvis.food.data.ActivityEntry
import com.jarvis.food.data.DiaryEntry
import com.jarvis.food.data.DietPrefs
import com.jarvis.food.data.EnergySettings
import com.jarvis.food.data.Goals
import com.jarvis.food.data.HealthDay
import com.jarvis.food.data.PantryItem
import com.jarvis.food.data.Nutrition
import com.jarvis.food.data.Profile
import com.jarvis.food.engine.Energy
import com.jarvis.food.engine.FoodDb
import com.jarvis.food.engine.Kitchen
import com.jarvis.food.engine.Macro
import com.jarvis.food.engine.MicroAnalysis
import com.jarvis.food.engine.Planner
import com.jarvis.food.engine.Shopping
import com.jarvis.food.engine.Solver
import com.jarvis.food.engine.toFood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

/** Test del motore: database, calcolo grammature, piano, adattamento all'attività, micronutrienti, spesa. */
class EngineTest {
    private val db: FoodDb = FoodDb.parse(File("src/main/assets/foods.json").readText(), File("src/main/assets/recipes.json").readText())
    private val profile = Profile(name = "Test", male = true, birthYear = 1997, heightCm = 178.0, startWeight = 80.0, targetWeight = 80.0, activity = 2, goal = 1, weeklyRate = 0.25)
    private val goals = Goals(fatPct = 27, auto = true)
    private val day = Energy.forDay(profile, goals, EnergySettings(mode = 0), 80.0, null, emptyList(), 0)

    private fun report(name: String, text: String) = println("[$name] $text")

    @Test fun databaseIsComplete() {
        assertTrue(db.foods.size >= 180)
        assertTrue(db.recipes.size >= 45)
        for (f in db.foods) {
            assertTrue("${f.id} senza energia", f.kcal >= 0)
            assertTrue("${f.id}: macro incoerenti", f.protein + f.carbs + f.fat <= 101)
        }
        for (r in db.recipes) for (i in r.ings) assertTrue("${r.id}: ${i.food}", db.food(i.food) != null)
        report("db", "${db.foods.size} alimenti, ${db.recipes.size} ricette")
    }

    @Test fun energyValuesMatchMacros() {
        // Controllo di coerenza dei dati USDA: kcal ≈ 4P + 4C + 9G (+2 kcal/g fibra), tolleranza ampia per i fattori di Atwater specifici.
        val bad = db.foods.filter { f ->
            val est = 4 * f.protein + 4 * f.carbs + 9 * f.fat + 2 * (f.micro["fiber"] ?: 0.0)
            f.kcal > 40 && abs(est - f.kcal) / f.kcal > 0.18 && f.group != "alcol"
        }
        report("atwater", "fuori tolleranza: ${bad.map { it.id }}")
        assertTrue("Troppi alimenti incoerenti: ${bad.map { it.id }}", bad.size <= 4)
    }

    @Test fun solverHitsTargets() {
        val targets = Planner.slotTargets(day.macro, DietPrefs())
        val prefs = DietPrefs()
        val errs = mutableListOf<Double>()
        val far = mutableListOf<String>()
        for (r in db.recipes) {
            val slot = prefs.slots.indexOfFirst { it.meal == (if (r.meal == "dessert") "spuntino" else r.meal) }
            val s = Solver.solve(r, db, targets[slot])
            errs += abs(s.kcalErrorPct)
            // Una ricetta con pochi ingredienti scalabili (es. due uova a colazione) non può raggiungere un pasto da 750 kcal:
            // il solver lo dichiara (errore alto) e i consigli la scartano. Qui si tollera una piccola quota.
            if (abs(s.kcalErrorPct) >= 15) far += r.id
            for (st in s.steps()) assertFalse("${r.id}: segnaposto non sostituito in '$st'", st.contains("{"))
            for (it in s.items) assertTrue("${r.id}: ${it.food.id} ${it.grams} g fuori porzione", it.grams <= Solver.maxServing(it.food) + 0.01)
        }
        assertTrue("Troppe ricette fuori obiettivo (${far.size}/${db.recipes.size}): $far", far.size <= db.recipes.size * 0.15)
        errs.sort()
        report("solver", "errore kcal mediano ${"%.1f".format(errs[errs.size / 2])}%, massimo ${"%.1f".format(errs.last())}%")
        assertTrue(errs[errs.size / 2] < 5.0)
    }

    @Test fun planDaysMatchDailyTargets() {
        val prefs = DietPrefs()
        val start = LocalDate.of(2026, 10, 19)
        val plan = Planner.generate(db, prefs, start, 7, day.macro)
        assertEquals(7, plan.size)
        val kcalErr = mutableListOf<Double>(); val protErr = mutableListOf<Double>()
        for (dp in plan) {
            assertEquals(4, dp.slots.size)
            val meals = Planner.solveDay(db, dp, day.macro, prefs)
            val tot = meals.fold(Macro.ZERO) { a, m -> a + m.solved.total }
            kcalErr += (tot.kcal - day.targetKcal) / day.targetKcal * 100
            protErr += (tot.protein - day.protein) / day.protein * 100
        }
        report("piano", "obiettivo ${day.targetKcal.toInt()} kcal / P ${day.protein.toInt()} g; scarti kcal % ${kcalErr.map { it.toInt() }}, proteine % ${protErr.map { it.toInt() }}")
        assertTrue(kcalErr.all { abs(it) <= 6 })
        assertTrue(protErr.all { abs(it) <= 15 })
        // Varietà: stessa ricetta nello stesso pasto non prima di 3 giorni.
        for (slot in 0..3) {
            val seq = plan.map { d -> d.slots.first { it.slot == slot }.recipeId }
            for (i in seq.indices) for (j in i + 1..minOf(i + 3, seq.lastIndex)) assertTrue("ripetizione $slot: $seq", seq[i] != seq[j])
        }
        val fishDays = plan.count { d -> d.slots.any { s -> db.recipeGroups(db.recipe(s.recipeId)!!).any { it.startsWith("pesce") } } }
        val legumeDays = plan.count { d -> d.slots.any { s -> "legumi" in db.recipeGroups(db.recipe(s.recipeId)!!) } }
        report("varietà", "giorni con pesce $fishDays, con legumi $legumeDays")
        assertTrue(fishDays >= 2 && legumeDays >= 2)
    }

    @Test fun exclusionsAreRespected() {
        val prefs = DietPrefs(exclude = listOf("lattosio", "pesce"))
        val plan = Planner.generate(db, prefs, LocalDate.of(2026, 10, 19), 7, day.macro)
        for (d in plan) for (s in d.slots) {
            val tags = db.recipeTags(db.recipe(s.recipeId)!!)
            assertFalse("${s.recipeId} contiene $tags", "lattosio" in tags || "pesce" in tags)
        }
    }

    @Test fun activityAdaptationHasNoDoubleCounting() {
        val fixed = Energy.forDay(profile, goals, EnergySettings(mode = 0), 80.0, HealthDay("x", 12000, 700.0), emptyList(), 12000)
        assertEquals(0.0, fixed.extraKcal, 0.001)
        val lazy = Energy.forDay(profile, goals, EnergySettings(mode = 2), 80.0, HealthDay("x", 2000, 100.0), emptyList(), 0)
        assertEquals(0.0, lazy.extraKcal, 0.001)
        // Una giornata molto più attiva del solito (oltre il livello del profilo) aggiunge calorie ai carboidrati.
        val active = Energy.forDay(profile, goals, EnergySettings(mode = 2, eatBackPct = 80), 80.0, HealthDay("x", 20000, 1800.0), emptyList(), 0)
        assertTrue("extra ${active.extraKcal}", active.extraKcal > 300)
        assertEquals(lazy.protein, active.protein, 0.001)
        assertEquals(lazy.fat, active.fat, 0.001)
        assertEquals(active.extraKcal / 4, active.carbs - lazy.carbs, 0.5)
        // Senza calorie attive dal watch: stima da passi + allenamenti.
        val est = Energy.forDay(profile, goals, EnergySettings(mode = 2), 80.0, HealthDay("x", 15000, null), emptyList(), 0)
        assertEquals("stima", est.source)
        val manual = Energy.forDay(profile, goals, EnergySettings(mode = 1), 80.0, null, listOf(ActivityEntry("a", "x", "Corsa", 120, 1500)), 3000)
        report("energia", "fisso ${fixed.targetKcal.toInt()}, watch poco attivo ${lazy.targetKcal.toInt()}, watch molto attivo ${active.targetKcal.toInt()} (+${active.extraKcal.toInt()}), manuale ${manual.targetKcal.toInt()}")
        assertTrue(manual.extraKcal > 0)
    }

    /** Regressione: la mattina presto, con pochi passi, l'obiettivo non deve scendere sotto quello del profilo. */
    @Test fun morningSyncDoesNotLowerTarget() {
        val fixed = Energy.forDay(profile, goals, EnergySettings(mode = 0), 80.0, null, emptyList(), 0)
        val morning = Energy.forDay(profile, goals, EnergySettings(mode = 2), 80.0, HealthDay("x", 132, 6.0), emptyList(), 0, 0.3)
        val noData = Energy.forDay(profile, goals, EnergySettings(mode = 2), 80.0, null, emptyList(), 0, 0.3)
        assertEquals(fixed.targetKcal, morning.targetKcal, 0.5)
        assertEquals(fixed.targetKcal, noData.targetKcal, 0.5)
        assertEquals(fixed.protein, morning.protein, 0.001)
        // Anche con il modello esteso del questionario.
        val p2 = profile.copy(detailed = true, job = 0, stepsLevel = 2, workouts = 4, workoutMin = 60, workoutIntensity = 1)
        val f2 = Energy.forDay(p2, goals, EnergySettings(mode = 0), 80.0, null, emptyList(), 0)
        val m2 = Energy.forDay(p2, goals, EnergySettings(mode = 2), 80.0, HealthDay("x", 132, 6.0), emptyList(), 0, 0.3)
        assertEquals(f2.targetKcal, m2.targetKcal, 0.5)
        report("sync mattina", "fisso ${fixed.targetKcal.toInt()} → watch ${morning.targetKcal.toInt()}")
    }

    @Test fun detailedQuestionnaireGivesPlausibleTdee() {
        val base = Profile(male = true, birthYear = 1997, heightCm = 178.0, startWeight = 80.0, detailed = true)
        val desk = Nutrition.tdee(base.copy(job = 0, stepsLevel = 1, workouts = 0), 80.0)
        val sedentaryOld = Nutrition.bmr(base, 80.0) * 1.2
        assertEquals(sedentaryOld * 1.03, desk, sedentaryOld * 0.03)
        val trains = Nutrition.tdee(base.copy(job = 0, stepsLevel = 2, workouts = 4, workoutMin = 60, workoutIntensity = 1), 80.0)
        assertTrue("allenamenti aumentano il fabbisogno", trains > desk + 150)
        val physical = Nutrition.tdee(base.copy(job = 2, stepsLevel = 4, workouts = 0), 80.0)
        assertTrue(physical > desk * 1.10)
        assertTrue("tetto prudente", physical < Nutrition.bmr(base, 80.0) * 1.7)
        // Massa grassa nota: Katch-McArdle.
        val kmr = Nutrition.bmr(base.copy(bodyFatPct = 15.0), 80.0)
        assertEquals(370 + 21.6 * 68.0, kmr, 0.01)
        // Il deficit è limitato al 25% del fabbisogno.
        val aggressive = base.copy(goal = -1, weeklyRate = 1.0)
        val t = Nutrition.tdee(aggressive, 80.0)
        assertEquals(-0.25 * t, Nutrition.safeDelta(aggressive, t), 0.01)
        // Gravidanza: nessun deficit.
        assertEquals(0.0, Nutrition.dailyDelta(aggressive.copy(special = 1)), 0.0)
        report("tdee", "scrivania ${desk.toInt()}, allenamenti ${trains.toInt()}, fisico ${physical.toInt()}")
    }

    @Test fun ageUsesBirthDate() {
        val p = Profile(birthYear = 1997, birthDate = "1997-04-12")
        assertEquals(29, Nutrition.age(p, LocalDate.of(2026, 10, 22)))
        assertEquals(28, Nutrition.age(p, LocalDate.of(2026, 4, 11)))
        assertEquals(29, Nutrition.age(Profile(birthYear = 1997), LocalDate.of(2026, 10, 22)))
    }

    @Test fun calibrationUsesPastWatchDays() {
        val today = LocalDate.of(2026, 10, 22)
        val s = EnergySettings(mode = 2, eatBackPct = 80)
        val tooFew = (1..3).associate { i -> today.minusDays(i.toLong()).toString() to HealthDay(today.minusDays(i.toLong()).toString(), 9000, 600.0) }
        assertEquals(null, Energy.calibration(profile, 80.0, s, tooFew, today))
        val week = (1..7).associate { i -> today.minusDays(i.toLong()).toString() to HealthDay(today.minusDays(i.toLong()).toString(), 9000, 600.0) }
        val c = Energy.calibration(profile, 80.0, s, week, today)!!
        assertEquals(Energy.measuredTdee(Nutrition.bmr(profile, 80.0), 600.0, 80), c, 0.01)
        // La calibrazione resta entro -15% / +20% del profilo.
        val huge = (1..7).associate { i -> today.minusDays(i.toLong()).toString() to HealthDay(today.minusDays(i.toLong()).toString(), 30000, 5000.0) }
        val e = Energy.expectedTdee(profile, 80.0, Energy.calibration(profile, 80.0, s, huge, today))
        assertEquals(Nutrition.tdee(profile, 80.0) * 1.20, e, 0.01)
        // Giorni senza watch indossato (pochi passi) non contano.
        val unworn = (1..7).associate { i -> today.minusDays(i.toLong()).toString() to HealthDay(today.minusDays(i.toLong()).toString(), 200, 0.0) }
        assertEquals(null, Energy.calibration(profile, 80.0, s, unworn, today))
    }

    @Test fun macroAdviceFollowsGoal() {
        val w = 80.0
        val cut = Profile(goal = -1, detailed = true, workouts = 4)
        val a = Nutrition.macroAdvice(cut, w, 2000.0)
        assertEquals("dimagrire con 2000 kcal → proteico", 1, a.best)
        assertTrue(a.perKg[1] > a.perKg[0])
        val keep = Profile(goal = 0, detailed = true, workouts = 3)
        assertEquals("mantenere con 2800 kcal → bilanciato", 0, Nutrition.macroAdvice(keep, w, 2800.0).best)
        // Preferenza low carb accettata solo se non ci si allena molto.
        val lc = Profile(goal = -1, dietStyle = 2, detailed = true, workouts = 1)
        assertEquals(2, Nutrition.macroAdvice(lc, w, 2000.0).best)
        val lcHeavy = lc.copy(workouts = 5, workoutIntensity = 2)
        assertTrue(Nutrition.macroAdvice(lcHeavy, w, 2000.0).best != 2)
        for (pr in Nutrition.PRESETS) assertEquals(100, pr.carbs + pr.protein + pr.fat)
    }

    @Test fun microAnalysisFindsLowIntake() {
        // Una settimana di sola pasta al pomodoro: vitamina C e B12 devono risultare basse.
        val pasta = db.recipe("spaghetti_pomodoro")!!
        val solved = Solver.solve(pasta, db, Macro(2600.0, 100.0, 380.0, 70.0))
        val food = solved.toFood()
        val today = LocalDate.of(2026, 10, 21)
        val diary = (0 until 7).map { DiaryEntry("e$it", today.minusDays(it.toLong()).toString(), 1, food, solved.grams, 0) }
        val rep = MicroAnalysis.analyze(diary, profile, solved.total.kcal, today)
        val low = rep.low.map { it.def.key }
        report("micro", "bassi: $low; giorni ${rep.loggedDays}; copertura ${(rep.coverage * 100).toInt()}%")
        assertEquals(7, rep.loggedDays)
        assertTrue("vitB12" in low)
        val tips = MicroAnalysis.tips(db, rep.statuses.first { it.def.key == "vitC" }, emptyList())
        assertTrue(tips.isNotEmpty())
        report("consigli vit C", tips.joinToString { "${it.grams.toInt()} g ${it.food.name} (+${it.pctOfRef.toInt()}%)" })
        val fish = rep.groups.first { it.group == "pesce" }
        assertEquals(null, fish.daysSince)
    }

    @Test fun shoppingListSubtractsPantry() {
        val r = Solver.solve(db.recipe("pasta_pollo_zucchine")!!, db, Macro(900.0, 55.0, 110.0, 25.0))
        val pastaG = r.items.first { it.food.id == "pasta" }.grams
        var n = 0
        val list = Shopping.build(db, listOf(r, r), listOf(PantryItem("p", "pasta", "Pasta", 1000.0)), emptyList()) { "id${n++}" }
        assertFalse("pasta già in dispensa", list.any { it.foodId == "pasta" })
        val pollo = list.first { it.foodId == "pollo" }
        assertEquals(r.items.first { it.food.id == "pollo" }.grams * 2, pollo.needGrams, 1.0)
        assertTrue(pollo.packs >= 1)
        report("spesa", list.joinToString { "${it.name} ${it.needGrams.toInt()} g (${it.packs}×${it.packGrams.toInt()})" } + " · pasta coperta ${pastaG * 2} g")
    }

    @Test fun pantrySuggestionsAndMatching() {
        assertEquals("uova", db.match("UOVA FRESCHE CAT.A X6")?.id)
        assertEquals("pasta", db.match("SPAGHETTI N.5 500G")?.id)
        assertEquals("latte_sl", db.match("LATTE SENZA LATTOSIO PS 1L")?.id)
        val pantry = listOf("uova", "spinaci", "basmati", "olio").map { PantryItem(it, it, it, 500.0) }
        val sug = Kitchen.suggest(db, pantry, "cena", { Macro(780.0, 45.0, 97.0, 23.0) }, { true })
        report("dispensa", sug.take(3).joinToString { "${it.solved.template.id} ${(it.coverage * 100).toInt()}%" })
        assertEquals("riso_uova_spinaci", sug.first().solved.template.id)
    }
}
