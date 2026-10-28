package com.jarvis.food

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
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
        // Geometry fixtures use ready catalogue data; the separate regression suite
        // covers asynchronous cold loading and the real category destinations.
        check(com.jarvis.food.engine.RecipeCats.get(Db.db).isNotEmpty())
        ui { Store.update { AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true)) }; Nav.clear(); Nav.tab = 0; Nav.diaryDate = day; Nav.planDate = day }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); android.os.SystemClock.sleep(400)
        val profile = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/consistency-$profile").apply { mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
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

    private fun singleLine(node: SemanticsNodeInteraction) {
        readable(node)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        layouts.forEach { assertEquals("Label split inside a word: ${it.layoutInput.text}", 1, it.lineCount) }
    }

    @Test fun emptyAndSingleWeightStatesDoNotInventHistory() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("nav-3").performClick()
            compose.onNodeWithTag("weight-empty", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            readable(compose.onNodeWithTag("weight-empty", useUnmergedTree = true))
            shot("progress-no-weight")
            compose.onNodeWithTag("calorie-sample-size", useUnmergedTree = true).performScrollTo().assertTextContains("0 giorni registrati", substring = true)
            compose.onNodeWithTag("calorie-day-detail", useUnmergedTree = true).performScrollTo().assertTextContains("Nessun pasto registrato", substring = true)
            shot("progress-no-calories")
            ui { Store.addWeight(day, 71.0) }
            compose.onNodeWithTag("weight-chart", useUnmergedTree = true).performScrollTo().assertContentDescriptionEquals("Una misurazione del peso")
            compose.onNodeWithTag("weight-history-caption", useUnmergedTree = true).performScrollTo().assertTextContains("Una misurazione", substring = true)
            readable(compose.onNodeWithTag("weight-goal-title", useUnmergedTree = true).performScrollTo())
            val remaining = compose.onNodeWithTag("weight-goal-remaining", useUnmergedTree = true).performScrollTo()
            readable(remaining)
            val goalLayouts = mutableListOf<TextLayoutResult>()
            remaining.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(goalLayouts) }
            assertTrue("Weight goal constrained to a narrow column", goalLayouts.single().lineCount <= 2)
            compose.onNodeWithTag("weight-goal-progress", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0f, 0f..1f)))
            shot("progress-one-weight")
        }
    }

    @Test fun planActionsRemainReadableAndKeepTheirBehaviour() {
        prepare()
        ui { Store.generatePlan(day.with(DayOfWeek.MONDAY), 7) }
        val meal = Store.plannedMeals(Store.current, day).first()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("nav-1").performClick()
            compose.onNodeWithTag("plan-completed-count", useUnmergedTree = true).assertTextEquals("0 di ${Store.plannedMeals(Store.current, day).size} pasti")
            compose.onNodeWithTag("plan-calendar-toggle").performClick()
            for (i in 0..6) singleLine(compose.onNodeWithTag("week-label-$i", useUnmergedTree = true))
            compose.onNodeWithTag("plan-calendar-toggle").performClick()
            shot("plan-overview")
            val recipe = compose.onNodeWithTag("plan-recipe-${meal.diaryMeal}").performScrollTo()
            recipe.assertHasClickAction()
            readable(compose.onNodeWithText(meal.solved.template.name, useUnmergedTree = true))
            if (ins.targetContext.resources.configuration.fontScale > 1.5f) {
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("plan-recipe-title-${meal.diaryMeal}", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue("Recipe title constrained to a narrow column", layouts.single().lineCount <= 6)
            }
            shot("plan-recipe")
            val eaten = compose.onNodeWithTag("plan-eaten-${meal.diaryMeal}").performScrollTo()
            eaten.assertHeightIsAtLeast(48.dp).performClick()
            compose.waitForIdle()
            assertTrue(Store.dayPlan(Store.current, day)!!.slots.first { it.slot == meal.slotIndex }.eatenEntryIds.isNotEmpty())
            shot("plan-eaten")
            compose.onNodeWithTag("plan-completed-count", useUnmergedTree = true).performScrollTo().assertTextEquals("1 di ${Store.plannedMeals(Store.current, day).size} pasti")
            compose.onNodeWithTag("plan-eaten-${meal.diaryMeal}").performScrollTo().performClick()
            compose.waitForIdle()
            assertTrue(Store.dayPlan(Store.current, day)!!.slots.first { it.slot == meal.slotIndex }.eatenEntryIds.isEmpty())
            compose.onNodeWithTag("plan-alternatives-${meal.diaryMeal}").performScrollTo().performClick()
            assertTrue(Nav.stack.last() is Screen.Alternatives)
            if (ins.targetContext.resources.configuration.fontScale > 1.5f) {
                val alternative = com.jarvis.food.engine.Planner.alternatives(Db.db, meal.solved.template.meal, meal.solved.target, Store.current.dietPrefs).first()
                readable(compose.onNodeWithTag("alternative-title-${alternative.template.id}", useUnmergedTree = true).performScrollTo())
            }
            shot("plan-alternatives")
        }
    }

    @Test fun diaryMenuAndKitchenKeepSecondaryActionsReachable() {
        prepare()
        ActivityScenario.launch(MainActivity::class.java).use {
            for (tab in listOf(0, 1, 2, 3, 5)) singleLine(compose.onNodeWithTag("nav-label-$tab", useUnmergedTree = true))
            for (i in 0..6) singleLine(compose.onNodeWithTag("week-label-$i", useUnmergedTree = true))
            if (ins.targetContext.resources.configuration.fontScale > 1.5f) singleLine(compose.onNodeWithTag("home-name", useUnmergedTree = true))
            shot("home")
            // Past-day rings must retain actual intake after changing the visual theme.
            val past = day.minusDays(1)
            val anchor = if (past.dayOfWeek == DayOfWeek.MONDAY) past.plusDays(1) else past.with(DayOfWeek.MONDAY)
            ui {
                Store.addEntry(past, 0, Food("history-fixture", "Pasto registrato", kcal = 200.0, protein = 0.0, carbs = 50.0, fat = 0.0), 500.0)
                Nav.diaryDate = anchor
            }
            compose.waitForIdle()
            val index = past.dayOfWeek.value - 1
            val fraction = (1000.0 / Store.energyFor(Store.current, past).targetKcal).toFloat().coerceIn(0f, 1f)
            compose.onNodeWithTag("week-progress-$index", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(fraction, 0f..1f)))
            compose.onNodeWithTag("week-day-$index").assertContentDescriptionEquals("${fullDay(past)}, 1.000 kcal registrate")
            shot("home-history-rings")
            compose.onNodeWithTag("week-day-$index").performClick()
            compose.waitForIdle()
            assertEquals(past, Nav.diaryDate)
            ui { Store.update { it.copy(diary = emptyList()) }; Nav.diaryDate = day }
            compose.waitForIdle()
            singleLine(compose.onNodeWithTag("diary-title-0", useUnmergedTree = true).performScrollTo())
            compose.onNodeWithText("Colazione").performScrollTo().performClick()
            readable(compose.onNodeWithText("Nessun alimento registrato", useUnmergedTree = true).performScrollTo())
            val dateLabel = compose.onNodeWithText(fullDay(day), useUnmergedTree = true)
            if (ins.targetContext.resources.configuration.fontScale > 1.5f) dateLabel.performScrollTo()
            readable(dateLabel)
            compose.onNodeWithContentDescription("Aggiungi a Colazione").assertHeightIsAtLeast(48.dp)
            compose.onNodeWithText("Altre azioni").performScrollTo().performClick()
            compose.onNodeWithText("Copia da ieri").assertIsDisplayed()
            compose.onNodeWithText("Copia su domani").assertIsDisplayed()
            shot("diary-menu")
            compose.onNodeWithText("Copia da ieri").performClick()
            compose.onNodeWithTag("water-minus").performScrollTo().assertHeightIsAtLeast(48.dp).assertIsNotEnabled()
            compose.onNodeWithTag("water-plus").performScrollTo().assertHeightIsAtLeast(52.dp).performClick()
            compose.waitForIdle()
            assertEquals(Store.current.glassMl, Store.current.water[day.toString()])
            shot("water-secondary-minus")
            compose.onNodeWithTag("water-minus").performScrollTo().assertIsEnabled().performClick()
            compose.waitForIdle()
            assertEquals(0, Store.current.water[day.toString()])
            compose.onNodeWithTag("water-minus").assertIsNotEnabled()
            ui { Nav.clear(); Nav.tab = 2; Nav.kitchenTab = 2 }
            compose.waitUntil(30_000) { compose.onAllNodesWithText("Ricetta del giorno").fetchSemanticsNodes().isNotEmpty() }
            for (i in 0..2) singleLine(compose.onNodeWithTag("kitchen-tab-label-$i", useUnmergedTree = true))
            shot("kitchen-overview")
            compose.onNodeWithText("Ricetta del giorno").performScrollTo()
            shot("kitchen-daily")
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Mostra tutte").fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithText("Vista compatta").fetchSemanticsNodes().isNotEmpty()
            }
            if (compose.onAllNodesWithText("Mostra tutte").fetchSemanticsNodes().isNotEmpty())
                compose.onNodeWithText("Mostra tutte").performScrollTo().performClick()
            compose.onNodeWithText("Vegana", useUnmergedTree = true).performScrollTo()
            readable(compose.onNodeWithText("Vegana", useUnmergedTree = true))
            singleLine(compose.onNodeWithText("Vegetariana", useUnmergedTree = true).performScrollTo())
            readable(compose.onNodeWithTag("category-count-Vegetariana", useUnmergedTree = true))
            singleLine(compose.onNodeWithText("Dimagrimento", useUnmergedTree = true).performScrollTo())
            compose.onNodeWithText("Vegana", useUnmergedTree = true).performScrollTo()
            shot("kitchen-categories")
        }
    }
}
