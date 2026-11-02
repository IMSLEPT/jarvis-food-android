package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import com.jarvis.food.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

class FoodFollowupFlowTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val ins=InstrumentationRegistry.getInstrumentation()
    private fun ui(block:()->Unit)=ins.runOnMainSync(block)
    private fun shot(name:String) {
        compose.waitForIdle();ins.waitForIdleSync();android.os.SystemClock.sleep(400)
        val profile=InstrumentationRegistry.getArguments().getString("captureName")?:"standard"
        val file=File(ins.targetContext.filesDir,"shots/food-followup-$profile/$name.png");file.parentFile!!.mkdirs()
        val b=ins.uiAutomation.takeScreenshot()
        try { file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) } } finally { b.recycle() }
    }
    private fun prepare() { Store.init(ins.targetContext);ui { Store.update { AppState(onboarded=true) };Nav.clear();Nav.tab=0 } }
    @Test fun foodAdviceExplainsPureAlmondsWithoutWritingDiary() {
        prepare()
        val food=Food("almond","100% crema di mandorle tostate",kcal=625.0,protein=21.0,carbs=6.0,fat=56.0,sugar=3.0,satFat=4.0,salt=.01)
        ui { Nav.go(Screen.FoodDetail(food,LocalDate.now(),0,15.0)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("food-guidance-details").performScrollTo()
            compose.waitForIdle();ins.waitForIdleSync();android.os.SystemClock.sleep(200)
            compose.onNodeWithTag("food-guidance-details").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Confronta prodotti e marche").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("100% frutta a guscio · conta la porzione").assertExists()
            assertTrue(Store.current.diary.isEmpty())
            shot("food-advice")
        }
    }
    @Test fun homeShowsThreeCompactDailyTipsWithoutHistory() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("everyday-food-guide").performScrollTo()
            compose.onAllNodes(hasTestTagPrefix("daily-food-tip-")).assertCountEquals(3)
            compose.onAllNodesWithText("Mostra la guida").assertCountEquals(0)
            compose.onAllNodes(hasTestTagPrefix("daily-food-tip-"))[0].performScrollTo().performClick()
            compose.onNodeWithTag("daily-tip-source").assertIsDisplayed()
            compose.onNodeWithText("Chiudi").performClick()
            assertTrue(Store.current.diary.isEmpty())
            shot("immediate-guide")
        }
    }

    private fun hasTestTagPrefix(prefix:String)=SemanticsMatcher("tag starts with $prefix") { node ->
        node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith(prefix)
    }
    @Test fun manualPlanPreservesEditedGramsAndDoesNotMarkEaten() {
        prepare();val date=LocalDate.now()
        val food=Food("custom:test","Alimento personale",kcal=250.0,protein=15.0,carbs=25.0,fat=10.0,source="custom")
        ui {
            Store.generatePlan(date,1)
            Store.setManualMeal(date,0,Recipe("manual:test","Il mio pasto",1,listOf(Ingredient(food,100.0))))
            Nav.go(Screen.ManualMeal(date,0))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Quantità").performScrollTo().performClick()
            compose.onNodeWithTag("manual-food-guidance").assertExists()
            compose.onNodeWithTag("manual-quantity").performTextReplacement("123,5")
            compose.onNodeWithText("Conferma quantità").performClick()
            compose.onNodeWithTag("manual-save").performClick()
            compose.waitForIdle()
            assertTrue(Store.current.diary.isEmpty())
            assertEquals(123.5,Store.dayPlan(Store.current,date)!!.slots.first { it.slot==0 }.manualRecipe!!.ingredients.single().grams,0.0)
            ui { Nav.go(Screen.RecipeView("manual:test",Macro(500.0,30.0,60.0,15.0),date,0)) }
            compose.onNodeWithTag("manual-eat").assertExists()
            shot("manual-meal")
            ui { Nav.go(Screen.Alternatives(date,0)) }
            compose.onNodeWithTag("alternative-search").assertExists()
            compose.onNodeWithTag("alternative-manual").assertExists()
            shot("alternatives-search")
            val currentMeal=Store.plannedMeals(Store.current,date).first { it.slotIndex==0 }
            val chosen=Planner.alternatives(Db.db,currentMeal.solved.template.meal,currentMeal.solved.target,Store.prefsFor(Store.current,date)).first()
            compose.waitUntil(20000) { compose.onAllNodesWithText(chosen.template.name).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(chosen.template.name).performScrollTo().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithTag("alternative-search").fetchSemanticsNodes().isEmpty() }
            compose.onAllNodesWithText("Ricetta non trovata").assertCountEquals(0)
            compose.onAllNodesWithText(chosen.template.name).assertAny(hasText(chosen.template.name))
            assertEquals(chosen.template.id,Store.dayPlan(Store.current,date)!!.slots.first { it.slot==0 }.recipeId)
            assertNull(Store.dayPlan(Store.current,date)!!.slots.first { it.slot==0 }.manualRecipe)
            assertTrue(Store.current.diary.isEmpty())
            shot("manual-to-catalog-recipe")
        }
    }
    @Test fun nowSuggestionsStayAboveSystemNavigation() {
        prepare();ui { Nav.go(Screen.WhatNow) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(20000) { compose.onAllNodesWithText("Aggiungi al diario").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Aggiungi al diario").onLast().performScrollTo()
            val bounds=compose.onNodeWithTag("what-now-content").fetchSemanticsNode().boundsInRoot
            scenario.onActivity { activity ->
                val view=activity.window.decorView
                val insets=androidx.core.view.ViewCompat.getRootWindowInsets(view)!!.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                assertTrue("Scrolling content overlaps Android navigation",bounds.bottom<=view.height-insets.bottom+2)
            }
            shot("safe-now-scroll")
        }
    }
}
