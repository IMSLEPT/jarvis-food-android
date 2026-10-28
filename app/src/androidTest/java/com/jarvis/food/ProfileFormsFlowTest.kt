package com.jarvis.food

import android.graphics.Bitmap
import android.os.SystemClock
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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ProfileFormsFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ins = InstrumentationRegistry.getInstrumentation()
    private val today = LocalDate.now()
    private fun ui(block: () -> Unit) = ins.runOnMainSync(block)
    private fun prepare(s: AppState) {
        Store.init(ins.targetContext)
        ui { Store.update { s }; Nav.clear(); Nav.tab = 0; Nav.diaryDate = today; Nav.planDate = today }
    }
    private fun shot(name: String) {
        compose.waitForIdle(); ins.waitForIdleSync(); SystemClock.sleep(450)
        val capture = InstrumentationRegistry.getArguments().getString("captureName") ?: "standard"
        val dir = File(ins.targetContext.filesDir, "shots/profile-$capture").apply { mkdirs() }
        val bitmap = ins.uiAutomation.takeScreenshot()
        try { File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
    private fun hideIme(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { a ->
            val input = a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            input.hideSoftInputFromWindow(a.window.decorView.windowToken, 0); a.currentFocus?.clearFocus()
        }
        compose.waitUntil(5000) {
            var visible = false
            scenario.onActivity { a -> visible = androidx.core.view.ViewCompat.getRootWindowInsets(a.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true }
            !visible
        }
        compose.waitForIdle()
    }
    @Test fun bodyDiagramRendersSelectsZonesAndPreservesMeasurementHistory() {
        val old = Measurement(today.minusDays(7).toString(), "vita", 84.0)
        val arm = Measurement(today.toString(), "braccio", 32.0)
        prepare(AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true), measurements = listOf(old, arm)))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ui { Nav.go(Screen.Measurements) }
            compose.onNodeWithTag("body-model-2d").assertIsDisplayed()
            // Verify the actual native artwork, including the active orange measuring line.
            compose.waitUntil(10000) {
                val rect = compose.onNodeWithTag("body-model-2d").fetchSemanticsNode().boundsInRoot
                val bitmap = ins.uiAutomation.takeScreenshot()
                val left = rect.left.toInt().coerceAtLeast(0); val top = rect.top.toInt().coerceAtLeast(0)
                val width = (rect.right.toInt().coerceAtMost(bitmap.width) - left).coerceAtLeast(0)
                val height = (rect.bottom.toInt().coerceAtMost(bitmap.height) - top).coerceAtLeast(0)
                var orange = 0; var body = 0
                if (width > 0 && height > 0) {
                    val pixels = IntArray(width * height); bitmap.getPixels(pixels, 0, width, left, top, width, height)
                    for (y in 0 until height step 4) for (x in 0 until width step 4) {
                        val color = pixels[y * width + x]; val r = android.graphics.Color.red(color); val g = android.graphics.Color.green(color); val b = android.graphics.Color.blue(color)
                        if (r in 220..250 && g in 95..135 && b in 40..80) orange++
                        if (r in 180..225 && g in 195..233 && b in 215..250 && b - r > 15) body++
                    }
                }
                bitmap.recycle()
                orange >= 12 && body >= 500
            }
            var safeBottom = 0
            scenario.onActivity { a -> safeBottom = a.window.decorView.height - (androidx.core.view.ViewCompat.getRootWindowInsets(a.window.decorView)?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0) }
            assertTrue("Measurement save overlaps system navigation", compose.onNode(hasText("Salva misura") and hasClickAction()).fetchSemanticsNode().boundsInRoot.bottom <= safeBottom + 1f)
            shot("body-2d-waist")
            // Every labelled point is accessible and updates the same native selection.
            for (key in listOf("petto", "braccio", "fianchi", "coscia", "collo", "vita")) {
                compose.onNodeWithTag("body-point-$key").performScrollTo().performClick().assertIsSelected()
            }
            compose.onNodeWithTag("body-model-2d").performScrollTo()
            // A direct tap on the silhouette also selects the anatomical site, not just its label.
            compose.onNodeWithTag("body-model-2d").performTouchInput {
                val dpScale = minOf(width / 360f, height / 520f)
                val startX = (width - 360f * dpScale) / 2f
                val startY = (height - 520f * dpScale) / 2f
                click(Offset(startX + 180f * dpScale, startY + 89f * dpScale))
            }
            compose.onNodeWithTag("body-point-collo").assertIsSelected()
            compose.onNodeWithText("Dove misurare · Collo").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("body-point-collo").performScrollTo()
            shot("body-2d-neck")
            compose.onNodeWithTag("body-point-vita").performScrollTo().performClick()
            compose.onNodeWithTag("measurement-value").performScrollTo().performTextReplacement("82,5")
            hideIme(scenario)
            shot("measurements-before-save")
            compose.onNodeWithText("Salva misura").assertIsEnabled().assertIsDisplayed().performClick()
            compose.waitUntil(5000) { Store.current.measurements.size == 3 }
            assertTrue(Store.current.measurements.contains(old)); assertTrue(Store.current.measurements.contains(arm))
            compose.onNodeWithTag("measurement-history-$today").performScrollTo().assertIsDisplayed()
            shot("measurements-history")
            compose.onNodeWithContentDescription("Modifica misura del $today").performClick()
            compose.onNodeWithTag("measurement-value").performScrollTo().performTextReplacement("81,5")
            hideIme(scenario)
            shot("measurements-before-update")
            compose.onNodeWithText("Aggiorna misura").assertIsEnabled().assertIsDisplayed().performClick()
            compose.waitUntil(5000) { Store.current.measurements.any { it.type == "vita" && it.date == today.toString() && it.value == 81.5 } }
            assertEquals(3, Store.current.measurements.size)
            val backup = Store.exportJson(); assertTrue(Store.importJson(backup).isSuccess)
            assertEquals(3, Store.current.measurements.size)
            compose.onNodeWithText("Massa grassa", substring = false).performScrollTo().performClick()
            compose.onNodeWithTag("measurement-value").performScrollTo().assertTextContains("Massa grassa (%)")
            compose.onNodeWithTag("body-model-2d").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sagoma 2D · Massa grassa"))
        }
    }
    @Test fun organizedGoalsKeepEditingAndSaveEveryValue() {
        prepare(AppState(onboarded = true, profile = Profile(name = "Alessandro", detailed = true), goals = Goals(kcal = 2131, auto = false)))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ui { Nav.go(Screen.Goals) }
            compose.onNodeWithTag("goals-summary").assertIsDisplayed()
            shot("goals-overview")
            compose.onNodeWithTag("goals-calories").performScrollTo().performTextReplacement("2300")
            hideIme(scenario)
            compose.onNodeWithTag("goal-pace-0").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithText("Quota di grassi").performScrollTo()
            shot("goals-macros")
            compose.onNodeWithContentDescription("Aumenta Obiettivo acqua").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Aumenta Passi al giorno").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Aumenta Bicchiere").performScrollTo().performClick()
            shot("goals-daily-habits")
            val before = Store.current
            compose.onNodeWithText("Salva obiettivi").performClick()
            compose.waitForIdle()
            assertEquals(2300, Store.current.goals.kcal)
            assertEquals(1, Store.current.profile.goal)
            assertEquals(before.goals.waterMl + 250, Store.current.goals.waterMl)
            assertEquals(before.goals.steps + 500, Store.current.goals.steps)
            assertEquals(before.glassMl + 50, Store.current.glassMl)
            assertEquals(100, Store.current.goals.carbsPct + Store.current.goals.proteinPct + Store.current.goals.fatPct)
            ui { Nav.go(Screen.ProfileEdit) }; shot("profile-details")
            ui { Nav.back(); Nav.go(Screen.Questionnaire(QStep.About)) }; shot("profile-questionnaire")
            ui { Nav.back(); Nav.go(Screen.Weight) }; shot("profile-weight")
            ui { Nav.back(); Nav.go(Screen.RemindersSet) }; shot("profile-reminders")
            ui { Nav.back(); Nav.go(Screen.Backup) }; shot("profile-backup")
        }
    }
}
