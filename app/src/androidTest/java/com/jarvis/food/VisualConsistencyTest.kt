package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
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
class VisualConsistencyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val day = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare() {
        Store.init(ins.targetContext)
        ui { Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true)) }; Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); android.os.SystemClock.sleep(400)
        val profile = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/consistency-$profile").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { ins.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun readable(node: SemanticsNodeInteraction) {
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertFalse("Clipped ${layout.layoutInput.text}", layout.multiParagraph.didExceedMaxLines)
            assertTrue("Vertical overflow ${layout.layoutInput.text}", layout.multiParagraph.height <= layout.size.height + 1f)
            for (line in 0 until layout.lineCount) {
                assertFalse("Truncated ${layout.layoutInput.text}", layout.isLineEllipsized(line))
                assertTrue("Horizontal overflow ${layout.layoutInput.text}", layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1f)
            }
        }
    }

    @Test fun emptyAndSingleWeightStatesDoNotInventHistory() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("nav-3").performClick()
            compose.onNodeWithTag("weight-empty").performScrollTo().assertIsDisplayed()
            readable(compose.onNodeWithTag("weight-empty"))
            shot("progress-no-weight")
            compose.onNodeWithTag("calorie-sample-size").performScrollTo().assertTextContains("0 giorni registrati", substring = true)
            compose.onNodeWithTag("calorie-day-detail").performScrollTo().assertTextContains("Nessun pasto registrato", substring = true)
            shot("progress-no-calories")
            ui { Store.addWeight(day, 71.0) }
            compose.onNodeWithTag("weight-chart").performScrollTo().assertContentDescriptionEquals("Una misurazione del peso")
            compose.onNodeWithTag("weight-history-caption").performScrollTo().assertTextContains("Una misurazione", substring = true)
            shot("progress-one-weight")
        }
    }

    @Test fun planActionsRemainReadableAndKeepTheirBehaviour() {
        prepare()
        ui { Store.generatePlan(day.with(DayOfWeek.MONDAY), 7) }
        val meal = Store.plannedMeals(Store.current, day).first()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("nav-1").performClick()
            shot("plan-overview")
            val recipe = compose.onNodeWithTag("plan-recipe-${meal.diaryMeal}").performScrollTo()
            recipe.assertHasClickAction()
            readable(compose.onNodeWithText(meal.solved.template.name, useUnmergedTree = true))
            shot("plan-recipe")
            val eaten = compose.onNodeWithTag("plan-eaten-${meal.diaryMeal}").performScrollTo()
            eaten.assertHeightIsAtLeast(48.dp).performClick()
            compose.waitForIdle()
            assertTrue(Store.dayPlan(Store.current, day)!!.slots.first { it.slot == meal.slotIndex }.eatenEntryIds.isNotEmpty())
            shot("plan-eaten")
            compose.onNodeWithTag("plan-eaten-${meal.diaryMeal}").performScrollTo().performClick()
            compose.waitForIdle()
            assertTrue(Store.dayPlan(Store.current, day)!!.slots.first { it.slot == meal.slotIndex }.eatenEntryIds.isEmpty())
            compose.onNodeWithTag("plan-alternatives-${meal.diaryMeal}").performScrollTo().performClick()
            assertTrue(Nav.stack.last() is Screen.Alternatives)
            shot("plan-alternatives")
        }
    }

    @Test fun diaryMenuAndKitchenKeepSecondaryActionsReachable() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            shot("home")
            compose.onNodeWithText("Colazione").performScrollTo().performClick()
            readable(compose.onNodeWithText("Nessun alimento registrato").performScrollTo())
            readable(compose.onNodeWithText(fullDay(day), useUnmergedTree = true))
            compose.onNodeWithContentDescription("Aggiungi a Colazione").assertHeightIsAtLeast(48.dp)
            compose.onNodeWithText("Altre azioni").performScrollTo().performClick()
            compose.onNodeWithText("Copia da ieri").assertIsDisplayed()
            compose.onNodeWithText("Copia su domani").assertIsDisplayed()
            shot("diary-menu")
            compose.onNodeWithText("Copia da ieri").performClick()
            ui { Nav.clear(); Nav.tab = 2; Nav.kitchenTab = 2 }
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Ricetta del giorno").fetchSemanticsNodes().isNotEmpty() }
            shot("kitchen-overview")
            compose.onNodeWithText("Ricetta del giorno").performScrollTo()
            shot("kitchen-daily")
            compose.onNodeWithText("Mostra tutte").performScrollTo().performClick()
            compose.onNodeWithText("Vegana", useUnmergedTree = true).performScrollTo()
            readable(compose.onNodeWithText("Vegana", useUnmergedTree = true))
            shot("kitchen-categories")
        }
    }
}
