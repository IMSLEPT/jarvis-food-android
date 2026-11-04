package com.jarvis.food

import android.graphics.Bitmap
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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TrainingDiaryFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val today = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); android.os.SystemClock.sleep(400)
        val profile = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/features-$profile").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { ins.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun prepare(state: AppState) {
        Store.init(ins.targetContext)
        ui { Store.update { state }; Nav.clear(); Nav.tab = 0; Nav.diaryDate = today; Nav.planDate = today }
    }

    private fun hideKeyboard(scenario: ActivityScenario<MainActivity>) {
        // Finish the native IME resize before scrolling/tapping a lazy-list result.
        scenario.onActivity { activity ->
            androidx.core.view.ViewCompat.getWindowInsetsController(activity.window.decorView)
                ?.hide(androidx.core.view.WindowInsetsCompat.Type.ime())
            val input = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            input.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
            activity.currentFocus?.clearFocus()
        }
        compose.waitUntil(timeoutMillis = 5000) {
            var visible = false
            scenario.onActivity { activity ->
                visible = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
            }
            !visible
        }
        compose.waitForIdle()
    }

    @Test fun replacementPreviewCanBeCancelledThenSavedWithoutDuplicates() {
        val pasta = Food("test-pasta", "Pasta di semola", kcal = 371.0, protein = 13.0, carbs = 71.0, fat = 1.5)
        val fish = Food("test-fish", "Merluzzo test", kcal = 100.0, protein = 20.0, carbs = 0.0, fat = 2.0)
        val first = DiaryEntry("replace-original", today.toString(), 1, pasta, 200.0, 100L)
        val other = first.copy(id = "untouched", food = pasta.copy(id = "test-other", name = "Altro alimento"), grams = 30.0)
        prepare(AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true), customFoods = listOf(fish), diary = listOf(first, other)))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("Pranzo").performScrollTo().performClick()
            compose.onNodeWithTag("diary-entry-${first.id}").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("P", substring = false).assertCountEquals(0)
            shot("diary-food-separation")
            compose.onNodeWithTag("diary-entry-${first.id}").performClick()
            compose.onNodeWithText("Sostituisci alimento").performScrollTo().performClick()
            compose.onNodeWithTag("replace-food-search").performTextInput("Merluzzo test")
            hideKeyboard(scenario)
            compose.onNodeWithTag("replacement-results").performScrollToNode(hasTestTag("replacement-${fish.id}"))
            compose.onNodeWithTag("replacement-${fish.id}").performClick()
            compose.waitForIdle()
            assertEquals(listOf(first, other), Store.current.diary)
            compose.onNodeWithText("Annulla").performClick()
            // Wait for the destination, not just a momentary idle frame during navigation.
            try {
                compose.waitUntil(timeoutMillis = 10000) {
                    compose.onAllNodesWithTag("replacement-results").fetchSemanticsNodes().isNotEmpty()
                }
            } catch (failure: AssertionError) {
                shot("replacement-cancel-destination-missing")
                throw failure
            }
            assertTrue(Nav.stack.lastOrNull() is Screen.ReplaceFood)
            assertEquals(listOf(first, other), Store.current.diary)
            compose.onNodeWithTag("replacement-results").performScrollToNode(hasTestTag("replacement-${fish.id}"))
            compose.onNodeWithTag("replacement-${fish.id}").performClick()
            compose.onNodeWithTag("food-quantity").performScrollTo().performTextReplacement("150")
            hideKeyboard(scenario)
            val save = compose.onNode(hasText("Sostituisci") and hasClickAction()).assertIsDisplayed().assertIsEnabled()
            // Insets can report IME hidden before its resize animation has finished.
            // Keep the real pointer click, but wait for a stable target instead of
            // tapping coordinates from a transient frame. Data assertions stay strict.
            var previous = save.fetchSemanticsNode().boundsInRoot
            var stableSince = android.os.SystemClock.uptimeMillis()
            compose.waitUntil(timeoutMillis = 5000) {
                val current = save.fetchSemanticsNode().boundsInRoot
                if (current != previous) { previous = current; stableSince = android.os.SystemClock.uptimeMillis() }
                android.os.SystemClock.uptimeMillis() - stableSince >= 150
            }
            save.performClick()
            try {
                compose.waitUntil(timeoutMillis = 5000) { Store.current.diary.firstOrNull { item -> item.id == first.id }?.food?.id == fish.id }
            } catch (failure: Throwable) {
                shot("replacement-save-failure")
                throw failure
            }
            compose.waitForIdle()
            val result = Store.current.diary
            assertEquals(2, result.size)
            assertEquals(first.copy(food = fish, grams = 150.0), result.first())
            assertEquals(other, result.last())
            assertEquals(150.0, Nutrition.of(result.first().food, result.first().grams).kcal, 0.01)
            assertTrue(Nav.stack.isEmpty())
            if (compose.onAllNodesWithTag("diary-entry-${first.id}").fetchSemanticsNodes().isEmpty()) compose.onNodeWithText("Pranzo").performScrollTo().performClick()
            compose.onNodeWithTag("diary-entry-${first.id}").performScrollTo().assertTextContains(fish.name)
            shot("diary-after-replacement")
        }
    }

    @Test fun targetsCanBeConfiguredOverriddenAndDisabledWithoutLosingLegacyGoals() {
        prepare(AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true), goals = Goals(kcal = 2000, auto = false),
            trainingNutrition = TrainingNutrition(trainingDays = listOf(today.dayOfWeek.value))))
        ActivityScenario.launch(MainActivity::class.java).use {
            ui { Nav.go(Screen.Profile) }
            compose.onNodeWithTag("profile-item-training-goals").performScrollTo().performClick()
            compose.onNodeWithTag("training-enabled").performClick()
            val tomorrow = today.plusDays(1)
            compose.onNodeWithTag("training-weekday-${tomorrow.dayOfWeek.value}").performScrollTo().assertIsNotSelected().performClick().assertIsSelected()
            val values = listOf("2600", "160", "330", "70", "2100", "150", "210", "60")
            for ((i, value) in values.withIndex()) {
                compose.onNodeWithTag("target-${if (i < 4) "on" else "off"}-${i % 4}").performScrollTo().performTextReplacement(value)
            }
            shot("training-targets-settings")
            compose.onNodeWithText("Salva giorni ON/OFF").performClick()
            compose.waitForIdle()
            assertTrue(Store.current.trainingNutrition.active)
            assertTrue(Store.current.trainingNutrition.isTraining(tomorrow))
            assertEquals(2600.0, Store.energyFor(Store.current, today).targetKcal, 0.0)
            val backup = Store.exportJson()
            assertTrue(Store.importJson(backup).isSuccess)
            assertEquals(160.0, Store.energyFor(Store.current, today).protein, 0.0)
            ui { Nav.clear() }
            compose.onNodeWithTag("day-type-label").assert(hasAnyAncestor(hasTestTag("today-day-type-bar")))
            // Redesign 4 novembre: "Configura giorni ON/OFF" è nella finestra del tipo di giornata.
            compose.onNodeWithTag("today-day-type-bar").performClick()
            compose.onNodeWithTag("day-type-configure").assert(hasAnyAncestor(hasTestTag("day-type-dialog"))).performClick()
            assertEquals(Screen.TrainingGoals, Nav.stack.last())
            ui { Nav.back() }
            shot("today-restored-day-bar")
            compose.onNodeWithTag("day-type-label").performScrollTo().performClick()
            for (choice in listOf("true", "false", "calendar")) compose.onNodeWithTag("day-type-choice-$choice").performScrollTo().assertHasClickAction().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
            shot("day-type-outlined-actions")
            compose.onNodeWithTag("day-type-choice-false").performScrollTo().performClick()
            compose.waitForIdle()
            assertEquals(2100.0, Store.energyFor(Store.current, today).targetKcal, 0.0)
            assertEquals(150.0, Store.energyFor(Store.current, today).protein, 0.0)
            assertTrue(Store.current.trainingNutrition.isTraining(today.plusWeeks(1)))
            shot("day-off-override")
            ui { Nav.tab = 1; Store.generatePlan(today.with(java.time.DayOfWeek.MONDAY), 7) }
            compose.onNodeWithTag("plan-summary-toggle").performScrollTo().performClick()
            compose.onNodeWithTag("day-type-label").assert(hasAnyAncestor(hasTestTag("plan-summary")))
            compose.onAllNodesWithTag("day-type-configure").assertCountEquals(0)
            compose.onAllNodesWithText("Opzioni del piano").assertCountEquals(0)
            compose.onNodeWithTag("plan-completed-count", useUnmergedTree = true).assertTextEquals("0 di ${Store.plannedMeals(Store.current, today).size} pasti")
            shot("plan-restored-summary")
            compose.onNodeWithTag("day-type-label").performScrollTo().performClick()
            compose.onNodeWithTag("day-type-choice-false").performScrollTo().assertIsSelected()
            shot("plan-day-type-outlined-actions")
            compose.onNodeWithTag("day-type-close").performClick()
            compose.onNodeWithTag("day-type-label").performScrollTo().performClick()
            compose.onNodeWithTag("day-type-choice-true").performScrollTo().performClick()
            assertEquals(2600.0, Store.energyFor(Store.current, today).targetKcal, 0.0)
            compose.onNodeWithTag("day-type-label").performScrollTo().performClick()
            compose.onNodeWithTag("day-type-choice-false").performScrollTo().performClick()
            assertEquals(2100.0, Store.energyFor(Store.current, today).targetKcal, 0.0)
            compose.onNodeWithTag("plan-options").performScrollTo().performClick()
            compose.onNodeWithText("Gestisci pasti e preferenze").assertIsDisplayed()
            compose.onNodeWithText("Configura giorni ON/OFF").performClick()
            assertEquals(Screen.TrainingGoals, Nav.stack.last())
            ui { Nav.back(); Nav.tab = 0 }
            ui { Nav.go(Screen.DayNutrients(today)) }
            compose.onNodeWithText("su 2100 kcal").assertIsDisplayed()
            ui { Nav.back() }
            compose.onNodeWithTag("day-type-label").performScrollTo().performClick()
            compose.onNodeWithText("Segui calendario settimanale").performClick()
            compose.waitForIdle()
            assertEquals(2600.0, Store.energyFor(Store.current, today).targetKcal, 0.0)
            ui { Nav.go(Screen.TrainingGoals) }
            compose.onNodeWithTag("training-enabled").performClick()
            compose.onNodeWithText("Salva giorni ON/OFF").performClick()
            compose.waitForIdle()
            assertFalse(Store.current.trainingNutrition.active)
            assertEquals(2000.0, Store.energyFor(Store.current, today).targetKcal, 0.0)
            assertEquals(2000, Store.current.goals.kcal)
        }
    }
}
