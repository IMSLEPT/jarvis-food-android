package com.jarvis.food

import android.graphics.Bitmap
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
            compose.onNodeWithTag("recipe-group-tempo").performScrollTo().performClick().assertIsSelected(); shot("22-cucina-filtri")
            compose.onNodeWithTag("kitchen-inspiration").performScrollTo().performClick()
            assertEquals(Screen.WhatNow, Nav.stack.last())
            ui { Nav.back() }; compose.waitForIdle()
            compose.onNodeWithTag("banner-tab-Cucina-1").performClick(); shot("23-cucina-dispensa")
            compose.onNodeWithTag("nav-3").performClick(); shot("24-progressi")
            compose.onNodeWithTag("banner-tab-Progressi-1").performClick().assertIsSelected(); shot("24b-progressi-grafici")
            compose.onNodeWithTag("nav-5").performClick(); shot("25-jarvis")
            compose.onNodeWithTag("header-profile").performClick()
            assertEquals(Screen.Profile, Nav.stack.last())
        }
    }
}
