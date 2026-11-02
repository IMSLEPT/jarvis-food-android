package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.food.data.*
import com.jarvis.food.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

class VideoCorrectionsFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val day = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        Store.init(ins.targetContext)
        ui { Store.update { AppState(onboarded = true, profile = Profile(detailed = true)) }; Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day }
    }
    private fun shot(name: String) {
        compose.waitForIdle()
        val profile = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val file = File(ins.targetContext.filesDir, "shots/video-$profile/$name.png").apply { parentFile!!.mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } } finally { bitmap.recycle() }
    }

    @Test fun timerMovesImmediatelyAndResumesWithoutReset() {
        prepare()
        ui { Nav.go(Screen.Fasting) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("fasting-start").performScrollTo().performClick()
            compose.waitUntil(10000) { Store.current.fasting.activeStart != null }
            val start = Store.current.fasting.activeStart
            compose.waitUntil(6000) {
                compose.onAllNodesWithTag("fasting-timer", useUnmergedTree = true).fetchSemanticsNodes().any { node ->
                    node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.any { it.text != "00:00:00" }
                }
            }
            compose.onNodeWithTag("fasting-stop").performScrollTo().assertIsDisplayed()
            val stop = compose.onNodeWithTag("fasting-stop").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithText("Le fasi del digiuno").performScrollTo()
            val phase = compose.onNodeWithText("Le fasi del digiuno").fetchSemanticsNode().boundsInRoot
            val stopAfterScroll = compose.onNodeWithTag("fasting-stop").fetchSemanticsNode().boundsInRoot
            assertTrue("Phase cards overlap fasting controls", phase.top > stopAfterScroll.bottom)
            shot("fasting-controls-and-phases")
            ui { Nav.back(); Nav.go(Screen.Fasting) }
            compose.onNodeWithTag("fasting-stop").performScrollTo().assertIsDisplayed()
            assertEquals(start, Store.current.fasting.activeStart)
            ui { Store.stopFast(false) }
        }
    }

    @Test fun brandedSlicesUseGramsAndSaveTheSelectedPieceCount() {
        prepare()
        val food = Food("slice-test", "Fette biscottate integrali", "Mulino Bianco", 387.0, 14.0, 60.0, 7.4, liquid = true)
        val id = Store.addEntry(day, 0, food, 60.0)
        ui { Nav.go(Screen.FoodDetail(food, day, 0, 60.0, Store.current.diary.first { it.id == id })) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("piece-count-4").performScrollTo().performClick()
            compose.onNodeWithTag("food-quantity").performScrollTo().assertTextContains("35,2", substring = true)
            compose.onNodeWithText("Salva").performClick()
            assertEquals(35.2, Store.current.diary.first { it.id == id }.grams, .001)
            assertFalse(Store.current.diary.first { it.id == id }.food.liquid)
            compose.onNodeWithTag("home-vitamins").performScrollTo().assertIsDisplayed()
            shot("home-vitamins-restored")
            // Empty advice remains hidden even while the neutral tracking panel is visible.
            compose.onAllNodesWithText("Un consiglio per te").assertCountEquals(0)
            compose.onNodeWithText("Colazione").performScrollTo().performClick()
            compose.onNodeWithTag("meal-barcode-0").performScrollTo().assertExists()
            shot("barcode-shortcut")
        }
    }

    @Test fun allThreeProgressCardsOpenReadOnlyDayDetails() {
        prepare()
        ui { Store.setWater(day, 500); Store.setSteps(day, 2504); Nav.tab = 3 }
        val before = Store.current
        ActivityScenario.launch(MainActivity::class.java).use {
            for (kind in listOf("macro", "water", "activity")) {
                compose.onNodeWithTag("progress-$kind-detail").performScrollTo().performClick()
                compose.onNodeWithTag("trend-$kind").assertIsDisplayed()
                compose.onNodeWithTag("trend-day-0").performScrollTo().performClick()
                compose.onNodeWithTag("trend-selected-value", useUnmergedTree = true).performScrollTo().assertTextContains("Nessun dato", substring = true)
                compose.onNodeWithTag("trend-day-6").performScrollTo().performClick()
                shot("detail-$kind")
                ui { Nav.back() }
            }
            assertEquals(before, Store.current)
        }
    }

    @Test fun dietSelectionNeedsConfirmationKeepsTargetsAndRejectsConflict() {
        prepare()
        ui { Store.generatePlan(day,7); Nav.tab = 1 }
        val before = Store.current
        val targets = Store.energyFor(before,day).macro
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("plan-diet-style").performScrollTo().performClick()
            compose.onNodeWithTag("diet-style-vegan").performScrollTo().performClick()
            compose.onNodeWithText("Annulla").performClick()
            assertEquals(before, Store.current)
            compose.onNodeWithTag("plan-diet-style").performScrollTo().performClick()
            compose.onNodeWithTag("diet-style-keto").performScrollTo().performClick()
            compose.onNodeWithTag("diet-style-confirm").assertIsNotEnabled()
            compose.onNodeWithTag("diet-style-error").performScrollTo().assertExists()
            shot("diet-incompatible-targets")
            assertEquals(before,Store.current)
            compose.onNodeWithTag("diet-style-omnivore").performScrollTo().performClick()
            shot("diet-choice-confirmation")
            compose.onNodeWithTag("diet-style-confirm").performClick()
            compose.waitUntil(60000) { Store.dayPlan(Store.current,day)?.styleId == "omnivore" }
            assertEquals(targets,Store.energyFor(Store.current,day).macro)
            assertEquals(before.goals,Store.current.goals)
            assertEquals(before.diary,Store.current.diary)
            assertEquals(before.plan.filter { it.date != day.toString() }, Store.current.plan.filter { it.date != day.toString() })
            compose.onNodeWithTag("plan-diet-style").performScrollTo().assertIsDisplayed()
            shot("diet-confirmed-plan")
        }
    }
}
