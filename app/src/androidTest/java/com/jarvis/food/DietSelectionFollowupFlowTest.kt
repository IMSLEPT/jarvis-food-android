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

class DietSelectionFollowupFlowTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val ins=InstrumentationRegistry.getInstrumentation()
    private val date=LocalDate.now()
    private fun ui(block:()->Unit)=ins.runOnMainSync(block)
    private fun prepare(minutes:Int=40) {
        Store.init(ins.targetContext)
        val target=DailyTargets(2200,120.0,260.0,75.0)
        ui {
            Store.update { AppState(onboarded=true,profile=Profile(detailed=true),
                trainingNutrition=TrainingNutrition(enabled=true,on=target,off=target,trainingDays=listOf(date.dayOfWeek.value)),
                dietPrefs=DietPrefs(maxMinutes=minutes,slots=listOf(MealSlotDef("Colazione","colazione",25,0),MealSlotDef("Spuntino mattutino","spuntino",10,4),
                    MealSlotDef("Pranzo","pranzo",30,1),MealSlotDef("Merenda","spuntino",10,3),MealSlotDef("Cena","cena",25,2)))) }
            Store.generatePlan(date,1); Nav.clear(); Nav.tab=1; Nav.planDate=date
        }
    }
    private fun openSelector() {
        compose.onNodeWithTag("plan-summary-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("plan-diet-style").performScrollTo().performClick()
    }
    private fun shot(name:String) {
        compose.waitForIdle();ins.waitForIdleSync();android.os.SystemClock.sleep(400)
        val profile=InstrumentationRegistry.getArguments().getString("captureName")?:"standard"
        val file=File(ins.targetContext.filesDir,"shots/diet-followup-$profile/$name.png").apply { parentFile!!.mkdirs() }
        val bitmap=ins.uiAutomation.takeScreenshot()
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } } finally { bitmap.recycle() }
    }
    @Test fun planControlsHaveEqualSizeAndSpaceBeforeDetails() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("plan-summary-toggle").performScrollTo().performClick()
            compose.onNodeWithTag("plan-nutrition-details").performScrollTo()
            val day=compose.onNodeWithTag("plan-day-type-cell").fetchSemanticsNode().boundsInRoot
            val style=compose.onNodeWithTag("plan-diet-style").fetchSemanticsNode().boundsInRoot
            val details=compose.onNodeWithTag("plan-nutrition-details").fetchSemanticsNode().boundsInRoot
            assertEquals(day.width,style.width,1f)
            assertEquals(day.height,style.height,1f)
            val density=ins.targetContext.resources.displayMetrics.density
            assertTrue("Details touch the diet controls",details.top-style.bottom>=8*density-1)
            shot("equal-plan-controls")
        }
    }
    @Test fun japaneseTimeFixIsExplicitAndSavesOnlyAfterConfirmation() {
        prepare(15)
        val before=Store.current
        val targets=Store.energyFor(before,date).macro
        ActivityScenario.launch(MainActivity::class.java).use {
            openSelector()
            compose.onNodeWithTag("diet-style-japanese").performScrollTo().performClick()
            compose.onNodeWithTag("diet-style-confirm").assertIsNotEnabled()
            compose.onNodeWithTag("diet-style-use-required-time").performScrollTo().assertIsDisplayed()
            shot("japanese-time-conflict")
            compose.onNodeWithTag("diet-style-use-required-time").performClick()
            assertEquals(before,Store.current)
            compose.onNodeWithTag("diet-style-confirm").assertIsEnabled().performClick()
            compose.waitUntil(60000) { Store.dayPlan(Store.current,date)?.styleId=="japanese" }
            assertEquals(25,Store.current.dietPrefs.maxMinutes)
            assertEquals(targets,Store.energyFor(Store.current,date).macro)
            assertEquals(before.diary,Store.current.diary)
            assertEquals(5,Store.dayPlan(Store.current,date)!!.slots.size)
            shot("japanese-confirmed")
        }
    }
    @Test fun healthyIsASeparateChoiceAndCancelPreservesThePlan() {
        prepare()
        val before=Store.current
        ActivityScenario.launch(MainActivity::class.java).use {
            openSelector()
            compose.onNodeWithTag("diet-style-healthy").performScrollTo().performClick()
            compose.onNodeWithText("OMS").performScrollTo().assertExists()
            shot("healthy-choice")
            compose.onNodeWithText("Annulla").performClick()
            assertEquals(before,Store.current)
            compose.onNodeWithTag("plan-diet-style").performScrollTo().performClick()
            compose.onNodeWithTag("diet-style-healthy").performScrollTo().performClick()
            compose.onNodeWithTag("diet-style-confirm").performClick()
            compose.waitUntil(60000) { Store.dayPlan(Store.current,date)?.styleId=="healthy" }
            assertEquals(before.goals,Store.current.goals)
            assertEquals(before.diary,Store.current.diary)
            assertEquals(before.dietPrefs.exclude,Store.current.dietPrefs.exclude)
            assertTrue(Store.dayPlan(Store.current,date)!!.slots.all { DietStyles.allowed(Db.db,Db.db.recipe(it.recipeId)!!,"healthy") })
            shot("healthy-confirmed")
        }
    }
}
