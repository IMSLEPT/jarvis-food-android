package com.jarvis.food

import com.jarvis.food.data.ActivityEntry
import com.jarvis.food.data.DiaryEntry
import com.jarvis.food.data.DietPrefs
import com.jarvis.food.data.EnergySettings
import com.jarvis.food.data.Goals
import com.jarvis.food.data.HealthDay
import com.jarvis.food.data.PantryItem
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
        assertEquals(180, db.foods.size)
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
        for (r in db.recipes) {
            val slot = prefs.slots.indexOfFirst { it.meal == r.meal }
            val s = Solver.solve(r, db, targets[slot])
            errs += abs(s.kcalErrorPct)
            assertTrue("${r.id}: ${s.kcalErrorPct}% kcal", abs(s.kcalErrorPct) < 15)
            for (st in s.steps()) assertFalse("${r.id}: segnaposto non sostituito in '$st'", st.contains("{"))
            for (it in s.items) assertTrue("${r.id}: ${it.food.id} ${it.grams} g fuori porzione", it.grams <= Solver.maxServing(it.food) + 0.01)
        }
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
        val active = Energy.forDay(profile, goals, EnergySettings(mode = 2, eatBackPct = 80), 80.0, HealthDay("x", 14000, 800.0), emptyList(), 0)
        assertTrue(active.extraKcal > 300)
        assertEquals(lazy.protein, active.protein, 0.001)
        assertEquals(lazy.fat, active.fat, 0.001)
        assertEquals(active.extraKcal / 4, active.carbs - lazy.carbs, 0.5)
        // Senza calorie attive dal watch: stima da passi + allenamenti.
        val est = Energy.forDay(profile, goals, EnergySettings(mode = 2), 80.0, HealthDay("x", 15000, null), emptyList(), 0)
        assertEquals("stima", est.source)
        val manual = Energy.forDay(profile, goals, EnergySettings(mode = 1), 80.0, null, listOf(ActivityEntry("a", "x", "Corsa", 60, 650)), 3000)
        report("energia", "fisso ${fixed.targetKcal.toInt()}, sedentario ${lazy.targetKcal.toInt()}, watch attivo ${active.targetKcal.toInt()} (+${active.extraKcal.toInt()}), manuale ${manual.targetKcal.toInt()}")
        assertTrue(manual.extraKcal > 0)
    }

    @Test fun microAnalysisFindsLowIntake() {
        // Una settimana di sola pasta al pomodoro: vitamina C e B12 devono risultare basse.
        val pasta = db.recipe("spaghetti_pomodoro")!!
        val solved = Solver.solve(pasta, db, Macro(2600.0, 100.0, 380.0, 70.0))
        val food = solved.toFood()
        val today = LocalDate.of(2026, 10, 21)
        val diary = (0 until 7).map { DiaryEntry("e$it", today.minusDays(it.toLong()).toString(), 1, food, solved.grams, 0) }
        val rep = MicroAnalysis.analyze(diary, profile, 2600.0, today)
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
