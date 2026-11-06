package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.*
import com.jarvis.food.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/** Redesign del 4 novembre 2026: screenshot di ogni componente aggiornato e controlli di comportamento. */
@RunWith(AndroidJUnit4::class)
class RedesignNovemberTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val day = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)

    private fun prepare(withFood: Boolean = true) {
        Store.init(ins.targetContext)
        check(com.jarvis.food.engine.RecipeCats.get(Db.db).isNotEmpty())
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true),
                trainingNutrition = TrainingNutrition(enabled = true, automatic = true)) }
            Store.generatePlan(day.with(DayOfWeek.MONDAY), 7)
            Store.setSteps(day, 3285)
            if (withFood) {
                Db.db.food("mela")?.let { Store.addEntry(day, 4, it.toFood(), 150.0) }
                for (d in 1L..6L) Db.db.food("mela")?.let { Store.addEntry(day.minusDays(d), 4, it.toFood(), 150.0) }
            }
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day
        }
    }

    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync()
        android.os.SystemClock.sleep(400)
        val config = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/redesign-$config").apply { mkdirs() }
        val image = ins.uiAutomation.takeScreenshot()
        try { File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { image.recycle() }
    }

    @Test fun todayRedesignKeepsBehaviourAndCaloriesGrowFromZero() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15000) { compose.onAllNodesWithTag("today-calories").fetchSemanticsNodes().isNotEmpty() }
            shot("01-oggi-top")
            // Percentuale e barra crescono con le calorie mangiate.
            val s = Store.current
            val eaten = Nutrition.sum(Store.entriesFor(s, day)).kcal
            val target = Store.energyFor(s, day).targetKcal
            val range = compose.onNodeWithTag("calorie-gauge", true).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
            assertEquals((eaten / target).toFloat().coerceIn(0f, 1f), range.current, 0.01f)
            assertTrue("La percentuale deve partire da zero", range.current < 0.5f)
            // Tipo di giornata: scheda e finestra.
            compose.onNodeWithTag("today-day-type-bar").performClick()
            compose.onNodeWithTag("day-type-dialog").assertExists(); shot("02-tipo-giornata")
            compose.onNodeWithTag("day-type-choice-true").performClick()
            assertEquals(true, Store.resolvedTraining(Store.current).overrides[day.toString()])
            compose.onNodeWithTag("today-day-type-bar").performClick()
            compose.onNodeWithTag("day-type-choice-calendar").performClick()
            assertNull(Store.resolvedTraining(Store.current).overrides[day.toString()])
            // Diario.
            compose.onNodeWithTag("diary-meal-0").performScrollTo(); shot("03-diario")
            compose.onNodeWithContentDescription("Aggiungi a Colazione").performClick()
            assertEquals(Screen.AddFood(day, 0), Nav.stack.last())
            ui { Nav.back() }; compose.waitForIdle()
            compose.onNodeWithTag("home-vitamins").performScrollTo(); shot("04-vitamine")
            compose.onNodeWithTag("everyday-food-guide").performScrollTo(); shot("05-per-te-oggi")
            compose.onNodeWithTag("daily-tips-all").performClick()
            compose.onNodeWithTag("diet-coach-dialog").assertExists()
            compose.onNodeWithTag("diet-coach-30").performClick(); shot("06-dieta-30-giorni")
            compose.onNodeWithText("Chiudi").performScrollTo().performClick()
            compose.onNodeWithTag("home-activity").performScrollTo(); shot("07-attivita")
            compose.onNodeWithText("Smartwatch").performScrollTo().performClick()
            assertEquals(Screen.Watch, Nav.stack.last())
        }
    }

    @Test fun planRedesignSelectorCardAndDialogs() {
        prepare(withFood = false)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("nav-1").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("plan-slot-strip").fetchSemanticsNodes().isNotEmpty() }
            shot("10-piano-top")
            val meals = Store.plannedMeals(Store.current, day)
            compose.onNodeWithTag("plan-slot-strip").performScrollTo(); shot("11-piano-pasti")
            compose.onNodeWithTag("plan-card-${meals.first().diaryMeal}").performScrollTo(); shot("12-piano-scheda")
            val second = meals[1]
            compose.onNodeWithTag("plan-slot-strip").performScrollTo()
            compose.onNodeWithTag("plan-slot-${second.diaryMeal}").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("plan-eaten-${second.diaryMeal}").performScrollTo().performClick()
            assertTrue(Store.dayPlan(Store.current, day)!!.slots.first { it.slot == second.slotIndex }.eatenEntryIds.isNotEmpty())
            compose.onNodeWithTag("plan-summary-toggle").performScrollTo().performClick(); shot("13-riepilogo")
            compose.onNodeWithTag("plan-diet-style").performScrollTo().performClick()
            compose.onNodeWithTag("diet-style-dialog").assertExists(); shot("14-alimentazione")
            compose.onNodeWithTag("diet-period-7").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("diet-style-vegan").performScrollTo().performClick(); shot("15-alimentazione-diete")
            compose.onNodeWithText("Annulla").performClick()
            compose.onNodeWithTag("day-type-label").performScrollTo().performClick()
            compose.onNodeWithTag("day-type-dialog").assertExists(); shot("16-piano-tipo-giornata")
            compose.onNodeWithTag("day-type-close").performClick()
        }
    }

    @Test fun kitchenProgressJarvisBanners() {
        prepare(withFood = false)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("nav-2").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithTag("kitchen-favourites").fetchSemanticsNodes().isNotEmpty() }
            shot("20-cucina-top")
            compose.onNodeWithTag("kitchen-inspiration").performScrollTo(); shot("21-cucina-esplora-ispirazione")
            compose.onNodeWithTag("recipe-group-strip").performScrollTo()
            val groupIds = listOf("pasto", "tempo", "kcal", "obiettivo", "dieta", "metodo", "cucina")
            val filterStrip = compose.onNodeWithTag("recipe-group-strip")
            var stripTop: Float? = null
            for (id in groupIds) {
                val button = compose.onNodeWithTag("recipe-group-$id")
                    .performScrollTo().performClick().assertIsSelected()
                val top = button.fetchSemanticsNode().boundsInRoot.top - filterStrip.fetchSemanticsNode().boundsInRoot.top
                stripTop?.let { assertEquals("Filter $id wrapped onto another row", it, top, 1f) }
                stripTop = top
                if (id == "pasto") shot("22-cucina-filtri")
                if (id == "cucina") shot("22b-cucina-filtri-ultimi")
            }
            compose.onNodeWithTag("kitchen-inspiration").performScrollTo().performClick()
            assertEquals(Screen.WhatNow, Nav.stack.last())
            ui { Nav.back() }; compose.waitForIdle()
            compose.onNodeWithTag("banner-tab-Cucina-1").performScrollTo().performClick(); shot("23-cucina-dispensa")
            compose.onNodeWithText("Fotografa lo scontrino").performScrollTo().performClick()
            assertEquals(Screen.PantryScan("scontrino"), Nav.stack.last())
            compose.onNodeWithText("Scatta foto").assertExists()
            compose.onNodeWithText("Galleria").assertExists()
            shot("23b-scontrino")
            ui { Nav.back() }
            compose.onNodeWithTag("banner-tab-Cucina-2").performScrollTo().performClick()
            compose.onNodeWithText("Prossimi 3 giorni").performScrollTo().performClick()
            assertTrue(Store.current.shoppingRange.endsWith("|3"))
            shot("23c-spesa")
            compose.onNodeWithTag("nav-3").performClick()
            if (ins.targetContext.resources.configuration.fontScale <= 1.2f) {
                val weight = compose.onNodeWithTag("progress-weight-summary").fetchSemanticsNode().boundsInRoot
                val calories = compose.onNodeWithTag("progress-calorie-summary").fetchSemanticsNode().boundsInRoot
                assertEquals(weight.height, calories.height, 1f)
            }
            shot("24-progressi")
            compose.onNodeWithTag("progress-water-detail").performScrollTo()
            shot("24a-progressi-acqua")
            compose.onNodeWithTag("progress-activity-detail").performScrollTo()
            shot("24d-progressi-attivita")
            compose.onNodeWithTag("banner-tab-Progressi-1").performScrollTo().performClick().assertIsSelected(); shot("24b-progressi-grafici")
            compose.onNodeWithTag("calorie-day-detail").performScrollTo()
            shot("24c-progressi-calorie")
            ui { Store.update { it.copy(ai = it.ai.copy(chatEngine = 1, apiKey = "layout-test-no-network")) } }
            compose.onNodeWithTag("nav-5").performClick()
            compose.onNodeWithText("Come posso aiutarti?").assertExists()
            shot("25-jarvis")
            compose.onNodeWithTag("header-profile").performClick()
            assertEquals(Screen.Profile, Nav.stack.last())
        }
    }

    @Test fun watchPercentageAndModesStayInSync() {
        prepare(withFood = false)
        ui {
            Store.update { current ->
                current.copy(trainingNutrition = TrainingNutrition(), energy = EnergySettings(mode = 2, eatBackPct = 80), health = mapOf(
                    day.toString() to HealthDay(day.toString(), steps = 4500, activeKcal = 1200.0, totalKcal = 2720.0,
                        restingHr = 61.0, sleepMinutes = 405)
                ))
            }
            Nav.go(Screen.Watch)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            shot("30-watch-top")
            val slider = compose.onNodeWithTag("watch-activity-slider").performScrollTo()
            val before = Store.current.energy.eatBackPct
            compose.onNodeWithTag("watch-activity-percentage").assertTextEquals("$before%")
            val target = Store.energyFor(Store.current, day).targetKcal
            slider.performTouchInput { swipe(start = Offset(width * .78f, height * .5f), end = Offset(width * .37f, height * .5f)) }
            val changed = Store.current.energy.eatBackPct
            assertTrue("Dragging must produce an intermediate percentage: $changed", changed in 25..49)
            assertNotEquals(target, Store.energyFor(Store.current, day).targetKcal, 0.1)
            compose.onNodeWithTag("watch-activity-percentage").assertTextEquals("$changed%")
            val expectedTarget = kcalFmt(Store.energyFor(Store.current, day).targetKcal)
            compose.onNodeWithTag("watch-calorie-target").assertTextEquals("$expectedTarget kcal")
            compose.onNodeWithTag("watch-calories").performScrollTo()
            shot("31-watch-calories")
            compose.onNodeWithText("Importa il peso da Health Connect").performScrollTo()
            compose.onNode(hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
                .performClick()
            assertFalse(Store.current.energy.importWeight)
            ui { Store.saveNow() }
            scenario.recreate()
            compose.onNodeWithTag("watch-activity-slider").performScrollTo()
            compose.onNodeWithTag("watch-activity-percentage").assertTextEquals("$changed%")
            compose.onNodeWithText("Livello fisso").performScrollTo().performClick().assertIsSelected()
            assertEquals(0, Store.current.energy.mode)
            compose.onNodeWithText("Attività a mano").performScrollTo().performClick().assertIsSelected()
            assertEquals(1, Store.current.energy.mode)
            compose.onNodeWithText("Come calcolo l'obiettivo di oggi").performScrollTo().performClick()
            assertEquals(Screen.EnergyInfo, Nav.stack.last())
            ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertEquals(Screen.Watch, Nav.stack.last())
            ui { Store.update { it.copy(trainingNutrition = TrainingNutrition(enabled = true, automatic = true)) } }
            compose.onNodeWithTag("watch-data-tiles").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Obiettivi ON/OFF attivi").assertExists()
            compose.onNodeWithText("FC a riposo", substring = true).assertExists()
            shot("32a-watch-data")
            compose.onNodeWithTag("watch-open-zepp").performScrollTo().assertIsDisplayed().assertHasClickAction()
            shot("32-watch-device")
        }
    }

}
