package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
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

@RunWith(AndroidJUnit4::class)
class ReferenceUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val day = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        Store.init(ins.targetContext)
        check(com.jarvis.food.engine.RecipeCats.get(Db.db).isNotEmpty())
        ui {
            Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true),
                trainingNutrition = TrainingNutrition(enabled = true, automatic = true)) }
            Store.generatePlan(day.with(DayOfWeek.MONDAY), 7)
            Store.setSteps(day, 3285)
            Nav.clear(); Nav.tab = 0; Nav.diaryDate = day
        }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync()
        val config = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/reference-$config").apply { mkdirs() }
        val image = ins.uiAutomation.takeScreenshot()
        try { File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { image.recycle() }
    }
    @Test fun identicalCalendarSynchronizesDatesAndKeepsSwipeAndArrowNavigation() {
        prepare()
        val saved = Store.current
        ActivityScenario.launch(MainActivity::class.java).use {
            val todayBounds = compose.onNodeWithTag("week-strip").fetchSemanticsNode().boundsInRoot
            shot("oggi")
            compose.onNodeWithTag("nav-1").performClick()
            val planBounds = compose.onNodeWithTag("week-strip").fetchSemanticsNode().boundsInRoot
            assertEquals(todayBounds, planBounds)
            shot("piano-chiuso")
            compose.onNodeWithTag("week-day-3").performClick()
            val selected = Nav.planDate
            assertEquals(selected, Nav.diaryDate)
            compose.onNodeWithTag("nav-0").performClick()
            assertEquals(selected, Nav.diaryDate)
            compose.onNodeWithTag("week-strip").performTouchInput { swipeLeft(durationMillis = 400) }
            compose.waitUntil(10000) { Nav.diaryDate == selected.plusWeeks(1) }
            compose.onNodeWithTag("nav-1").performClick()
            assertEquals(selected.plusWeeks(1), Nav.planDate)
            compose.onNodeWithTag("week-previous").performClick()
            compose.waitUntil(10000) { Nav.planDate == selected }
            assertEquals(saved.diary, Store.current.diary)
            assertEquals(saved.plan, Store.current.plan)
        }
    }
    @Test fun summaryMealActionsAndHomeIllustrationsRemainLive() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithTag("jarvis-chef-banner").performScrollTo(); shot("jarvis-chef")
            compose.onNodeWithTag("jarvis-chef-banner").performClick()
            assertEquals(Screen.WhatNow, Nav.stack.last())
            ui { Nav.back() }
            compose.onNodeWithTag("home-vitamins").performScrollTo(); shot("vitamine")
            compose.onNodeWithTag("everyday-food-guide").performScrollTo(); shot("idee")
            compose.onNodeWithTag("home-activity").performScrollTo(); shot("attivita")
            compose.onNodeWithTag("nav-1").performClick()
            compose.onNodeWithTag("plan-summary-toggle").performScrollTo().performClick()
            compose.onNodeWithTag("plan-summary-content").assertExists()
            shot("piano-aperto")
            scenario.recreate()
            compose.onNodeWithTag("plan-summary-content").assertExists()
            compose.onNodeWithTag("plan-diet-style").performScrollTo().performClick()
            // Dismiss dialog through the existing action, without changing saved preferences.
            compose.onNodeWithText("Annulla").performClick()
            val meals = Store.plannedMeals(Store.current, day)
            val slot = meals.last()
            compose.onNodeWithTag("plan-slot-strip").performScrollTo()
            compose.onNodeWithTag("plan-slot-${slot.diaryMeal}").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("plan-eaten-${slot.diaryMeal}").performScrollTo().performClick()
            compose.onNodeWithTag("plan-summary-toggle").performScrollTo()
            compose.onNodeWithTag("plan-completed-count", true).assertTextEquals("1 di ${meals.size} pasti")
            shot("piano-dati-aggiornati")
            compose.onNodeWithTag("plan-summary-toggle").performClick()
            compose.onNodeWithTag("plan-summary-content").assertDoesNotExist()
            shot("piano-richiuso")
        }
    }
}
